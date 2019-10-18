# Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
#Ignored.NewRVersionMigration
foo.bar <- function(x, y, z) { print(x); print(y); print(z) }

setClass("A", representation(a = "numeric"))
setClass("B", representation(b = "logical"))
setMethod("foo.bar", signature = list(y = "A", z = "B"), function(x, y, z) { print("primitive, A, B") })
setMethod("foo.bar", signature = list(y = "B", z = "A"), function(x, y, z) { print("primitive, B, A") })

foo.bar(1, 2, 3)
foo.bar(1, new("A"), new("B"))
foo.bar(1, new("B"), new("A"))
