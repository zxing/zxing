"""SCons.Tool.swig

Tool-specific initialization for swig.

There normally shouldn't be any need to import this module directly.
It will usually be imported through the generic SCons.Tool.Tool()
selection method.

"""

#
# Copyright (c) 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010 The SCons Foundation
#
# Permission is hereby granted, free of charge, to any person obtaining
# a copy of this software and associated documentation files (the
# "Software"), to deal in the Software without restriction, including
# without limitation the rights to use, copy, modify, merge, publish,
# distribute, sublicense, and/or sell copies of the Software, and to
# permit persons to whom the Software is furnished to do so, subject to
# the following conditions:
#
# The above copyright notice and this permission notice shall be included
# in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
# KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
# WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
# LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
# OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
# WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#

__revision__ = "src/engine/SCons/Tool/swig.py 5023 2010/06/14 22:05:46 scons"

import os.path
import re
import subprocess

import SCons.Action
import SCons.Defaults
import SCons.Scanner
import SCons.Tool
import SCons.Util

SwigAction = SCons.Action.Action('$SWIGCOM', '$SWIGCOMSTR')

def swigSuffixEmitter(env, source):
    if '-c++' in SCons.Util.CLVar(env.subst("$SWIGFLAGS", source=source)):
        return '$SWIGCXXFILESUFFIX'
    else:
        return '$SWIGCFILESUFFIX'

# Match '%module test', as well as '%module(directors="1") test'
# Also allow for test to be quoted (SWIG permits double quotes, but not single)
_reModule = re.compile(r'%module(\s*\(.*\))?\s+("?)(.+)\2')

def _find_modules(src):
    """Find all modules referenced by %module lines in `src`, a SWIG .i file.
       Returns a list of all modules, and a flag set if SWIG directors have
       been requested (SWIG will generate an additional header file in this
       case.)"""
    directors = 0
    mnames = []
    try:
        matches = _reModule.findall(open(src).read())
    except IOError:
        # If the file's not yet generated, guess the module name from the filename
        matches = []
        mnames.append(os.path.splitext(src)[0])

    for m in matches:
        mnames.append(m[2])
        directors = directors or m[0].find('directors') >= 0
    return mnames, directors

def _add_director_header_targets(target, env):
    # Directors only work with C++ code, not C
    suffix = env.subst(env['SWIGCXXFILESUFFIX'])
    # For each file ending in SWIGCXXFILESUFFIX, add a new target director
    # header by replacing the ending with SWIGDIRECTORSUFFIX.
    for x in target[:]:
        n = x.name
        d = x.dir
        if n[-len(suffix):] == suffix:
            target.append(d.File(n[:-len(suffix)] + env['SWIGDIRECTORSUFFIX']))

def _swigEmitter(target, source, env):
    swigflags = env.subst("$SWIGFLAGS", target=target, source=source)
    flags = SCons.Util.CLVar(swigflags)
    for src in source:
        src = str(src.rfile())
        mnames = None
        if "-python" in flags and "-noproxy" not in flags:
            if mnames is None:
                mnames, directors = _find_modules(src)
            if directors:
                _add_director_header_targets(target, env)
            python_files = [m + ".py" for m in mnames]
            outdir = env.subst('$SWIGOUTDIR', target=target, source=source)
            # .py files should be generated in SWIGOUTDIR if specified,
            # otherwise in the same directory as the target
            if outdir:
                python_files = [env.fs.File(os.path.join(outdir, j)) for j in python_files]
            else:
                python_files = [target[0].dir.File(m) for m in python_files]
            target.extend(python_files)
        if "-java" in flags:
            if mnames is None:
                mnames, directors = _find_modules(src)
            if directors:
                _add_director_header_targets(target, env)
            java_files = [[m + ".java", m + "JNI.java"] for m in mnames]
            java_files = SCons.Util.flatten(java_files)
            outdir = env.subst('$SWIGOUTDIR', target=target, source=source)
            if outdir:
                 java_files = [os.path.join(outdir, j) for j in java_files]
            java_files = list(map(env.fs.File, java_files))
            for jf in java_files:
                t_from_s = lambda t, p, s, x: t.dir
                SCons.Util.AddMethod(jf, t_from_s, 'target_from_source')
            target.extend(java_files)
    return (target, source)

def _get_swig_version(env):
    """Run the SWIG command line tool to get and return the version number"""
    pipe = SCons.Action._subproc(env, [env['SWIG'], '-version'],
                                 stdin = 'devnull',
                                 stderr = 'devnull',
                                 stdout = subprocess.PIPE)
    if pipe.wait() != 0: return

    out = pipe.stdout.read()
    match = re.search(r'SWIG Version\s+(\S+)$', out, re.MULTILINE)
    if match:
        return match.group(1)

def generate(env):
    """Add Builders and construction variables for swig to an Environment."""
    c_file, cxx_file = SCons.Tool.createCFileBuilders(env)

    c_file.suffix['.i'] = swigSuffixEmitter
    cxx_file.suffix['.i'] = swigSuffixEmitter

    c_file.add_action('.i', SwigAction)
    c_file.add_emitter('.i', _swigEmitter)
    cxx_file.add_action('.i', SwigAction)
    cxx_file.add_emitter('.i', _swigEmitter)

    java_file = SCons.Tool.CreateJavaFileBuilder(env)

    java_file.suffix['.i'] = swigSuffixEmitter

    java_file.add_action('.i', SwigAction)
    java_file.add_emitter('.i', _swigEmitter)

    env['SWIG']              = 'swig'
    env['SWIGVERSION']       = _get_swig_version(env)
    env['SWIGFLAGS']         = SCons.Util.CLVar('')
    env['SWIGDIRECTORSUFFIX'] = '_wrap.h'
    env['SWIGCFILESUFFIX']   = '_wrap$CFILESUFFIX'
    env['SWIGCXXFILESUFFIX'] = '_wrap$CXXFILESUFFIX'
    env['_SWIGOUTDIR']       = r'${"-outdir \"%s\"" % SWIGOUTDIR}'
    env['SWIGPATH']          = []
    env['SWIGINCPREFIX']     = '-I'
    env['SWIGINCSUFFIX']     = ''
    env['_SWIGINCFLAGS']     = '$( ${_concat(SWIGINCPREFIX, SWIGPATH, SWIGINCSUFFIX, __env__, RDirs, TARGET, SOURCE)} $)'
    env['SWIGCOM']           = '$SWIG -o $TARGET ${_SWIGOUTDIR} ${_SWIGINCFLAGS} $SWIGFLAGS $SOURCES'

    expr = '^[ \t]*%[ \t]*(?:include|import|extern)[ \t]*(<|"?)([^>\s"]+)(?:>|"?)'
    scanner = SCons.Scanner.ClassicCPP("SWIGScan", ".i", "SWIGPATH", expr)

    env.Append(SCANNERS = scanner)

def exists(env):
    return env.Detect(['swig'])

# Local Variables:
# tab-width:4
# indent-tabs-mode:nil
# End:
# vim: set expandtab tabstop=4 shiftwidth=4:
