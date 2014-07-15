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

ifneq ($(shell uname), Darwin)
OS_DIR=linux
LIB_DERIVED=lib/$(OS_DIR)/libRDerived.so 
else
OS_DIR=darwin
LIB_DERIVED=lib/$(OS_DIR)/libRDerived.dylib
endif

.PHONY: all clean cleanlib cleanobj force

SRC = src
OBJ = lib/$(OS_DIR)

C_SOURCES := $(wildcard $(SRC)/*.c)
F_SOURCES := $(wildcard $(SRC)/*.f)

C_OBJECTS := $(subst $(SRC),$(OBJ),$(C_SOURCES:.c=.o))
F_OBJECTS := $(subst $(SRC),$(OBJ),$(F_SOURCES:.f=.o))


all:
	@echo $(LIB_DERIVED) is up to date

force: $(LIB_DERIVED)

$(LIB_DERIVED): $(C_OBJECTS) $(F_OBJECTS)
ifneq ($(shell uname), Darwin)
	gcc -fPIC -shared -o ./lib/linux/libRDerived.so $(C_OBJECTS) $(F_OBJECTS)
else
	gcc -dynamiclib -undefined dynamic_lookup -o ./lib/darwin/libRDerived.dylib $(C_OBJECTS) $(F_OBJECTS)
endif

$(OBJ)/%.o: $(SRC)/%.c
	gcc -fPIC -O2 -c $< -o $@

$(OBJ)/%.o: $(SRC)/%.f
	gfortran -fPIC -O2 -c $< -o $@


cleanlib:
	rm -f ./lib/$(OS_DIR)/libRDerived.*

clean: cleanobj

cleanobj:
	rm -f ./lib/$(OS_DIR)/*.o
