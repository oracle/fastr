# Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

foo <- function(x, ...) UseMethod("foo")
foo.default <- function(x, ...) { cat("called foo.default with ", x, "\n"); print(list(...)); }
foo.bar <- function(x, ...) { cat("called foo.bar with ", x, "\n"); print(list(...)); }

# in foo.baz NextMethod gets additional argument "3" that should be passed to foo.bar.
# However, the first argument: vector of characters, is ignored
foo.baz <- function(x, ...) { cat("called foo.baz with ", x, "\n"); print(list(...)); NextMethod(c('something', 'foo'), x, 3); }

val <- 42
class(val) <- c('baz', 'bar')
foo(val)