# Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

library(testrffi)

# Replace n-th string from `str` character vector with `replacement`.
# `n` is zero-based
replace_nth_str <- function(str, i, replacement) {
    stopifnot(is.character(str) && length(str) > 0)
    stopifnot(is.integer(i) && length(i) == 1)
    stopifnot(is.character(replacement) && length(replacement) == 1)
    .Call("replace_nth_str", str, i, replacement)
}

# Returns n-th element from `str`.
nth_str <- function(str, i) {
    stopifnot(is.character(str) && length(str) > 0)
    stopifnot(is.integer(i) && length(i) == 1)
    .Call("nth_str", str, i)
}

create_empty_str <- function(i) {
    stopifnot(is.integer(i) && length(i) == 1)
    .Call("create_empty_str", i)
}

# Rest of the native tests
run_all_native_tests <- function() {
    .Call("str_tests")
}

s <- c("a", "b", "c")
stopifnot(nth_str(s, 0L) == "a")
stopifnot(nth_str(s, 1L) == "b")
stopifnot(nth_str(s, 2L) == "c")

# Replace some elements of a character vector in place with wrapper functions.
stopifnot( replace_nth_str(c("a", "b"), 0L, "foo") == c("foo", "b"))
stopifnot( replace_nth_str(c("a", "b"), 1L, "foo") == c("a", "foo"))

s <- c("a", "b", "c")
replace_nth_str(s, 0L, "X")
stopifnot(s == c("X", "b", "c"))
replace_nth_str(s, 2L, "Y")
stopifnot(s == c("X", "b", "Y"))

# Create a vector in native and manipulate with it in R.
s <- create_empty_str(3L)
s[1] <- "X"
stopifnot(s == c("X", "", ""))
replace_nth_str(s, 1L, "Y")
stopifnot(s == c("X", "Y", ""))
s[3] <- "Z"
stopifnot(s == c("X", "Y", "Z"))

# Run rest of the native tests
run_all_native_tests()