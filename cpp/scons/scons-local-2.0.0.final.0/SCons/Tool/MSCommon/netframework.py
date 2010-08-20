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

__revision__ = "src/engine/SCons/Tool/MSCommon/netframework.py 5023 2010/06/14 22:05:46 scons"

__doc__ = """
"""

import os
import re

from common import read_reg, debug

# Original value recorded by dcournapeau
_FRAMEWORKDIR_HKEY_ROOT = r'Software\Microsoft\.NETFramework\InstallRoot'
# On SGK's system
_FRAMEWORKDIR_HKEY_ROOT = r'Software\Microsoft\Microsoft SDKs\.NETFramework\v2.0\InstallationFolder'

def find_framework_root():
    # XXX: find it from environment (FrameworkDir)
    try:
        froot = read_reg(_FRAMEWORKDIR_HKEY_ROOT)
        debug("Found framework install root in registry: %s" % froot)
    except WindowsError, e:
        debug("Could not read reg key %s" % _FRAMEWORKDIR_HKEY_ROOT)
        return None

    if not os.path.exists(froot):
        debug("%s not found on fs" % froot)
        return None

    return froot

def query_versions():
    froot = find_framework_root()
    if froot:
        contents = os.listdir(froot)

        l = re.compile('v[0-9]+.*')
        versions = [e for e in contents if l.match(e)]

        def versrt(a,b):
            # since version numbers aren't really floats...
            aa = a[1:]
            bb = b[1:]
            aal = aa.split('.')
            bbl = bb.split('.')
            # sequence comparison in python is lexicographical
            # which is exactly what we want.
            # Note we sort backwards so the highest version is first.
            return cmp(bbl,aal)

        versions.sort(versrt)
    else:
        versions = []

    return versions

# Local Variables:
# tab-width:4
# indent-tabs-mode:nil
# End:
# vim: set expandtab tabstop=4 shiftwidth=4:
