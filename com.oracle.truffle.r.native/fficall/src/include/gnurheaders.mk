#
# Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

# headers that we refer to indirectly (allows version/location independence in source)
GNUR_CONFIG_H := $(GNUR_HOME)/src/include/config.h
GNUR_CONTOUR_COMMON_H := $(GNUR_HOME)/src/main/contour-common.h
GNUR_GRAPHICS_H := $(GNUR_HOME)/src/include/Graphics.h
GNUR_GRAPHICSBASE_H := $(GNUR_HOME)/src/include/GraphicsBase.h
GNUR_FILEIO_H := $(GNUR_HOME)/src/include/Fileio.h
GNUR_RGRAPHICS_H := $(GNUR_HOME)/src/include/Rgraphics.h
GNUR_INTERNAL_H := $(GNUR_HOME)/src/include/Internal.h
GNUR_NMATH_H := $(GNUR_HOME)/src/nmath/nmath.h
GNUR_PRINT_H := $(GNUR_HOME)/src/include/Print.h
GNUR_RLOCALE_H := $(GNUR_HOME)/src/include/rlocale.h

GNUR_HEADER_DEFS := -DGNUR_GRAPHICS_H=\"$(GNUR_GRAPHICS_H)\" -DGNUR_GRAPHICSBASE_H=\"$(GNUR_GRAPHICSBASE_H)\" \
    -DGNUR_RGRAPHICS_H=\"$(GNUR_RGRAPHICS_H)\" -DGNUR_INTERNAL_H=\"$(GNUR_INTERNAL_H)\" \
	-DGNUR_NMATH_H=\"$(GNUR_NMATH_H)\" -DGNUR_PRINT_H=\"$(GNUR_PRINT_H)\" -DGNUR_RLOCALE_H=\"$(GNUR_RLOCALE_H)\" \
	-DGNUR_FILEIO_H=\"$(GNUR_FILEIO_H)\" -DGNUR_CONFIG_H=\"$(GNUR_CONFIG_H)\" \
	-DGNUR_CONTOUR_COMMON_H=\"$(GNUR_CONTOUR_COMMON_H)\"
