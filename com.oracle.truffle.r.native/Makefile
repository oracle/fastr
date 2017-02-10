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

.PHONY: all clean 

export TOPDIR = $(CURDIR)
export FASTR_R_HOME=$(abspath $(TOPDIR)/..)
export FASTR_LIB_DIR=$(FASTR_R_HOME)/lib
export FASTR_NATIVE_DIR = $(TOPDIR)
export R_VERSION = $(subst R-,,$(notdir $(basename $(basename $(wildcard $(FASTR_R_HOME)/libdownloads/R-*.gz)))))
export GNUR_HOME = $(TOPDIR)/gnur/R-$(R_VERSION)

# Completely accurate dependency analysis is very difficult for this project, so use a version number
# to force a clean build, and elsewhere use sentinels to avoid rebuilding when we can't compute the 
# dependencies accurately.

all: checkversion
	$(MAKE) -C gnur
	$(MAKE) -C include
	$(MAKE) -C fficall
	$(MAKE) -C library
	$(MAKE) -C run
	cp version.source version.built

clean: 
	$(MAKE) -C include clean
	$(MAKE) -C fficall clean
	$(MAKE) -C library clean
	$(MAKE) -C run clean
	$(MAKE) -C gnur clean

version.source := $(shell cat version.source)

#$(info version: $(version.source))
ifeq ($(wildcard version.built),)
	doclean = 1
else
version.built := $(shell cat version.built)
#$(info version.built: $(version.built))
ifneq ($(version.source),$(version.built))
	doclean = 1
else
	doclean = 0
endif
endif

#$(info doclean: $(doclean))
checkversion:
ifeq ($(doclean),1)
	$(MAKE) clean
endif

	