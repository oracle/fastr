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

#
# This Makefile contains the recipe for creating a modified version of 
# GnuR's libappl, essentially just the Fortran components. Unfortunately
# we can't just copy the GnuR library as it contains some compiled C code
# that refers to GnuR-internal symbols.
#

ifneq ($(MAKECMDGOALS),clean)
include $(TOPDIR)/platform.mk
endif

.PHONY: all clean cleanlib cleanobj force libr

SRC = src
OBJ = lib
LIBDIR = $(abspath $(TOPDIR)/../lib)

C_SOURCES := $(wildcard $(SRC)/*.c)

C_OBJECTS := $(subst $(SRC),$(OBJ),$(C_SOURCES:.c=.o))
F_OBJECTS := $(wildcard $(GNUR_HOME)/src/appl/d*.o)

LIB_APPL := $(LIBDIR)/libappl$(DYLIB_EXT)

all: $(LIB_APPL) libr

$(LIB_APPL): $(C_OBJECTS) $(F_OBJECTS)
	mkdir -p $(LIBDIR)
	$(DYLIB_LD) $(DYLIB_LDFLAGS)  -o $(LIB_APPL) $(C_OBJECTS) $(F_OBJECTS) $(LIBS)

$(C_OBJECTS): | $(OBJ)

$(OBJ):
	mkdir -p $(OBJ)

# On Darwin we need to create a libR.dylib in which xerbla goes 
# as the libRlapack library has a dependency on libR and expecte xerbla to
# be defined there. We could fix this if we cared by not using the GnuR instance of the 
# libRlapack and rebuilding it from scratch.

ifeq ($(OS_NAME), Darwin)
LIB_R := $(LIBDIR)/libR$(DYLIB_EXT)

libr: $(LIB_R)

$(LIB_R): $(OBJ)/xerbla.o
	mkdir -p $(LIBDIR)
	$(DYLIB_LD) $(DYLIB_LDFLAGS) -o $(LIB_R) -current_version $(R_VERSION) -compatibility_version $(R_VERSION) $(OBJ)/xerbla.o

cleanlibr:
	rm -f $(LIB_R)

else
libr: 

cleanlibr:

endif

FFI_INCLUDES = -I$(TOPDIR)/include -I$(TOPDIR)/include/R_ext

$(OBJ)/%.o: $(SRC)/%.c
	$(CC) $(CFLAGS) $(FFI_INCLUDES) -c $< -o $@

cleanlib:
	rm -f $(LIBDIR)/libappl.*

clean: cleanobj cleanlibr

cleanobj:
	rm -f $(OBJ)/*.o
