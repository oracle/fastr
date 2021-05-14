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

.PHONY: all clean repo clean_repo clean_recommended

SUBDIRS = testrffi vanilla tests4 gnurtests altreprffitests
NATIVE_RECOMMENDED_PROJECT = $(subst test.native,native.recommended,$(TOPDIR))
R_VERSION = 4.0.3
export GNUR_HOME_BINARY_UNPACKED = $(FASTR_R_HOME)/libdownloads/R-$(R_VERSION)
ifndef GNUR_HOME_BINARY
FASTR_R_HOME = $(abspath $(TOPDIR)/..)
export GNUR_HOME_BINARY = $(GNUR_HOME_BINARY_UNPACKED)
$(info GNUR_HOME_BINARY not set. Assuming the default location at $(GNUR_HOME_BINARY))
endif
GNUR_RECOMMENDED := $(wildcard $(GNUR_HOME_BINARY_UNPACKED)/src/library/Recommended/*.tgz)

# We have to copy the GNU R recommended packages into this project
# to keep the mx archiver happy, which asserts that every path
# is inside the project.

# WARNING: If you add/delete anything in this project you must update mx_fastr_dists.py
# to reflect the changes, e.g., adding a new SUBDIR

all: repo copy_recommended

# runs make in all the packages directories
# creates the PACKAGES, PACAKGES.gz, etc. files in the pkgs repository
repo:
	for dir in $(SUBDIRS); do \
		$(MAKE) PACKAGE=$$dir -C $$dir || exit 1; \
	done
	(cd $(REPO_DIR); $(GNUR_HOME_BINARY)/bin/R --vanilla --no-echo -e "tools::write_PACKAGES('.', type='source')")
	ls -R $(REPO_DIR) || true
	cat $(REPO_DIR)/PACKAGES || true

copy_recommended:
	mkdir -p recommended
	echo cp $(GNUR_RECOMMENDED) recommended
	cp $(GNUR_RECOMMENDED) recommended
	touch copy_recommended

clean: clean_repo clean_recommended

clean_repo:
	for dir in $(SUBDIRS); do \
		$(MAKE) PACKAGE=$$dir -C $$dir clean || exit 1; \
	done
	rm -f $(REPO_DIR)/PACKAGES*
	
clean_recommended:
	rm -rf recommended copy_recommended
