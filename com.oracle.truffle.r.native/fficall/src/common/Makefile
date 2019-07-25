#
# Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 3 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 3 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 3 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#

# This builds the GNUR files that are compiled directly, and local overrides

ifneq ($(MAKECMDGOALS),clean)
include $(FASTR_NATIVE_DIR)/platform.mk

endif

.PHONY: all clean

# location of compiled code (.o files)
OBJ = ../../lib/common

GNUR_APPL_SRC = $(GNUR_HOME)/src/appl
GNUR_APPL_F_FILES := $(notdir $(wildcard $(GNUR_APPL_SRC)/d*.f))
GNUR_APPL_C_FILES := $(notdir $(wildcard $(GNUR_APPL_SRC)/*.c))

GNUR_MAIN_C_FILES = errors.c colors.c complex.c devices.c engine.c format.c graphics.c plot.c plot3d.c plotmath.c rlocale.c sort.c duplicate.c
GNUR_MAIN_SRC = $(GNUR_HOME)/src/main

GNUR_UNIX_C_FILES = sys-std.c
GNUR_UNIX_SRC = $(GNUR_HOME)/src/unix

GNUR_C_OBJECTS := $(addprefix $(OBJ)/, $(GNUR_APPL_C_FILES:.c=.o) $(GNUR_MAIN_C_FILES:.c=.o) $(GNUR_UNIX_C_FILES:.c=.o))
GNUR_F_OBJECTS := $(addprefix $(OBJ)/, $(GNUR_APPL_F_FILES:.f=.o))

C_SOURCES = $(wildcard *.c)
C_OBJECTS := $(patsubst %.c,$(OBJ)/%.o,$(C_SOURCES))

F_SOURCES = $(wildcard *.f)
F_OBJECTS := $(patsubst %.f,$(OBJ)/%.o,$(F_SOURCES))

JNI_INCLUDES = -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/$(JDK_OS_DIR)
FFI_INCLUDES = -I$(FASTR_NATIVE_DIR)/include -I$(FASTR_NATIVE_DIR)/include/R_ext
INCLUDES := -I $(abspath ../include) $(JNI_INCLUDES) $(FFI_INCLUDES)

include ../include/gnurheaders.mk

ifeq ($(OS_NAME), SunOS)
    SUPPRESS_WARNINGS :=
else
    SUPPRESS_WARNINGS := -Wno-int-conversion -Wno-implicit-function-declaration
endif

all: Makefile $(C_OBJECTS) $(F_OBJECTS) $(GNUR_C_OBJECTS) $(GNUR_F_OBJECTS)

$(C_OBJECTS): | $(OBJ)

$(GNUR_C_OBJECTS): | $(OBJ)

$(GNUR_F_OBJECTS): | $(OBJ)

# Remove the comment to debug which header files are used where
# CFLAGS := $(CFLAGS) -H

$(OBJ):
	mkdir -p $(OBJ)

$(OBJ)/%.o: $(GNUR_APPL_SRC)/%.c
	$(CC) $(CFLAGS) $(INCLUDES) $(GNUR_HEADER_DEFS) $(SUPPRESS_WARNINGS) -c $< -o $@

$(OBJ)/%.o: $(GNUR_MAIN_SRC)/%.c
	$(CC) $(CFLAGS) $(INCLUDES) $(GNUR_HEADER_DEFS) $(SUPPRESS_WARNINGS) -c $< -o $@

$(OBJ)/%.o: $(GNUR_UNIX_SRC)/%.c
	$(CC) $(CFLAGS) -DHAVE_CONFIG_H $(INCLUDES) $(GNUR_HEADER_DEFS) $(SUPPRESS_WARNINGS) -c $< -o $@

$(OBJ)/%.o: %.c $(FASTR_NATIVE_DIR)/include/Rinternals.h
	$(CC) $(CFLAGS) $(INCLUDES) $(GNUR_HEADER_DEFS) $(SUPPRESS_WARNINGS) -c $< -o $@

$(OBJ)/%.o: %.f
	$(FC) $(FFLAGS) $(FPICFLAGS) -c $< -o $@

$(OBJ)/%.o: $(GNUR_APPL_SRC)/%.f
	$(FC) $(FFLAGS) $(FPICFLAGS) -c $< -o $@

clean:
	rm -rf $(OBJ)

