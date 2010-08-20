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

# Portions of the following are derived from the compat.py file in
# Twisted, under the following copyright:
#
# Copyright (c) 2001-2004 Twisted Matrix Laboratories

__doc__ = """
Compatibility idioms for builtins names

This module adds names to the builtins module for things that we want
to use in SCons but which don't show up until later Python versions than
the earliest ones we support.

This module checks for the following builtins names:

        all()
        any()
        sorted()
        memoryview()

Implementations of functions are *NOT* guaranteed to be fully compliant
with these functions in later versions of Python.  We are only concerned
with adding functionality that we actually use in SCons, so be wary
if you lift this code for other uses.  (That said, making these more
nearly the same as later, official versions is still a desirable goal,
we just don't need to be obsessive about it.)

If you're looking at this with pydoc and various names don't show up in
the FUNCTIONS or DATA output, that means those names are already built in
to this version of Python and we don't need to add them from this module.
"""

__revision__ = "src/engine/SCons/compat/_scons_builtins.py 5023 2010/06/14 22:05:46 scons"

import builtins

try:
    all
except NameError:
    # Pre-2.5 Python has no all() function.
    def all(iterable):
        """
        Returns True if all elements of the iterable are true.
        """
        for element in iterable:
            if not element:
                return False
        return True
    builtins.all = all
    all = all

try:
    any
except NameError:
    # Pre-2.5 Python has no any() function.
    def any(iterable):
        """
        Returns True if any element of the iterable is true.
        """
        for element in iterable:
            if element:
                return True
        return False
    builtins.any = any
    any = any

try:
    memoryview
except NameError:
    # Pre-2.7 doesn't have the memoryview() built-in.
    class memoryview(object):
        def __init__(self, obj):
            # wrapping buffer in () keeps the fixer from changing it
            self.obj = (buffer)(obj)
        def __getitem__(self, indx):
            if isinstance(indx, slice):
                return self.obj[indx.start:indx.stop]
            else:
                return self.obj[indx]
    builtins.memoryview = memoryview

try:
    sorted
except NameError:
    # Pre-2.4 Python has no sorted() function.
    #
    # The pre-2.4 Python list.sort() method does not support
    # list.sort(key=) nor list.sort(reverse=) keyword arguments, so
    # we must implement the functionality of those keyword arguments
    # by hand instead of passing them to list.sort().
    def sorted(iterable, cmp=None, key=None, reverse=False):
        if key is not None:
            result = [(key(x), x) for x in iterable]
        else:
            result = iterable[:]
        if cmp is None:
            # Pre-2.3 Python does not support list.sort(None).
            result.sort()
        else:
            result.sort(cmp)
        if key is not None:
            result = [t1 for t0,t1 in result]
        if reverse:
            result.reverse()
        return result
    builtins.sorted = sorted

#if sys.version_info[:3] in ((2, 2, 0), (2, 2, 1)):
#    def lstrip(s, c=string.whitespace):
#        while s and s[0] in c:
#            s = s[1:]
#        return s
#    def rstrip(s, c=string.whitespace):
#        while s and s[-1] in c:
#            s = s[:-1]
#        return s
#    def strip(s, c=string.whitespace, l=lstrip, r=rstrip):
#        return l(r(s, c), c)
#
#    object.__setattr__(str, 'lstrip', lstrip)
#    object.__setattr__(str, 'rstrip', rstrip)
#    object.__setattr__(str, 'strip', strip)

# Local Variables:
# tab-width:4
# indent-tabs-mode:nil
# End:
# vim: set expandtab tabstop=4 shiftwidth=4:
