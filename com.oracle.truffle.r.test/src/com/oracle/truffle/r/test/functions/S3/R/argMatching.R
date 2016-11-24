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

g.default <- function(y,...) { cat('g.default args:\n'); print(list(if(missing(y)) NULL else y,...)); }
g.c <- function(x,...) { cat('g.c args:\n'); print(list(if(missing(x)) NULL else x,...)); }
g <- function(x,...) { cat('dispatch\n'); UseMethod('g') }
v <- structure(42,class='c');
sd <- function(i,r) { cat('side effect ',i, '\n');r }

g(y=v)
g(x=v)
g(y=v,x=42)

# here we should have hit the cache limit of CallMatcherNode: the following tests the generic call matcher:

g(y=42,x=v)
g(y=v,z=42)

g(y=sd('y',v), z=sd('z',42))
g(z=sd('z',42), y=sd('y',v))
g()