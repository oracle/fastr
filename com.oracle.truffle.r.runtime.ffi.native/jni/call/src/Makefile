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

.PHONY: all clean

HOSTOS = $(shell uname)
ifeq ($(HOSTOS),Darwin)
   HOSTOS_LC = darwin
endif
ifeq ($(HOSTOS),Linux)
   HOSTOS_LC = linux
endif

ifeq ($(TOPDIR),)
    TOPDIR = $(abspath ../../..)
endif

BIN = ../bin/$(HOSTOS_LC)
SRC = $(CURDIR)
C_SOURCES := $(wildcard *.c)
ifneq ($(HOSTOS), Darwin)
    C_LIBNAME := lib$(C_SOURCES:.c=.so)
else
    C_LIBNAME := lib$(C_SOURCES:.c=.dylib)
endif
C_OBJECTS := $(BIN)/$(C_SOURCES:.c=.o)
C_LIB := $(BIN)/$(C_LIBNAME)

JNI_INCLUDES = -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/$(HOSTOS_LC)
FFI_INCLUDES = -I$(TOPDIR)/jni/include

INCLUDE_DIR := $(JNI_INCLUDES) $(FFI_INCLUDES)

all: $(C_LIB)

$(C_LIB): $(BIN) $(C_OBJECTS)
ifneq ($HOSTOS, Darwin)
	gcc -fPIC -shared -o $(C_LIB) $(C_OBJECTS)
else
	gcc -dynamiclib -undefined dynamic_lookup -o $(C_LIB) $(C_OBJECTS)
endif

$(BIN):
	mkdir -p $(BIN)

$(BIN)/%.o: %.c
	gcc $(INCLUDE_DIR) -fPIC -O2 -c $< -o $@

clean:
	rm -rf $(BIN)
