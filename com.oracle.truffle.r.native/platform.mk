#
# Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

OSNAME := $(shell uname)

ifeq ($(OSNAME), Linux)
  OS_DIR      := linux
  CC          := gcc
  FC          := gfortran
  CFLAGS      := -fPIC -O2
  LDFLAGS     := -fPIC -shared
  SHARED_EXT  := so
  LIBS        := -lgfortran
else ifeq ($(OSNAME), SunOS)
  OS_DIR      := solaris
  CC          := cc
  FC          := f90
  CFLAGS      := -m64 -O -xcode=pic13
  LDFLAGS     := -G -m64
  SHARED_EXT  := so
else ifeq ($(OSNAME), Darwin)
  OS_DIR      := darwin
  CC          := gcc
  FC          := gfortran
  CFLAGS      := -fPIC -O2
  LDFLAGS     := -dynamiclib -undefined dynamic_lookup
  SHARED_EXT  := dylib
else
all:
	@echo "This Makefile does not know how to compile for $(OSNAME)"
	@false
endif

CFLAGS := $(CFLAGS) -DFASTR
