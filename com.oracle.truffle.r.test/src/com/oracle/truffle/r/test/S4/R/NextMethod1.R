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
setClass("A")
setClass("B", contains = "A")
setClass("C", contains = "A")

setClass("D", representation(id = "numeric"))
setClass("E", contains = "D")


setGeneric("foo.bar", function(x, y) {
   standardGeneric("foo.bar")
 })
 
 setMethod("foo.bar", 
  signature(x = "C", y = "D"),
  function(x, y) {  
   callNextMethod()
   message("foo.bar(C, D)")
  })
  
 setMethod("foo.bar", 
  signature(x = "A", y = "D"), 
  function(x, y) {
    message("foo.bar(A, D)")
  })
  
  setMethod("foo.bar", 
  signature(x = "B", y = "D"),
  function(x, y) {
    callNextMethod()
    message("foo.bar(B, D)")
  })

foo.bar(new("B"), new("D"))

setMethod("foo.bar", 
  signature(x = "C", y = "E"),
  function(x, y) {
    callNextMethod()
    message("foo.bar(C, E)")
})

foo.bar(new("C"), new("E"))

foo.bar(new("B"), new("E"))
