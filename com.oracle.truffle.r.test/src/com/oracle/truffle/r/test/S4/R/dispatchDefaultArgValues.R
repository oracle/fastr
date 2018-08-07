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
setClassUnion("DDAVindex", members =  c("numeric", "logical", "character"))
setClass("DDAVFoo", representation(a = "numeric", b = "numeric"))

subsetFoo <- function(x, i, j, drop) {
  cat(paste0("subsetFoo[",i,",",j,",drop=",drop,"]\n"))
  c(x@a[[i]], x@b[[j]])
}

subsetFooMissingDrop <- function(x, i, j, drop) {
  cat(paste0("subsetFooMissingDrop[",i,",",j,",drop=missing]\n"))
  c(x@a[[i]], x@b[[j]])  
}
setMethod("[", signature(x = "DDAVFoo", i = "DDAVindex", j = "DDAVindex", drop = "logical"), subsetFoo)
setMethod("[", signature(x = "DDAVFoo", i = "DDAVindex", j = "DDAVindex", drop = "missing"), subsetFooMissingDrop)

obj <- new("DDAVFoo", a=c(1,2,3),b=c(4,5,6))
obj[2,3]

obj[drop=T,j=3,i=2]