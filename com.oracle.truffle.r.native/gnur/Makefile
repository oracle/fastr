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

# This extracts, configures and builds GnuR for the current platform.
# FastR does not, obviously, need all of GnuR to be built; only those pieces
# that it shares, but is simpler and safer to just build all of it. The relevant 
# pieces are then copied to other FastR directories

include ../platform.mk

.PHONY: all clean config build copy

TARGET_DIR := ../builtinlibs/lib
BLAS_TARGET := $(TARGET_DIR)/libRblas.$(SHARED_EXT)
LAPACK_TARGET := $(TARGET_DIR)/libRlapack.$(SHARED_EXT)
export PCRE_TARGET := $(TARGET_DIR)/libpcre.$(SHARED_EXT)

all: Makefile $(GNUR_DIR) config build copy

$(GNUR_DIR): 
	tar xf $(TOPDIR)/../lib/R-$(R_VERSION).tar.gz
	
config: $(GNUR_DIR)/Makefile

$(GNUR_DIR)/Makefile: 
	(cd $(GNUR_DIR); ./configure --with-x=no --without-recommended-packages >& /dev/null)

build: $(GNUR_DIR)/bin/R

$(GNUR_DIR)/bin/R:
	(cd $(GNUR_DIR); make >& make.log)

copy: $(TARGET_DIR) $(BLAS_TARGET) $(LAPACK_TARGET) $(PCRE_TARGET)

$(TARGET_DIR):
	mkdir -p $(TARGET_DIR)

$(BLAS_TARGET): $(GNUR_DIR)/lib/libRblas.$(SHARED_EXT)
	cp $(GNUR_DIR)/lib/libRblas.$(SHARED_EXT) $(BLAS_TARGET)

$(LAPACK_TARGET): $(GNUR_DIR)/lib/libRlapack.$(SHARED_EXT)
	cp $(GNUR_DIR)/lib/libRlapack.$(SHARED_EXT) $(LAPACK_TARGET)

$(PCRE_TARGET):
	$(MAKE) -f Makefile.pcre

clean:
	rm -rf $(GNUR_DIR)
	rm -f $(BLAS_TARGET) $(LAPACK_TARGET) $(PCRE_TARGET)
