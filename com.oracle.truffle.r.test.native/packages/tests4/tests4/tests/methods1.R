#
# Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
stopifnot(require(methods))
stopifnot(require(tests4))

setGeneric("legs", function(object) {
  standardGeneric("legs")
})

setClass("Animal")
setClass("Mammal", representation(legs = "integer"), contains = "Animal")
setClass("Elephant", contains = "Mammal")
setClass("Kangaroo", contains = "Mammal")
setClass("Bird", contains = "Animal")

setMethod("legs", signature(object = "Mammal"), function(object) {
  object@legs
})

setMethod("legs", signature("Elephant"), function(object) 4)
setMethod("legs", signature("Kangaroo"),   function(object) 2)
setMethod("legs", signature("Bird"),   function(object) 2)

res<-print(showMethods("legs"))
removeGeneric("legs")
print(res)
