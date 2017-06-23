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

ifneq ($(MAKECMDGOALS),clean)
include $(TOPDIR)/platform.mk
endif

.PHONY: all clean

# location of compiled code (.o files)
OBJ = ../../lib

C_HDRS := $(wildcard *.h)

LOCAL_C_SOURCES = $(wildcard *.c)
TRUFFLE_COMMON_C_SOURCES := $(wildcard ../truffle_common/*.c)
TRUFFLE_COMMON_H_SOURCES := $(wildcard ../truffle_common/*.h)
C_SOURCES := $(LOCAL_C_SOURCES) $(TRUFFLE_COMMON_C_SOURCES)
#$(info C_SOURCES=$(C_SOURCES))
TRUFFLE_COMMON_C_OBJECTS := $(addprefix $(OBJ)/, $(notdir $(TRUFFLE_COMMON_C_SOURCES:.c=.o)))
LOCAL_C_OBJECTS := $(patsubst %.c,$(OBJ)/%.o,$(LOCAL_C_SOURCES))
C_OBJECTS := $(LOCAL_C_OBJECTS) $(TRUFFLE_COMMON_C_OBJECTS)
#$(info C_OBJECTS=$(C_OBJECTS))

FFI_INCLUDES = -I$(TOPDIR)/include -I$(TOPDIR)/include/R_ext
#NFI_INCLUDES is set in environment (by mx)
LOCAL_INCLUDES = -I . -I $(abspath ../include) -I $(abspath ../common)

INCLUDES := $(LOCAL_INCLUDES) $(FFI_INCLUDES) $(NFI_INCLUDES)

# uncomment to see exactly where headers are being read from
#CFLAGS := $(CFLAGS) -H

all: Makefile $(C_OBJECTS) 

$(C_OBJECTS): | $(OBJ)

$(OBJ):
	mkdir -p $(OBJ)

$(OBJ)/%.o: %.c $(TOPDIR)/include/Rinternals.h rffiutils.h $(TRUFFLE_COMMON_H_SOURCES) ../common/rffi_upcallsindex.h $(C_HDRS)
	$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

$(OBJ)/%.o: ../truffle_common/%.c $(TOPDIR)/include/Rinternals.h $(TRUFFLE_COMMON_H_SOURCES) ../common/rffi_upcallsindex.h $(C_HDRS)
	$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

# for debugging, to see what's really being compiled
$(OBJ)/%.E: %.c $(TOPDIR)/include/Rinternals.h
	$(CC) -E $(CFLAGS) $(INCLUDES) -c $< > $@

$(OBJ)/%.E: ../truffle_common/%.c $(TOPDIR)/include/Rinternals.h
	$(CC) -E $(CFLAGS) $(INCLUDES) -c $< > $@

clean:
	rm -rf $(OBJ)
