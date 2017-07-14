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
# lib.mk copies the GNU R library/base dir to the FastR library dir
# so we have to build the "uncompressed" variant of GNU R in the PRE phase.
LIB_PKG_PRE = pre.done 
LIB_PKG_POST = post.done
CLEAN_PKG = clean.done
endif

include ../lib.mk

ifeq ($(FASTR_RFFI),managed)
# The following changes GnuR's build script makebasedb.R so that it does not
# compress the lazy load database, then it (re)builds GnuR 
pre.done:
	cp $(GNUR_HOME)/src/library/base/makebasedb.R $(GNUR_HOME)/src/library/base/makebasedb.R.tmp
	sed 's|compress = TRUE|compress = FALSE|g' $(GNUR_HOME)/src/library/base/makebasedb.R.tmp > $(GNUR_HOME)/src/library/base/makebasedb.R
# force a base rebuild
	rm $(GNUR_HOME)/src/library/base/all.R
	(cd $(GNUR_HOME); $(MAKE))
	touch pre.done

# original base assumes that "base" DLL is loaded, we change it to check for that first
post.done:
	sed 's|## populate C\/Fortran symbols|if (length(getLoadedDLLs()) > 0)|g' $(FASTR_LIBRARY_DIR)/base/R/base > $(FASTR_LIBRARY_DIR)/base/R/base.tmp
	mv $(FASTR_LIBRARY_DIR)/base/R/base.tmp $(FASTR_LIBRARY_DIR)/base/R/base
	mv $(GNUR_HOME)/src/library/base/makebasedb.R.tmp $(GNUR_HOME)/src/library/base/makebasedb.R
# force a base rebuild
	rm $(GNUR_HOME)/src/library/base/all.R
# rebuild GNU R in compressed mode
	(cd $(GNUR_HOME); $(MAKE))
	touch post.done
	find $(FASTR_LIBRARY_DIR)/base -exec touch {} +

clean.done:
	rm -f post.done pre.done
endif
