#
# Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
# A first step in the build is to make symbolic links to those files and also to
# do some necessary modofications to a copy of fft.c.

.PHONY: cleanpkg

GNUR_FFT = $(addprefix $(GNUR_HOME)/src/library/stats/src/, fft.c)

LIB_PKG_PRE = $(addprefix $(OBJ)/, fft.o) linked
CLEAN_PKG := cleanpkg


# have to include this here for PKG_LIBS
ifneq ($(MAKECMDGOALS),clean)
include $(TOPDIR)/platform.mk
endif

PKG_LIBS := $(LAPACK_LIBS) $(BLAS_LIBS) -lappl -L$(FASTR_LIB_DIR) $(FLIBS)

include ../lib.mk

LINKED_F_FILES = bsplvd.f bvalue.f bvalus.f eureka.f hclust.f kmns.f lminfl.f loessf.f ppr.f qsbart.f sgram.f \
               sinerp.f sslvrg.f stl.f stxwx.f
GNUR_LINKED_F_FILES = $(addprefix $(GNUR_HOME)/src/library/stats/src/, $(LINKED_F_FILES))

LINKED_C_FILES = isoreg.c kmeans.c loessc.c monoSpl.c sbart.c
GNUR_LINKED_C_FILES = $(addprefix $(GNUR_HOME)/src/library/stats/src/, $(LINKED_C_FILES))

linked: $(GNUR_LINKED_F_FILES) $(GNUR_LINKED_C_FILES)
	$(foreach file,$(LINKED_C_FILES),ln -sf $(GNUR_HOME)/src/library/stats/src/$(file) $(SRC)/$(file);)
	$(foreach file,$(LINKED_F_FILES),ln -sf $(GNUR_HOME)/src/library/stats/src/$(file) $(SRC)/$(file) ;)
	touch linked

# this dance ensures that C_SOURCES is set correctly regardless of whether the linked/modified files 
# have been created or not (C_SOURCES will have been set in ../lib.mk)
NON_LOCAL_C_SOURCES = $(addprefix $(SRC)/, $(LINKED_C_FILES))
C_LOCAL_SOURCES = $(filter-out  $(NON_LOCAL_C_SOURCES),$(wildcard $(SRC)/*.c))
C_SOURCES := $(NON_LOCAL_C_SOURCES) $(C_LOCAL_SOURCES)

#$(info NON_LOCAL_C_SOURCES=$(NON_LOCAL_C_SOURCES))
#$(info LOCAL_C_SOURCES=$(C_LOCAL_SOURCES))
#$(info C_SOURCES=$(C_SOURCES))

$(C_OBJECTS): | $(OBJ)

$(SRC)/fft.c: $(GNUR_FFT) src/ed_fft
	ed $(GNUR_FFT) < src/ed_fft

cleanpkg:
	rm -f linked
	rm -f $(SRC)/fft.c
	rm -f $(NON_LOCAL_C_SOURCES)
	rm -f $(addprefix $(SRC)/, $(LINKED_F_FILES))
