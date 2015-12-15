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
    TOPDIR = $(abspath ../../..)
endif

ifneq ($(MAKECMDGOALS),clean)
include $(TOPDIR)/platform.mk
endif

.PHONY: all clean links

OBJ = ../../lib

COMMON = ../common
C_COMMON_SOURCES := $(notdir $(wildcard $(COMMON)/*.c))
#$(info C_COMMON_SOURCE=$(C_COMMON_SOURCES))

# if link was already set, C_COMMON_SOURCES will be included twice without following filter
C_LOCAL_SOURCES := $(filter-out  $(C_COMMON_SOURCES),$(wildcard *.c))
#$(info C_LOCAL_SOURCES=$(C_LOCAL_SOURCES))
C_HDRS := $(wildcard *.h)
C_LIBNAME := librfficall$(DYLIB_EXT)
C_LIB := $(TOPDIR)/../lib/$(C_LIBNAME)

C_SOURCES = $(C_LOCAL_SOURCES) $(C_COMMON_SOURCES)
#$(info C_SOURCES=$(C_SOURCES))
C_OBJECTS := $(patsubst %.c,$(OBJ)/%.o,$(C_SOURCES))
#$(info C_OBJECTS=$(C_OBJECTS))

F_SOURCES := $(wildcard *.f)
F_OBJECTS := $(patsubst %.f,$(OBJ)/%.o,$(F_SOURCES))

JNI_INCLUDES = -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/$(JDK_OS_DIR)
FFI_INCLUDES = -I$(TOPDIR)/include -I$(TOPDIR)/gnur/R-3.1.3/src/include -I$(TOPDIR)/include/R_ext

# The need for thhis should be reviewed
GNUR_INCLUDES = -I$(TOPDIR)/gnur/R-3.1.3/src/include -I$(TOPDIR)/gnur/R-3.1.3/src/nmath

INCLUDES := $(JNI_INCLUDES) $(FFI_INCLUDES) $(GNUR_INCLUDES)

all: Makefile $(C_COMMON_SOURCES) $(C_LIB) 

links:
	$(foreach file,$(C_COMMON_SOURCES),ln -sf $(COMMON)/$(file) $(file);)	

$(C_LIB): $(OBJ) $(C_OBJECTS) $(F_OBJECTS)
	$(DYLIB_LD) $(DYLIB_LDFLAGS) -o $(C_LIB) $(C_OBJECTS) $(F_OBJECTS)

$(OBJ):
	mkdir -p $(OBJ)

$(OBJ)/%.o: %.c $(TOPDIR)/include/Rinternals.h $(C_HDRS)
	$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@
	
$(OBJ)/%.E: %.c $(TOPDIR)/include/Rinternals.h
	$(CC) -E $(CFLAGS) $(INCLUDES) -c $< > $@

$(OBJ)/%.o: %.f
	$(F77) $(FFLAGS) $(FPICFLAGS) -c $< -o $@

clean:
	rm -rf $(OBJ) $(C_LIB)
	$(foreach file,$(C_COMMON_SOURCES),rm -f $(file);)	
