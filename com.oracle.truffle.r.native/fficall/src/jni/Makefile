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

# Master Makefile for librffi. Includes ../common/common.mk to include common files in the build

ifeq ($(TOPDIR),)
    TOPDIR = $(abspath ../../..)
endif

ifneq ($(MAKECMDGOALS),clean)
include $(TOPDIR)/platform.mk
endif

.PHONY: all clean

# location of compiled code (.o files)
OBJ = ../../lib

COMMON = $(abspath ../common)

include $(COMMON)/common.mk

C_COMMON_SOURCES := $(notdir $(wildcard $(COMMON)/*.c))
$(info C_COMMON_SOURCES=$(C_COMMON_SOURCES))

F_COMMON_SOURCES := $(notdir $(wildcard $(COMMON)/*.f))
F_OBJECTS := $(patsubst %.f,$(OBJ)/%.o,$(F_COMMON_SOURCES))

C_LOCAL_SOURCES := $(wildcard *.c)
$(info C_LOCAL_SOURCES=$(C_LOCAL_SOURCES))

C_HDRS := $(wildcard *.h)
C_LIBNAME := librfficall$(DYLIB_EXT)
C_LIB := $(TOPDIR)/../lib/$(C_LIBNAME)

C_SOURCES = $(C_LOCAL_SOURCES) $(C_COMMON_SOURCES)
C_OBJECTS := $(patsubst %.c,$(OBJ)/%.o,$(C_LOCAL_SOURCES)) $(patsubst %.c,$(OBJ)/%.o,$(C_COMMON_SOURCES))
$(info C_OBJECTS=$(C_OBJECTS))

JNI_INCLUDES = -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/$(JDK_OS_DIR)
LOCAL_INCLUDES = -I. -I$(COMMON) 
FFI_INCLUDES = -I$(TOPDIR)/include -I$(TOPDIR)/include/R_ext

INCLUDES := $(LOCAL_INCLUDES) $(JNI_INCLUDES) $(FFI_INCLUDES)

# uncomment to see exactly where headers are being read from
#CFLAGS := $(CFLAGS) -H

all: Makefile $(C_LIB) 

$(C_LIB): $(OBJ) $(C_OBJECTS) $(F_OBJECTS) $(GNUR_C_OBJECTS)
	$(DYLIB_LD) $(DYLIB_LDFLAGS) -o $(C_LIB) $(C_OBJECTS) $(F_OBJECTS) $(GNUR_C_OBJECTS)

$(OBJ):
	mkdir -p $(OBJ)

# rule for sources in thgis directory (no connection to GnuR)
$(OBJ)/%.o: %.c $(TOPDIR)/include/Rinternals.h $(C_HDRS)
	$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

# rule for common, needs access to GnuR, suppress warnings
$(OBJ)/%.o: $(COMMON)/%.c $(TOPDIR)/include/Rinternals.h $(C_HDRS)
	$(CC) $(CFLAGS) -DGNUR_HOME=$(GNUR_HOME) $(INCLUDES) $(GNUR_HEADER_DEFS) $(SUPPRESS_WARNINGS) -c $< -o $@

$(OBJ)/%.o: $(COMMON)/%.f
	$(F77) $(FFLAGS) $(FPICFLAGS) -c $< -o $@

# for debugging, to see what's really being compiled
$(OBJ)/%.E: %.c $(TOPDIR)/include/Rinternals.h
	$(CC) -E $(CFLAGS) $(INCLUDES) -c $< > $@

clean:
	rm -rf $(OBJ) $(C_LIB)
