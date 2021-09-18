#
# Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

# Builds libR
#
# We build all the source files with the given compiler (CC), which may be the Labs LLVM clang that produces embedded bitcode.
#
# In order to support running both backend (NFI and Sulong) in one process, we produce three artifacts here:
#  * libR.so - dummy library, which standard name in standard location that most of the packages will link with
#    - when the native loader/Sulong is loading a package (e.g., stats.so), it loads this dummy libR.so
#  * libRnative.so - library with actual implementation of the R API for NFI
#    - dlopen'ed by FastR into the global space (RTLD_GLOBAL)
#  * libRllvm.so - library with actual implementation of the R API for Sulong
#    - FastR explicitly loads this via Sulong
#
# This is the only situation, where we produce different artifacts for NFI/LLVM. When building pacakges,
# we produce only one so file with embedded bitcode that is supposed to be used for both NFI and LLVM execution
# depending on the user's preference.
#
# The LLVM and NFI builds are done by Makefiles (invoked from here) in their resp. directories: truffle_nfi and truffle_llvm,
# they also pull files from truffle_common. These two builds produce their output (object files) to "lib/nfi" and "lib/llvm"
# directories, which are siblings to this file.
#
# The common files (which implement C functionality typically pulled from GNU-R and independent of FastR) have their own Makefile,
# which also puts the results into "lib/common".
#
# All the Makefiles use $(TOPDIR)/platform.mk which defines CC, CFLAGS, etc.

ifneq (,$(wildcard $(TOPDIR)/platform.mk))
include $(TOPDIR)/platform.mk
else
ifneq ($(MAKECMDGOALS),clean)
$(error no platform.mk available)
endif
endif

.PHONY: all clean

C_LIBNAME := libR$(DYLIB_EXT)
NFI_LIBNAME := libRnative$(DYLIB_EXT)
LLVM_LIBNAME := libRllvm$(DYLIB_EXT)

R_LIB := $(FASTR_LIB_DIR)/$(C_LIBNAME)
NFI_LIB := $(FASTR_LIB_DIR)/$(NFI_LIBNAME)
LLVM_LIB := $(FASTR_LIB_DIR)/$(LLVM_LIBNAME)

ifeq ($(OS_NAME), Darwin)
VERSION_FLAGS := -current_version $(R_VERSION) -compatibility_version $(R_VERSION)
endif

all: $(R_LIB)

# use sentinels to avoid (usually unnecessary) rebuilds.
# N.B. if things change in the subdirs, a clean must be invoked
# to remove the sentinels

F2C := -lf2c

$(R_LIB): fficall.done
ifeq ($(FASTR_RFFI),managed)
	# nop
else
ifeq ($(OS_NAME),Darwin)
	# On Darwin libRnative and libRllvm link to libR so that once they are loaded manually by FastR, libR is loaded
	# Moreover, libR links with libRblas and libRlapack, so those get loaded transitively too
	# Note that packages do not link with libR properly via @rpath, but that is OK since libR will be loaded explicitly by FastR
	$(DYLIB_LD) $(DYLIB_LDFLAGS) $(shell echo $(PKG_LDFLAGS_OVERRIDE)) -Wl,-rpath,@loader_path/ -o $(R_LIB) -L$(FASTR_LIB_DIR) -lRblas -lRlapack $(VERSION_FLAGS)
	$(DYLIB_LD) $(DYLIB_LDFLAGS) $(shell echo $(PKG_LDFLAGS_OVERRIDE)) -Wl,-rpath,@loader_path/ -Wl,-undefined,dynamic_lookup -o $(NFI_LIB) $(wildcard lib/nfi/*.o) $(wildcard lib/common/*.o) $(wildcard lib/JavaGD/*.o) -L$(FASTR_LIB_DIR) -ldl -lR -lpcre2-8 -lz $(F2C) -liconv $(VERSION_FLAGS)
	$(DYLIB_LD) $(DYLIB_LDFLAGS) $(shell echo $(PKG_LDFLAGS_OVERRIDE)) -Wl,-rpath,@loader_path/ -Wl,-undefined,dynamic_lookup -o $(LLVM_LIB) $(wildcard lib/llvm/*.o) $(wildcard lib/common/*.o) $(wildcard lib/JavaGD/*.o) -L$(FASTR_LIB_DIR) -L$(LLVM_LIBS_DIR) -lpolyglot-mock -ldl -lR -lpcre2-8 -lz $(F2C) -liconv $(VERSION_FLAGS)

	# make the libraries that we just created relocable
	install_name_tool -id @rpath/libR.dylib $(R_LIB)
	install_name_tool -id @rpath/$(NFI_LIBNAME) $(NFI_LIB)
	install_name_tool -id @rpath/$(LLVM_LIBNAME) $(LLVM_LIB)

	# libRblas/lapack should resolve any "@path" placeholder in their dependencies list to the directory where they are located
	install_name_tool -add_rpath @loader_path/ $(FASTR_LIB_DIR)/libRblas.dylib
	install_name_tool -add_rpath @loader_path/ $(FASTR_LIB_DIR)/libRlapack.dylib

	# fixup the dependency libR -> libRblas/lapack
	# we already configured libR to use its location for "@rpath" placeholder when building it,
	# now we need to switch the libRblas/lapack dependencies from absolute paths to "@rpath/..." paths
	install_name_tool -change libRblas.dylib @rpath/libRblas.dylib $(R_LIB)
	install_name_tool -change libRlapack.dylib @rpath/libRlapack.dylib $(R_LIB)
	install_name_tool -change libf2c.so @rpath/libf2c$(DYLIB_EXT) $(R_LIB)

	# fixup the dependency libRnative/llvm -> libR in the same way as above
	install_name_tool -change $(FASTR_LIB_DIR)/libR.dylib @rpath/libR.dylib $(NFI_LIB)
	install_name_tool -change $(FASTR_LIB_DIR)/libR.dylib @rpath/libR.dylib $(LLVM_LIB)

	# TODO: check if we really need this and linking with libpolyglot-mock
	# given that we use -Wl,-undefined,dynamic_lookup when building libRllvm
	install_name_tool -change bin/libpolyglot-mock.dylib $(LLVM_LIBS_DIR)/libpolyglot-mock.dylib $(LLVM_LIB)

    	# check if we captured libpcre2-8/libz, rpath those in libR
	python $(FASTR_R_HOME)/mx.fastr/copylib.py updatelib $(FASTR_LIB_DIR) $(FASTR_R_HOME)
else
	# not Darwin:
	$(DYLIB_LD) $(DYLIB_LDFLAGS) $(shell echo $(PKG_LDFLAGS_OVERRIDE)) -Wl,-rpath,'$$ORIGIN' -o $(R_LIB) -L$(FASTR_LIB_DIR) -lRblas -lRlapack
	$(DYLIB_LD) $(DYLIB_LDFLAGS) $(shell echo $(PKG_LDFLAGS_OVERRIDE)) -Wl,-rpath,'$$ORIGIN' -o $(NFI_LIB) $(wildcard lib/nfi/*.o) $(wildcard lib/common/*.o) $(wildcard lib/JavaGD/*.o) -L$(FASTR_LIB_DIR) -lRblas -lRlapack -ldl -lpcre2-8 -lz $(F2C)
    # We do not link with Rlapack and Rblas so that we can load those libraries manually later after loading libR
    # Otherwise Sulong would attempt to load Rlapack and Rblas and fail, because there is a dependency cycle:
    # libR provides xerbla_, but Rlapack/Rblas depend on it, it seems that Sulong is not able to resolve this unlike the native loader
	$(DYLIB_LD) $(DYLIB_LDFLAGS) $(shell echo $(PKG_LDFLAGS_OVERRIDE)) -Wl,-rpath,'$$ORIGIN' -o $(LLVM_LIB) $(wildcard lib/llvm/*.o) $(wildcard lib/common/*.o) $(wildcard lib/JavaGD/*.o) -L$(FASTR_LIB_DIR) -ldl -lpcre2-8 -lz $(F2C)
endif # Darwin
endif # managed

ifeq ($(FASTR_RFFI),managed)
fficall.done: common.done
	touch fficall.done
else
fficall.done: JavaGD.done common.done fficallllvm.done
	$(MAKE) -C src/truffle_nfi all
	touch fficall.done

export R_PACKAGE_DIR="lib"
fficallllvm.done: common.done
	$(MAKE) -C src/truffle_llvm all
	touch fficallllvm.done
endif #managed

JavaGD.done:
	$(MAKE) -C src/JavaGD all
	touch JavaGD.done

common.done:
	$(MAKE) -C src/common all
	touch common.done

clean:
	$(MAKE) -C src/common clean
	rm -f src/truffle_*/*.o
	rm -f $(R_LIB) $(NFI_LIB) $(LLVM_LIB)
	rm -f fficall.done
	rm -f common.done
	rm -f fficallllvm.done
	$(MAKE) -C src/JavaGD clean
	rm -f JavaGD.done
ifneq ($(FASTR_RFFI),managed)
	$(MAKE) -C src/truffle_nfi clean
	$(MAKE) -C src/truffle_llvm clean
endif
