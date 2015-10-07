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

# A script to install CRAN packages, with a blacklist mechanism starting from a known
# set of packages that we cannot handle, e.g. Rcpp (due to C++)
# By default all packages are candidates for installation, but this
# can be limited by a regexp pattern

# By default, we use the CRAN mirror specified by --cran-mirror or env var CRAN_MIRROR.
# If unset, defaults to "http://cran.cnr.berkeley.edu/"
# However, a local copy of the CRAN repo can be used either by setting the LOCAL_CRAN_REPO env variable or setting --contrib-url

# Packages are installed into the directory specified by the --lib arg (or R_LIBS_USER env var)

# Blacklisted packages nor their dependents will not be installed. By default the list of blacklisted
# packages will be read from the file in the --blacklist-file arg or the PACKAGE_BLACKLIST env var.
# If unset, defaults to "package.blacklist", and will be created if necessary.

# The env var R_LIBS_USER must be set to the directory where the install should take place.

# A single package install can be handled in three ways, based on the install-mode argument (default system):
#   system: use a subprocess via the system2 command
#   internal: direct call to tools::install.packages
#   context: run in separate FastR context

args <- commandArgs(TRUE)

usage <- function() {
	cat(paste("usage: Rscript [--contriburl url] [--cran-mirror url] [--lib] [--verbose | -v] [-V] [--dryrun]",
                      "[--no-install | -n] [--create-blacklist] [--blacklist-file file] [--ignore-blacklist]",
					  "[--testcount count]", "[--install-mode mode]",
					  "[package-pattern]\n"))
	quit(status=1)
}

# blacklist is a vector of package (names) that are known to be bad, i.e. uninstallable.
# the result is a vector of new packages that depend/import/linkto any package on blacklist
create.blacklist.with <- function(blacklist, iter) {
	this.blacklist <- vector()

	trim <- function (x) gsub("^\\s+|\\s+$", "", x)

	strip.version <- function(x) gsub("\\s*\\(.*\\)$", "", x)

	if (very.verbose) {
		cat("Iteration: ", iter, "\n\n")
	}
	for (i in (1:length(rownames(avail.pkgs)))) {
		pkg <- avail.pkgs[i, ]
		pkgName <- pkg["Package"]
		if (!(pkgName %in% blacklist)) {
			if (very.verbose) {
				cat("Processing: ", pkgName, "\n")
			}
			all.deps <- vector()
			for (dep in c("Depends", "Imports", "LinkingTo")) {
				deps <- pkg[dep]
				if (!is.na(dep)) {
					if (very.verbose) {
						cat(dep, " deps for: ", pkgName, " ", deps, "\n")
					}
					all.deps <-  append(all.deps, strip.version(trim(unlist(strsplit(deps, fixed=T, ",")))))
				}
			}
			if (very.verbose) {
				cat("all.deps for: ", pkgName," ", all.deps, "\n")
			}

			match.result <- match(blacklist, all.deps, nomatch=0)
			in.result <- match.result > 0
			if (any(in.result)) {
				if (verbose) {
					names(all.deps) <- NULL
					cat("adding: ", pkg["Package"], "to blacklist (", all.deps[match.result], ")\n")
				}
				this.blacklist <- append(this.blacklist, pkg["Package"])
			}
		}
	}

	names(this.blacklist) <- NULL
	this.blacklist
}

# iteratively adds to blacklist until no new blackisted packages are found
create.blacklist.iter <- function(blacklist) {
	v <-blacklist
	result <-v
	iter <- 1
	while (length(v) > 0) {
		v <- create.blacklist.with(result, iter)
		result <- append(result, v)
		iter <- iter + 1
	}
	result
}

# known to be uninstallable
# uses C++
cplusplus <- c("Rcpp", "Segmentor3IsBack", "QUIC", "kernlab", "adaptivetau", "geepack", "caTools", "amap", "rgenoud", "stringi", "rjson", "ars",
		"e1071", "aylmer", "cpm")
# tcltk
tcltk <- c("AnnotLists", "tcltk2", "aplpack")
# parser bugs
parserbug <- c("R2HTML")
# e.g., unimplemented builtin, assertion error
core <- c("ade4", "ABCoptim", "lattice", "aidar", "DBI", "SparseM", "quantreg", "doParallel", "ApacheLogProcessor", "aplore3",
		"vignettes", "archiDART", "corpcor", "acss.data")
# e.g. complex replacement assignments
trufflevisitor.nyi <- c("colorspace", "R.methodsS3")
# problems with native code
nativeinstall <- c("Rglpk", "overlap", "adimpro", "deSolve")
# S4 anything using S4 objects
s4 <- c("matrixStats", "AcceptanceSampling", "biglm", "analyz", "RCurl", "anfis", "aod", "ascii", "childsds")
# graphics
graphics <- c("Cairo", "rgl")
# incomplete definitions from Rmath.h
math <- c("mvtnorm")
# serialize
serialize <- c("actuar", "spam", "codetools", "iterators", "apc", "apsrtable", "assertthat", "citbcmst", "cubfits")
# fortran related
fortran <- c("appell", "blockmodeling", "clues", "rootSolve", "cts", "bayesQR", "cvplogistic")
initial.blacklist <- c(cplusplus, tcltk, parserbug, core, math, trufflevisitor.nyi, nativeinstall, s4, graphics, serialize, fortran)

create.blacklist <- function() {
	create.blacklist.iter(initial.blacklist)
}

abort <- function(msg) {
	print(msg)
	quit("no", 1)
}

set.contriburl <- function() {
	# if contriburl is set explicitly that's all we need
	if (!is.na(contriburl)) {
		return
	}

	# check for env var setting
	contriburl <<- Sys.getenv("LOCAL_CRAN_REPO", unset=NA)
	if (!is.na(contriburl)) {
		return
	}

	# set from the cran-mirror value
	if (is.na(cran.mirror)) {
		# not set on command line
		cran.mirror <<- Sys.getenv("CRAN_MIRROR", unset = "http://cran.cnr.berkeley.edu/")
	}
	r <- getOption("repos")
	r["CRAN"] <- cran.mirror
	options(repos = r)
	contriburl <<- contrib.url(r, "source")

}

set.package.blacklist <- function() {
	if (is.na(blacklist.file)) {
	    # not set on command line
		blacklist.file <<- Sys.getenv("PACKAGE_BLACKLIST", unset="package.blacklist")
	}
	if (!create.blacklist.file) {
		if (!file.exists(blacklist.file)) {
			cat(paste("blacklist file", blacklist.file, "does not exist, creating\n"))
			create.blacklist.file <<- T
		}
	}
}

# find the available packages from contriburl and match those against pkg.pattern
# sets global variables avail.pkgs and toinstall.pkgs
get.pkgs <- function() {
	avail.pkgs <<- available.packages(contriburl=contriburl, type="source")
	matched.avail.pkgs <- apply(avail.pkgs, 1, function(x) grepl(pkg.pattern, x["Package"]))
	toinstall.pkgs <<-avail.pkgs[matched.avail.pkgs, , drop=F]
}

# performs the installation, or logs what it would install if dry.run = T
# either creates the blacklist or reads it from a file
do.install <- function() {
	get.pkgs()

	if (create.blacklist.file) {
		blacklist <- create.blacklist()
		writeLines(sort(blacklist), con=blacklist.file)
	} else {
		if (ignore.blacklist) {
			blacklist <- character()
		} else {
			blacklist <- readLines(con=file(blacklist.file))
		}
	}

	install.pkgs <- function(pkgnames) {
		if (verbose&& !dry.run) {
			cat("packages to install (+dependents):\n")
			for (pkgname in pkgnames) {
				cat(pkgname, "\n")
			}
		}
		for (pkgname in pkgnames) {
			if (pkgname %in% blacklist) {
				cat("not installing:", pkgname, " - blacklisted\n")
			} else {
				if (dry.run) {
					cat("would install:", pkgname, "\n")
				} else {
					cat("installing:", pkgname, "\n")
					install.package(pkgname)
				}
			}
		}

	}

	if (install) {
		if (is.na(testcount)) {
			# install all non-blacklisted packages in toinstall.pkgs
			install.pkgs(rownames(toinstall.pkgs))
		} else {
			# install testcount packages taken at random from toinstall.pkgs
			matched.toinstall.pkgs <- apply(toinstall.pkgs, 1, function(x) !(x["Package"] %in% blacklist))
			test.avail.pkgs <<-toinstall.pkgs[matched.toinstall.pkgs, , drop=F]
			test.avail.pkgnames <- rownames(test.avail.pkgs)
			rands <- sample(1:length(test.avail.pkgnames))
			test.pkgnames <- character(testcount)
			for (i in (1:testcount)) {
				test.pkgnames[[i]] <- test.avail.pkgnames[[rands[[i]]]]
			}
			install.pkgs(test.pkgnames)
		}
	}
}

install.package <- function(pkgname) {
	if (install.mode == "system") {
		system.install(pkgname)
	} else if (install.mode == "internal") {
		install.packages(pkgname, contriburl=contriburl, type="source", lib=lib.install, INSTALL_opts="--install-tests")
	} else if (install.mode == "context") {
		stop("context install-mode not implemented\n")
	}
}

system.install <- function(pkgname) {
	script <- file.path(R.home(), "com.oracle.truffle.r.test.cran/r/install.package.R")
	rscript = file.path(R.home(), "bin/Rscript")
	args <- c(script, pkgname, contriburl, lib.install)
	rc <- system2(rscript, args)
	rc
}

# parse the command line arguments when run as a script
parse.args <- function() {
	while (length(args)) {
		a <- args[1L]
		if (a %in% c("-h", "--help")) {
			usage()
		} else if (a == "--contriburl") {
			if (length(args) >= 2L) {
				contriburl <<- args[2L]
				args <<- args[-1L]
			} else {
				usage()
			}
		} else if (a == "--verbose" || a == "-v") {
			verbose <<- T
		} else if (a == "-V") {
			verbose <<- T
			very.verbose <<- T
		} else if (a == "--no-install" || a == "-n") {
			install <<- F
		} else if (a == "--dryrun") {
			dry.run <<- T
		} else if (a == "--create-blacklist") {
			create.blacklist.file <<- T
		} else if (a == "--ignore-blacklist") {
			ignore.blacklist <<- T
		} else if (a == "--blacklist-file") {
			if (length(args) >= 2L) {
				blacklist.file <<- args[2L]
				args <<- args[-1L]
			} else {
				usage()
			}
		} else if (a == "--cran-mirror") {
			if (length(args) >= 2L) {
				cran.mirror <<- args[2L]
				args <<- args[-1L]
			} else {
				usage()
			}
		} else if (a == "--lib") {
			if (length(args) >= 2L) {
				lib.install <<- args[2L]
				args <<- args[-1L]
			} else {
				usage()
			}
		} else if (a == "--testcount") {
			if (length(args) >= 2L) {
				testcount <<- as.integer(args[2L])
				if (is.na(testcount)) {
					usage()
				}
				args <<- args[-1L]
			} else {
				usage()
			}
		} else if (a == "--install-mode") {
			if (length(args) >= 2L) {
				install.mode <<- args[2L]
				if (!(install.mode %in% c("system", "internal", "context"))) {
					usage()
				}
				args <<- args[-1L]
			} else {
				usage()
			}
		} else {
			if (grepl("^-.*", a)) {
				usage()
			}
			pkg.pattern <<- a
			break
		}

		args <<- args[-1L]
	}
}

cat.args <- function() {
	if (verbose) {
		cat("cran.mirror:", cran.mirror, "\n")
		cat("contriburl:", contriburl, "\n")
		cat("blacklist.file:", blacklist.file, "\n")
		cat("lib.install:", lib.install, "\n")
		cat("install:", install, "\n")
		cat("dry.run:", dry.run, "\n")
		cat("create.blacklist:", create.blacklist, "\n")
		cat("ignore.blacklist:", ignore.blacklist, "\n")
		cat("pkg.pattern:", pkg.pattern, "\n")
		cat("contriburl:", contriburl, "\n")
		cat("testcount:", testcount, "\n")
		cat("install.mode:", install.mode, "\n")
	}
}

check.libs <- function() {
	if (is.na(lib.install)) {
		lib.install <<- Sys.getenv("R_LIBS_USER", unset=NA)
	}
	if (is.na(lib.install)) {
		abort("--lib path or R_LIBS_USER must be set")
	}
	if (!file.exists(lib.install) || is.na(file.info(lib.install)$isdir)) {
		abort(paste(lib.install, "does not exist or is not a directory"))
	}
}

run <- function() {
    parse.args()
	check.libs()
	set.contriburl()
	set.package.blacklist()
    cat.args()
    do.install()
}

cran.mirror <- NA
contriburl <- NA
blacklist.file <- NA
lib.install <- NA

pkg.pattern <- "^.*"
verbose <- F
very.verbose <- F
install <- T
dry.run <- F
avail.pkgs <- NULL
toinstall.pkgs <- NULL
create.blacklist.file <- F
ignore.blacklist <- F
testcount <- NA
install.mode <- "system"

if (!interactive()) {
    run()
}

