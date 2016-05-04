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

# A script to do a single package installation (+dependents), called from install.cran.packages.R.
# It exists as a separate script only to avoid internal FastR errors from killing the
# entire installation process for multiple package installation tests.

# args:
# pkgname, contriburl, lib

args <- commandArgs(TRUE)

parse.args <- function() {
	if (length(args)) {
		pkgname <<- args[[1]]
		contriburl<<- args[[2]]
		lib.install <<- args[[3]]
	}
}

run <- function() {
	parse.args()
	# TODO install Suggests for vingette testing
	install.packages(pkgname, contriburl=contriburl, type="source", lib=lib.install, INSTALL_opts="--install-tests")
}

if (!interactive()) {
	run()
}
