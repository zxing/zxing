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

__doc__ = """
SCons compatibility package for old Python versions

This subpackage holds modules that provide backwards-compatible
implementations of various things that we'd like to use in SCons but which
only show up in later versions of Python than the early, old version(s)
we still support.

Other code will not generally reference things in this package through
the SCons.compat namespace.  The modules included here add things to
the builtins namespace or the global module list so that the rest
of our code can use the objects and names imported here regardless of
Python version.

Simply enough, things that go in the builtins name space come from
our _scons_builtins module.

The rest of the things here will be in individual compatibility modules
that are either: 1) suitably modified copies of the future modules that
we want to use; or 2) backwards compatible re-implementations of the
specific portions of a future module's API that we want to use.

GENERAL WARNINGS:  Implementations of functions in the SCons.compat
modules are *NOT* guaranteed to be fully compliant with these functions in
later versions of Python.  We are only concerned with adding functionality
that we actually use in SCons, so be wary if you lift this code for
other uses.  (That said, making these more nearly the same as later,
official versions is still a desirable goal, we just don't need to be
obsessive about it.)

We name the compatibility modules with an initial '_scons_' (for example,
_scons_subprocess.py is our compatibility module for subprocess) so
that we can still try to import the real module name and fall back to
our compatibility module if we get an ImportError.  The import_as()
function defined below loads the module as the "real" name (without the
'_scons'), after which all of the "import {module}" statements in the
rest of our code will find our pre-loaded compatibility module.
"""

__revision__ = "src/engine/SCons/compat/__init__.py 5023 2010/06/14 22:05:46 scons"

import os
import sys
import imp   # Use the "imp" module to protect imports from fixers.

def import_as(module, name):
    """
    Imports the specified module (from our local directory) as the
    specified name, returning the loaded module object.
    """
    dir = os.path.split(__file__)[0]
    return imp.load_module(name, *imp.find_module(module, [dir]))

def rename_module(new, old):
    """
    Attempts to import the old module and load it under the new name.
    Used for purely cosmetic name changes in Python 3.x.
    """
    try:
        sys.modules[new] = imp.load_module(old, *imp.find_module(old))
        return True
    except ImportError:
        return False


rename_module('builtins', '__builtin__')
import _scons_builtins


try:
    import hashlib
except ImportError:
    # Pre-2.5 Python has no hashlib module.
    try:
        import_as('_scons_hashlib', 'hashlib')
    except ImportError:
        # If we failed importing our compatibility module, it probably
        # means this version of Python has no md5 module.  Don't do
        # anything and let the higher layer discover this fact, so it
        # can fall back to using timestamp.
        pass

try:
    set
except NameError:
    # Pre-2.4 Python has no native set type
    import_as('_scons_sets', 'sets')
    import builtins, sets
    builtins.set = sets.Set


try:
    import collections
except ImportError:
    # Pre-2.4 Python has no collections module.
    import_as('_scons_collections', 'collections')
else:
    try:
        collections.UserDict
    except AttributeError:
        exec('from UserDict import UserDict as _UserDict')
        collections.UserDict = _UserDict
        del _UserDict
    try:
        collections.UserList
    except AttributeError:
        exec('from UserList import UserList as _UserList')
        collections.UserList = _UserList
        del _UserList
    try:
        collections.UserString
    except AttributeError:
        exec('from UserString import UserString as _UserString')
        collections.UserString = _UserString
        del _UserString


try:
    import io
except ImportError:
    # Pre-2.6 Python has no io module.
    import_as('_scons_io', 'io')


try:
    os.devnull
except AttributeError:
    # Pre-2.4 Python has no os.devnull attribute
    _names = sys.builtin_module_names
    if 'posix' in _names:
        os.devnull = '/dev/null'
    elif 'nt' in _names:
        os.devnull = 'nul'
    os.path.devnull = os.devnull
try:
    os.path.lexists
except AttributeError:
    # Pre-2.4 Python has no os.path.lexists function
    def lexists(path):
        return os.path.exists(path) or os.path.islink(path)
    os.path.lexists = lexists


# When we're using the '-3' option during regression tests, importing
# cPickle gives a warning no matter how it's done, so always use the
# real profile module, whether it's fast or not.
if os.environ.get('SCONS_HORRIBLE_REGRESSION_TEST_HACK') is None:
    # Not a regression test with '-3', so try to use faster version.
    # In 3.x, 'pickle' automatically loads the fast version if available.
    rename_module('pickle', 'cPickle')


# In 3.x, 'profile' automatically loads the fast version if available.
rename_module('profile', 'cProfile')


# Before Python 3.0, the 'queue' module was named 'Queue'.
rename_module('queue', 'Queue')


# Before Python 3.0, the 'winreg' module was named '_winreg'
rename_module('winreg', '_winreg')


try:
    import subprocess
except ImportError:
    # Pre-2.4 Python has no subprocess module.
    import_as('_scons_subprocess', 'subprocess')

try:
    sys.intern
except AttributeError:
    # Pre-2.6 Python has no sys.intern() function.
    import builtins
    try:
        sys.intern = builtins.intern
    except AttributeError:
        # Pre-2.x Python has no builtin intern() function.
        def intern(x):
           return x
        sys.intern = intern
        del intern
try:
    sys.maxsize
except AttributeError:
    # Pre-2.6 Python has no sys.maxsize attribute
    # Wrapping sys in () is silly, but protects it from 2to3 renames fixer
    sys.maxsize = (sys).maxint


if os.environ.get('SCONS_HORRIBLE_REGRESSION_TEST_HACK') is not None:
    # We can't apply the 'callable' fixer until the floor is 2.6, but the
    # '-3' option to Python 2.6 and 2.7 generates almost ten thousand
    # warnings.  This hack allows us to run regression tests with the '-3'
    # option by replacing the callable() built-in function with a hack
    # that performs the same function but doesn't generate the warning.
    # Note that this hack is ONLY intended to be used for regression
    # testing, and should NEVER be used for real runs.
    from types import ClassType
    def callable(obj):
        if hasattr(obj, '__call__'): return True
        if isinstance(obj, (ClassType, type)): return True
        return False
    import builtins
    builtins.callable = callable
    del callable


# Local Variables:
# tab-width:4
# indent-tabs-mode:nil
# End:
# vim: set expandtab tabstop=4 shiftwidth=4:
