#
# Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

ifeq ($(FASTR_RFFI),managed)
BASE_UNCOMPRESSED = $(addprefix $(FASTR_LIBRARY_DIR)/base/R/, base.rdb)
BASE_SCRIPT = $(addprefix $(FASTR_LIBRARY_DIR)/base/R/, base)
LIB_PKG_POST = $(BASE_UNCOMPRESSED) $(BASE_SCRIPT)
endif

include ../lib.mk

ifeq ($(FASTR_RFFI),managed)
# original base assumes that "base" DLL is loaded, we change it to check for that first
$(BASE_SCRIPT): $(GNUR_HOME)/library/base/R/base
	sed -i '/## populate C\/Fortran symbols/a if (length(getLoadedDLLs()) > 0)' $(FASTR_LIBRARY_DIR)/base/R/base
# The following changes GnuR's build script makebasedb.R so that it does not
# compress the lazy load database, then it builds GnuR and copies the
# uncompressed base package database to FastR library location
$(BASE_UNCOMPRESSED): $(GNUR_HOME)/src/library/base/all.R
	sed -i 's|compress = TRUE|compress = FALSE|g' $(GNUR_HOME)/src/library/base/makebasedb.R
	(cd $(GNUR_HOME); $(MAKE))
	cp $(GNUR_HOME)/library/$(PKG)/R/base.rd* $(FASTR_LIBRARY_DIR)/base/R/
	sed -i 's|compress = FALSE|compress = TRUE|g' $(GNUR_HOME)/src/library/base/makebasedb.R
	(cd $(GNUR_HOME); $(MAKE))
endif
