# Ignored
# Note: once this works, we can remove nextMethodAdditionalArgs2.R,
# which is the same test without promises evaluation check

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

# tests the scenario when NextMethod has some additional arguments.
# Note: it seems that those additional arguments are taken into account only
# if named, otherwise the value is ignored (see 'matched-positionally?')
# see also nextMethodArgsMatching.R

withClass <- function(x, cls) { class(x) <- cls; x }

side <- function(x) { cat("evaluated ", x, "\n"); x }

foo <- function(x, ...) UseMethod("foo")
foo.bar <- function(x, a, b, cc, c='def-bar', ...) {
    cat("foo.bar with: \n");
    print(list(x=x, a=a, b=b, c=c, cc=cc, named=list(...)))
}

foo.baz <-   function(x, d, a=1, c='def-baz', cc='def-cc-baz', ...) NextMethod(x, side('matched-positionally?'), c='explicit-from-baz', f='named-from-baz')
foo.bazz <-  function(x, d, a=1, c='def-baz', ...) NextMethod(c='explicit-from-baz', f='named-from-baz')
foo.bazzz <- function(x, d, a=1, c='def-baz', ...) NextMethod('foo', x, side('matched-positionally?'), c='explicit-from-baz', f='named-from-baz')

foo(withClass(42, c('baz', 'bar')), 'caller-d', a='caller-a', b='caller-b', e='caller-e')
foo(withClass(42, c('bazz', 'bar')), 'caller-d', a='caller-a', b='caller-b', e='caller-e')
foo(withClass(42, c('bazzz', 'bar')), 'caller-d', a='caller-a', b='caller-b', e='caller-e')
