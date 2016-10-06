#
# Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#
import os
import platform
import subprocess
import shutil
import mx

def _darwin_extract_realpath(lib, libpath):
    '''
    If libpath has a dependency on lib, return the path in the library, else None
    '''
    try:
        output = subprocess.check_output(['otool', '-L', libpath])
        lines = output.split('\n')
        for line in lines[1:]:
            if lib in line:
                parts = line.split(' ')
                return parts[0].strip()
        return None
    except subprocess.CalledProcessError:
        mx.abort('copylib: otool failed')

def _copylib(lib, libpath, target):
    '''
    Just copying libxxx.so/dylib isn't sufficient as versioning is involved.
    The path is likely a symlink to libxxx.so.n, for example, but what is really
    important is the version that is encoded in the shared library itself.
    Unfortunately getting that info is is OS specific.
    '''
    if platform.system() == 'Darwin':
        real_libpath = _darwin_extract_realpath(lib, libpath)
    else:
        try:
            output = subprocess.check_output(['objdump', '-p', libpath])
            lines = output.split('\n')
            for line in lines:
                if 'SONAME' in line:
                    ix = line.find('lib' + lib)
                    real_libpath = os.path.join(os.path.dirname(libpath), line[ix:])
        except subprocess.CalledProcessError:
            mx.abort('copylib: otool failed')
    # copy both files
    shutil.copy(real_libpath, target)
    libpath_base = os.path.basename(libpath)
    os.chdir(target)
    if libpath != real_libpath:
        # create a symlink
        if os.path.exists(libpath_base):
            os.remove(libpath_base)
        os.symlink(os.path.basename(real_libpath), libpath_base)
    # On Darwin we change the id to use @rpath
    if platform.system() == 'Darwin':
        try:
            subprocess.check_call(['install_name_tool', '-id', '@rpath/' + libpath_base, libpath_base])
        except subprocess.CalledProcessError:
            mx.abort('copylib: install_name_tool failed')
    # TODO @rpath references within the library?
    mx.log('copied ' + lib + ' library from ' + libpath + ' to ' + target)

def copylib(args):
    '''
    This supports a configuration where no explicit setting (e.g. LD_LIBRARY_PATH) is
    required at runtime for the libraries that are required by FastR, e.g. pcre.
    The easy case is when the libraries are already installed with the correct versions
    in one of the directories, e.g./usr/lib, that is searched by default by the system linker -
    in which case no configuration is required.

    Otherwise, since systems vary considerably in where such libraries are located, the general solution
    is to copy libraries located in non-system locations into the FastR 'lib' directory. N.B. GNU R is
    even more picky than FastR about library versions and depends on a larger set, so the local
    copy embedded in FastR is built using PKG_LDFLAGS_OVERRIDE to specify the location of necessary external
    libraries. However, the result of this analysis isn't captured anywhere, so we re-analyze here.
    If PKG_LDFLAGS_OVERRIDE is unset, we assume the libraries are located in the system directories
    and do nothing.
    '''
    if os.environ.has_key('PKG_LDFLAGS_OVERRIDE'):
        parts = os.environ['PKG_LDFLAGS_OVERRIDE'].split(' ')
        ext = '.dylib' if platform.system() == 'Darwin' else '.so'
        lib_prefix = 'lib' + args[0]
        plain_libpath = lib_prefix + ext
        for part in parts:
            path = part.strip('"').lstrip('-L')
            for f in os.listdir(path):
                if f.startswith(lib_prefix):
                    if os.path.exists(os.path.join(path, plain_libpath)):
                        f = plain_libpath
                    target_dir = args[1]
                    if not os.path.exists(os.path.join(target_dir, f)):
                        _copylib(args[0], os.path.join(path, f), args[1])
                    return 0

    mx.log(args[0] + ' not found in PKG_LDFLAGS_OVERRIDE, assuming system location')

def updatelib(args):
    '''
    If we captured a library then, on Darwin, we patch up the references
    in the target library passed as argument to use @rpath.
    args:
      0 directory containing library
    '''
    ignore_list = ['R', 'Rblas', 'Rlapack', 'jniboot']

    def ignorelib(name):
        for ignore in ignore_list:
            x = 'lib' + ignore + '.dylib'
            if x == name:
                return True
        return False

    libdir = args[0]
    cap_libs = []
    libs = []
    for lib in os.listdir(libdir):
        if not os.path.islink(os.path.join(libdir, lib)):
            libs.append(lib)
        if ignorelib(lib) or os.path.islink(os.path.join(libdir, lib)):
            continue
        cap_libs.append(lib)
    # for each of the libs, check whether they depend
    # on any of the captured libs, @rpath the dependency if so
    for lib in libs:
        targetlib = os.path.join(libdir, lib)
        for cap_lib in cap_libs:
            try:
                real_libpath = _darwin_extract_realpath(cap_lib, targetlib)
                if real_libpath and not '@rpath' in real_libpath:
                    cmd = ['install_name_tool', '-change', real_libpath, '@rpath/' + cap_lib, targetlib]
                    subprocess.check_call(cmd)
            except subprocess.CalledProcessError:
                mx.abort('update: install_name_tool failed')
