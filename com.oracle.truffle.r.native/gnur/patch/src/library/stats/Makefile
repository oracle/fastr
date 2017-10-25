#
# Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
#
# Completeness of the stats package is a work in progress
# Currently we compile a subset of the .c and .f files from the
# GnuR stats library source - those that do not depend on GnuR internals (always true for .f)
# Also do some necessary modifications to a copy of fft.c.

# have to include this here for PKG_LIBS
ifneq ($(MAKECMDGOALS),clean)
include $(TOPDIR)/platform.mk
endif

PKG_LIBS := $(LAPACK_LIBS) $(BLAS_LIBS) -L$(FASTR_LIB_DIR) $(FLIBS)
PKG_INCLUDES = -I src

include ../lib.mk
