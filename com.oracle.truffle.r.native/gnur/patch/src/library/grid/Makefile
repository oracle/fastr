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

ifneq ($(MAKECMDGOALS),clean)
include $(TOPDIR)/platform.mk
endif

.PHONY: all clean

# Note: FASTR_LIBRARY_DIR is defined by the upper level Makefile that calls us
PKGNAME := grid
PKGDIR := $(FASTR_LIBRARY_DIR)/$(PKGNAME)

$(PKGDIR): $(PKGNAME)
	$(GNUR_HOME_BINARY)/bin/R CMD INSTALL --library=$(FASTR_LIBRARY_DIR) $(PKGNAME)

all: $(PKGDIR)

clean:
	rm -rf $(PKGDIR)
	rm $(PKGNAME)/src/*.o
	rm $(PKGNAME)/src/*.so

# Original version that builds grid from GNU-R:
# This is necessary so that #include "grid.h" works
# PKG_INCLUDES = -I src
#
# include ../lib.mk
