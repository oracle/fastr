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

ifneq (,$(wildcard $(TOPDIR)/platform.mk))
include $(TOPDIR)/platform.mk
else
ifneq ($(MAKECMDGOALS),clean)
$(error no platform.mk available)
endif
endif

OBJ = lib

include $(TOPDIR)/fficall/src/include/gnurheaders.mk

C_SOURCES_EXCLUDED := src/par-common.c

GNUR_INCLUDES := -I$(TOPDIR)/fficall/src/include
GRAPHICS_INCLUDES := -I$(GNUR_HOME)/src/library/graphics
PKG_INCLUDES := $(GNUR_INCLUDES) $(GNUR_HEADER_DEFS) $(GRAPHICS_INCLUDES)
# Suppress warnings
ifeq ($(OS_NAME), SunOS)
    XTRA_C_OPTS :=
else
	XTRA_C_OPTS := -Wno-int-conversion -Wno-implicit-function-declaration
endif

include ../lib.mk

# plot.c needs to include the GNUR internal Print.h
$(OBJ)/plot.o: src/plot.c
	$(CC) $(CFLAGS) $(GNUR_INCLUDES) $(INCLUDES) $(GNUR_HEADER_DEFS) $(GRAPHICS_INCLUDES) -c $< -o $@
