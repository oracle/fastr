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

# links to the standard R header files with some minor modifications for FASTR

.PHONY: all clean

SRC=src

R_EXT_HEADERS := $(wildcard $(GNUR_HOME)/include/R_ext/*.h)
#$(info R_EXT_HEADERS=$(R_EXT_HEADERS))
R_EXT_HEADERS_FILENAMES := $(notdir $(R_EXT_HEADERS))
R_EXT_HEADERS_LOCAL := src/R_ext/GraphicsEngine.h
#$(info R_EXT_HEADERS_LOCAL=$(R_EXT_HEADERS_LOCAL))
R_EXT_HEADERS_TO_LINK := $(filter-out $(notdir $(R_EXT_HEADERS_LOCAL)),$(R_EXT_HEADERS_FILENAMES))
#$(info R_EXT_HEADERS_TO_LINK=$(R_EXT_HEADERS_TO_LINK))
R_HEADERS := $(wildcard $(GNUR_HOME)/include/*.h)
R_HEADERS_FILENAMES := $(notdir $(R_HEADERS))
#$(info R_HEADERS_FILENAMES=$(R_HEADERS_FILENAMES))
R_HEADERS_LOCAL := src/libintl.h src/Rinternals.h src/Rinterface.h
#$(info R_HEADERS_LOCAL=$(R_HEADERS_LOCAL))>
R_HEADERS_TO_LINK := $(filter-out $(notdir $(R_HEADERS_LOCAL)),$(R_HEADERS_FILENAMES))
#$(info R_HEADERS_TO_LINK=$(R_HEADERS_TO_LINK))

all: linked

linked: ed_Rinternals ed_Rinterface_gcntx ed_Rinterface_interactive ed_GraphicsEngine
	mkdir -p R_ext
	$(foreach file,$(R_HEADERS_TO_LINK),ln -sf $(GNUR_HOME)/include/$(file) $(file);)
	ln -sf src/libintl.h
	ed $(GNUR_HOME)/include/Rinternals.h < ed_Rinternals
	ed $(GNUR_HOME)/include/Rinterface.h < ed_Rinterface_gcntx
#	ed $(GNUR_HOME)/include/Rinterface.h < ed_Rinterface_interactive
	ed $(GNUR_HOME)/include/R_ext/GraphicsEngine.h < ed_GraphicsEngine
	$(foreach file,$(R_EXT_HEADERS_TO_LINK),ln -sf $(GNUR_HOME)/include/R_ext/$(file) R_ext/$(file);)
#	cp $(R_EXT_HEADERS_LOCAL) R_ext
	touch linked

clean:
	rm -rf include linked
	$(foreach file,$(R_HEADERS_FILENAMES),rm -f $(file);)
	$(foreach file,$(R_EXT_HEADERS_FILENAMES),rm -f R_ext/$(file);)
