#
# Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
#

print("Hello, World! (from file)")
print("Creating a Java object in FastR")

clazz <- new.java.class("java.util.Date")
obj <- new.external(clazz, as.external.long(as.integer(Sys.time())*1000))
print(obj$toString())

# add classpath entry to be able to use our class
java.addToClasspath("build/classes")
clazz <- new.java.class("com.oracle.truffle.r.JavaMessage")
obj <- new.external(clazz, "Hi there")
print(obj$getMessage())

JS_MIME_TYPE <- "application/javascript"
eval.external(JS_MIME_TYPE, source='var s = "Hello from Javascript"; print(s)')
eval.external(JS_MIME_TYPE, path="JS/main.js")

