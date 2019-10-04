#
# Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

# This builds the "recommended" packages that are bundled with GNU R
# It has to be built separately from the "native" project that contains
# the packages because that is built first and before FastR is completely built
# N.B. As this takes quite a while the building is conditional on the
# FASTR_RELEASE environment variable

FASTR_R_HOME := $(abspath $(VPATH)/..)
NATIVE_PROJECT_DIR := $(FASTR_R_HOME)/com.oracle.truffle.r.native
TOPDIR := $(FASTR_R_HOME)/com.oracle.truffle.r.native.recommended
PKGS_REPO_DIR := repo/src/contrib

ifdef MX_VERBOSE
VERBOSE=--verbose
endif

# The package tar balls are copied from $(GNUR_HOME_BINARY)/src/library/Recommended
ifndef GNUR_HOME_BINARY
FASTR_R_HOME = $(abspath $(TOPDIR)/..)
R_VERSION = $(subst R-,,$(notdir $(basename $(basename $(wildcard $(FASTR_R_HOME)/libdownloads/R-*.gz)))))
export GNUR_HOME_BINARY = $(FASTR_R_HOME)/libdownloads/R-$(R_VERSION)
$(info GNUR_HOME_BINARY not set. Assuming the default location at $(GNUR_HOME_BINARY))
endif

ifneq (,$(wildcard $(NATIVE_PROJECT_DIR)/platform.mk))
include $(NATIVE_PROJECT_DIR)/platform.mk
else
ifneq ($(MAKECMDGOALS),clean)
$(error no platform.mk available)
endif
endif

.PHONY: all clean

# order matters due to inter-package dependencies
ifdef FASTR_RELEASE
ifdef FASTR_NO_RECOMMENDED
GNUR_RECOMMENDED_PKGNAMES := codetools
else
GNUR_RECOMMENDED_PKGNAMES := codetools MASS boot class cluster lattice nnet spatial Matrix survival KernSmooth foreign nlme rpart
endif
else
# codetools must always be installed because of S4
GNUR_RECOMMENDED_PKGNAMES := codetools
endif # FASTR_RELEASE

GNUR_RECOMMENDED_TARS := $(foreach pkg, $(GNUR_RECOMMENDED_PKGNAMES),$(GNUR_HOME_BINARY)/src/library/Recommended/$(pkg)*.tar.gz)
e :=
c := )|(
clist = $(subst $e $e,$c,$(strip $1))
# This will create a string like "^((codetools)|(MASS)|...)$"
GNUR_RECOMMENDED_PATTERN := "^(($(call clist,$(GNUR_RECOMMENDED_PKGNAMES))))$$"

all: install.recommended

$(info GNUR_RECOMMENDED_TARS=$(GNUR_RECOMMENDED_TARS))
$(info GNUR_RECOMMENDED_PATTERN=$(GNUR_RECOMMENDED_PATTERN))

ifneq ($(FASTR_RFFI),managed)
$(PKGS_REPO_DIR):
	mkdir -p $(PKGS_REPO_DIR)
	cp $(GNUR_RECOMMENDED_TARS) $(PKGS_REPO_DIR)
	(cd $(PKGS_REPO_DIR); $(GNUR_HOME_BINARY)/bin/R --vanilla --slave -e "tools::write_PACKAGES('.', type='source')")

install.recommended: $(PKGS_REPO_DIR)
ifdef R_RECOMMENDED_PKGS_CACHE_DIR
	mx -p $(FASTR_R_HOME) r-pkgcache $(VERBOSE) --vm fastr --library fastr=$(FASTR_R_HOME)/library --repos RECOMMENDED=$(PKGS_REPO_DIR) --cache-dir $(R_RECOMMENDED_PKGS_CACHE_DIR) --pkg-pattern $(GNUR_RECOMMENDED_PATTERN)
else
	for pkgtar in $(GNUR_RECOMMENDED_TARS); do \
		$(FASTR_R_HOME)/bin/R --polyglot CMD INSTALL --library=$(FASTR_R_HOME)/library $$pkgtar; \
	done
endif
ifeq ($(OS_NAME),Darwin)
	for pkgname in $(GNUR_RECOMMENDED_PKGNAMES); do \
		if [ -e $(FASTR_R_HOME)/library/$$pkgname/libs/$$pkgname.so ] ; then \
			install_name_tool -id @rpath/../library/$$pkgname/libs/$$pkgname.so $(FASTR_R_HOME)/library/$$pkgname/libs/$$pkgname.so; \
			install_name_tool -add_rpath @loader_path/../../../lib/ $(FASTR_R_HOME)/library/$$pkgname/libs/$$pkgname.so; \
			mx -p $(FASTR_R_HOME) rupdatelib $(FASTR_R_HOME)/library/$$pkgname/libs; \
		fi \
	done
endif
	touch install.recommended
else
install.recommended:
endif
	touch install.recommended

clean:
	for pkgname in $(GNUR_RECOMMENDED_PKGNAMES); do \
		rm -rf $(FASTR_R_HOME)/library/$$pkgname; \
	done
	rm -f install.recommended
	rm -rf $(PKGS_REPO_DIR)
