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

include ../../platform.mk

.PHONY: all clean cleanlib cleanobj force libr

PKG = tools

SRC = src
OBJ = lib/$(OS_DIR)

C_SOURCES := $(wildcard $(SRC)/*.c)

C_OBJECTS := $(subst $(SRC),$(OBJ),$(C_SOURCES:.c=.o))

LIBDIR := $(OBJ)

# packages seem to use .so even on Mac OS X and no "lib"
LIB_PKG := $(OBJ)/$(PKG).so


all: $(LIB_PKG)

$(OBJ):
	mkdir -p $(OBJ)

$(LIB_PKG): $(OBJ) $(OBJ)/tools.o
	mkdir -p $(LIBDIR)
	$(CC) $(LDFLAGS) -o $(LIB_PKG) $(OBJ)/tools.o

$(OBJ)/%.o: $(SRC)/%.c
	$(CC) $(CFLAGS) -c $< -o $@

$(OBJ)/%.o: $(SRC)/%.f
	$(FC) $(CFLAGS) -c $< -o $@

clean: cleanobj cleanlib

cleanlib:
	rm -f $(LIB_PKG)

cleanobj:
	rm -f $(LIBDIR)/*.o
	
