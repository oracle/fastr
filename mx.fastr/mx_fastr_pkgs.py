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

from argparse import ArgumentParser
from os.path import join, abspath
import shutil, os
import mx_fastr

def pkgtest(args):
    '''used for package installation/testing'''
    parser = ArgumentParser(prog='r test')
    parser.add_argument('--ok-only', action='store_true', help='only install/test packages from the ok.packages file')
    parser.add_argument('--install-only', action='store_true', help='just install packages, do not test')
    # sundry options understood by installpkgs R code
    parser.add_argument('--pkg-count', action='store', help='number of packages to install/test', default='100')
    parser.add_argument('--ignore-blacklist', action='store_true', help='pass --ignore-blacklist')
    parser.add_argument('--install-dependents-first', action='store_true', help='pass -install-dependents-first')
    parser.add_argument('--print-ok-installs', action='store_true', help='pass --print-ok-installs')
    args = parser.parse_args(args)

    libinstall = abspath("lib.install.cran")
    # make sure its empty
    shutil.rmtree(libinstall, ignore_errors=True)
    os.mkdir(libinstall)
    install_tmp = "install.tmp"
    shutil.rmtree(install_tmp, ignore_errors=True)
    os.mkdir(install_tmp)
    os.environ["TMPDIR"] = install_tmp
    os.environ['R_LIBS_USER'] = libinstall
    stacktrace_args = ['--J', '@-DR:-PrintErrorStacktracesToFile -DR:+PrintErrorStacktraces']

    install_args = ['--pkg-count', args.pkg_count]
    if args.ok_only:
        # only install/test packages that have been successfully installed
        install_args += ['--pkg-filelist', join(mx_fastr._cran_test_project(), 'ok.packages')]
    if not args.install_only:
        install_args += ['--run-tests']
    if args.ignore_blacklist:
        install_args += ['--ignore-blacklist']
    if args.install_dependents_first:
        install_args += ['--install-dependents-first']
    if args.print_ok_installs:
        install_args += ['--print-ok-installs']

    class OutputCapture:
        def __init__(self):
            self.data = ""
        def __call__(self, data):
            print data,
            self.data += data

    out = OutputCapture()

    rc = mx_fastr.installpkgs(stacktrace_args + install_args, out=out, err=out)

    shutil.rmtree(install_tmp, ignore_errors=True)
    return rc

