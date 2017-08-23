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

OBJ = lib
SRC = src

ifeq ($(FASTR_RFFI),nfi)
XTRA_C_SOURCES += $(SRC)/truffle_nfi/gramRd_nfi.c
XTRA_C_OBJECTS += $(OBJ)/gramRd_nfi.o
else
ifeq ($(FASTR_RFFI),llvm)
XTRA_C_SOURCES += $(SRC)/truffle_llvm/gramRd_llvm.c
XTRA_C_OBJECTS += $(OBJ)/gramRd_llvm.o
SULONG_DIR = $(abspath $(FASTR_R_HOME)/../sulong)

SULONG_INCLUDES = -I$(SULONG_DIR)/include
endif
endif

include ../lib.mk

$(C_OBJECTS): | $(OBJ)

$(OBJ)/gramRd_nfi.o: $(SRC)/truffle_nfi/gramRd_nfi.c
	$(CC) $(CFLAGS) $(FFI_INCLUDES) $(NFI_INCLUDES) -c $< -o $@

$(OBJ)/gramRd_llvm.o: $(SRC)/truffle_llvm/gramRd_llvm.c
	$(CC) $(CFLAGS) $(FFI_INCLUDES) $(SULONG_INCLUDES) -c $< -o $@
