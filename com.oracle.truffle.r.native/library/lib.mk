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

# Common Makefile for creating FastR variants of the default packages
# The common part of the archive (everything except the .so) is created
# by untar'ing the comnressed archive (from GnuR). Then the FastR .so file is created
# and overwrites the default. The libraries are stored in the directory denoted
# FASTR_LIBDIR.

include ../../platform.mk

.PHONY: all clean cleanlib cleanobj force libr libcommon 

PKG = $(PACKAGE)

SRC = src
OBJ = lib/$(OS_DIR)

C_SOURCES := $(wildcard $(SRC)/*.c)

C_OBJECTS := $(subst $(SRC),$(OBJ),$(C_SOURCES:.c=.o))

H_SOURCES := $(wildcard $(SRC)/*.h)

LIBDIR := $(OBJ)

# packages seem to use .so even on Mac OS X and no "lib"
LIB_PKG := $(OBJ)/$(PKG).so

JNI_INCLUDES = -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/$(OS_DIR)
FFI_INCLUDES = -I$(TOPDIR)/include/jni

INCLUDES := $(JNI_INCLUDES) $(FFI_INCLUDES)

PKGDIR := $(FASTR_LIBDIR)/$(PKG)
PKGTAR := $(SRC)/$(PKG).tar.gz

all: libcommon $(LIB_PKG)

libcommon: $(PKGDIR)

$(PKGDIR):
	tar xf $(PKGTAR) -C $(FASTR_LIBDIR)

$(OBJ):
	mkdir -p $(OBJ)

$(LIB_PKG): $(OBJ) $(C_OBJECTS)
	mkdir -p $(LIBDIR)
	$(CC) $(LDFLAGS) -o $(LIB_PKG) $(C_OBJECTS)
	cp $(LIB_PKG) $(FASTR_LIBDIR)/$(PKG)/libs

$(OBJ)/%.o: $(SRC)/%.c  $(H_SOURCES)
	$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

$(OBJ)/%.o: $(SRC)/%.f
	$(FC) $(CFLAGS) -c $< -o $@

clean: cleanobj cleanlib

cleanlib:
	rm -f $(LIB_PKG)

cleanobj:
	rm -f $(LIBDIR)/*.o
	
