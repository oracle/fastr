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

GNUR_C_FILES = init.c
GRAMRD_C = gramRd.c
GRAMRD_OBJ := $(addprefix $(OBJ)/, gramRd.o)

GNUR_C_OBJECTS := $(addprefix $(OBJ)/, $(GNUR_C_FILES:.c=.o)) $(GRAMRD_OBJ)

LIB_PKG_PRE = $(GRAMRD_OBJ)

CLEAN_PKG = rm_gramRd

ifeq ($(FASTR_RFFI),nfi)
XTRA_C_SOURCES += $(SRC)/truffle_nfi/gramRd_nfi.c
XTRA_C_OBJECTS += $(OBJ)/gramRd_nfi.o
else
ifeq ($(FASTR_RFFI),llvm)
XTRA_C_SOURCES += $(SRC)/truffle_llvm/gramRd_llvm.c
XTRA_C_OBJECTS += $(OBJ)/gramRd_llvm.o
else
ifeq ($(FASTR_RFFI),jni)
XTRA_C_SOURCES += $(SRC)/jni/gramRd_jni.c
XTRA_C_OBJECTS += $(OBJ)/gramRd_jni.o
endif
endif
endif

include ../lib.mk

# in case already generated
C_OBJECTS := $(filter-out $(GRAMRD_OBJ), $(C_OBJECTS))

$(C_OBJECTS): | $(OBJ)

$(SRC)/gramRd.c: $(GNUR_SRC)/gramRd.c
	mx mkgramrd $(GNUR_SRC)/gramRd.c $(SRC)/gramRd.c

$(OBJ)/%.o: $(GNUR_SRC)/%.c
	$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

$(OBJ)/gramRd_nfi.o: $(SRC)/truffle_nfi/gramRd_nfi.c
	$(CC) $(CFLAGS) $(FFI_INCLUDES) -c $< -o $@

$(OBJ)/gramRd_jni.o: $(SRC)/jni/gramRd_jni.c
	$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

rm_gramRd:
	rm -f $(SRC)/gramRd.c
