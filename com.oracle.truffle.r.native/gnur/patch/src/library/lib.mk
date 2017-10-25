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

# Common Makefile for creating FastR variants of the default packages
# The common part of the archive (everything except the .so) is created
# by copying the directory in the GnuR build. Then the FastR .so file is created
# and overwrites the default. The libraries are stored in the directory denoted
# FASTR_LIBRARY_DIR.

# A package that requires special processing before the library is built should
# define LIB_PKG_PRE and for post processing define LIB_PKG_POST in its Makefile.
# If a package-specfic clean is needed it should define CLEAN_PKG. PKG_INCLUDES can be
# used to define package-specific includes. XTRA_C_OBJECTS can be used to specify extra
# object files. PKG_LIBS can specify additional libraries required by the package.
# XTRA_C_OPTS and XTRA_F_OPTS can be used to specify additional C/Fortran compiler options.

# A package uses C_SOURCES_EXCLUDED and F_SOURCES_EXCLUDED to specify excluded C/Fortran 
# sources.

# If no library file is produced, a package sets NO_LIBRARY to 1.

ifneq ($(MAKECMDGOALS),clean)
include $(TOPDIR)/platform.mk
endif

.PHONY: all clean cleanlib cleanobj force libr libcommon 

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

ifeq ($(OS_NAME), SunOS)
    SUPPRESS_WARNINGS :=
else
    SUPPRESS_WARNINGS := -Wno-int-conversion -Wno-implicit-function-declaration
endif

ifeq ($(NO_LIBRARY),)
all: $(LIB_PKG_PRE) libcommon $(LIB_PKG) $(LIB_PKG_POST) 
else
all: $(LIB_PKG_PRE) libcommon $(LIB_PKG_POST)
endif

libcommon: $(PKGDIR)

# Copy the package's artifacts to FASTR_LIBRARY_DIR. The binaries will be replaced at a later stage.
$(PKGDIR): $(GNUR_HOME_BINARY)/library/$(PKG)
	(cd $(GNUR_HOME_BINARY)/library; tar cf - $(PKG)) | (cd $(FASTR_LIBRARY_DIR); tar xf -)
	touch $(PKGDIR)

$(C_OBJECTS): | $(OBJ)

$(F_OBJECTS): | $(OBJ)

$(OBJ):
	mkdir -p $(OBJ)

$(LIB_PKG): $(C_OBJECTS) $(F_OBJECTS) $(PKGDIR) $(XTRA_C_OBJECTS)
	mkdir -p $(LIBDIR)
	$(DYLIB_LD) $(DYLIB_LDFLAGS) -o $(LIB_PKG) $(C_OBJECTS) $(F_OBJECTS) $(XTRA_C_OBJECTS) $(PKG_LIBS)
	mkdir -p $(FASTR_LIBRARY_DIR)/$(PKG)/libs
	cp $(LIB_PKG) $(FASTR_LIBRARY_DIR)/$(PKG)/libs
ifeq ($(OS_NAME),Darwin)
ifneq ($(FASTR_RFFI),llvm)	
	install_name_tool -id @rpath/../library/$(PKG)/libs/$(PKG).so $(FASTR_LIBRARY_DIR)/$(PKG)/libs/$(PKG).so
endif
endif

$(OBJ)/%.o: $(SRC)/%.c $(H_SOURCES)
	$(CC) $(CFLAGS) $(INCLUDES) $(XTRA_C_OPTS) -c $< -o $@

$(OBJ)/%.o: $(SRC)/%.f
	$(F77) $(FFLAGS) $(FPICFLAGS) $(XTRA_F_OPTS) -c $< -o $@

clean: $(CLEAN_PKG)
	rm -rf $(LIBDIR)/*
	rm -rf $(FASTR_LIBRARY_DIR)/$(PKG)

