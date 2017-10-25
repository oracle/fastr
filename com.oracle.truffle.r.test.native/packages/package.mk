#
# Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

# This "builds" a test package, resulting in a tar file,
# which is then loaded by the unit tests in TestRPackages.
# It uses R CMD build ...
# Currently we can't use FastR for this step as FastR isn't completely built
# when this is built (it's part of the build)
# The resulting package is stored in the "repo/src/contrib" folder

# test packages may include .Rin files that are preprocessed to create an
# actual test file. Unfortunately while R CND check will do this, tools::testInstalledPackage
# does not (surely a bug). So we generate the files here.

.PHONY: all

PKG_FILES = $(shell find $(PACKAGE) -type f -name '*')

PKG_TAR = $(REPO_DIR)/$(PACKAGE)_1.0.tar.gz

RIN_FILES = $(shell find $(PACKAGE) -type f -name '*.Rin')
RIN_R_FILES = $(subst .Rin,.R, $(RIN_FILES))
TEMPLATE_FILE := ../Rutils/template.R

all: $(RIN_R_FILES) $(PKG_TAR)

$(PKG_TAR): $(PKG_FILES)
	(cd $(REPO_DIR); TZDIR=/usr/share/zoneinfo/ $(GNUR_HOME_BINARY)/bin/R CMD build $(CURDIR)/$(PACKAGE))

$(RIN_R_FILES): $(RIN_FILES)
	for rf in $(RIN_FILES); do \
		TEMPLATE_FILE=$(TEMPLATE_FILE) $(GNUR_HOME_BINARY)/bin/Rscript $$rf $$rf || exit 1; \
	done

clean:
	rm -f $(PKG_TAR) $(RIN_R_FILES)

