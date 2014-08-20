#
# Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
# This Makefile contains the recipe for creating libRDerived. However, it is distributed
# as a "source" in the repository file to avoid users having to have gfortran installed.
# Use "make cleanlib; make force" to force a rebuild.
#

OSNAME := $(shell uname)

ifeq ($(OSNAME), Linux)
  OS_DIR      := linux
  CC          := gcc
  FC          := gfortran
  CFLAGS      := -fPIC -O2
  LDFLAGS     := -fPIC -shared
  SHARED_EXT  := so
  LIBS        := -lgfortran
else ifeq ($(OSNAME), SunOS)
  OS_DIR      := sunos
  CC          := cc
  FC          := f90
  CFLAGS      := -m64 -O -xcode=pic13
  LDFLAGS     := -G -m64
  SHARED_EXT  := so
else ifeq ($(OSNAME), Darwin)
  OS_DIR      := darwin
  CC          := gcc
  FC          := gfortran
  CFLAGS      := -fPIC -O2
  LDFLAGS     := -dynamiclib -undefined dynamic_lookup
  SHARED_EXT  := dylib
else
all:
	@echo "This Makefile does not know how to compile for $(OSNAME)"
	@false
endif

.PHONY: all clean cleanlib cleanobj force

SRC = src
OBJ = lib/$(OS_DIR)

C_SOURCES := $(wildcard $(SRC)/*.c)
F_SOURCES := $(wildcard $(SRC)/*.f)

C_OBJECTS := $(subst $(SRC),$(OBJ),$(C_SOURCES:.c=.o))
F_OBJECTS := $(subst $(SRC),$(OBJ),$(F_SOURCES:.f=.o))

LIBDIR := ./lib/$(OS_DIR)
LIB_DERIVED := lib/$(OS_DIR)/libRDerived.$(SHARED_EXT)

all:
	@echo $(LIB_DERIVED) is up to date

force: $(LIB_DERIVED)
	mkdir -p $(LIBDIR)
# build+install blas and lapack too
#	$(MAKE) -C src/blas
#	cp src/blas/libRblas.$(SHARED_EXT) $(LIBDIR)
#	$(MAKE) -C src/lapack
#	cp src/lapack/libRlapack.$(SHARED_EXT) $(LIBDIR)

$(LIB_DERIVED): $(C_OBJECTS) $(F_OBJECTS)
	$(CC) $(LDFLAGS) -o $(LIBDIR)/libRDerived.$(SHARED_EXT) $(C_OBJECTS) $(F_OBJECTS) $(LIBS)

$(OBJ)/%.o: $(SRC)/%.c
	$(CC) $(CFLAGS) -c $< -o $@

$(OBJ)/%.o: $(SRC)/%.f
	$(FC) $(CFLAGS) -c $< -o $@


cleanlib:
	rm -f $(LIBDIR)/libRDerived.* $(LIBDIR)/libRblas.$(SHARED_EXT) $(LIBDIR)/libRlapack.$(SHARED_EXT)

clean: cleanobj
#	$(MAKE) -C src/blas clean
#	$(MAKE) -C src/lapack clean

cleanobj:
	rm -f $(LIBDIR)/*.o
