#
# Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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

# This "builds" the "fastr" package, which defines functions that can
# access the internals of the FastR implementation, e.g., print Truffle ASTs.
# It is a "real" package, and loaded in the same way as any R package.

# We use a 'tar' file of the sources as the sentinel for whether the INSTALL step is needed
# Since this is just R code, we use GnuR to do the INSTALL. In case the system does not have R 
# installed we use the one we built.

.PHONY: all

PKG_FILES = $(shell find src/ -type f -name '*')
INSTALL_SENTINEL = $(FASTR_LIBRARY_DIR)/fastr/DESCRIPTION

PKG_TAR = lib/fastr.tar

all: $(INSTALL_SENTINEL)

$(PKG_TAR): $(PKG_FILES)
	mkdir -p lib
	(cd src; tar cf ../$(PKG_TAR) *)

$(INSTALL_SENTINEL): $(PKG_TAR)
	$(GNUR_HOME)/bin/R CMD INSTALL --library=$(FASTR_LIBRARY_DIR) src
	
clean:
	rm -f $(PKG_TAR)

