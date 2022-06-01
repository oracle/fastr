#  Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
#  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
#  This code is free software; you can redistribute it and/or modify it
#  under the terms of the GNU General Public License version 3 only, as
#  published by the Free Software Foundation.
#
#  This code is distributed in the hope that it will be useful, but WITHOUT
#  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
#  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
#  version 3 for more details (a copy is included in the LICENSE file that
#  accompanied this code).
#
#  You should have received a copy of the GNU General Public License version
#  3 along with this work; if not, write to the Free Software Foundation,
#  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
#  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
#  or visit www.oracle.com if you need additional information or have any
#  questions.
"""
TODO: This is a work in progress and should be implemented as soon as a new package cache
that uses artifact_uploader is available.
"""
import os
import shutil
import subprocess
import tempfile
import unittest
from typing import List
from unittest import TestCase


class PkgCacheOutput:
    def __init__(self):
        pass


class TestPackageCache(TestCase):
    FASTR_HOME = None
    GNUR_HOME = None
    PKGCACHE_DIR = None

    @classmethod
    def setUpClass(cls) -> None:
        if os.path.basename(os.getcwd()) == "fastr":
            cls.FASTR_HOME = os.getcwd()
        else:
            cls.FASTR_HOME = os.path.abspath(os.path.join(os.getcwd(), "..", "..", ".."))
        assert os.path.basename(cls.FASTR_HOME) == "fastr"
        cls.PKGCACHE_DIR = tempfile.mkdtemp()
        assert os.path.exists(cls.PKGCACHE_DIR)

    @classmethod
    def tearDownClass(cls) -> None:
        shutil.rmtree(cls.PKGCACHE_DIR, ignore_errors=True)

    def test_print_api_checksum_fastr(self):
        ret = subprocess.run(["mx", "r-pkgcache", "--print-api-checksum", "--vm", "fastr"], capture_output=True, text=True)
        self.assertTrue("fastr: " in ret.stdout)

    def test_print_api_checksum_gnur(self):
        ret = subprocess.run(["mx", "r-pkgcache", "--print-api-checksum", "--vm", "gnur"], capture_output=True, text=True)
        self.assertTrue("gnur: " in ret.stdout)

    def test_install_simple_package(self):
        repos = "CRAN=https://graalvm.oraclecorp.com/fastr-mran-mirror/snapshot/2021-02-01/"
        # abind has only two base dependencies (that should already be installed by default) - methods and utils.
        subprocess.run(["mx", "r-pkgcache", "--fastr-home", TestPackageCache.FASTR_HOME, "--gnur-home", TestPackageCache.GNUR_HOME,
                        "--pkg-pattern", "abind"])
        pass

    def test_install_pkg_with_one_dep(self):
        pass

    def test_repo(self):
        pass

    def _run_pkgcache(self, args: List[str]) -> PkgCacheOutput:
        pass


if __name__ == '__main__':
    unittest.main()
