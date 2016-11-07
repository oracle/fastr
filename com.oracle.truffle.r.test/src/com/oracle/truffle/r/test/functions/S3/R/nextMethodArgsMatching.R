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

# tests that the signature of the call of the S3 dispatch function is matched to
# the signature of the target of NextMethod. Note that default value of 'c' in
# foo.baz is ignored and 'cc' in foo.bar is matched positionally with 'caller-d'.

withClass <- function(x, cls) { class(x) <- cls; x }

# a - matched by name param
# b - unmatched named param, but matched named in foo.bar
# c - default param (no value given), default (no value given) in foo.bar
# cc - default param (no value given), not default default in foo.bar
# d - matched positional param
# e - unmatched named param, stays unmatched named in foo.bar

foo <- function(x, ...) UseMethod("foo")
foo.bar <- function(x, a, b, cc, c='def-bar', ...) {
    cat("foo.bar with: \n");
    print(list(x=x, a=a, b=b, c=c, cc=cc, named=list(...)))
}

foo.baz <-   function(x, d, a=1, c='def-baz', cc='def-cc-baz', ...) NextMethod(x)
foo.bazz <-  function(x, d, a=1, c='def-baz', ...) NextMethod()
foo.bazzz <- function(x, d, a=1, c='def-baz', ...) NextMethod('foo')

foo(withClass(42, c('baz', 'bar')), 'caller-d', a='caller-a', b='caller-b', e='caller-e')
foo(withClass(42, c('bazz', 'bar')), 'caller-d', a='caller-a', b='caller-b', e='caller-e')
foo(withClass(42, c('bazzz', 'bar')), 'caller-d', a='caller-a', b='caller-b', e='caller-e')
