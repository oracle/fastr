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

`@`(getClass("ClassUnionRepresentation"), "virtual")
`@`(getClass("ClassUnionRepresentation"), "virtual")
try(`@`(getClass("ClassUnionRepresentation"), c("virtual", "foo")))
getClass("ClassUnionRepresentation")@virtual
getClass("ClassUnionRepresentation")@.S3Class
c(42)@.Data
x<-42; `@`(x, ".Data")
x<-42; `@`(x, .Data)
x<-42; slot(x, ".Data")
setClass("foo", contains="numeric"); x<-new("foo"); res<-x@.Data; removeClass("foo"); res
setClass("foo", contains="numeric"); x<-new("foo"); res<-slot(x, ".Data"); removeClass("foo"); res
try(getClass("ClassUnionRepresentation")@foo)
try(c(42)@foo)
x<-42; attr(x, "foo")<-7; try(x@foo)
x<-42; attr(x, "foo")<-7; slot(x, "foo")
x<-c(42); class(x)<-"bar"; try(x@foo)
x<-getClass("ClassUnionRepresentation"); slot(x, "virtual")
x<-getClass("ClassUnionRepresentation"); try( slot(x, virtual))
x<-function() 42; attr(x, "foo")<-7; y<-asS4(x); y@foo
x<-NULL; try(`@`(x, foo))
x<-NULL; try(x@foo)
x<-paste0(".", "Data"); y<-42; slot(y, x)
