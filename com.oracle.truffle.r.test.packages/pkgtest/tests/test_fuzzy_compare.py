#
# Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 3 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 3 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 3 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#
from unittest import TestCase
from pkgtest.fuzzy_compare import fuzzy_compare

class TestFuzzyCompare(TestCase):
    @staticmethod
    def run_test(fastr_file, gnur_file):
        fastr_content = []
        with open(fastr_file) as f:
            fastr_content = f.readlines()

        gnur_content = []
        with open(gnur_file) as f:
            gnur_content = f.readlines()

        return fuzzy_compare(gnur_content, fastr_content, gnur_file, fastr_file)

    def test1(self):
        self.assertEqual(self.run_test("fastr1.Rout", "gnur1.Rout"), (0, 1, 0))

    def test2(self):
        self.assertEqual(self.run_test("fastr2.Rout", "gnur2.Rout"), (1, 0, 1))

    def test3(self):
        self.assertEqual(self.run_test("fastr3.Rout", "gnur3.Rout"), (1, 1, 1))

    def test4(self):
        self.assertEqual(self.run_test("fastr4.Rout", "gnur4.Rout"), (1, 0, 1))

    def test_cprr(self):
        self.assertEqual(self.run_test("fastr_cprr-Ex.Rout", "gnur_cprr-Ex.Rout"), (0, 26, 0))

    def test_xts(self):
        self.assertEqual(self.run_test("fastr_xts-Ex.Rout", "gnur_xts-Ex.Rout"), (1, 46, 26))

    def test_mime(self):
        self.assertEqual(self.run_test("fastr_mime.Rout", "gnur_mime.Rout"), (0, 1, 0))

    def test_iterators(self):
        self.assertEqual(self.run_test("fastr_iterators.Rout", "gnur_iterators.Rout"), (0, 89, 0))

    def test_rjson(self):
        self.assertEqual(self.run_test("fastr_rjson.Rout", "gnur_rjson.Rout"), (0, 45, 0))
