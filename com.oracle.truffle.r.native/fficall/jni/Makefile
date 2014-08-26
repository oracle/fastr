#
# Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
    TOPDIR = $(abspath ../..)
endif

$(warning USE_RINTERNALS=$(USE_R_INTERNALS))
include $(TOPDIR)/platform.mk

.PHONY: all clean

OBJ = lib
SRC = src
C_SOURCES := $(wildcard $(SRC)/*.c)
C_LIBNAME := libcall.$(SHARED_EXT)
C_OBJECTS := $(subst $(SRC),$(OBJ),$(C_SOURCES:.c=.o))
C_LIB := $(OBJ)/$(C_LIBNAME)

JNI_INCLUDES = -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/$(OS_DIR)
FFI_INCLUDES = -I$(TOPDIR)/include/jni

INCLUDES := $(JNI_INCLUDES) $(FFI_INCLUDES)

all: $(C_LIB)

$(C_LIB): $(OBJ) $(C_OBJECTS)
	$(CC) $(LDFLAGS) -o $(C_LIB) $(C_OBJECTS)

$(OBJ):
	mkdir -p $(OBJ)

$(OBJ)/%.o: $(SRC)/%.c $(TOPDIR)/include/jni/Rinternals.h
	$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

clean:
	rm -rf $(OBJ)
