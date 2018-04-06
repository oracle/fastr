# Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

eval(expression({
	excludedPkgs <- c("rJava")
	excludedPkgsMsgs <- c(paste0(
		"CRAN rJava is not supported on FastR, but you can download and install rJava compatible replacement package ",
		"from https://github.com/oracle/fastr/master/com.oracle.truffle.r.pkgs/rJava.\n",
		"  Install it using 'R --jvm CMD INSTALL {fastr}/com.oracle.truffle.r.pkgs/rJava' and make sure that 'which R' points to FastR. "))

	fastRPkgFilter <- function (av) {
		# The following statement will assign the url of the FastR clone of rJava, when ready (possibly on GitHub).
		#av["rJava","Repository"] <- "https://github.com/oracle/fastr/master/com.oracle.truffle.r.pkgs/rJava"
		found <- rownames(av) %in% excludedPkgs
		if (any(found)) {
			av <- av[-which(found),]
		}
		av
	}
	options(available_packages_filters = list(add = TRUE, fastRPkgFilter))
	
	getDependencies.original <- getDependencies
	getDependencies <- function(pkgs, dependencies = NA, available = NULL, lib = .libPaths()[1L], binary = FALSE) {
		res <- getDependencies.original(pkgs, dependencies, available, lib, binary)	

		found <- excludedPkgs %in% pkgs
		if (any(found)) {
			foundPkgMsgs <- excludedPkgsMsgs[which(found)]
			warning(paste(foundPkgMsgs, collapse="\n"), call. = FALSE)
		}
		
		res
	}
	
}), asNamespace("utils"))