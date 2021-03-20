# Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

# Tests native serialization

stopifnot(require(testrffi))

ser_and_deser <- function(object) {
    serialized_data <- rffi.serialize(object)
    unserialized_object <- unserialize(serialized_data)
    stopifnot( all.equal(unserialized_object, object))
}

ser_and_deser(1:10)
ser_and_deser(c(1,2,3))
ser_and_deser(c("Hello", "World!"))
ser_and_deser(list(a=1, b="hello", c=42))

e <- new.env()
e$x <- 42
e$y <- "hello"
ser_and_deser(e)

f <- function() 42
ser_and_deser(f)
