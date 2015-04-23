#
# Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

# Sets up the "bin" directory with scripts mostly copied from GnuR.
# The FastR'ness is handled in the "R" file in the "bin/exec" subdirectory.
# Rscript is an exception as this is a separate program in GnuR and
# stored directly in the "bin" directory.
#
# The R script defines the R_HOME environment variable from R_HOME_DIR
# which is set in the script during the GnuR build. This has to be changed.

.PHONY: bindir all rcmd

FASTR_R_HOME := $(abspath $(TOPDIR)/..)
FASTR_BINDIR = $(FASTR_R_HOME)/bin
FASTR_ETCDIR = $(FASTR_R_HOME)/etc

R_SCRIPT := $(addprefix $(GNUR_HOME)/bin/,R)
SUPPORT_SCRIPTS := $(addprefix $(GNUR_HOME)/bin/,BATCH COMPILE INSTALL Rcmd)

# Not all of these work unchanged
ETC_FILES := $(addprefix $(GNUR_HOME)/etc/,javaconf,ldpaths,Renviron,repositories)

all: rundirs rcmds

rundirs:
	mkdir -p $(FASTR_BINDIR)
	mkdir -p $(FASTR_BINDIR)/exec
	mkdir -p $(FASTR_ETCDIR)
	
rcmds: $(FASTR_BINDIR)/R

$(FASTR_BINDIR)/R: Makefile R.sh Rscript.sh Rscript_exec.sh
	cp R.sh $(FASTR_BINDIR)/exec/R
	cp Rscript_exec.sh $(FASTR_BINDIR)/exec/Rscript
	cp Rscript.sh $(FASTR_BINDIR)/Rscript
	chmod +x $(FASTR_BINDIR)/R $(FASTR_BINDIR)/exec/R $(FASTR_BINDIR)/exec/Rscript $(FASTR_BINDIR)/Rscript
	cp $(SUPPORT_SCRIPTS) $(FASTR_BINDIR)
	sed -e 's!\(^R_HOME_DIR=\)\(.*\)!\1"$(FASTR_R_HOME)"!' < $(R_SCRIPT) > $(FASTR_BINDIR)/R
	touch $(FASTR_ETCDIR)/ldpaths
	cp $(GNUR_HOME)/etc/Renviron $(FASTR_ETCDIR)
	cp Makeconf.etc $(FASTR_ETCDIR)/Makeconf

clean:
	rm -rf $(FASTR_BINDIR)
	rm -f Makeconf.etc
