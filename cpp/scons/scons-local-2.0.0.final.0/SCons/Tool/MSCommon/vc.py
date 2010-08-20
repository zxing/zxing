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

# TODO:
#   * supported arch for versions: for old versions of batch file without
#     argument, giving bogus argument cannot be detected, so we have to hardcode
#     this here
#   * print warning when msvc version specified but not found
#   * find out why warning do not print
#   * test on 64 bits XP +  VS 2005 (and VS 6 if possible)
#   * SDK
#   * Assembly
__revision__ = "src/engine/SCons/Tool/MSCommon/vc.py 5023 2010/06/14 22:05:46 scons"

__doc__ = """Module for Visual C/C++ detection and configuration.
"""
import SCons.compat

import os
import platform
from string import digits as string_digits

import SCons.Warnings

import common

debug = common.debug

import sdk

get_installed_sdks = sdk.get_installed_sdks


class VisualCException(Exception):
    pass

class UnsupportedVersion(VisualCException):
    pass

class UnsupportedArch(VisualCException):
    pass

class MissingConfiguration(VisualCException):
    pass

class NoVersionFound(VisualCException):
    pass

class BatchFileExecutionError(VisualCException):
    pass

# Dict to 'canonalize' the arch
_ARCH_TO_CANONICAL = {
    "amd64"     : "amd64",
    "emt64"     : "amd64",
    "i386"      : "x86",
    "i486"      : "x86",
    "i586"      : "x86",
    "i686"      : "x86",
    "ia64"      : "ia64",
    "itanium"   : "ia64",
    "x86"       : "x86",
    "x86_64"    : "amd64",
}

# Given a (host, target) tuple, return the argument for the bat file. Both host
# and targets should be canonalized.
_HOST_TARGET_ARCH_TO_BAT_ARCH = {
    ("x86", "x86"): "x86",
    ("x86", "amd64"): "x86_amd64",
    ("amd64", "amd64"): "amd64",
    ("amd64", "x86"): "x86",
    ("x86", "ia64"): "x86_ia64"
}

def get_host_target(env):
    debug('vc.py:get_host_target()')

    host_platform = env.get('HOST_ARCH')
    if not host_platform:
        host_platform = platform.machine()
        # TODO(2.5):  the native Python platform.machine() function returns
        # '' on all Python versions before 2.6, after which it also uses
        # PROCESSOR_ARCHITECTURE.
        if not host_platform:
            host_platform = os.environ.get('PROCESSOR_ARCHITECTURE', '')
            
    # Retain user requested TARGET_ARCH
    req_target_platform = env.get('TARGET_ARCH')
    debug('vc.py:get_host_target() req_target_platform:%s'%req_target_platform)

    if  req_target_platform:
        # If user requested a specific platform then only try that one.
        target_platform = req_target_platform
    else:
        target_platform = host_platform
        
    try:
        host = _ARCH_TO_CANONICAL[host_platform.lower()]
    except KeyError, e:
        msg = "Unrecognized host architecture %s"
        raise ValueError(msg % repr(host_platform))

    try:
        target = _ARCH_TO_CANONICAL[target_platform.lower()]
    except KeyError, e:
        raise ValueError("Unrecognized target architecture %s" % target_platform)

    return (host, target,req_target_platform)

_VCVER = ["10.0", "9.0", "9.0Exp","8.0", "8.0Exp","7.1", "7.0", "6.0"]

_VCVER_TO_PRODUCT_DIR = {
        '10.0': [
            r'Microsoft\VisualStudio\10.0\Setup\VC\ProductDir'],
        '9.0': [
            r'Microsoft\VisualStudio\9.0\Setup\VC\ProductDir'],
        '9.0Exp' : [
            r'Microsoft\VCExpress\9.0\Setup\VC\ProductDir'],
        '8.0': [
            r'Microsoft\VisualStudio\8.0\Setup\VC\ProductDir'],
        '8.0Exp': [
            r'Microsoft\VCExpress\8.0\Setup\VC\ProductDir'],
        '7.1': [
            r'Microsoft\VisualStudio\7.1\Setup\VC\ProductDir'],
        '7.0': [
            r'Microsoft\VisualStudio\7.0\Setup\VC\ProductDir'],
        '6.0': [
            r'Microsoft\VisualStudio\6.0\Setup\Microsoft Visual C++\ProductDir']
}
        
def msvc_version_to_maj_min(msvc_version):
   msvc_version_numeric = ''.join([x for  x in msvc_version if x in string_digits + '.'])

   t = msvc_version_numeric.split(".")
   if not len(t) == 2:
       raise ValueError("Unrecognized version %s (%s)" % (msvc_version,msvc_version_numeric))
   try:
       maj = int(t[0])
       min = int(t[1])
       return maj, min
   except ValueError, e:
       raise ValueError("Unrecognized version %s (%s)" % (msvc_version,msvc_version_numeric))

def is_host_target_supported(host_target, msvc_version):
    """Return True if the given (host, target) tuple is supported given the
    msvc version.

    Parameters
    ----------
    host_target: tuple
        tuple of (canonalized) host-target, e.g. ("x86", "amd64") for cross
        compilation from 32 bits windows to 64 bits.
    msvc_version: str
        msvc version (major.minor, e.g. 10.0)

    Note
    ----
    This only check whether a given version *may* support the given (host,
    target), not that the toolchain is actually present on the machine.
    """
    # We assume that any Visual Studio version supports x86 as a target
    if host_target[1] != "x86":
        maj, min = msvc_version_to_maj_min(msvc_version)
        if maj < 8:
            return False

    return True

def find_vc_pdir(msvc_version):
    """Try to find the product directory for the given
    version.

    Note
    ----
    If for some reason the requested version could not be found, an
    exception which inherits from VisualCException will be raised."""
    root = 'Software\\'
    if common.is_win64():
        root = root + 'Wow6432Node\\'
    try:
        hkeys = _VCVER_TO_PRODUCT_DIR[msvc_version]
    except KeyError:
        debug("Unknown version of MSVC: %s" % msvc_version)
        raise UnsupportedVersion("Unknown version %s" % msvc_version)

    for key in hkeys:
        key = root + key
        try:
            comps = common.read_reg(key)
        except WindowsError, e:
            debug('find_vc_dir(): no VC registry key %s' % repr(key))
        else:
            debug('find_vc_dir(): found VC in registry: %s' % comps)
            if os.path.exists(comps):
                return comps
            else:
                debug('find_vc_dir(): reg says dir is %s, but it does not exist. (ignoring)'\
                          % comps)
                raise MissingConfiguration("registry dir %s not found on the filesystem" % comps)
    return None

def find_batch_file(env,msvc_version,host_arch,target_arch):
    """
    Find the location of the batch script which should set up the compiler
    for any TARGET_ARCH whose compilers were installed by Visual Studio/VCExpress
    """
    pdir = find_vc_pdir(msvc_version)
    if pdir is None:
        raise NoVersionFound("No version of Visual Studio found")
        
    debug('vc.py: find_batch_file() pdir:%s'%pdir)

    # filter out e.g. "Exp" from the version name
    msvc_ver_numeric = ''.join([x for x in msvc_version if x in string_digits + "."])
    vernum = float(msvc_ver_numeric)
    if 7 <= vernum < 8:
        pdir = os.path.join(pdir, os.pardir, "Common7", "Tools")
        batfilename = os.path.join(pdir, "vsvars32.bat")
    elif vernum < 7:
        pdir = os.path.join(pdir, "Bin")
        batfilename = os.path.join(pdir, "vcvars32.bat")
    else: # >= 8
        batfilename = os.path.join(pdir, "vcvarsall.bat")

    if not os.path.exists(batfilename):
        debug("Not found: %s" % batfilename)
        batfilename = None
    
    installed_sdks=get_installed_sdks()
    for _sdk in installed_sdks:
        sdk_bat_file=_sdk.get_sdk_vc_script(host_arch,target_arch)
        sdk_bat_file_path=os.path.join(pdir,sdk_bat_file)
        debug('vc.py:find_batch_file() sdk_bat_file_path:%s'%sdk_bat_file_path)
        if os.path.exists(sdk_bat_file_path):
            return (batfilename,sdk_bat_file_path)
        else:
            debug("vc.py:find_batch_file() not found:%s"%sdk_bat_file_path)
    else:
        return (batfilename,None)

__INSTALLED_VCS_RUN = None

def cached_get_installed_vcs():
    global __INSTALLED_VCS_RUN

    if __INSTALLED_VCS_RUN is None:
        ret = get_installed_vcs()
        __INSTALLED_VCS_RUN = ret

    return __INSTALLED_VCS_RUN

def get_installed_vcs():
    installed_versions = []
    for ver in _VCVER:
        debug('trying to find VC %s' % ver)
        try:
            if find_vc_pdir(ver):
                debug('found VC %s' % ver)
                installed_versions.append(ver)
            else:
                debug('find_vc_pdir return None for ver %s' % ver)
        except VisualCException, e:
            debug('did not find VC %s: caught exception %s' % (ver, str(e)))
    return installed_versions

def reset_installed_vcs():
    """Make it try again to find VC.  This is just for the tests."""
    __INSTALLED_VCS_RUN = None

def script_env(script, args=None):
    stdout = common.get_output(script, args)
    # Stupid batch files do not set return code: we take a look at the
    # beginning of the output for an error message instead
    olines = stdout.splitlines()
    if olines[0].startswith("The specified configuration type is missing"):
        raise BatchFileExecutionError("\n".join(olines[:2]))

    return common.parse_output(stdout)

def get_default_version(env):
    debug('get_default_version()')

    msvc_version = env.get('MSVC_VERSION')
    msvs_version = env.get('MSVS_VERSION')
    
    debug('get_default_version(): msvc_version:%s msvs_version:%s'%(msvc_version,msvs_version))

    if msvs_version and not msvc_version:
        SCons.Warnings.warn(
                SCons.Warnings.DeprecatedWarning,
                "MSVS_VERSION is deprecated: please use MSVC_VERSION instead ")
        return msvs_version
    elif msvc_version and msvs_version:
        if not msvc_version == msvs_version:
            SCons.Warnings.warn(
                    SCons.Warnings.VisualVersionMismatch,
                    "Requested msvc version (%s) and msvs version (%s) do " \
                    "not match: please use MSVC_VERSION only to request a " \
                    "visual studio version, MSVS_VERSION is deprecated" \
                    % (msvc_version, msvs_version))
        return msvs_version
    if not msvc_version:
        installed_vcs = cached_get_installed_vcs()
        debug('installed_vcs:%s' % installed_vcs)
        if not installed_vcs:
            msg = 'No installed VCs'
            debug('msv %s\n' % repr(msg))
            SCons.Warnings.warn(SCons.Warnings.VisualCMissingWarning, msg)
            return None
        msvc_version = installed_vcs[0]
        debug('msvc_setup_env: using default installed MSVC version %s\n' % repr(msvc_version))

    return msvc_version

def msvc_setup_env_once(env):
    try:
        has_run  = env["MSVC_SETUP_RUN"]
    except KeyError:
        has_run = False

    if not has_run:
        msvc_setup_env(env)
        env["MSVC_SETUP_RUN"] = True

def msvc_find_valid_batch_script(env,version):
    debug('vc.py:msvc_find_valid_batch_script()')
    # Find the host platform, target platform, and if present the requested
    # target platform
    (host_platform, target_platform,req_target_platform) = get_host_target(env)

    # If the user hasn't specifically requested a TARGET_ARCH, and
    # The TARGET_ARCH is amd64 then also try 32 bits if there are no viable
    # 64 bit tools installed
    try_target_archs = [target_platform]
    if not req_target_platform and target_platform=='amd64':
        try_target_archs.append('x86')

    d = None
    for tp in try_target_archs:
        # Set to current arch.
        env['TARGET_ARCH']=tp
        
        debug("vc.py:msvc_find_valid_batch_script() trying target_platform:%s"%tp)
        host_target = (host_platform, tp)
        if not is_host_target_supported(host_target, version):
            warn_msg = "host, target = %s not supported for MSVC version %s" % \
                (host_target, version)
            SCons.Warnings.warn(SCons.Warnings.VisualCMissingWarning, warn_msg)
        arg = _HOST_TARGET_ARCH_TO_BAT_ARCH[host_target]
        
        # Try to locate a batch file for this host/target platform combo
        try:
            (vc_script,sdk_script) = find_batch_file(env,version,host_platform,tp)
            debug('vc.py:msvc_find_valid_batch_script() vc_script:%s sdk_script:%s'%(vc_script,sdk_script))
        except VisualCException, e:
            msg = str(e)
            debug('Caught exception while looking for batch file (%s)' % msg)
            warn_msg = "VC version %s not installed.  " + \
                       "C/C++ compilers are most likely not set correctly.\n" + \
                       " Installed versions are: %s"
            warn_msg = warn_msg % (version, cached_get_installed_vcs())
            SCons.Warnings.warn(SCons.Warnings.VisualCMissingWarning, warn_msg)
            continue
        
        # Try to use the located batch file for this host/target platform combo
        debug('vc.py:msvc_find_valid_batch_script() use_script 2 %s, args:%s\n' % (repr(vc_script), arg))
        if vc_script:
            try:
                d = script_env(vc_script, args=arg)
            except BatchFileExecutionError, e:
                debug('vc.py:msvc_find_valid_batch_script() use_script 3: failed running VC script %s: %s: Error:%s'%(repr(vc_script),arg,e))
                vc_script=None
        if not vc_script and sdk_script:
            debug('vc.py:msvc_find_valid_batch_script() use_script 4: trying sdk script: %s'%(sdk_script))
            try:
                d = script_env(sdk_script,args=[])
            except BatchFileExecutionError,e:
                debug('vc.py:msvc_find_valid_batch_script() use_script 5: failed running SDK script %s: Error:%s'%(repr(sdk_script),e))
                continue
        elif not vc_script and not sdk_script:
            debug('vc.py:msvc_find_valid_batch_script() use_script 6: Neither VC script nor SDK script found')
            continue
    
    # If we cannot find a viable installed compiler, reset the TARGET_ARCH
    # To it's initial value
    if not d:
        env['TARGET_ARCH']=req_target_platform
    
    return d
    

def msvc_setup_env(env):
    debug('msvc_setup_env()')

    version = get_default_version(env)
    if version is None:
        warn_msg = "No version of Visual Studio compiler found - C/C++ " \
                   "compilers most likely not set correctly"
        SCons.Warnings.warn(SCons.Warnings.VisualCMissingWarning, warn_msg)
        return None
    debug('msvc_setup_env: using specified MSVC version %s\n' % repr(version))

    # XXX: we set-up both MSVS version for backward
    # compatibility with the msvs tool
    env['MSVC_VERSION'] = version
    env['MSVS_VERSION'] = version
    env['MSVS'] = {}

    
    use_script = env.get('MSVC_USE_SCRIPT', True)
    if SCons.Util.is_String(use_script):
        debug('vc.py:msvc_setup_env() use_script 1 %s\n' % repr(use_script))
        d = script_env(use_script)
    elif use_script:      
        d = msvc_find_valid_batch_script(env,version)
        debug('vc.py:msvc_setup_env() use_script 2 %s\n' % d)
        if not d:
            return d
    else:
        debug('MSVC_USE_SCRIPT set to False')
        warn_msg = "MSVC_USE_SCRIPT set to False, assuming environment " \
                   "set correctly."
        SCons.Warnings.warn(SCons.Warnings.VisualCMissingWarning, warn_msg)
        return None

    for k, v in d.items():
        debug('vc.py:msvc_setup_env() env:%s -> %s'%(k,v))
        env.PrependENVPath(k, v, delete_existing=True)

def msvc_exists(version=None):
    vcs = cached_get_installed_vcs()
    if version is None:
        return len(vcs) > 0
    return version in vcs
    
