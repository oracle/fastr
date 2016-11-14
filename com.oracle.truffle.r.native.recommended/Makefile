#
# Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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

# This builds the "recommended" packages that are bundled with GNU R
# It has to be built separately from the "native" project that contains
# the packages because that is built first and before FastR is completely built
# N.B. As this takes quite a while the building is conditional on the
# FASTR_RELEASE environment variable

.PHONY: all clean

FASTR_R_HOME := $(abspath $(CURDIR)/..)
NATIVE_PROJECT := $(subst native.recommended,native,$(CURDIR))
R_VERSION := $(notdir $(wildcard $(NATIVE_PROJECT)/gnur/R-*))
GNUR_HOME := $(NATIVE_PROJECT)/gnur/$(R_VERSION)
# order matters due to inter-package dependencies
GNUR_RECOMMENDED_PKGNAMES := MASS boot class cluster lattice nnet spatial Matrix survival KernSmooth foreign nlme rpart codetools
GNUR_RECOMMENDED_TARS := $(foreach pkg, $(GNUR_RECOMMENDED_PKGNAMES),$(GNUR_HOME)/src/library/Recommended/$(pkg).tgz)
#$(info GNUR_RECOMMENDED_TARS=$(GNUR_RECOMMENDED_TARS))

all: install.recommended

ifdef FASTR_RELEASE
install.recommended: $(GNUR_RECOMMENDED_TARS)
	for pkgtar in $(GNUR_RECOMMENDED_TARS); do \
		$(FASTR_R_HOME)/bin/R CMD INSTALL --library=$(FASTR_R_HOME)/library $$pkgtar; \
	done
	touch install.recommended
else
install.recommended:
endif

clean:
	for pkgname in $(GNUR_RECOMMENDED_PKGNAMES); do \
		rm -rf $(FASTR_R_HOME)/library/$$pkgname; \
	done
	rm -f install.recommended

