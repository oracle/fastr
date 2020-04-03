#
# Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
OBJ = ../../lib/JavaGD

C_SOURCES = $(wildcard *.c)
C_OBJECTS := $(patsubst %.c,$(OBJ)/%.o,$(C_SOURCES))

FFI_INCLUDES = -I$(FASTR_NATIVE_DIR)/include -I$(FASTR_NATIVE_DIR)/include/R_ext
INCLUDES := -I $(abspath ../include) $(JNI_INCLUDES) $(FFI_INCLUDES)

include ../include/gnurheaders.mk

all: Makefile $(C_OBJECTS)

$(C_OBJECTS): | $(OBJ)

$(GNUR_C_OBJECTS): | $(OBJ)

$(OBJ):
	mkdir -p $(OBJ)

$(OBJ)/%.o: %.c
	$(CC) $(CFLAGS) $(INCLUDES) $(GNUR_HEADER_DEFS) $(SUPPRESS_WARNINGS) -c $< -o $@

clean:
	rm -rf $(OBJ)

