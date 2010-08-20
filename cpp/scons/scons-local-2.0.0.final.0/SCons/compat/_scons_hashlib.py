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
hashlib backwards-compatibility module for older (pre-2.5) Python versions

This does not not NOT (repeat, *NOT*) provide complete hashlib
functionality.  It only wraps the portions of MD5 functionality used
by SCons, in an interface that looks like hashlib (or enough for our
purposes, anyway).  In fact, this module will raise an ImportError if
the underlying md5 module isn't available.
"""

__revision__ = "src/engine/SCons/compat/_scons_hashlib.py 5023 2010/06/14 22:05:46 scons"

import md5
from string import hexdigits

class md5obj(object):

    md5_module = md5

    def __init__(self, name, string=''):
        if not name in ('MD5', 'md5'):
            raise ValueError("unsupported hash type")
        self.name = 'md5'
        self.m = self.md5_module.md5()

    def __repr__(self):
        return '<%s HASH object @ %#x>' % (self.name, id(self))

    def copy(self):
        import copy
        result = copy.copy(self)
        result.m = self.m.copy()
        return result

    def digest(self):
        return self.m.digest()

    def update(self, arg):
        return self.m.update(arg)

    def hexdigest(self):
        return self.m.hexdigest()

new = md5obj

def md5(string=''):
    return md5obj('md5', string)

# Local Variables:
# tab-width:4
# indent-tabs-mode:nil
# End:
# vim: set expandtab tabstop=4 shiftwidth=4:
