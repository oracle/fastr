#
# Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
# When building the final artifact, we either build only libR.so from the source files in common and NFI specific directories,
# and if FASTR_RFFI is "LLVM", we build also libR.sol, which is a regular so file with embedded bitcode, but that one is built
# from the source files in common and LLVM specific directories.
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
LLVM_LIBNAME := libR$(DYLIB_EXT)l
C_LIB := $(FASTR_LIB_DIR)/$(C_LIBNAME)
LLVM_LIB := $(FASTR_LIB_DIR)/$(LLVM_LIBNAME)
R_LIBNAME := libR$(DYLIB_EXT)
R_LIBNAME_DUMMY := libR_dummy$(DYLIB_EXT)
R_LIB := $(FASTR_LIB_DIR)/$(R_LIBNAME)
R_LIB_DUMMY := $(FASTR_LIB_DIR)/$(R_LIBNAME_DUMMY)

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
	$(DYLIB_LD) $(DYLIB_LDFLAGS) -Wl,-rpath,@loader_path/ -o $(R_LIB) $(wildcard lib/nfi/*.o) $(wildcard lib/common/*.o) -L$(FASTR_LIB_DIR) -ldl -lRblas -lRlapack -lpcre -lz $(F2C) -liconv $(VERSION_FLAGS)
	$(DYLIB_LD) $(DYLIB_LDFLAGS) -Wl,-rpath,@loader_path/ -o $(R_LIB_DUMMY) -L$(FASTR_LIB_DIR) -ldl -lRblas -lRlapack -lpcre -lz $(VERSION_FLAGS)
	$(DYLIB_LD) $(DYLIB_LDFLAGS) -Wl,-rpath,@loader_path/ -o $(LLVM_LIB) $(wildcard lib/llvm/*.o) $(wildcard lib/common/*.o) -L$(FASTR_LIB_DIR) -L$(LLVM_LIBS_DIR) -lpolyglot-mock -ldl -lRblas -lRlapack -lpcre -lz $(F2C) -liconv $(VERSION_FLAGS)

	install_name_tool -add_rpath @loader_path/ $(FASTR_LIB_DIR)/libRblas.dylib
	install_name_tool -add_rpath @loader_path/ $(FASTR_LIB_DIR)/libRlapack.dylib
	
	# The following commands will be changed to use @loader_path (or @rpath) instead of the absolute paths $(LLVM_LIBS_DIR) and $(FASTR_LIB_DIR)
	# when Sulong supports @loader_path (resp. @rpath) substitution.
	install_name_tool -change bin/libpolyglot-mock.dylib $(LLVM_LIBS_DIR)/libpolyglot-mock.dylib $(LLVM_LIB)
	#install_name_tool -change @rpath/libRblas.dylib $(FASTR_LIB_DIR)/libRblas.dylib $(LLVM_LIB)
	#install_name_tool -change @rpath/libRlapack.dylib $(FASTR_LIB_DIR)/libRlapack.dylib $(LLVM_LIB)
	#install_name_tool -change @rpath/libpcre.dylib $(FASTR_LIB_DIR)/libpcre.dylib $(LLVM_LIB)
	#install_name_tool -change @rpath/libz.dylib $(FASTR_LIB_DIR)/libz.dylib $(LLVM_LIB)
	#install_name_tool -change @rpath/libf2c.so $(FASTR_LIB_DIR)/libf2c$(DYLIB_EXT) $(LLVM_LIB)
	
	install_name_tool -change libRblas.dylib @rpath/libRblas.dylib $(R_LIB)
	install_name_tool -change libRlapack.dylib @rpath/libRlapack.dylib $(R_LIB)
	install_name_tool -change libf2c.so @rpath/libf2c$(DYLIB_EXT) $(R_LIB)
	install_name_tool -id @rpath/libR.dylib $(R_LIB)
	install_name_tool -id @rpath/libR.dylibl $(LLVM_LIB)
# check if we captured libpcre/libz, rpath those in libR
	mx -p $(FASTR_R_HOME) rupdatelib $(FASTR_LIB_DIR)
else
	# not Darwin:
	$(DYLIB_LD) $(DYLIB_LDFLAGS) $(shell echo $(PKG_LDFLAGS_OVERRIDE)) -Wl,-rpath,'$$ORIGIN' -o $(R_LIB) $(wildcard lib/nfi/*.o) $(wildcard lib/common/*.o) -L$(FASTR_LIB_DIR) -lRblas -lRlapack -ldl -lpcre -lz $(F2C)
	$(DYLIB_LD) $(DYLIB_LDFLAGS) $(shell echo $(PKG_LDFLAGS_OVERRIDE)) -Wl,-rpath,'$$ORIGIN' -o $(R_LIB_DUMMY) -L$(FASTR_LIB_DIR) -lRblas -lRlapack -ldl -lpcre -lz
    # We do not link with Rlapack and Rblas so that we can load those libraries manually later after loading libR
    # Otherwise Sulong would attempt to load Rlapack and Rblas and fail, because there is a dependency cycle:
    #     libR provides xerbla_, but Rlapack/Rblas depend on it, it seems that Sulong is not able to resolve this unlike the native loader
	$(DYLIB_LD) $(DYLIB_LDFLAGS) $(shell echo $(PKG_LDFLAGS_OVERRIDE)) -Wl,-rpath,'$$ORIGIN' -o $(LLVM_LIB) $(wildcard lib/llvm/*.o) $(wildcard lib/common/*.o) -L$(FASTR_LIB_DIR) -ldl -lpcre -lz $(F2C)
endif # Darwin
endif # managed

ifeq ($(FASTR_RFFI),managed)
fficall.done: common.done
	touch fficall.done
else
fficall.done: common.done fficallllvm.done
	$(MAKE) -C src/truffle_nfi all
	touch fficall.done

export R_PACKAGE_DIR="lib"
fficallllvm.done: common.done
	$(MAKE) -C src/truffle_llvm all
	touch fficallllvm.done
endif #managed

common.done:
	$(MAKE) -C src/common all
	touch common.done

clean:
	$(MAKE) -C src/common clean
	rm -rf src/truffle_*/*.o
	rm -rf $(R_LIB)
	rm -rf fficall.done
	rm -rf common.done
	rm -rf fficallllvm.done
ifneq ($(FASTR_RFFI),managed)
	$(MAKE) -C src/truffle_nfi clean
	$(MAKE) -C src/truffle_llvm clean
endif
