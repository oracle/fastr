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

create_promise <- function(expr, env) {
  stopifnot(is.environment(env))
  .Call("promises_create_promise", expr, env)
}

run_native_tests <- function() {
  .Call("promises_tests")
}

prom <- create_promise(quote(1+1), globalenv())
stopifnot(prom == 2L)

# Create a promise with different environment
env <- new.env()
env$func <- function(x) x
prom <- create_promise(quote(func(42)), env)
stopifnot(prom == 42)

# Create a nested promise
nested_prom <- create_promise(quote(1+1), globalenv())
prom <- create_promise(nested_prom, globalenv())
stopifnot(prom == 2)

# Run rest of the native tests
run_native_tests()
