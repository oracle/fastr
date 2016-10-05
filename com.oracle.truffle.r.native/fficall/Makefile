#
# Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

# Builds libR

ifneq (,$(wildcard $(TOPDIR)/platform.mk))
include $(TOPDIR)/platform.mk
else
ifneq ($(MAKECMDGOALS),clean)
$(error no platform.mk available)
endif
endif

.PHONY: all clean objs

R_LIBNAME := libR$(DYLIB_EXT)
R_LIB := $(FASTR_LIB_DIR)/$(R_LIBNAME)
BOOTJNI_LIBNAME := libjniboot$(DYLIB_EXT)
BOOTJNI_LIB := $(FASTR_LIB_DIR)/$(BOOTJNI_LIBNAME)

ifeq ($(OS_NAME), Darwin)
VERSION_FLAGS := -current_version $(R_VERSION) -compatibility_version $(R_VERSION)
endif

BLAS_TARGET := $(FASTR_LIB_DIR)/libRblas$(DYLIB_EXT)
LAPACK_TARGET := $(FASTR_LIB_DIR)/libRlapack$(DYLIB_EXT)

all: $(R_LIB) $(BOOTJNI_LIB)

$(R_LIB): objs 
ifeq ($(OS_NAME),Darwin)
	$(DYLIB_LD) $(DYLIB_LDFLAGS) -Wl,-rpath,$(FASTR_LIB_DIR) -o $(R_LIB) $(wildcard lib/*.o) -L$(FASTR_LIB_DIR) -lRblas -lRlapack  -lpcre -lz $(VERSION_FLAGS)
	install_name_tool -change libRblas.dylib @rpath/libRblas.dylib $(R_LIB)
	install_name_tool -change libRlapack.dylib @rpath/libRlapack.dylib $(R_LIB)
else
	$(DYLIB_LD) $(DYLIB_LDFLAGS) -Wl,-rpath,$(FASTR_LIB_DIR) -o $(R_LIB) $(wildcard lib/*.o) -L$(FASTR_LIB_DIR) -lRblas -lRlapack  -lpcre -lz
endif

objs:
	$(MAKE) -C src/common all
	$(MAKE) -C src/jni all

$(BOOTJNI_LIB): bootobjs
	$(DYLIB_LD) $(DYLIB_LDFLAGS) -o $(BOOTJNI_LIB) src/jniboot/jniboot.o $(VERSION_FLAGS)

bootobjs:
	$(MAKE) -C src/jniboot all

clean:
	$(MAKE) -C src/common clean
	$(MAKE) -C src/jni clean
	rm -rf $(R_LIB)
	
