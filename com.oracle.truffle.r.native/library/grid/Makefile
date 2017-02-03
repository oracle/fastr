#
# Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

.PHONY: cleanpkg

OBJ = lib

GNUR_C_FILES := gpar.c just.c layout.c matrix.c register.c unit.c util.c viewport.c

GNUR_GRID = $(addprefix $(GNUR_HOME)/src/library/grid/src/, grid.c)
GRID_OBJECT = $(addprefix $(OBJ)/, grid.o)

GNUR_GRID_STATE = $(addprefix $(GNUR_HOME)/src/library/grid/src/, state.c)
GRID_STATE_OBJECT = $(addprefix $(OBJ)/, state.o)

GNUR_C_OBJECTS := $(addprefix $(OBJ)/, $(GNUR_C_FILES:.c=.o)) $(GRID_OBJECT) $(GRID_STATE_OBJECT)
LIB_PKG_PRE = $(GRID_OBJECT) $(GRID_STATE_OBJECT)
CLEAN_PKG := cleanpkg

# This is necessary so that #include "grid.h" works
PKG_INCLUDES = -I $(GNUR_SRC) 

include ../lib.mk

# Why is this necessary? Because if grid.c and state.c have been created by editing, 
# lib.mk will include them in C_OBJECTS but they're already in GNUR_C_OBJECTS (uncreated case)
C_OBJECTS := $(filter-out $(GRID_OBJECT) $(GRID_STATE_OBJECT), $(C_OBJECTS))

$(C_OBJECTS): | $(OBJ)

$(SRC)/grid.c: $(GNUR_GRID) src/sed_grid
	sed -f src/sed_grid $(GNUR_GRID) > src/grid.c

$(SRC)/state.c: $(GNUR_GRID_STATE) src/sed_state
	sed -f src/sed_state $(GNUR_GRID_STATE) > src/state.c

# obj files from c.o.t.r.n/library/grid/src are handled in lib.mk
# obj files from gnur to c.o.t.r.n/library/grid/lib are handled here
$(OBJ)/%.o: $(GNUR_SRC)/%.c
	$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

cleanpkg:
	rm -f $(SRC)/grid.c $(SRC)/state.c
