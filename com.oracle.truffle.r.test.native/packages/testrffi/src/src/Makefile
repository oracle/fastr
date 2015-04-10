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

.PHONY: all clean

C_SOURCES := $(wildcard *.c)
# R uses the .so extension on Mac OS X
C_OBJECTS := $(C_SOURCES:.c=.o)
C_LIB := testrffi.so

NATIVE_PROJECT=$(FASTR_HOME)/com.oracle.truffle.r.native
ifneq ($(MAKECMDGOALS),clean)
include $(NATIVE_PROJECT)/platform.mk
endif

ifeq ($(FASTR_INSTALL), GNUR)
    INCLUDES := -I $(R_HOME)/include
    ifeq ($(OS_NAME), Darwin)
        FRAMEWORKFLAGS := -F${R_HOME}/../.. -framework R
    else
        FRAMEWORKFLAGS :=
    endif
    LDFLAGS :=  $(LDFLAGS) $(FRAMEWORKFLAGS)
else
    INCLUDES := -I $(NATIVE_PROJECT)/include/jni/include -I $(FASTR_JAVA_HOME)/include -I $(FASTR_JAVA_HOME)/include/$(OS_DIR)
endif


all: $(C_LIB)

$(C_LIB): $(C_OBJECTS)
	$(DYLIB_LD) $(DYLIB_LDFLAGS)  -o $(C_LIB) $(C_OBJECTS)

%.o: %.c
	$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

clean:
	rm $(C_LIB) $(C_OBJECTS)

