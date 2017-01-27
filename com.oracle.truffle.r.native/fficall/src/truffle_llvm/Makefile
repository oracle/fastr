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

# This compiles everything in this directory, plus all the code in ../common,
# including the code referenced in common from GnuR, with the -DFASTR_LLVM flag.
# This creates an object file with no compiled C/Fortan code, just the equivalent LLVM IR
# Since, at present, the resulting shared library (libR) must include both the real and the dummy
# object files, we have to avoid a name clash on the object file,which we achieve by appending
# "_llvm" to the name of the object file. The wrapper compilers use this name to create the
# symbol that is looked up to find the LLVM IR at runtime.

# N.B. -g -O2 (which is the FFLAGS default from platform.mk) is currently suppressed
# due to sulong limitations

ifneq ($(MAKECMDGOALS),clean)
include $(TOPDIR)/platform.mk
endif

.PHONY: all clean

include ../include/gnurheaders.mk

# location of compiled code (.o files)
OBJ = ../../lib

GNUR_APPL_C_FILES = pretty.c interv.c
GNUR_APPL_SRC = $(GNUR_HOME)/src/appl
GNUR_APPL_F_FILES := $(wildcard $(GNUR_APPL_SRC)/d*.f)

GNUR_MAIN_C_FILES = colors.c devices.c engine.c format.c graphics.c plot.c plot3d.c plotmath.c rlocale.c sort.c
GNUR_MAIN_SRC = $(GNUR_HOME)/src/main

GNUR_C_OBJECTS := $(addprefix $(OBJ)/, $(GNUR_APPL_C_FILES:.c=.o) $(GNUR_MAIN_C_FILES:.c=.o))
LLVM_GNUR_C_OBJECTS := $(GNUR_C_OBJECTS:.o=_llvm.o)
#$(info LLVM_GNUR_C_OBJECTS: $(LLVM_GNUR_C_OBJECTS))

GNUR_F_OBJECTS := $(addprefix $(OBJ)/, $(notdir $(GNUR_APPL_F_FILES:.f=.o)))
LLVM_GNUR_F_OBJECTS := $(GNUR_F_OBJECTS:.o=_llvm.o)
#$(info LLVM_GNUR_F_OBJECTS: $(LLVM_GNUR_F_OBJECTS))

C_HDRS := $(wildcard *.h)

LOCAL_C_SOURCES := $(wildcard *.c) 
COMMON_C_SOURCES := $(wildcard ../common/*.c)
C_SOURCES := $(LOCAL_C_SOURCES) $(COMMON_C_SOURCES)
LOCAL_C_OBJECTS := $(addprefix $(OBJ)/, $(LOCAL_C_SOURCES:.c=.o))
COMMON_C_OBJECTS := $(addprefix $(OBJ)/, $(notdir $(COMMON_C_SOURCES:.c=.o)))
C_OBJECTS := $(LOCAL_C_OBJECTS) $(COMMON_C_OBJECTS)
LLVM_C_OBJECTS := $(C_OBJECTS:.o=_llvm.o)
#$(info LLVM_C_OBJECTS=$(LLVM_C_OBJECTS))

SULONG_DIR = $(abspath $(FASTR_R_HOME)/../sulong)

SULONG_INCLUDES = -I$(SULONG_DIR)/include
FFI_INCLUDES = -I$(TOPDIR)/include
LOCAL_INCLUDES = -I . -I $(abspath ../include)

INCLUDES := $(LOCAL_INCLUDES) $(FFI_INCLUDES) $(SULONG_INCLUDES)

CFLAGS := $(CFLAGS) -DFASTR_LLVM
#FFLAGS := $(FFLAGS) -DFASTR_LLVM
FFLAGS := -DFASTR_LLVM

# uncomment to see exactly where headers are being read from
#CFLAGS := $(CFLAGS) -H

all: Makefile $(LLVM_C_OBJECTS) $(LLVM_GNUR_COBJECTS) $(LLVM_GNUR_F_OBJECTS)

$(C_OBJECTS): | $(OBJ)

$(GNUR_C_OBJECTS): | $(OBJ)

$(GNUR_F_OBJECTS): | $(OBJ)

$(OBJ):
	mkdir -p $(OBJ)

$(OBJ)/%_llvm.o: $(GNUR_APPL_SRC)/%.c
	$(CC) $(CFLAGS) $(INCLUDES) $(GNUR_HEADER_DEFS) $(SUPPRESS_WARNINGS) -c $< -o $@

$(OBJ)/%_llvm.o: $(GNUR_MAIN_SRC)/%.c
	$(CC) $(CFLAGS) $(INCLUDES) $(GNUR_HEADER_DEFS) $(SUPPRESS_WARNINGS) -c $< -o $@

$(OBJ)/%_llvm.o: %.c $(FASTR_NATIVE_DIR)/include/Rinternals.h rffiutils.h
	$(CC) $(CFLAGS) $(INCLUDES) $(GNUR_HEADER_DEFS) -I../variable_defs $(SUPPRESS_WARNINGS) -c $< -o $@

$(OBJ)/%_llvm.o: ../common/%.c $(FASTR_NATIVE_DIR)/include/Rinternals.h
	$(CC) $(CFLAGS) $(INCLUDES) $(GNUR_HEADER_DEFS) $(SUPPRESS_WARNINGS) -c $< -o $@

$(OBJ)/%_llvm.o: $(GNUR_APPL_SRC)/%.f
	$(F77) $(FFLAGS) $(FPICFLAGS) -c $< -o $@

clean:
	rm -rf $(OBJ)
