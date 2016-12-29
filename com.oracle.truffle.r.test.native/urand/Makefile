#
# Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

ifeq ($(TOPDIR),)
    TOPDIR = $(abspath ..)
endif

NATIVE_PROJECT = $(subst test.native,native,$(TOPDIR))

ifneq ($(MAKECMDGOALS),clean)
include $(NATIVE_PROJECT)/platform.mk
endif

.PHONY: all clean

OBJ = lib
SRC = src
C_SOURCES := $(wildcard $(SRC)/*.c)
# Since this library is loaded explicitly we keep a consistent
# extension so that the test script is portable for TestExpectedOutput
C_LIBNAME := liburand.so
C_OBJECTS := $(subst $(SRC),$(OBJ),$(C_SOURCES:.c=.o))
C_LIB := $(OBJ)/$(C_LIBNAME)


INCLUDE_DIR := $(NATIVE_PROJECT)/include

all: $(C_LIB)

$(C_LIB): $(OBJ) $(C_OBJECTS)
	$(DYLIB_LD) $(DYLIB_LDFLAGS)  -o $(C_LIB) $(C_OBJECTS)

$(OBJ):
	mkdir -p $(OBJ)

$(OBJ)/%.o: $(SRC)/%.c
	$(CC) $(CFLAGS) -I$(INCLUDE_DIR) -c $< -o $@

clean:
	rm -rf $(OBJ)
