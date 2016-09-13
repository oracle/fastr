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

.PHONY: all clean make_subdirs clean_subdirs clean_recommended

SUBDIRS = testrffi vanilla tests4
NATIVE_PROJECT = $(subst test.native,native,$(TOPDIR))
R_VERSION := $(notdir $(wildcard $(NATIVE_PROJECT)/gnur/R-*))
GNUR_HOME := $(NATIVE_PROJECT)/gnur/$(R_VERSION)
GNUR_RECOMMENDED := $(wildcard $(GNUR_HOME)/src/library/Recommended/*.tgz)

# We have to copy the GNU R recommended packages into this project
# to keep the mx archiver happy, which asserts that every path
# is inside the project.

# WARNING: If you add/delete anything in this project you must update mx_fastr_dists.py
# to reflect the changes, e.g., adding a new SUBDIR

all: make_subdirs copy_recommended

make_subdirs:
	for dir in $(SUBDIRS); do \
		$(MAKE) PACKAGE=$$dir -C $$dir || exit 1; \
	done

copy_recommended:
	mkdir -p recommended
	cp $(GNUR_RECOMMENDED) recommended
	touch copy_recommended

clean: clean_subdirs clean_recommended

clean_subdirs:
	for dir in $(SUBDIRS); do \
		$(MAKE) PACKAGE=$$dir -C $$dir clean || exit 1; \
	done
	
clean_recommended:
	rm -rf recommended clean_recommended
