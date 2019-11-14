# Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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

# WIP: this will be framework for simple "specification" like tests that
# will allow to mix C and R code in one file

# Compiles and loads C code given as either a string or filename
loadC <- function(src) {
  if (!file.exists(code)) {
    src <- tempfile(fileext = '.c')
    cat(code, file=src)
  }
  obj <- tempfile(fileext = '.so')
  res <- system2('R', c('CMD', 'SHLIB', '-o', obj, src))
  if (res != 0L) {
    stop("Error during the compilation")
  }
  dyn.load(obj)
}