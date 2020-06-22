# Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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

# Script that generates all the possible data representations of R object
# This is used to test the heap dumps viewing in VisualVM

# -----
# primitive values

intprim <- 42L
doubleprim <- 42
logicalprim <- T
complexprim <- 3+1i
strprim <- "hello"
rawprim <- as.raw(41L)

# -----
# simple array backed vectors

intarr <- c(42L, 33L)
doublearr <- c(42, 33)
logicalarr <- c(T, F, T)
complexarr <- c(3+1i, 4, 3i)
strarr <- c("hello", "world")
rawarr <- as.raw(c(41L, 34L))
listarr <- list(1,2,3,4,5)

# -----
# compact representations

intseq <- 1:99
doubleseq <- seq(1, 9, by = pi)
strseq <- as.character(intseq)

# -----
# named array backed vectors

intarrnamed <- c(a=42L, b=33L)
doublearrnamed <- c(a=42, b=33)
logicalarrnamed <- c(a=T, b=F, q=T)
complexarrnamed <- c(a=3+1i, b=4, q=3i)
strarrnamed <- c(a="hello", b="world")
rawarrnamed <- as.raw(c(a=41L, b=34L))
listarrnamed <- list(field1 = 1, field2 = 2)

# -----
# attaching other attributes

attributed1 <- structure(42L, myattr1 = "myattr value", myattr2 = 42.42)
matrixvec <- matrix(c(1,2,3,6,7,8,9,1,1), ncol = 3)

# -----
# S4 objects

setClass("User", representation(name = "character", lucky_num = "numeric"))
john <- new("User", name = "John Doe", lucky_num = 42L)

# -----
# other object types

myenv <- list2env(list(item1 = 42, itemB = "B"))
pairl <- as.pairlist(list(a = 1, b = 2))
symb <- as.symbol("mysymbol")
lang <- quote(foo(42))
expression <- as.expression(quote(foo(42 + bar)))

library(testrffi)
external_ptr <- rffi.createExternalPtr(42L, "myexternalptrtag", "myexternalptrprot")

# -----
if (!is.null(R.version[['engine']]) && R.version[['engine']] == 'FastR') {

  jai <- new(java.type('int[]'), 6);
  jad <- new(java.type('double[]'), 7);
  jabool <- new(java.type('boolean[]'), 8);
  jabyte <- new(java.type('byte[]'), 6);
  jastr <- new(java.type('java.lang.String[]'), 2);
  jastr[[1]] <- "A"
  jastr[[2]] <- "B"

  # -----
  # foreign object wrappers:

  wrapperi <- as.integer(jai)
  wrapperd <- as.double(jad)
  wrapperl <- as.logical(jabool)
  wrapperr <- as.raw(jabyte)
  wrappers <- as.character(jastr)

  wrapperc <- as.complex(jad)
  wrapperlist <- as.list(jai)

  # -----
  # closures

  closured <- as.double(wrapperi)
  closures <- as.character(wrapperi)

  # does not produce closure yet (TBD)
  closurei <- as.integer(wrapperd)
  closurec <- as.complex(wrapperd)
  closurer <- as.raw(wrapperi)
  closurel <- as.logical(wrapperi)

  # -----
  # off heap data

  nativestrvec <- c("a", "b")
  charlenres <- rffi.char_length(nativestrvec)
  charsxp <- api.STRING_ELT(nativestrvec, 0) # gives single instance of CharSXPWrapper

  nativeintvec <- rffi.populateIntVector(10)
  nativelogical <- rffi.populateLogicalVector(11)
  nativedoublevec <- rffi.populateDoubleVector(12)
  nativestrvec <- rffi.populateCharacterVector(12)
  nativecomplexvec <- rffi.populateComplexVector(12)
  nativecomplexvec <- rffi.populateRawVector(12)

  nativelist1 <- list(1,2,3)
  nativelist2 <- list("a", "b", "c")
  sharelistres <- rffi.shareListElement(nativelist1, 1, nativelist2, 1)

} # end of FastR only code

# To print all the FastR objects classes
# for (n in ls()) {
#   .fastr.inspect(get(n), inspectVectorData=T)
# }