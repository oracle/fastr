# Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

eval(expression({

	.fastr.addHelpPath('/com/oracle/truffle/r/library/utils/Rd')

	fastrRepoPath <- Sys.getenv("INSTALL_FASTR_PKGS_REPO_PATH")

	install.fastr.packages <- function(pkgs, lib, INSTALL_opts=character(0)) {
		if (is.null(fastrRepoPath) || fastrRepoPath == "" || !file.exists(fastrRepoPath)) {
			workDir <- tempdir()
			download.file('https://api.github.com/repos/oracle/fastr/tarball/master', file.path(workDir, 'fastr-repo.tar.gz'))
			origFiles <- list.files(workDir)
			untar(file.path(workDir, 'fastr-repo.tar.gz'), exdir=workDir)
			repoName <- setdiff(list.files(workDir), origFiles)
			fastrRepoPath <<- file.path(workDir, repoName)
		}
		for (pkg in pkgs) {
			pkgPath <- file.path(fastrRepoPath, 'com.oracle.truffle.r.pkgs', pkg)
			if (file.exists(pkgPath)) {
                if (missing(lib)) {
				    install.packages(pkgPath, repos=NULL, INSTALL_opts=INSTALL_opts)
                } else {
				    install.packages(pkgPath, lib=lib, repos=NULL, INSTALL_opts=INSTALL_opts)
                }
			} else {
				stop(paste0("FastR doesn't provide patched version of package ", pkg, ". Use install.packages to install it."));
			}
		}
		invisible(NULL)
	}

	pkgWarnings <- c(
		rJava = c(paste0(
			"CRAN rJava is not supported on FastR, but you can download and install rJava compatible replacement package ",
			"from https://github.com/oracle/fastr/master/com.oracle.truffle.r.pkgs/rJava.\n",
			"You can run function install.fastr.packages('rJava') to install it from GitHub.")),
		data.table = c(paste0(
			"CRAN data.table uses some C API that FastR cannot emulate, there is a patched version of data.table available ",
			"at https://github.com/oracle/fastr/master/com.oracle.truffle.r.pkgs/data.table.\n",
			"You can run function install.fastr.packages('data.table') to install it from GitHub."))
	)

	excludedPkgs <- c('rJava', 'data.table')

	fastRPkgFilter <- function (av) {
		# The following statement will assign the url of the FastR clone of rJava, when ready (possibly on GitHub).
		# Note: this will not work, the following code is creating the URL like so: {repo}/{pkgName}_{version}.
		# What we can do is to override certain URLs in the Download builtin
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
		found <- names(pkgWarnings) %in% pkgs
		if (any(found)) {
			for (w in pkgWarnings[found]) {
				warning(w, call. = FALSE)
			}
		}
		res
	}
	
}), asNamespace("utils"))

# export new public functions
exports <- asNamespace("utils")[[".__NAMESPACE__."]][['exports']]
assign('install.fastr.packages', 'install.fastr.packages', envir = exports)
