#
# Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
setClass("Product", representation(name = "character", price = "numeric"))
setClass("TV", contains = "Product")
setClass("Laptop", contains = "Product")

setClass("OrderProcessor", representation(name = "character"))
setClass("LicensingProcessor", contains = "OrderProcessor")

setGeneric("processOrder", function(v, i) {
   standardGeneric("processOrder")
})

setMethod("processOrder",
 signature(v = "Product", i = "OrderProcessor"), 
 function(v, i) {
   print(paste0(v@name, " ordered for ", v@price))
 })

setMethod("processOrder", 
 signature(v = "TV", i = "OrderProcessor"),
 function(v, i) {  
   callNextMethod()
   print(paste0("Notifying TV companies by ", i@name))
})

setMethod("processOrder", 
  signature(v = "Laptop", i = "OrderProcessor"),
  function(v, i) {
    callNextMethod()
    print(paste0("Getting OS license by ", i@name))
})

setMethod("processOrder", 
  signature(v = "Laptop", i = "LicensingProcessor"),
  function(v, i) {
    callNextMethod()
    print(paste0("Checking SW licenses by ", i@name))
})
