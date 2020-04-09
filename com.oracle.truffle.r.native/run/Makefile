#
# Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

# Sets up the "bin" directory with scripts mostly copied from GnuR.
# The FastR'ness is handled in the "R" file in the "bin/exec" subdirectory.
# Rscript is a separate program in GnuR and stored directly in the "bin" directory,
# but in FastR it is just a slight variant of "R". However, we cannot put
# a FastR-specific Rscript in "exec" because the install_packages code
# treats everything in there except "R" as a sub-architecture, so we put in
# execRextras.
#
# The R script defines the R_HOME environment variable from R_HOME_DIR
# which is set in the script during the GnuR build. This has to be changed.

# For building R packages with native code we use the Makeconf file from GnuR etc
# with some minor changes

.PHONY: bindir all rcmd

FASTR_BIN_DIR := $(FASTR_R_HOME)/bin
FASTR_DOC_DIR := $(FASTR_R_HOME)/doc
FASTR_ETC_DIR := $(FASTR_R_HOME)/etc
FASTR_SHARE_DIR := $(FASTR_R_HOME)/share
FASTR_INCLUDE_DIR := $(FASTR_R_HOME)/include

R_SCRIPT := $(addprefix $(GNUR_HOME_BINARY)/bin/,R)
BIN_FILES := $(wildcard $(GNUR_HOME_BINARY)/bin/*)

DOC_FILES := $(wildcard $(GNUR_HOME_BINARY)/doc/*)

SHARE_FILES := $(addprefix $(GNUR_HOME_BINARY)/share/,R Rd make java encodings)

FASTR_TOOLS := $(wildcard $(FASTR_NATIVE_DIR)/run/fastr_tools/*)

OS_NAME := $(shell uname -s)
ifeq ($(OS_NAME),Darwin)
	LIB_PATH_VAR := DYLD_FALLBACK_LIBRARY_PATH
else
	LIB_PATH_VAR := LD_LIBRARY_PATH
endif


all: rundirs rcmds includedir

rundirs:
	mkdir -p $(FASTR_BIN_DIR)
	mkdir -p $(FASTR_DOC_DIR)
	mkdir -p $(FASTR_BIN_DIR)/exec
	mkdir -p $(FASTR_BIN_DIR)/execRextras
	mkdir -p $(FASTR_ETC_DIR)
	mkdir -p $(FASTR_SHARE_DIR)

rcmds: $(FASTR_BIN_DIR)/R

$(FASTR_BIN_DIR)/R: Makefile R.sh Rscript.sh Rscript_exec.sh Rclasspath.sh
	cp -r $(BIN_FILES) $(FASTR_BIN_DIR)
	# Note: in the GraalVM release support distribution we override exec/R and Rscript
	# with scripts R_launcher and Rscript_launcher from com.oracle.truffle.r.release
	# overide bin/exec/R
	cp R.sh $(FASTR_BIN_DIR)/exec/R
	# override bin/Rscript
	cp Rscript_exec.sh $(FASTR_BIN_DIR)/execRextras/Rscript
	cp Rscript.sh $(FASTR_BIN_DIR)/Rscript
	# for embedded support
	cp Rclasspath.sh $(FASTR_BIN_DIR)/execRextras/Rclasspath
	chmod +x $(FASTR_BIN_DIR)/exec/R $(FASTR_BIN_DIR)/execRextras/Rscript $(FASTR_BIN_DIR)/Rscript $(FASTR_BIN_DIR)/execRextras/Rclasspath
	chmod +x $(FASTR_BIN_DIR)/R
	touch $(FASTR_ETC_DIR)/ldpaths
	#######
	# FastR specific hand-written configuration files
	cp fastr_etc/$(shell uname)/Makeconf.llvm $(FASTR_ETC_DIR)/Makeconf.llvm
	cp fastr_etc/$(shell uname)/Makeconf.llvm $(FASTR_ETC_DIR)/Makeconf
	cp fastr_etc/$(shell uname)/Makeconf.native $(FASTR_ETC_DIR)/Makeconf.native
	cp fastr_etc/Shared/Makevars.site.debug $(FASTR_ETC_DIR)
	cp fastr_etc/$(shell uname)/ldpaths.llvm $(FASTR_ETC_DIR)/ldpaths.llvm
	cp fastr_etc/$(shell uname)/ldpaths.llvm $(FASTR_ETC_DIR)/ldpaths
	cp fastr_etc/$(shell uname)/ldpaths.native $(FASTR_ETC_DIR)/ldpaths.native
	(sed 's/%%GRAALVM_VERSION%%/$(GRAALVM_VERSION)/' fastr_etc/$(shell uname)/Renviron | \
	 sed 's/%%R_VERSION_MAJ_MIN%%/$(R_VERSION_MAJ_MIN)/' > $(FASTR_ETC_DIR)/Renviron)
	cp fastr_etc/Shared/native-packages $(FASTR_ETC_DIR)
	cp fastr_etc/Shared/javaconf $(FASTR_ETC_DIR)/javaconf
	# FastR uses different default CRAN mirror, set to a fixed MRAN snapshot
	# Note: DEFAULT_CRAN_MIRROR file is FastR specific, GNU-R doesn't recognize this configuration file
	echo $(DEFAULT_CRAN_MIRROR) > $(FASTR_ETC_DIR)/DEFAULT_CRAN_MIRROR
	# Available R packages repositories configuration: taken directly from GNU-R
	cp $(GNUR_HOME_BINARY)/etc/repositories $(FASTR_ETC_DIR)/repositories
	# Additional tools provided by FastR copied to the "bin" directory
	cp $(FASTR_TOOLS) $(FASTR_BIN_DIR)
	$(GNUR_HOME_BINARY)/bin/Rscript gen-safe-forward-tools.R $(FASTR_BIN_DIR)

	# TODO: these may also need some filtering
	cp -r $(SHARE_FILES) $(FASTR_SHARE_DIR)	
	cp -r $(DOC_FILES) $(FASTR_DOC_DIR)
	# override licence file to GPLv3
	cp $(FASTR_R_HOME)/COPYING $(FASTR_DOC_DIR)
	# simple way of prepending text without sed, drawback is we copy the file twice
	echo 'All Oracle contributions in the FastR source code are licensed as GPL version 3, so that FastR as a whole is distributed under GPL version 3.' > $(FASTR_DOC_DIR)/COPYRIGHTS
	echo 'The original content of the COPYRIGHTS file from GNU R follows:' >> $(FASTR_DOC_DIR)/COPYRIGHTS
	echo '-------------------------' >> $(FASTR_DOC_DIR)/COPYRIGHTS
	cat $(GNUR_HOME_BINARY_UNPACKED)/doc/COPYRIGHTS >> $(FASTR_DOC_DIR)/COPYRIGHTS
	# overrides
	cp examples-header.R examples-footer.R $(FASTR_SHARE_DIR)/R

includedir: $(FASTR_R_HOME)/include/Rinternals.h

$(FASTR_R_HOME)/include/Rinternals.h: Makefile $(TOPDIR)/include/Rinternals.h
	mkdir -p $(FASTR_R_HOME)/include
	cp -r $(TOPDIR)/include/* $(FASTR_R_HOME)/include

clean:
	rm -rf $(FASTR_BIN_DIR)
	rm -rf $(FASTR_ETC_DIR)
	rm -rf $(FASTR_SHARE_DIR)
	rm -rf $(FASTR_INCLUDE_DIR)
