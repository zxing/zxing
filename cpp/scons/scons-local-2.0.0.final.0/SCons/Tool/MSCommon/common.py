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

__revision__ = "src/engine/SCons/Tool/MSCommon/common.py 5023 2010/06/14 22:05:46 scons"

__doc__ = """
Common helper functions for working with the Microsoft tool chain.
"""

import copy
import os
import subprocess
import re

import SCons.Util


logfile = os.environ.get('SCONS_MSCOMMON_DEBUG')
if logfile == '-':
    def debug(x):
        print x
elif logfile:
    try:
        import logging
    except ImportError:
        debug = lambda x: open(logfile, 'a').write(x + '\n')
    else:
        logging.basicConfig(filename=logfile, level=logging.DEBUG)
        debug = logging.debug
else:
    debug = lambda x: None


_is_win64 = None

def is_win64():
    """Return true if running on windows 64 bits.
    
    Works whether python itself runs in 64 bits or 32 bits."""
    # Unfortunately, python does not provide a useful way to determine
    # if the underlying Windows OS is 32-bit or 64-bit.  Worse, whether
    # the Python itself is 32-bit or 64-bit affects what it returns,
    # so nothing in sys.* or os.* help.  

    # Apparently the best solution is to use env vars that Windows
    # sets.  If PROCESSOR_ARCHITECTURE is not x86, then the python
    # process is running in 64 bit mode (on a 64-bit OS, 64-bit
    # hardware, obviously).
    # If this python is 32-bit but the OS is 64, Windows will set
    # ProgramW6432 and PROCESSOR_ARCHITEW6432 to non-null.
    # (Checking for HKLM\Software\Wow6432Node in the registry doesn't
    # work, because some 32-bit installers create it.)
    global _is_win64
    if _is_win64 is None:
        # I structured these tests to make it easy to add new ones or
        # add exceptions in the future, because this is a bit fragile.
        _is_win64 = False
        if os.environ.get('PROCESSOR_ARCHITECTURE','x86') != 'x86':
            _is_win64 = True
        if os.environ.get('PROCESSOR_ARCHITEW6432'):
            _is_win64 = True
        if os.environ.get('ProgramW6432'):
            _is_win64 = True
    return _is_win64


def read_reg(value):
    return SCons.Util.RegGetValue(SCons.Util.HKEY_LOCAL_MACHINE, value)[0]

def has_reg(value):
    """Return True if the given key exists in HKEY_LOCAL_MACHINE, False
    otherwise."""
    try:
        SCons.Util.RegOpenKeyEx(SCons.Util.HKEY_LOCAL_MACHINE, value)
        ret = True
    except WindowsError:
        ret = False
    return ret

# Functions for fetching environment variable settings from batch files.

def normalize_env(env, keys, force=False):
    """Given a dictionary representing a shell environment, add the variables
    from os.environ needed for the processing of .bat files; the keys are
    controlled by the keys argument.

    It also makes sure the environment values are correctly encoded.

    If force=True, then all of the key values that exist are copied
    into the returned dictionary.  If force=false, values are only
    copied if the key does not already exist in the copied dictionary.

    Note: the environment is copied."""
    normenv = {}
    if env:
        for k in env.keys():
            normenv[k] = copy.deepcopy(env[k]).encode('mbcs')

        for k in keys:
            if k in os.environ and (force or not k in normenv):
                normenv[k] = os.environ[k].encode('mbcs')

    return normenv

def get_output(vcbat, args = None, env = None):
    """Parse the output of given bat file, with given args."""
    
    if env is None:
        # Create a blank environment, for use in launching the tools
        env = SCons.Environment.Environment(tools=[])

    # TODO:  This is a hard-coded list of the variables that (may) need
    # to be imported from os.environ[] for v[sc]*vars*.bat file
    # execution to work.  This list should really be either directly
    # controlled by vc.py, or else derived from the common_tools_var
    # settings in vs.py.
    vars = [
        'COMSPEC',
        'VS90COMNTOOLS',
        'VS80COMNTOOLS',
        'VS71COMNTOOLS',
        'VS70COMNTOOLS',
        'VS60COMNTOOLS',
    ]
    env['ENV'] = normalize_env(env['ENV'], vars, force=False)

    if args:
        debug("Calling '%s %s'" % (vcbat, args))
        popen = SCons.Action._subproc(env,
                                     '"%s" %s & set' % (vcbat, args),
                                     stdin = 'devnull',
                                     stdout=subprocess.PIPE,
                                     stderr=subprocess.PIPE)
    else:
        debug("Calling '%s'" % vcbat)
        popen = SCons.Action._subproc(env,
                                     '"%s" & set' % vcbat,
                                     stdin = 'devnull',
                                     stdout=subprocess.PIPE,
                                     stderr=subprocess.PIPE)

    # Use the .stdout and .stderr attributes directly because the
    # .communicate() method uses the threading module on Windows
    # and won't work under Pythons not built with threading.
    stdout = popen.stdout.read()
    stderr = popen.stderr.read()
    if stderr:
        # TODO: find something better to do with stderr;
        # this at least prevents errors from getting swallowed.
        import sys
        sys.stderr.write(stderr)
    if popen.wait() != 0:
        raise IOError(stderr.decode("mbcs"))

    output = stdout.decode("mbcs")
    return output

def parse_output(output, keep = ("INCLUDE", "LIB", "LIBPATH", "PATH")):
    # dkeep is a dict associating key: path_list, where key is one item from
    # keep, and pat_list the associated list of paths

    dkeep = dict([(i, []) for i in keep])

    # rdk will  keep the regex to match the .bat file output line starts
    rdk = {}
    for i in keep:
        rdk[i] = re.compile('%s=(.*)' % i, re.I)

    def add_env(rmatch, key, dkeep=dkeep):
        plist = rmatch.group(1).split(os.pathsep)
        for p in plist:
            # Do not add empty paths (when a var ends with ;)
            if p:
                p = p.encode('mbcs')
                # XXX: For some reason, VC98 .bat file adds "" around the PATH
                # values, and it screws up the environment later, so we strip
                # it. 
                p = p.strip('"')
                dkeep[key].append(p)

    for line in output.splitlines():
        for k,v in rdk.items():
            m = v.match(line)
            if m:
                add_env(m, k)

    return dkeep

# TODO(sgk): unused
def output_to_dict(output):
    """Given an output string, parse it to find env variables.

    Return a dict where keys are variables names, and values their content"""
    envlinem = re.compile(r'^([a-zA-z0-9]+)=([\S\s]*)$')
    parsedenv = {}
    for line in output.splitlines():
        m = envlinem.match(line)
        if m:
            parsedenv[m.group(1)] = m.group(2)
    return parsedenv

# TODO(sgk): unused
def get_new(l1, l2):
    """Given two list l1 and l2, return the items in l2 which are not in l1.
    Order is maintained."""

    # We don't try to be smart: lists are small, and this is not the bottleneck
    # is any case
    new = []
    for i in l2:
        if i not in l1:
            new.append(i)

    return new

# Local Variables:
# tab-width:4
# indent-tabs-mode:nil
# End:
# vim: set expandtab tabstop=4 shiftwidth=4:
