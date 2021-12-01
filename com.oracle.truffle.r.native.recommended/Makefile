#
# Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
# FASTR_RELEASE and FASTR_NO_RECOMMENDED environment variables

FASTR_R_HOME := $(abspath $(VPATH)/..)
NATIVE_PROJECT_DIR := $(FASTR_R_HOME)/com.oracle.truffle.r.native
TOPDIR := $(FASTR_R_HOME)/com.oracle.truffle.r.native.recommended

ifdef MX_VERBOSE
VERBOSE=--verbose
endif

FASTR_R_HOME = $(abspath $(TOPDIR)/..)
R_VERSION = 4.0.3
# The package tar balls are copied from $(GNUR_HOME_BINARY_UNPACKED)/src/library/Recommended
export GNUR_HOME_BINARY_UNPACKED = $(FASTR_R_HOME)/libdownloads/R-$(R_VERSION)

ifndef GNUR_HOME_BINARY
export GNUR_HOME_BINARY = $(GNUR_HOME_BINARY_UNPACKED)
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
GNUR_RECOMMENDED_PKGNAMES_TS = $(GNUR_RECOMMENDED_PKGNAMES:=.ts)

# Recommended packages that are used by fastr in a specific version, i.e., taken from other sources than GNUR_RECOMMENDED_BINARY
PKGS_WITH_CUSTOM_VERSION := cluster rpart
RPART_TAR := $(FASTR_R_HOME)/libdownloads/rpart.tar.gz
CLUSTER_TAR := $(FASTR_R_HOME)/libdownloads/cluster.tar.gz

GNUR_RECOMMENDED_TAR_DIR = $(GNUR_HOME_BINARY_UNPACKED)/src/library/Recommended/
# We do not copy all the recommended packages from gnur's directory. Instead, there are some
# packages that we take from different sources.
GNUR_RECOMMENDED_TARS := \
$(foreach pkg, \
	$(filter-out $(PKGS_WITH_CUSTOM_VERSION),$(GNUR_RECOMMENDED_PKGNAMES)), \
	$(GNUR_RECOMMENDED_TAR_DIR)/$(pkg)*.tar.gz \
)
ALL_RECOMMENDED_TARS := $(GNUR_RECOMMENDED_TARS) $(RPART_TAR) $(CLUSTER_TAR)

## need to allow parallel installs
INSTALL_OPTS = --pkglock --data-compress=xz --no-staged-install

$(info GNUR_RECOMMENDED_PKGNAMES = $(GNUR_RECOMMENDED_PKGNAMES))
$(info ALL_RECOMMENDED_TARS = $(ALL_RECOMMENDED_TARS))

# in managed mode, we do nothing
ifneq ($(FASTR_RFFI),managed)

all: install.recommended

# ------------------------
# If FASTR_RECOMMENDED_BINARY is defined, we just copy the binaries from there
ifdef FASTR_RECOMMENDED_BINARY

$(info Using FastR recommended packages binary: $(FASTR_RECOMMENDED_BINARY))

install.recommended: $(ALL_RECOMMENDED_TARS)
#	@if ! (mx -p $(FASTR_R_HOME) r-pkgcache --print-api-checksum --vm fastr | tail -n 1 | diff -q $(FASTR_RECOMMENDED_BINARY)/api-checksum.txt -); then \
#		echo "******************************"; \
#		echo "ERROR:"; \
#		echo "The checksum of FastR header files does not match the checksum that the recommended packages were build against."; \
#		echo "You can run appropriate CI jobs in FastR repo to generate a new version of recommended packages binary and then upload it to the appropriate place."; \
#		echo "******************************"; \
#		echo "Actual checksum of FastR headers:"; \
#		mx -p $(FASTR_R_HOME) r-pkgcache --print-api-checksum --vm fastr; \
#		echo "Expected checksum by the recommended pkgs:"; \
#		cat $(FASTR_RECOMMENDED_BINARY)/api-checksum.txt; \
#		echo "******************************"; \
#		exit 1; \
#	fi
	cp -R $(FASTR_RECOMMENDED_BINARY)/pkgs/* $(FASTR_R_HOME)/library
	touch install.recommended

# ------------------------
# If FASTR_RECOMMENDED_BINARY is not defined, we just build the packages
else 

%.ts: $(GNUR_RECOMMENDED_TAR_DIR)%.tgz
	@echo Installing recommended package `basename $<`
	@_R_CHECK_INSTALL_DEPENDS_=no FASTR_USE_F2C=true $(FASTR_R_HOME)/bin/R --polyglot CMD INSTALL $(INSTALL_OPTS) --library=$(FASTR_R_HOME)/library $< &> $@.out || (cat $@.out && exit 1)
	@echo Finished installing recommended package `basename $<`

install.recommended: $(GNUR_RECOMMENDED_PKGNAMES_TS)
ifeq ($(OS_NAME),Darwin)
	set -e; \
	for pkgname in $(GNUR_RECOMMENDED_PKGNAMES); do \
		if [ -e $(FASTR_R_HOME)/library/$$pkgname/libs/$$pkgname.so ] ; then \
			install_name_tool -id @rpath/../library/$$pkgname/libs/$$pkgname.so $(FASTR_R_HOME)/library/$$pkgname/libs/$$pkgname.so; \
			install_name_tool -add_rpath @loader_path/../../../lib/ $(FASTR_R_HOME)/library/$$pkgname/libs/$$pkgname.so; \
			python $(FASTR_R_HOME)/mx.fastr/copylib.py updatelib $(FASTR_R_HOME)/library/$$pkgname/libs $(FASTR_R_HOME); \
		fi \
	done
endif
	touch install.recommended

Matrix.ts: lattice.ts
mgcv.ts: nlme.ts MASS.ts Matrix.ts
nlme.ts: lattice.ts
class.ts: MASS.ts
survival.ts: Matrix.ts
## and for static html links
boot.ts: MASS.ts survival.ts
cluster.ts: MASS.ts

endif # FASTR_RECOMMENDED_BINARY

clean:
	for pkgname in $(GNUR_RECOMMENDED_PKGNAMES); do \
		rm -rf $(FASTR_R_HOME)/library/$$pkgname; \
	done
	rm -f install.recommended

endif # FASTR_RFFI != managed
