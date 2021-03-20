#
# Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

# For grid, we need some more patches including patches in the R code (revert
# the "new style" units internal improvement that does not work with our
# re-implementation of grid internals).
#
# Therefore we do not reuse the grid installed in GNU-R, but we build grid
# from scratch from our own patched version that lives next to this Makefile
# With that grid distribution, we continue like with other packages replacing
# the shared library with one that we build from our sources (from "src" next
# to this Makefile)

# As a temporary solution, this is copy&paste of ../lib.mk + the necessary changes

ifneq ($(MAKECMDGOALS),clean)
include $(TOPDIR)/platform.mk
endif

.PHONY: all clean cleanlib cleanobj force libr libcommon

# This is necessary so that #include "grid.h" works
PKG_INCLUDES = -I src

PKG = $(PACKAGE)

SRC = src
OBJ = lib

C_SOURCES := $(wildcard $(SRC)/*.c)
C_SOURCES := $(filter-out $(C_SOURCES_EXCLUDED), $(C_SOURCES))

C_OBJECTS := $(subst $(SRC)/,$(OBJ)/,$(C_SOURCES:.c=.o))

F_SOURCES := $(wildcard $(SRC)/*.f)
F_SOURCES := $(filter-out $(F_SOURCES_EXCLUDED), $(F_SOURCES))

F_OBJECTS := $(subst $(SRC)/,$(OBJ)/,$(F_SOURCES:.f=.o))

H_SOURCES := $(wildcard $(SRC)/*.h)

LIBDIR := $(OBJ)

# packages seem to use .so even on Mac OS X and no "lib"
LIB_PKG := $(OBJ)/$(PKG).so

JNI_INCLUDES = -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/$(JDK_OS_DIR)
FFI_INCLUDES = -I$(TOPDIR)/include -I$(TOPDIR)/include/R_ext

INCLUDES := $(JNI_INCLUDES) $(FFI_INCLUDES) $(PKG_INCLUDES)

PKGDIR := $(FASTR_LIBRARY_DIR)/$(PKG)

F2C := -lf2c

SUPPRESS_WARNINGS := -Wno-int-conversion -Wno-implicit-function-declaration

ifeq ($(NO_LIBRARY),)
all: $(LIB_PKG_PRE) libcommon $(LIB_PKG) $(LIB_PKG_POST) 
else
all: $(LIB_PKG_PRE) libcommon $(LIB_PKG_POST)
endif

libcommon: $(PKGDIR)

# Copy the package's artifacts to FASTR_LIBRARY_DIR. The binaries will be replaced at a later stage.
$(PKGDIR): grid
	$(GNUR_HOME_BINARY)/bin/R CMD INSTALL --library=$(FASTR_LIBRARY_DIR) grid
	touch $(PKGDIR)

$(C_OBJECTS): | $(OBJ)

$(F_OBJECTS): | $(OBJ)

$(OBJ):
	mkdir -p $(OBJ)

ifeq ($(OS_NAME),Darwin)
  RPATH_OPT = -Wl,-undefined,dynamic_lookup
else
  RPATH = $$ORIGIN/../../../lib/
  RPATH_OPT = -Wl,-rpath='$(RPATH)'
endif

$(LIB_PKG): $(C_OBJECTS) $(F_OBJECTS) $(PKGDIR) $(XTRA_C_OBJECTS)
	mkdir -p $(LIBDIR)
	$(DYLIB_LD) $(DYLIB_LDFLAGS) -L$(FASTR_LIB_DIR) $(RPATH_OPT) -o $(LIB_PKG) $(C_OBJECTS) $(F_OBJECTS) $(XTRA_C_OBJECTS) $(PKG_LIBS) -lR -lz $(F2C) -lRlapack
	mkdir -p $(FASTR_LIBRARY_DIR)/$(PKG)/libs
	cp "$(LIB_PKG)" $(FASTR_LIBRARY_DIR)/$(PKG)/libs
ifeq ($(OS_NAME),Darwin)
	install_name_tool -id @rpath/../library/$(PKG)/libs/$(PKG).so $(FASTR_LIBRARY_DIR)/$(PKG)/libs/$(PKG).so
endif

$(OBJ)/%.o: $(SRC)/%.c $(H_SOURCES)
	$(CC) $(CFLAGS) $(INCLUDES) $(XTRA_C_OPTS) -c $< -o $@

$(OBJ)/%.o: $(SRC)/%.f
	$(FC) $(FFLAGS) $(FPICFLAGS) $(XTRA_F_OPTS) -c $< -o $@

clean: $(CLEAN_PKG)
	rm -rf $(LIBDIR)/*
	rm -rf $(FASTR_LIBRARY_DIR)/$(PKG)
