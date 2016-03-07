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
#
# Completeness of the stats package is a work in progress
# Currently we compile a subset of the .c and .f files from the
# GnuR stats library source - those that do not depend on GnuR internals (always true for .f)
# Also do some necessary modifications to a copy of fft.c.

.PHONY: cleanpkg nametool

GNUR_FFT = $(addprefix $(GNUR_HOME)/src/library/stats/src/, fft.c)
FFT_OBJECT = $(addprefix $(OBJ)/, fft.o)
LIB_PKG_PRE = $(FFT_OBJECT)
LIB_PKG_POST = nametool
CLEAN_PKG := cleanpkg


# have to include this here for PKG_LIBS
ifneq ($(MAKECMDGOALS),clean)
include $(TOPDIR)/platform.mk
endif

PKG_LIBS := $(LAPACK_LIBS) $(BLAS_LIBS) -L$(FASTR_LIB_DIR) $(FLIBS)
PKG_INCLUDES = -I $(GNUR_SRC)

OBJ = lib

GNUR_F_FILES = bsplvd.f bvalue.f bvalus.f eureka.f hclust.f kmns.f lminfl.f loessf.f ppr.f qsbart.f sgram.f \
               sinerp.f sslvrg.f stl.f stxwx.f

GNUR_F_OBJECTS = $(addprefix $(OBJ)/, $(GNUR_F_FILES:.f=.o))

GNUR_C_FILES = init.c isoreg.c kmeans.c loessc.c monoSpl.c sbart.c

GNUR_C_OBJECTS := $(addprefix $(OBJ)/, $(GNUR_C_FILES:.c=.o)) $(FFT_OBJECT)

include ../lib.mk

# Why is this necessary? Because if fft.c has been created by editing, lib.mk will
# include it in C_OBJECTS but it's already in GNUR_C_OBJECTS (uncreated case)
C_OBJECTS := $(filter-out $(FFT_OBJECT), $(C_OBJECTS))

$(C_OBJECTS): | $(OBJ)

$(SRC)/fft.c: $(GNUR_FFT) src/ed_fft
	ed $(GNUR_FFT) < src/ed_fft

$(OBJ)/%.o: $(GNUR_SRC)/%.c
	$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

$(OBJ)/%.o: $(GNUR_SRC)/%.f
	$(F77) $(FFLAGS) $(FPICFLAGS) -c $< -o $@

# ensure that the references to Rblas, Rlapack and appl are absolute
nametool:
ifeq ($(OS_NAME),Darwin)
	install_name_tool -change libRblas.dylib $(FASTR_R_HOME)/lib/libRblas.dylib $(FASTR_LIBRARY_DIR)/$(PKG)/libs/$(PKG).so
	install_name_tool -change libRlapack.dylib $(FASTR_R_HOME)/lib/libRlapack.dylib $(FASTR_LIBRARY_DIR)/$(PKG)/libs/$(PKG).so
endif

cleanpkg:
	rm -f $(SRC)/fft.c
