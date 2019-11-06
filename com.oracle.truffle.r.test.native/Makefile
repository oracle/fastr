#
# Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

.PHONY: all clean

export TOPDIR = $(CURDIR)
# all output goes into the com.oracle.truffle.r.test project for packaging into a single distribution
export MX_OUTPUT_DIR = $(abspath $(TOPDIR)/../mxbuild/com.oracle.truffle.r.test/bin/com/oracle/truffle/r/test)
export REPO_DIR := $(TOPDIR)/packages/repo/src/contrib
export FASTR_NATIVE_DIR = $(subst test.native,native,$(TOPDIR))
export FASTR_R_HOME=$(abspath $(TOPDIR)/..)
export FASTR_LIB_DIR=$(FASTR_R_HOME)/lib

OSNAME := $(shell uname)

all:
	mkdir -p $(REPO_DIR)
	$(MAKE) -C urand
	$(MAKE) -C packages
ifeq ($(FASTR_RFFI),nfi)
	$(MAKE) -C embedded
endif
ifeq ($(FASTR_RFFI),)
	$(MAKE) -C embedded
endif

clean:
	$(MAKE) -C urand clean
	$(MAKE) -C packages clean
ifeq ($(FASTR_RFFI),nfi)
	$(MAKE) -C embedded clean
endif
ifeq ($(FASTR_RFFI),)
	$(MAKE) -C embedded clean
endif
	rm -f $(REPO_DIR)/*
