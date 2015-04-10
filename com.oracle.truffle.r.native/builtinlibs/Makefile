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

C_SOURCES := $(wildcard $(SRC)/*.c)

C_OBJECTS := $(subst $(SRC),$(OBJ),$(C_SOURCES:.c=.o))
F_OBJECTS := $(wildcard $(GNUR_DIR)/src/appl/d*.o)

LIBDIR := $(OBJ)
LIB_APPL := $(OBJ)/libappl$(DYLIB_EXT)

all: $(LIB_APPL) libr

$(LIB_APPL): $(C_OBJECTS) $(F_OBJECTS)
	mkdir -p $(LIBDIR)
	$(DYLIB_LD) $(DYLIB_LDFLAGS)  -o $(LIB_APPL) $(C_OBJECTS) $(F_OBJECTS) $(LIBS)

# On Darwin we need to create a dummy libR.dylib
ifeq ($(OS_NAME), Darwin)
LIB_RDUMMY := $(OBJ)/libR$(DYLIB_EXT)

libr: $(LIB_RDUMMY)
$(LIB_RDUMMY): $(OBJ)/rdummy.o
	mkdir -p $(LIBDIR)
	$(DYLIB_LD) $(DYLIB_LDFLAGS) -o $(LIB_RDUMMY) -current_version $(R_VERSION) -compatibility_version $(R_VERSION) $(OBJ)/rdummy.o

cleanlibr:
	rm -f $(LIB_RDUMMY)

else
libr: 

cleanlibr:

endif

$(OBJ)/%.o: $(SRC)/%.c
	$(CC) $(CFLAGS) -c $< -o $@

cleanlib:
	rm -f $(LIBDIR)/libappl.*

clean: cleanobj cleanlibr

cleanobj:
	rm -f $(LIBDIR)/*.o
