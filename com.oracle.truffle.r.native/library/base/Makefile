#
# Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

RPROFILE := $(FASTR_LIBRARY_DIR)/base/R/Rprofile
RPROFILE_ORIG := $(RPROFILE).orig
LIB_PKG_POST = $(RPROFILE)

include ../lib.mk

# edit the Rprofile to add fastr as a default package
# sed's edit in place option with backup is not portable

$(LIB_PKG_POST): $(RPROFILE_ORIG)

$(RPROFILE_ORIG):
	cp $(RPROFILE) $(RPROFILE_ORIG)
	sed -f sed_profile $(RPROFILE_ORIG) > $(RPROFILE)
