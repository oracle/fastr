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

# Builds libR

ifneq (,$(wildcard $(TOPDIR)/platform.mk))
include $(TOPDIR)/platform.mk
else
ifneq ($(MAKECMDGOALS),clean)
$(error no platform.mk available)
endif
endif

.PHONY: all clean

C_LIBNAME := libR$(DYLIB_EXT)
C_LIB := $(FASTR_LIB_DIR)/$(C_LIBNAME)
R_LIBNAME := libR$(DYLIB_EXT)
R_LIB := $(FASTR_LIB_DIR)/$(R_LIBNAME)
JNIBOOT_LIBNAME := libjniboot$(DYLIB_EXT)
JNIBOOT_LIB := $(FASTR_LIB_DIR)/$(JNIBOOT_LIBNAME)

FASTR_COMPILERS_DIR := $(FASTR_R_HOME)/mx.fastr/compilers
HAVE_SULONG := $(shell $(FASTR_COMPILERS_DIR)/have_sulong)

ifeq ($(OS_NAME), Darwin)
VERSION_FLAGS := -current_version $(R_VERSION) -compatibility_version $(R_VERSION)
endif

BLAS_TARGET := $(FASTR_LIB_DIR)/libRblas$(DYLIB_EXT)
LAPACK_TARGET := $(FASTR_LIB_DIR)/libRlapack$(DYLIB_EXT)

all: $(R_LIB) $(JNIBOOT_LIB)

# use sentinels to avoid (usually unnecessary) rebuilds.
# N.B. if things change in the subdirs, a clean must be invoked
# to remove the sentinels

$(R_LIB): jni.done 
ifeq ($(OS_NAME),Darwin)
	$(DYLIB_LD) $(DYLIB_LDFLAGS) -Wl,-rpath,@loader_path/ -o $(R_LIB) $(wildcard lib/*.o) -L$(FASTR_LIB_DIR) -lRblas -lRlapack  -lpcre -lz $(VERSION_FLAGS)
	install_name_tool -change libRblas.dylib @rpath/libRblas.dylib $(R_LIB)
	install_name_tool -change libRlapack.dylib @rpath/libRlapack.dylib $(R_LIB)
	install_name_tool -id @rpath/libR.dylib $(R_LIB)
# check if we captured libpcre/libz, rpath those in libR
	mx rupdatelib $(FASTR_LIB_DIR)
else
	$(DYLIB_LD) $(DYLIB_LDFLAGS) $(shell echo $(PKG_LDFLAGS_OVERRIDE)) -Wl,-rpath,'$$ORIGIN' -o $(R_LIB) $(wildcard lib/*.o) -L$(FASTR_LIB_DIR) -lRblas -lRlapack  -lpcre -lz
endif

jni.done:
	$(MAKE) -C src/common all
	$(MAKE) -C src/jni all
ifeq ($(HAVE_SULONG),yes)
	$(MAKE) -C src/truffle all
endif
	touch jni.done

$(JNIBOOT_LIB): jniboot.done
	$(DYLIB_LD) $(DYLIB_LDFLAGS) -o $(JNIBOOT_LIB) src/jniboot/jniboot.o $(VERSION_FLAGS)
ifeq ($(OS_NAME),Darwin)
	install_name_tool -id @rpath/libjniboot.dylib $(JNIBOOT_LIB)
endif

jniboot.done:
	$(MAKE) -C src/jniboot all
	touch jniboot.done

clean:
	$(MAKE) -C src/common clean
	$(MAKE) -C src/jni clean
ifeq ($(HAVE_SULONG),yes)
	$(MAKE) -C src/truffle clean
endif
	rm -rf $(R_LIB)
	rm -rf $(JNIBOOT_LIB)
	rm -rf jni.done jniboot.done

