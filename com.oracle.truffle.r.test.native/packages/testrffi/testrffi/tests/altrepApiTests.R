# Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

# Contains tests for some ALTREP API functions for standard vectors.

stopifnot( require(testrffi))

# Integer API
integer_api_tests <- function(int_vec) {
    for (i in 1:length(int_vec)) {
        api.INTEGER_ELT(int_vec, i)
    }
    api.INTEGER_IS_SORTED(int_vec)
    api.INTEGER_NO_NA(int_vec)
}
integer_api_tests(1:10)
integer_api_tests(as.integer(c(14, 51, 157, 42, 20, 15, 15)))

# Real API
real_api_tests <- function(real_vec) {
    for (i in 1:length(real_vec)) {
        api.REAL_ELT(real_vec, i)
    }
    api.REAL_IS_SORTED(real_vec)
    api.REAL_NO_NA(real_vec)
}
real_api_tests(as.double(1:100))
real_api_tests(as.double(c(0, 0, 0, 0, 1)))

# Logical API
logical_api_tests <- function(lgl_vec) {
    for (i in 1:length(lgl_vec)) {
        api.LOGICAL_ELT(lgl_vec, i)
    }
    api.LOGICAL_IS_SORTED(lgl_vec)
    api.LOGICAL_NO_NA(lgl_vec)
}
logical_api_tests(c(TRUE, TRUE, FALSE, TRUE))
logical_api_tests(c(TRUE, FALSE))
logical_api_tests(TRUE)

# String API
string_api_tests <- function(str_vec) {
    for (i in 1:length(str_vec)) {
        api.STRING_ELT(str_vec, i)
    }
    api.STRING_IS_SORTED(str_vec)
    api.STRING_NO_NA(str_vec)
}
string_api_tests(c("hello", "world"))
string_api_tests(c(""))
string_api_tests(c("a", "a", "a"))
string_api_tests(c("a", "b", "c"))

# Raw API
raw_vec <- as.raw(c(12, 117, 45, 0, 1))
for (i in 1:length(raw_vec)) {
    api.RAW_ELT(raw_vec, i)
}
