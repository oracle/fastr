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

# tests the execution of promises when NextMethod has some additional arguments
# that may override the original arguments. See also nextMethodArgsMatching.R

withClass <- function(x, cls) { class(x) <- cls; x }
side <- function(x) { cat("evaluated ", x, "\n"); x }

foo <- function(x, ...) UseMethod("foo")
foo.bar <- function(x, a, b, ...) {
    cat("foo.bar with: \n");
    print(list(x=x, a=a, b=b, named=list(...)))
}

foo.baz <-   function(x, ...) { cat("foo.baz\n"); NextMethod(x, a='a-from-baz') }
foo.bazz <-  function(x, a, b, ...) NextMethod(x, a='a-from-bazz')
foo.bazzz <-  function(x, a='a-default', b, ...) NextMethod(x, a='a-from-bazz')

foo(withClass(42, c('baz', 'bar')), a=side('a-from-caller'), b=side('b-from-caller'))
foo(withClass(42, c('bazz', 'bar')), a=side('a-from-caller'), b=side('b-from-caller'))
foo(withClass(42, c('bazzz', 'bar')), b='b-from-caller')
