#
# Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

LLVM_TOOLS := $(wildcard $(FASTR_NATIVE_DIR)/llvm_tools/*)

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

	# overide bin/R
	cp R.sh $(FASTR_BIN_DIR)/exec/R
	# override bin/Rscript
	cp Rscript_exec.sh $(FASTR_BIN_DIR)/execRextras/Rscript
	cp Rscript.sh $(FASTR_BIN_DIR)/Rscript
	# for embedded support
	cp Rclasspath.sh $(FASTR_BIN_DIR)/execRextras/Rclasspath
	chmod +x $(FASTR_BIN_DIR)/exec/R $(FASTR_BIN_DIR)/execRextras/Rscript $(FASTR_BIN_DIR)/Rscript $(FASTR_BIN_DIR)/execRextras/Rclasspath
	# update R_HOME_DIR to FastR, --help to -r-help because --help is handled by FastR, and pass args to Rcmd (esp. --jvm)
	(sed -e 's!^\(R_HOME_DIR=\)\(.*\)!\1"$(FASTR_R_HOME)"!' | \
	 sed -e 's/    -h.--help./    --r-help\)/' | \
	 sed -e 's!      exec sh "$${R_HOME}/bin/Rcmd" "$${@}" ;;!      FASTR_INTERNAL_ARGS=`echo "$${FASTR_INTERNAL_ARGS} $${args}" | $${SED} -e 's/^[[:space:]]*//' | $${SED} -e 's/[[:blank:]]*$$//'` exec sh "$${R_HOME}/bin/Rcmd" "$${@}" ;;!') < $(R_SCRIPT) > $(FASTR_BIN_DIR)/R
	chmod +x $(FASTR_BIN_DIR)/R
	touch $(FASTR_ETC_DIR)/ldpaths
	cp $(shell uname)/Renviron $(FASTR_ETC_DIR)/Renviron
	mkdir -p $(FASTR_ETC_DIR)/src/include
	cp config.h.in $(FASTR_ETC_DIR)/src/include
	cp Renviron.in $(FASTR_ETC_DIR)/Renviron.in
	cp $(GNUR_HOME_BINARY)/etc/ldpaths.in $(FASTR_ETC_DIR)/ldpaths.in
	cp $(GNUR_HOME_BINARY)/etc/Makeconf Makeconf.etc
	cp $(GNUR_HOME_BINARY)/etc/javaconf $(FASTR_ETC_DIR)/javaconf
	cp $(GNUR_HOME_BINARY)/etc/repositories $(FASTR_ETC_DIR)/repositories
	cp $(GNUR_HOME_BINARY)/etc/ldpaths $(FASTR_ETC_DIR)/ldpaths
	# the ed script adds -DFASTR to compiler options and removes JAVA related variables
	ed Makeconf.etc < edMakeconf.etc

	echo $(DEFAULT_CRAN_MIRROR) > $(FASTR_ETC_DIR)/DEFAULT_CRAN_MIRROR

	# Alter the configuration which is used for building R packages
	# Create symlinks to the Labs LLVM tools inside FASTR_HOME/bin
	# Copy our f2c-wrapper tool to FASTR_HOME/bin
	# Change the variables like CC in FASTR_HOME/etc/Makevars to point to these tools
	# TODO: ar tool
	ed Makeconf.etc < edMakeconf.etc.llvm
	sed -i -e 's/-fopenmp//' Makeconf.etc
	sed -i -e 's~LIBR0 =~LIBR0 = -lR~' Makeconf.etc
	sed -i -e 's~LDFLAGS =~LDFLAGS = -Wl,-rpath,$(FASTR_R_HOME)/lib/ -L$(FASTR_R_HOME)/lib -lf2c~' Makeconf.etc
	sed -i -e 's~^CXXFLAGS =\(.*\)~CXXFLAGS = \1 $(FASTR_CXXFLAGS)~g' Makeconf.etc
	# A workaround for a Sulong issue: TODO: the issue ID
	sed -i -e 's/-O2//' Makeconf.etc
	# the undefined option is not compatible with LLVM bundling
	sed -i -e 's/-undefined dynamic_lookup//' Makeconf.etc

	cp $(LLVM_TOOLS) $(FASTR_BIN_DIR)

	cp Makeconf.etc $(FASTR_ETC_DIR)/Makeconf
	cp Makevars.site.debug $(FASTR_ETC_DIR)

	## deploy helper script that can build native-image of FastR and update the launchers in GraalVM
	cp install_r_native_image $(FASTR_BIN_DIR)
	## prepare the configuration artefacts for the manual FastR configuration in GraalVM
	cp configure_fastr $(FASTR_BIN_DIR)
	chmod +x $(FASTR_BIN_DIR)/configure_fastr
	cp configure.ac $(FASTR_ETC_DIR)
	cp configure $(FASTR_ETC_DIR)
	cp edMakeconf.etc $(FASTR_ETC_DIR)
	cp edMakeconf.etc.llvm $(FASTR_ETC_DIR)
	cp $(GNUR_HOME_BINARY)/etc/Makeconf.in $(FASTR_ETC_DIR)
	cp native-packages $(FASTR_ETC_DIR)
	cp $(GNUR_HOME_BINARY)/VERSION $(FASTR_ETC_DIR)
	cp -R $(GNUR_HOME_BINARY)/m4 $(FASTR_ETC_DIR)
	cp -R $(GNUR_HOME_BINARY)/tools $(FASTR_ETC_DIR)

	cp -r $(SHARE_FILES) $(FASTR_SHARE_DIR)
	# TODO may need filtering
	cp -r $(DOC_FILES) $(FASTR_DOC_DIR)
	# override licence file to GPLv3
	cp $(FASTR_R_HOME)/COPYING $(FASTR_DOC_DIR)
	# simple way of prepending text without sed, drawback is we copy the file twice
	echo 'All Oracle contributions in the FastR source code are licensed as GPL version 3, so that FastR as a whole is distributed under GPL version 3.' > $(FASTR_DOC_DIR)/COPYRIGHTS
	echo 'The original content of the COPYRIGHTS file from GNU R follows:' >> $(FASTR_DOC_DIR)/COPYRIGHTS
	echo '-------------------------' >> $(FASTR_DOC_DIR)/COPYRIGHTS
	cat $(GNUR_HOME_BINARY)/doc/COPYRIGHTS >> $(FASTR_DOC_DIR)/COPYRIGHTS
	# overrides
	cp examples-header.R examples-footer.R $(FASTR_SHARE_DIR)/R
	# record the FFI version
	bash -c "echo -n $(FASTR_RFFI) >$(FASTR_ETC_DIR)/ffibuildtype"

includedir: $(FASTR_R_HOME)/include/Rinternals.h

$(FASTR_R_HOME)/include/Rinternals.h: Makefile $(TOPDIR)/include/Rinternals.h
	mkdir -p $(FASTR_R_HOME)/include
	cp -r $(TOPDIR)/include/* $(FASTR_R_HOME)/include

clean:
	rm -rf $(FASTR_BIN_DIR)
	rm -rf $(FASTR_ETC_DIR)
	rm -rf $(FASTR_SHARE_DIR)
	rm -rf $(FASTR_INCLUDE_DIR)
