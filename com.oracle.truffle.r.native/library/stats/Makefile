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

.PHONY: cleanfft

LIB_PKG_PRE = $(OBJ)/fft.o
GNUR_FFT := $(GNUR_HOME)/src/library/stats/src/fft.c
CLEAN_PKG := cleanfft

include ../lib.mk

# The above will set set C_SOURCES to all the .c files in src
# If we happen to have a pre-generated fft.c, then ok, otherwise
# we need to add it. Can't always add it as then we get it duplicated
# in C_OBJECTS, which causes a link failure

HAVE_FFT_SRC := $(shell [ -f $(SRC)/fft.c ] && echo "exists")
ifneq ($(HAVE_FFT_SRC), exists)
C_SOURCES := $(C_SOURCES) $(SRC)/fft.c
C_OBJECTS := $(C_OBJECTS) $(OBJ)/fft.o
endif

$(C_OBJECTS): | $(OBJ)

$(SRC)/fft.c: $(GNUR_FFT) src/ed_fft
	ed $(GNUR_FFT) < src/ed_fft

cleanfft:
	rm -f $(SRC)/fft.c
