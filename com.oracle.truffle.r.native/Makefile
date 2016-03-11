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

.PHONY: all clean 

export TOPDIR = $(CURDIR)
export FASTR_R_HOME=$(abspath $(TOPDIR)/..)
export FASTR_LIB_DIR=$(FASTR_R_HOME)/lib
export FASTR_NATIVE_DIR = $(TOPDIR)
export R_VERSION = 3.2.4
export GNUR_HOME = $(TOPDIR)/gnur/R-$(R_VERSION)

all: 
	$(MAKE) -C gnur
	$(MAKE) -C include
	$(MAKE) -C osextras
	$(MAKE) -C fficall
	$(MAKE) -C library
	$(MAKE) -C run

clean: 
	$(MAKE) -C include clean
	$(MAKE) -C osextras clean
	$(MAKE) -C fficall clean
	$(MAKE) -C library clean
	$(MAKE) -C run clean
	$(MAKE) -C gnur clean

