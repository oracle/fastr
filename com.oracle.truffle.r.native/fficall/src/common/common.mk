#
# Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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

# This defines the GNUR files that are compiled directly, local overrides, plus -D defines that allow the
# header files that redirect to GnuR versions to be location/version independent. It is included
# by the actual implementation Makefile, e.g.in ../jni

GNUR_APPL_C_FILES = pretty.c
GNUR_APPL_SRC = $(GNUR_HOME)/src/appl

GNUR_MAIN_C_FILES = colors.c devices.c engine.c graphics.c plot.c plot3d.c plotmath.c rlocale.c sort.c
GNUR_MAIN_SRC = $(GNUR_HOME)/src/main

GNUR_C_OBJECTS := $(addprefix $(OBJ)/, $(GNUR_APPL_C_FILES:.c=.o) $(GNUR_MAIN_C_FILES:.c=.o))

# headers that we refer to indirectly (allows version/location independence in source)
GNUR_GRAPHICS_H := $(GNUR_HOME)/src/include/Graphics.h
GNUR_GRAPHICSBASE_H := $(GNUR_HOME)/src/include/GraphicsBase.h
GNUR_RGRAPHICS_H := $(GNUR_HOME)/src/include/Rgraphics.h
GNUR_INTERNAL_H := $(GNUR_HOME)/src/include/Internal.h
GNUR_NMATH_H := $(GNUR_HOME)/src/nmath/nmath.h
GNUR_PRINT_H := $(GNUR_HOME)/src/include/Print.h
GNUR_RLOCALE_H := $(GNUR_HOME)/src/include/rlocale.h

GNUR_HEADER_DEFS := -DGNUR_GRAPHICS_H=\"$(GNUR_GRAPHICS_H)\" -DGNUR_GRAPHICSBASE_H=\"$(GNUR_GRAPHICSBASE_H)\" \
    -DGNUR_RGRAPHICS_H=\"$(GNUR_RGRAPHICS_H)\" -DGNUR_INTERNAL_H=\"$(GNUR_INTERNAL_H)\" \
	-DGNUR_NMATH_H=\"$(GNUR_NMATH_H)\" -DGNUR_PRINT_H=\"$(GNUR_PRINT_H)\" -DGNUR_RLOCALE_H=\"$(GNUR_RLOCALE_H)\"

SUPPRESS_WARNINGS := -Wno-int-conversion -Wno-implicit-function-declaration

$(OBJ)/%.o: $(GNUR_APPL_SRC)/%.c
	$(CC) $(CFLAGS) $(INCLUDES) $(GNUR_HEADER_DEFS) $(SUPPRESS_WARNINGS) -c $< -o $@

$(OBJ)/%.o: $(GNUR_MAIN_SRC)/%.c
	$(CC) $(CFLAGS) $(INCLUDES) $(GNUR_HEADER_DEFS) $(SUPPRESS_WARNINGS) -c $< -o $@
