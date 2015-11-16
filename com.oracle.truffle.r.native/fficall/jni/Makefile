#
# Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

ifneq ($(MAKECMDGOALS),clean)
include $(TOPDIR)/platform.mk
endif

.PHONY: all clean common_links

OBJ = ../lib
SRC = src
C_LOCAL_SOURCES := $(wildcard $(SRC)/*.c)
C_HDRS := $(wildcard $(SRC)/*.h)
C_LIBNAME := librfficall$(DYLIB_EXT)
C_LIB := $(TOPDIR)/../lib/$(C_LIBNAME)

COMMON = ../common
C_COMMON_SOURCES := $(notdir $(wildcard $(COMMON)/src/*.c))

C_SOURCES = $(C_LOCAL_SOURCES) $($(C_COMMON_SOURCES)
C_OBJECTS := $(subst $(SRC),$(OBJ),$(C_SOURCES:.c=.o))
$(info C_SOURCES=$(C_SOURCES))
$(info C_COMMON_SOURCE=$(C_COMMON_SOURCES))
$(info C_OBJECTS=$(C_OBJECTS))

JNI_INCLUDES = -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/$(JDK_OS_DIR)
FFI_INCLUDES = -I$(TOPDIR)/include -I$(TOPDIR)/gnur/R-3.1.3/src/include -I$(TOPDIR)/include/R_ext

INCLUDES := $(JNI_INCLUDES) $(FFI_INCLUDES)

all: Makefile common_links $(C_LIB) 

common_links:
	$(foreach file,$(C_COMMON_SOURCES),ln -sf ../$(COMMON)/$(file) $(SRC)/$(file);)	

$(C_LIB): $(OBJ) $(C_OBJECTS)
	$(DYLIB_LD) $(DYLIB_LDFLAGS) -o $(C_LIB) $(C_OBJECTS)

$(OBJ):
	mkdir -p $(OBJ)

$(OBJ)/%.o: $(SRC)/%.c $(TOPDIR)/include/Rinternals.h $(C_HDRS)
	$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@
	
$(OBJ)/%.E: $(SRC)/%.c $(TOPDIR)/include/Rinternals.h
	$(CC) -E $(CFLAGS) $(INCLUDES) -c $< > $@

clean:
	rm -rf $(OBJ) $(C_LIB)
	$(foreach file,$(C_COMMON_SOURCES),rm -f $(SRC)/$(file);)	
