"""SCons.Tool.tex

Tool-specific initialization for TeX.
Generates .dvi files from .tex files

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

__revision__ = "src/engine/SCons/Tool/tex.py 5023 2010/06/14 22:05:46 scons"

import os.path
import re
import shutil
import sys

import SCons.Action
import SCons.Node
import SCons.Node.FS
import SCons.Util
import SCons.Scanner.LaTeX

Verbose = False

must_rerun_latex = True

# these are files that just need to be checked for changes and then rerun latex
check_suffixes = ['.toc', '.lof', '.lot', '.out', '.nav', '.snm']

# these are files that require bibtex or makeindex to be run when they change
all_suffixes = check_suffixes + ['.bbl', '.idx', '.nlo', '.glo', '.acn']

#
# regular expressions used to search for Latex features
# or outputs that require rerunning latex
#
# search for all .aux files opened by latex (recorded in the .fls file)
openout_aux_re = re.compile(r"INPUT *(.*\.aux)")

#printindex_re = re.compile(r"^[^%]*\\printindex", re.MULTILINE)
#printnomenclature_re = re.compile(r"^[^%]*\\printnomenclature", re.MULTILINE)
#printglossary_re = re.compile(r"^[^%]*\\printglossary", re.MULTILINE)

# search to find rerun warnings
warning_rerun_str = '(^LaTeX Warning:.*Rerun)|(^Package \w+ Warning:.*Rerun)'
warning_rerun_re = re.compile(warning_rerun_str, re.MULTILINE)

# search to find citation rerun warnings
rerun_citations_str = "^LaTeX Warning:.*\n.*Rerun to get citations correct"
rerun_citations_re = re.compile(rerun_citations_str, re.MULTILINE)

# search to find undefined references or citations warnings
undefined_references_str = '(^LaTeX Warning:.*undefined references)|(^Package \w+ Warning:.*undefined citations)'
undefined_references_re = re.compile(undefined_references_str, re.MULTILINE)

# used by the emitter
auxfile_re = re.compile(r".", re.MULTILINE)
tableofcontents_re = re.compile(r"^[^%\n]*\\tableofcontents", re.MULTILINE)
makeindex_re = re.compile(r"^[^%\n]*\\makeindex", re.MULTILINE)
bibliography_re = re.compile(r"^[^%\n]*\\bibliography", re.MULTILINE)
listoffigures_re = re.compile(r"^[^%\n]*\\listoffigures", re.MULTILINE)
listoftables_re = re.compile(r"^[^%\n]*\\listoftables", re.MULTILINE)
hyperref_re = re.compile(r"^[^%\n]*\\usepackage.*\{hyperref\}", re.MULTILINE)
makenomenclature_re = re.compile(r"^[^%\n]*\\makenomenclature", re.MULTILINE)
makeglossary_re = re.compile(r"^[^%\n]*\\makeglossary", re.MULTILINE)
makeglossaries_re = re.compile(r"^[^%\n]*\\makeglossaries", re.MULTILINE)
makeacronyms_re = re.compile(r"^[^%\n]*\\makeglossaries", re.MULTILINE)
beamer_re = re.compile(r"^[^%\n]*\\documentclass\{beamer\}", re.MULTILINE)

# search to find all files included by Latex
include_re = re.compile(r'^[^%\n]*\\(?:include|input){([^}]*)}', re.MULTILINE)
includeOnly_re = re.compile(r'^[^%\n]*\\(?:include){([^}]*)}', re.MULTILINE)

# search to find all graphics files included by Latex
includegraphics_re = re.compile(r'^[^%\n]*\\(?:includegraphics(?:\[[^\]]+\])?){([^}]*)}', re.MULTILINE)

# search to find all files opened by Latex (recorded in .log file)
openout_re = re.compile(r"OUTPUT *(.*)")

# list of graphics file extensions for TeX and LaTeX
TexGraphics   = SCons.Scanner.LaTeX.TexGraphics
LatexGraphics = SCons.Scanner.LaTeX.LatexGraphics

# An Action sufficient to build any generic tex file.
TeXAction = None

# An action to build a latex file.  This action might be needed more
# than once if we are dealing with labels and bibtex.
LaTeXAction = None

# An action to run BibTeX on a file.
BibTeXAction = None

# An action to run MakeIndex on a file.
MakeIndexAction = None

# An action to run MakeIndex (for nomencl) on a file.
MakeNclAction = None

# An action to run MakeIndex (for glossary) on a file.
MakeGlossaryAction = None

# An action to run MakeIndex (for acronyms) on a file.
MakeAcronymsAction = None

# Used as a return value of modify_env_var if the variable is not set.
_null = SCons.Scanner.LaTeX._null

modify_env_var = SCons.Scanner.LaTeX.modify_env_var

def check_file_error_message(utility, filename='log'):
    msg = '%s returned an error, check the %s file\n' % (utility, filename)
    sys.stdout.write(msg)

def FindFile(name,suffixes,paths,env,requireExt=False):
    if requireExt:
        name,ext = SCons.Util.splitext(name)
        # if the user gave an extension use it.
        if ext:
            name = name + ext
    if Verbose:
        print " searching for '%s' with extensions: " % name,suffixes

    for path in paths:
        testName = os.path.join(path,name)
        if Verbose:
            print " look for '%s'" % testName
        if os.path.exists(testName):
            if Verbose:
                print " found '%s'" % testName
            return env.fs.File(testName)
        else:
            name_ext = SCons.Util.splitext(testName)[1]
            if name_ext:
                continue

            # if no suffix try adding those passed in
            for suffix in suffixes:
                testNameExt = testName + suffix
                if Verbose:
                    print " look for '%s'" % testNameExt

                if os.path.exists(testNameExt):
                    if Verbose:
                        print " found '%s'" % testNameExt
                    return env.fs.File(testNameExt)
    if Verbose:
        print " did not find '%s'" % name
    return None

def InternalLaTeXAuxAction(XXXLaTeXAction, target = None, source= None, env=None):
    """A builder for LaTeX files that checks the output in the aux file
    and decides how many times to use LaTeXAction, and BibTeXAction."""

    global must_rerun_latex

    # This routine is called with two actions. In this file for DVI builds
    # with LaTeXAction and from the pdflatex.py with PDFLaTeXAction
    # set this up now for the case where the user requests a different extension
    # for the target filename
    if (XXXLaTeXAction == LaTeXAction):
       callerSuffix = ".dvi"
    else:
       callerSuffix = env['PDFSUFFIX']

    basename = SCons.Util.splitext(str(source[0]))[0]
    basedir = os.path.split(str(source[0]))[0]
    basefile = os.path.split(str(basename))[1]
    abspath = os.path.abspath(basedir)

    targetext = os.path.splitext(str(target[0]))[1]
    targetdir = os.path.split(str(target[0]))[0]

    saved_env = {}
    for var in SCons.Scanner.LaTeX.LaTeX.env_variables:
        saved_env[var] = modify_env_var(env, var, abspath)

    # Create base file names with the target directory since the auxiliary files
    # will be made there.   That's because the *COM variables have the cd
    # command in the prolog. We check
    # for the existence of files before opening them--even ones like the
    # aux file that TeX always creates--to make it possible to write tests
    # with stubs that don't necessarily generate all of the same files.

    targetbase = os.path.join(targetdir, basefile)

    # if there is a \makeindex there will be a .idx and thus
    # we have to run makeindex at least once to keep the build
    # happy even if there is no index.
    # Same for glossaries and nomenclature
    src_content = source[0].get_text_contents()
    run_makeindex = makeindex_re.search(src_content) and not os.path.exists(targetbase + '.idx')
    run_nomenclature = makenomenclature_re.search(src_content) and not os.path.exists(targetbase + '.nlo')
    run_glossary = makeglossary_re.search(src_content) and not os.path.exists(targetbase + '.glo')
    run_glossaries = makeglossaries_re.search(src_content) and not os.path.exists(targetbase + '.glo')
    run_acronyms = makeacronyms_re.search(src_content) and not os.path.exists(targetbase + '.acn')

    saved_hashes = {}
    suffix_nodes = {}

    for suffix in all_suffixes:
        theNode = env.fs.File(targetbase + suffix)
        suffix_nodes[suffix] = theNode
        saved_hashes[suffix] = theNode.get_csig()

    if Verbose:
        print "hashes: ",saved_hashes

    must_rerun_latex = True

    #
    # routine to update MD5 hash and compare
    #
    def check_MD5(filenode, suffix):
        global must_rerun_latex
        # two calls to clear old csig
        filenode.clear_memoized_values()
        filenode.ninfo = filenode.new_ninfo()
        new_md5 = filenode.get_csig()

        if saved_hashes[suffix] == new_md5:
            if Verbose:
                print "file %s not changed" % (targetbase+suffix)
            return False        # unchanged
        saved_hashes[suffix] = new_md5
        must_rerun_latex = True
        if Verbose:
            print "file %s changed, rerunning Latex, new hash = " % (targetbase+suffix), new_md5
        return True     # changed

    # generate the file name that latex will generate
    resultfilename = targetbase + callerSuffix

    count = 0

    while (must_rerun_latex and count < int(env.subst('$LATEXRETRIES'))) :
        result = XXXLaTeXAction(target, source, env)
        if result != 0:
            return result

        count = count + 1

        must_rerun_latex = False
        # Decide if various things need to be run, or run again.

        # Read the log file to find warnings/errors
        logfilename = targetbase + '.log'
        logContent = ''
        if os.path.exists(logfilename):
            logContent = open(logfilename, "rb").read()


        # Read the fls file to find all .aux files
        flsfilename = targetbase + '.fls'
        flsContent = ''
        auxfiles = []
        if os.path.exists(flsfilename):
            flsContent = open(flsfilename, "rb").read()
            auxfiles = openout_aux_re.findall(flsContent)
        if Verbose:
            print "auxfiles ",auxfiles

        # Now decide if bibtex will need to be run.
        # The information that bibtex reads from the .aux file is
        # pass-independent. If we find (below) that the .bbl file is unchanged,
        # then the last latex saw a correct bibliography.
        # Therefore only do this on the first pass
        if count == 1:
            for auxfilename in auxfiles:
                target_aux = os.path.join(targetdir, auxfilename)
                if os.path.exists(target_aux):
                    content = open(target_aux, "rb").read()
                    if content.find("bibdata") != -1:
                        if Verbose:
                            print "Need to run bibtex"
                        bibfile = env.fs.File(targetbase)
                        result = BibTeXAction(bibfile, bibfile, env)
                        if result != 0:
                            check_file_error_message(env['BIBTEX'], 'blg')
                            return result
                        must_rerun_latex = check_MD5(suffix_nodes['.bbl'],'.bbl')
                        break

        # Now decide if latex will need to be run again due to index.
        if check_MD5(suffix_nodes['.idx'],'.idx') or (count == 1 and run_makeindex):
            # We must run makeindex
            if Verbose:
                print "Need to run makeindex"
            idxfile = suffix_nodes['.idx']
            result = MakeIndexAction(idxfile, idxfile, env)
            if result != 0:
                check_file_error_message(env['MAKEINDEX'], 'ilg')
                return result

        # TO-DO: need to add a way for the user to extend this list for whatever
        # auxiliary files they create in other (or their own) packages
        # Harder is case is where an action needs to be called -- that should be rare (I hope?)

        for index in check_suffixes:
            check_MD5(suffix_nodes[index],index)

        # Now decide if latex will need to be run again due to nomenclature.
        if check_MD5(suffix_nodes['.nlo'],'.nlo') or (count == 1 and run_nomenclature):
            # We must run makeindex
            if Verbose:
                print "Need to run makeindex for nomenclature"
            nclfile = suffix_nodes['.nlo']
            result = MakeNclAction(nclfile, nclfile, env)
            if result != 0:
                check_file_error_message('%s (nomenclature)' % env['MAKENCL'],
                                         'nlg')
                #return result

        # Now decide if latex will need to be run again due to glossary.
        if check_MD5(suffix_nodes['.glo'],'.glo') or (count == 1 and run_glossaries) or (count == 1 and run_glossary):
            # We must run makeindex
            if Verbose:
                print "Need to run makeindex for glossary"
            glofile = suffix_nodes['.glo']
            result = MakeGlossaryAction(glofile, glofile, env)
            if result != 0:
                check_file_error_message('%s (glossary)' % env['MAKEGLOSSARY'],
                                         'glg')
                #return result

        # Now decide if latex will need to be run again due to acronyms.
        if check_MD5(suffix_nodes['.acn'],'.acn') or (count == 1 and run_acronyms):
            # We must run makeindex
            if Verbose:
                print "Need to run makeindex for acronyms"
            acrfile = suffix_nodes['.acn']
            result = MakeAcronymsAction(acrfile, acrfile, env)
            if result != 0:
                check_file_error_message('%s (acronyms)' % env['MAKEACRONYMS'],
                                         'alg')
                return result

        # Now decide if latex needs to be run yet again to resolve warnings.
        if warning_rerun_re.search(logContent):
            must_rerun_latex = True
            if Verbose:
                print "rerun Latex due to latex or package rerun warning"

        if rerun_citations_re.search(logContent):
            must_rerun_latex = True
            if Verbose:
                print "rerun Latex due to 'Rerun to get citations correct' warning"

        if undefined_references_re.search(logContent):
            must_rerun_latex = True
            if Verbose:
                print "rerun Latex due to undefined references or citations"

        if (count >= int(env.subst('$LATEXRETRIES')) and must_rerun_latex):
            print "reached max number of retries on Latex ,",int(env.subst('$LATEXRETRIES'))
# end of while loop

    # rename Latex's output to what the target name is
    if not (str(target[0]) == resultfilename  and  os.path.exists(resultfilename)):
        if os.path.exists(resultfilename):
            print "move %s to %s" % (resultfilename, str(target[0]), )
            shutil.move(resultfilename,str(target[0]))

    # Original comment (when TEXPICTS was not restored):
    # The TEXPICTS enviroment variable is needed by a dvi -> pdf step
    # later on Mac OSX so leave it
    #
    # It is also used when searching for pictures (implicit dependencies).
    # Why not set the variable again in the respective builder instead
    # of leaving local modifications in the environment? What if multiple
    # latex builds in different directories need different TEXPICTS?
    for var in SCons.Scanner.LaTeX.LaTeX.env_variables:
        if var == 'TEXPICTS':
            continue
        if saved_env[var] is _null:
            try:
                del env['ENV'][var]
            except KeyError:
                pass # was never set
        else:
            env['ENV'][var] = saved_env[var]

    return result

def LaTeXAuxAction(target = None, source= None, env=None):
    result = InternalLaTeXAuxAction( LaTeXAction, target, source, env )
    return result

LaTeX_re = re.compile("\\\\document(style|class)")

def is_LaTeX(flist,env,abspath):
    """Scan a file list to decide if it's TeX- or LaTeX-flavored."""

    # We need to scan files that are included in case the
    # \documentclass command is in them.

    # get path list from both env['TEXINPUTS'] and env['ENV']['TEXINPUTS']
    savedpath = modify_env_var(env, 'TEXINPUTS', abspath)
    paths = env['ENV']['TEXINPUTS']
    if SCons.Util.is_List(paths):
        pass
    else:
        # Split at os.pathsep to convert into absolute path
        paths = paths.split(os.pathsep)

    # now that we have the path list restore the env
    if savedpath is _null:
        try:
            del env['ENV']['TEXINPUTS']
        except KeyError:
            pass # was never set
    else:
        env['ENV']['TEXINPUTS'] = savedpath
    if Verbose:
        print "is_LaTeX search path ",paths
        print "files to search :",flist

    # Now that we have the search path and file list, check each one
    for f in flist:
        if Verbose:
            print " checking for Latex source ",str(f)

        content = f.get_text_contents()
        if LaTeX_re.search(content):
            if Verbose:
                print "file %s is a LaTeX file" % str(f)
            return 1
        if Verbose:
            print "file %s is not a LaTeX file" % str(f)

        # now find included files
        inc_files = [ ]
        inc_files.extend( include_re.findall(content) )
        if Verbose:
            print "files included by '%s': "%str(f),inc_files
        # inc_files is list of file names as given. need to find them
        # using TEXINPUTS paths.

        # search the included files
        for src in inc_files:
            srcNode = FindFile(src,['.tex','.ltx','.latex'],paths,env,requireExt=False)
            # make this a list since is_LaTeX takes a list.
            fileList = [srcNode,]
            if Verbose:
                print "FindFile found ",srcNode
            if srcNode is not None:
                file_test = is_LaTeX(fileList, env, abspath)

            # return on first file that finds latex is needed.
            if file_test:
                return file_test

        if Verbose:
            print " done scanning ",str(f)

    return 0

def TeXLaTeXFunction(target = None, source= None, env=None):
    """A builder for TeX and LaTeX that scans the source file to
    decide the "flavor" of the source and then executes the appropriate
    program."""

    # find these paths for use in is_LaTeX to search for included files
    basedir = os.path.split(str(source[0]))[0]
    abspath = os.path.abspath(basedir)

    if is_LaTeX(source,env,abspath):
        result = LaTeXAuxAction(target,source,env)
        if result != 0:
            check_file_error_message(env['LATEX'])
    else:
        result = TeXAction(target,source,env)
        if result != 0:
            check_file_error_message(env['TEX'])
    return result

def TeXLaTeXStrFunction(target = None, source= None, env=None):
    """A strfunction for TeX and LaTeX that scans the source file to
    decide the "flavor" of the source and then returns the appropriate
    command string."""
    if env.GetOption("no_exec"):

        # find these paths for use in is_LaTeX to search for included files
        basedir = os.path.split(str(source[0]))[0]
        abspath = os.path.abspath(basedir)

        if is_LaTeX(source,env,abspath):
            result = env.subst('$LATEXCOM',0,target,source)+" ..."
        else:
            result = env.subst("$TEXCOM",0,target,source)+" ..."
    else:
        result = ''
    return result

def tex_eps_emitter(target, source, env):
    """An emitter for TeX and LaTeX sources when
    executing tex or latex. It will accept .ps and .eps
    graphics files
    """
    (target, source) = tex_emitter_core(target, source, env, TexGraphics)

    return (target, source)

def tex_pdf_emitter(target, source, env):
    """An emitter for TeX and LaTeX sources when
    executing pdftex or pdflatex. It will accept graphics
    files of types .pdf, .jpg, .png, .gif, and .tif
    """
    (target, source) = tex_emitter_core(target, source, env, LatexGraphics)

    return (target, source)

def ScanFiles(theFile, target, paths, file_tests, file_tests_search, env, graphics_extensions, targetdir, aux_files):
    """ For theFile (a Node) update any file_tests and search for graphics files
    then find all included files and call ScanFiles recursively for each of them"""

    content = theFile.get_text_contents()
    if Verbose:
        print " scanning ",str(theFile)

    for i in range(len(file_tests_search)):
        if file_tests[i][0] is None:
            file_tests[i][0] = file_tests_search[i].search(content)

    incResult = includeOnly_re.search(content)
    if incResult:
        aux_files.append(os.path.join(targetdir, incResult.group(1)))
    if Verbose:
        print "\include file names : ", aux_files
    # recursively call this on each of the included files
    inc_files = [ ]
    inc_files.extend( include_re.findall(content) )
    if Verbose:
        print "files included by '%s': "%str(theFile),inc_files
    # inc_files is list of file names as given. need to find them
    # using TEXINPUTS paths.

    for src in inc_files:
        srcNode = FindFile(src,['.tex','.ltx','.latex'],paths,env,requireExt=False)
        if srcNode is not None:
            file_tests = ScanFiles(srcNode, target, paths, file_tests, file_tests_search, env, graphics_extensions, targetdir, aux_files)
    if Verbose:
        print " done scanning ",str(theFile)
    return file_tests

def tex_emitter_core(target, source, env, graphics_extensions):
    """An emitter for TeX and LaTeX sources.
    For LaTeX sources we try and find the common created files that
    are needed on subsequent runs of latex to finish tables of contents,
    bibliographies, indices, lists of figures, and hyperlink references.
    """
    basename = SCons.Util.splitext(str(source[0]))[0]
    basefile = os.path.split(str(basename))[1]
    targetdir = os.path.split(str(target[0]))[0]
    targetbase = os.path.join(targetdir, basefile)

    basedir = os.path.split(str(source[0]))[0]
    abspath = os.path.abspath(basedir)
    target[0].attributes.path = abspath
    
    #
    # file names we will make use of in searching the sources and log file
    #
    emit_suffixes = ['.aux', '.log', '.ilg', '.blg', '.nls', '.nlg', '.gls', '.glg', '.alg'] + all_suffixes
    auxfilename = targetbase + '.aux'
    logfilename = targetbase + '.log'
    flsfilename = targetbase + '.fls'

    env.SideEffect(auxfilename,target[0])
    env.SideEffect(logfilename,target[0])
    env.SideEffect(flsfilename,target[0])
    if Verbose:
        print "side effect :",auxfilename,logfilename,flsfilename
    env.Clean(target[0],auxfilename)
    env.Clean(target[0],logfilename)
    env.Clean(target[0],flsfilename)

    content = source[0].get_text_contents()

    idx_exists = os.path.exists(targetbase + '.idx')
    nlo_exists = os.path.exists(targetbase + '.nlo')
    glo_exists = os.path.exists(targetbase + '.glo')
    acr_exists = os.path.exists(targetbase + '.acn')

    # set up list with the regular expressions
    # we use to find features used
    file_tests_search = [auxfile_re,
                         makeindex_re,
                         bibliography_re,
                         tableofcontents_re,
                         listoffigures_re,
                         listoftables_re,
                         hyperref_re,
                         makenomenclature_re,
                         makeglossary_re,
                         makeglossaries_re,
                         makeacronyms_re,
                         beamer_re ]
    # set up list with the file suffixes that need emitting
    # when a feature is found
    file_tests_suff = [['.aux'],
                  ['.idx', '.ind', '.ilg'],
                  ['.bbl', '.blg'],
                  ['.toc'],
                  ['.lof'],
                  ['.lot'],
                  ['.out'],
                  ['.nlo', '.nls', '.nlg'],
                  ['.glo', '.gls', '.glg'],
                  ['.glo', '.gls', '.glg'],
                  ['.acn', '.acr', '.alg'],
                  ['.nav', '.snm', '.out', '.toc'] ]
    # build the list of lists
    file_tests = []
    for i in range(len(file_tests_search)):
        file_tests.append( [None, file_tests_suff[i]] )

    # TO-DO: need to add a way for the user to extend this list for whatever
    # auxiliary files they create in other (or their own) packages

    # get path list from both env['TEXINPUTS'] and env['ENV']['TEXINPUTS']
    savedpath = modify_env_var(env, 'TEXINPUTS', abspath)
    paths = env['ENV']['TEXINPUTS']
    if SCons.Util.is_List(paths):
        pass
    else:
        # Split at os.pathsep to convert into absolute path
        paths = paths.split(os.pathsep)

    # now that we have the path list restore the env
    if savedpath is _null:
        try:
            del env['ENV']['TEXINPUTS']
        except KeyError:
            pass # was never set
    else:
        env['ENV']['TEXINPUTS'] = savedpath
    if Verbose:
        print "search path ",paths

    aux_files = []
    file_tests = ScanFiles(source[0], target, paths, file_tests, file_tests_search, env, graphics_extensions, targetdir, aux_files)

    for (theSearch,suffix_list) in file_tests:
        if theSearch:
            for suffix in suffix_list:
                env.SideEffect(targetbase + suffix,target[0])
                if Verbose:
                    print "side effect :",targetbase + suffix
                env.Clean(target[0],targetbase + suffix)

    for aFile in aux_files:
        aFile_base = SCons.Util.splitext(aFile)[0]
        env.SideEffect(aFile_base + '.aux',target[0])
        if Verbose:
            print "side effect :",aFile_base + '.aux'
        env.Clean(target[0],aFile_base + '.aux')
    # read fls file to get all other files that latex creates and will read on the next pass
    # remove files from list that we explicitly dealt with above
    if os.path.exists(flsfilename):
        content = open(flsfilename, "rb").read()
        out_files = openout_re.findall(content)
        myfiles = [auxfilename, logfilename, flsfilename, targetbase+'.dvi',targetbase+'.pdf']
        for filename in out_files[:]:
            if filename in myfiles:
                out_files.remove(filename)
        env.SideEffect(out_files,target[0])
        if Verbose:
            print "side effect :",out_files
        env.Clean(target[0],out_files)

    return (target, source)


TeXLaTeXAction = None

def generate(env):
    """Add Builders and construction variables for TeX to an Environment."""

    global TeXLaTeXAction
    if TeXLaTeXAction is None:
        TeXLaTeXAction = SCons.Action.Action(TeXLaTeXFunction,
                              strfunction=TeXLaTeXStrFunction)

    env.AppendUnique(LATEXSUFFIXES=SCons.Tool.LaTeXSuffixes)

    generate_common(env)

    import dvi
    dvi.generate(env)

    bld = env['BUILDERS']['DVI']
    bld.add_action('.tex', TeXLaTeXAction)
    bld.add_emitter('.tex', tex_eps_emitter)

def generate_common(env):
    """Add internal Builders and construction variables for LaTeX to an Environment."""

    # A generic tex file Action, sufficient for all tex files.
    global TeXAction
    if TeXAction is None:
        TeXAction = SCons.Action.Action("$TEXCOM", "$TEXCOMSTR")

    # An Action to build a latex file.  This might be needed more
    # than once if we are dealing with labels and bibtex.
    global LaTeXAction
    if LaTeXAction is None:
        LaTeXAction = SCons.Action.Action("$LATEXCOM", "$LATEXCOMSTR")

    # Define an action to run BibTeX on a file.
    global BibTeXAction
    if BibTeXAction is None:
        BibTeXAction = SCons.Action.Action("$BIBTEXCOM", "$BIBTEXCOMSTR")

    # Define an action to run MakeIndex on a file.
    global MakeIndexAction
    if MakeIndexAction is None:
        MakeIndexAction = SCons.Action.Action("$MAKEINDEXCOM", "$MAKEINDEXCOMSTR")

    # Define an action to run MakeIndex on a file for nomenclatures.
    global MakeNclAction
    if MakeNclAction is None:
        MakeNclAction = SCons.Action.Action("$MAKENCLCOM", "$MAKENCLCOMSTR")

    # Define an action to run MakeIndex on a file for glossaries.
    global MakeGlossaryAction
    if MakeGlossaryAction is None:
        MakeGlossaryAction = SCons.Action.Action("$MAKEGLOSSARYCOM", "$MAKEGLOSSARYCOMSTR")

    # Define an action to run MakeIndex on a file for acronyms.
    global MakeAcronymsAction
    if MakeAcronymsAction is None:
        MakeAcronymsAction = SCons.Action.Action("$MAKEACRONYMSCOM", "$MAKEACRONYMSCOMSTR")

    env['TEX']      = 'tex'
    env['TEXFLAGS'] = SCons.Util.CLVar('-interaction=nonstopmode -recorder')
    env['TEXCOM']   = 'cd ${TARGET.dir} && $TEX $TEXFLAGS ${SOURCE.file}'

    env['PDFTEX']      = 'pdftex'
    env['PDFTEXFLAGS'] = SCons.Util.CLVar('-interaction=nonstopmode -recorder')
    env['PDFTEXCOM']   = 'cd ${TARGET.dir} && $PDFTEX $PDFTEXFLAGS ${SOURCE.file}'

    env['LATEX']        = 'latex'
    env['LATEXFLAGS']   = SCons.Util.CLVar('-interaction=nonstopmode -recorder')
    env['LATEXCOM']     = 'cd ${TARGET.dir} && $LATEX $LATEXFLAGS ${SOURCE.file}'
    env['LATEXRETRIES'] = 3

    env['PDFLATEX']      = 'pdflatex'
    env['PDFLATEXFLAGS'] = SCons.Util.CLVar('-interaction=nonstopmode -recorder')
    env['PDFLATEXCOM']   = 'cd ${TARGET.dir} && $PDFLATEX $PDFLATEXFLAGS ${SOURCE.file}'

    env['BIBTEX']      = 'bibtex'
    env['BIBTEXFLAGS'] = SCons.Util.CLVar('')
    env['BIBTEXCOM']   = 'cd ${TARGET.dir} && $BIBTEX $BIBTEXFLAGS ${SOURCE.filebase}'

    env['MAKEINDEX']      = 'makeindex'
    env['MAKEINDEXFLAGS'] = SCons.Util.CLVar('')
    env['MAKEINDEXCOM']   = 'cd ${TARGET.dir} && $MAKEINDEX $MAKEINDEXFLAGS ${SOURCE.file}'

    env['MAKEGLOSSARY']      = 'makeindex'
    env['MAKEGLOSSARYSTYLE'] = '${SOURCE.filebase}.ist'
    env['MAKEGLOSSARYFLAGS'] = SCons.Util.CLVar('-s ${MAKEGLOSSARYSTYLE} -t ${SOURCE.filebase}.glg')
    env['MAKEGLOSSARYCOM']   = 'cd ${TARGET.dir} && $MAKEGLOSSARY ${SOURCE.filebase}.glo $MAKEGLOSSARYFLAGS -o ${SOURCE.filebase}.gls'

    env['MAKEACRONYMS']      = 'makeindex'
    env['MAKEACRONYMSSTYLE'] = '${SOURCE.filebase}.ist'
    env['MAKEACRONYMSFLAGS'] = SCons.Util.CLVar('-s ${MAKEACRONYMSSTYLE} -t ${SOURCE.filebase}.alg')
    env['MAKEACRONYMSCOM']   = 'cd ${TARGET.dir} && $MAKEACRONYMS ${SOURCE.filebase}.acn $MAKEACRONYMSFLAGS -o ${SOURCE.filebase}.acr'

    env['MAKENCL']      = 'makeindex'
    env['MAKENCLSTYLE'] = 'nomencl.ist'
    env['MAKENCLFLAGS'] = '-s ${MAKENCLSTYLE} -t ${SOURCE.filebase}.nlg'
    env['MAKENCLCOM']   = 'cd ${TARGET.dir} && $MAKENCL ${SOURCE.filebase}.nlo $MAKENCLFLAGS -o ${SOURCE.filebase}.nls'

def exists(env):
    return env.Detect('tex')

# Local Variables:
# tab-width:4
# indent-tabs-mode:nil
# End:
# vim: set expandtab tabstop=4 shiftwidth=4:
