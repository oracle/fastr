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

# A script to install and optionally test CRAN packages, with a blacklist mechanism starting
# from a known set of packages that we cannot handle, e.g. Rcpp (due to C++)
# By default all packages are candidates for installation, but this
# can be limited in the following ways:
#
# 1. by a regexp pattern which must be the last argument on the command line
# 2. by an explicit list from a file given by the --pkg-filelist option
# 3. from the set of installed packages found in the lib install directory (option --pkg-list-installed)
#    (useful primarily for testing a set of pre-installed packages

# By default, we use the CRAN mirror specified by --cran-mirror or env var CRAN_MIRROR.
# If unset, defaults to "http://cran.cnr.berkeley.edu/"
# However, a local copy of the CRAN repo can be used either by setting the LOCAL_CRAN_REPO env variable or setting --contrib-url

# Packages are installed into the directory specified by the --lib arg (or R_LIBS_USER env var)

# Blacklisted packages nor their dependents will not be installed. By default the list of blacklisted
# packages will be read from the file in the --blacklist-file arg or the PACKAGE_BLACKLIST env var.
# If unset, defaults to "package.blacklist", and will be created if necessary. The initial set of
# blacklisted packages are read from the file specified by --initial-blacklist-file (defaults to
# value of env var INITIAL_PACKAGE_BLACKLIST or initial.package.blacklist if unset). This is
# DCF file with entries of the form:
# Package: name
# Reason: reason

# The env var R_LIBS_USER or the option --lib must be set to the directory where the install should take place.
# N.B. --lib works for installation. However, when running tests ( --run-tests), it does not and
# R_LIBS_USER must be set instead (as well) since some of the test code has explicit "library(foo)" calls
# without a "lib.loc" argument.

# A single package install can be handled in three ways, based on the run-mode argument (default system):
#   system: use a subprocess via the system2 command
#   internal: direct call to tools::install.packages
#   context: run in separate FastR context

# By default dependents are installed implicitly by the utils::install.packages function.
# However, if --install.dependents is set, the dependents of a package P are installed explicitly
# in (transitive) dependency order and, if any install fails, the install for P (and any remaining
# dependents) is aborted.

# test output goes to a directory derived from the '--testdir dir' option (default 'test'). Each package's test output is
# stored in a subdirectory named after the package.

args <- commandArgs(TRUE)

usage <- function() {
	cat(paste("usage: Rscript [--contriburl url] [--cran-mirror url] [--lib] [--verbose | -v] [-V] [--dryrun]",
                      "[--no-install | -n] [--create-blacklist] [--blacklist-file file] [--ignore-blacklist]",
					  "[--initial-blacklist-file file]",
					  "[--testcount count]", "[--ok-pkg-filelist file]",
					  "[--install.dependents]",
					  "[--run-mode mode]",
					  "[--pkg-filelist file]",
					  "[--run-tests]",
					  "[--testdir dir]",
					  "[--pkg-list-installed]",
					  "[--print-ok-installs]",
					  "[--list-versions]",
                      "[package-pattern] \n"))
	quit(status=1)
}

trim <- function (x) gsub("^\\s+|\\s+$", "", x)

strip.version <- function(x) gsub("\\s*\\(.*\\)$", "", x)

default.packages <- c("R", "base", "grid", "splines", "utils",
		"compiler", "grDevices", "methods", "stats", "stats4",
		"datasets", "graphics", "parallel", "tools", "tcltk")

# returns a vector of package names that are the direct dependents of pkg
direct.depends <- function(pkg) {
	pkgName <- pkg["Package"]
	all.deps <- character()
	for (dep in c("Depends", "Imports", "LinkingTo")) {
		deps <- pkg[dep]
		if (!is.na(deps)) {
			if (very.verbose) {
				cat(dep, " deps for: ", pkgName, " ", deps, "\n")
			}
			deplist <- strip.version(trim(unlist(strsplit(deps, fixed=T, ","))))
			# strip out R and the default packages
			deplist <- deplist[!(deplist %in% default.packages)]
			all.deps <- append(all.deps, deplist)
		}
	}
	unname(all.deps)
}

# returns the transitive set of dependencies in install order
install.order <- function(pkgs, pkg, depth=0L) {

	ndup.append <- function(v, name) {
		if (!name %in% v) {
			v <- append(v, name)
		}
		v
	}

	pkgName <- pkg["Package"]
	result <- character()
	directs <- direct.depends(pkg)
	for (direct in directs) {
		# check it is in avail.pkgs (cran)
		if (direct %in% avail.pkgs.rownames) {
			direct.result <- install.order(pkgs, pkgs[direct, ], depth=depth + 1)
			for (dr in direct.result) {
				result <- ndup.append(result, dr)
		    }
	    }
    }
	if (depth > 0L) {
	    result <- append(result, pkgName)
    }
	unname(result)
}

# blacklist is a vector of package (names) that are known to be uninstallable.
# the result is a vector of new packages that depend/import/linkto any package on blacklist
create.blacklist.with <- function(blacklist, iter) {
	this.blacklist <- vector()

	if (very.verbose) {
		cat("Iteration: ", iter, "\n\n")
	}
	for (i in (1:length(avail.pkgs.rownames))) {
		pkg <- avail.pkgs[i, ]
		pkgName <- pkg["Package"]
		if (!(pkgName %in% blacklist)) {
			if (very.verbose) {
				cat("Processing: ", pkgName, "\n")
			}
			all.deps = direct.depends(pkg)
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

create.blacklist <- function() {
	create.blacklist.iter(rownames(initial.blacklist))
}

abort <- function(msg) {
	print(msg)
	quit("no", 1)
}

set.contriburl <- function() {
	# if contriburl is set explicitly that's all we need
	if (!is.na(contriburl)) {
		return(contriburl)
	}

	# check for env var setting
	contriburl <<- Sys.getenv("LOCAL_CRAN_REPO", unset=NA)
	if (!is.na(contriburl)) {
		return(contriburl)
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
	contriburl
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

set.initial.package.blacklist <- function() {
	if (is.na(initial.blacklist.file)) {
		# not set on command line
		this_package <- "com.oracle.truffle.r.test.cran"
		initial.blacklist.file <<- Sys.getenv("INITIAL_PACKAGE_BLACKLIST", unset=file.path(this_package, "initial.package.blacklist"))
	}

}

get.installed.pkgs <- function() {
	pkg.filelist <- character();
	pkg.excludes <- character()
	pkgdirs <- list.files(lib.install, no..=T)
	# check for failed installs
	for (pkgname in pkgdirs) {
		if (grepl("00LOCK-", pkgname)) {
			pkg.exclude <- gsub("00LOCK-", "", pkgname)
			pkg.excludes <- append(pkg.excludes, pkg.exclude)
			if (verbose) {
				cat("excluding package with failed install:", pkg.exclude, "\n")
			}
		}
	}
	for (pkgname in pkgdirs) {
		if (!grepl("00LOCK-", pkgname) && !pkgname %in% pkg.excludes) {
			pkg.filelist <- append(pkg.filelist, pkgname)
		}
	}
	pkg.filelist
}

installed.ok <- function(pkgname) {
	# try to determine if the install was successful
	# 1. There must be a directory lib.install/pkgname
	# 2. There must not be a directory lib.install/00LOCK-pkgname
	if (!file.exists(file.path(lib.install, pkgname))) {
		return(FALSE)
	}
	if (file.exists(file.path("00LOCK-", pkgname))) {
		return(FALSE)
	}
	return(TRUE)
}

# find the available packages from contriburl and match those against pkg.pattern
# sets global variables avail.pkgs and toinstall.pkgs, the latter being
# of the same type as avail.pkgs but containing only those packages to install
get.pkgs <- function() {
	avail.pkgs <<- available.packages(contriburl=contriburl, type="source")
	avail.pkgs.rownames <<- rownames(avail.pkgs)
	if (pkg.list.installed) {
		pkg.filelist <- get.installed.pkgs()
		if (length(pkg.filelist) == 0) {
			stop("no installed packages")
		}
	}
	if (length(pkg.filelist) == 0) {
		match.fun <- function(x) grepl(pkg.pattern, x["Package"])
	} else {
		match.fun <- function(x) x["Package"] %in% pkg.filelist
	}
	matched.avail.pkgs <- apply(avail.pkgs, 1, match.fun)
	toinstall.pkgs <<-avail.pkgs[matched.avail.pkgs, , drop=F]
}

# performs the installation, or logs what it would install if dry.run = T
# either creates the blacklist or reads it from a file
do.it <- function() {
	get.pkgs()
	get.initial.package.blacklist()

	if (create.blacklist.file) {
		blacklist <- create.blacklist()
		writeLines(sort(blacklist), blacklist.file)
	} else {
		if (ignore.blacklist) {
			blacklist <- character()
		} else {
			blacklist <- readLines(blacklist.file)
		}
	}

	# Serially install the packages in pkgnames.
	# Return TRUE if the entire install succeeded, FALSE otherwise
	# If are.dependents=T, this is a nested install of the dependents
	# of one of the initial list.
	install.pkgs <- function(pkgnames, are.dependents=F) {
		if (verbose && !dry.run) {
			cat("packages to install (+dependents):\n")
			for (pkgname in pkgnames) {
				cat(pkgname, "\n")
			}
		}
		install.count = 1
		install.total = length(pkgnames)
		result <- TRUE
		for (pkgname in pkgnames) {
			dependent.install.ok <- T
			if (pkgname %in% blacklist) {
				cat("not installing:", pkgname, " - blacklisted\n")
			} else {
				if (!are.dependents && install.dependents) {
					dependents <- install.order(avail.pkgs, avail.pkgs[pkgname, ])
					dependent.install.ok = install.pkgs(dependents, are.dependents=T)
				}
				if (dry.run) {
					cat("would install:", pkgname, "\n")
				} else {
					if (dependent.install.ok) {
						if (pkgname %in% names(install.status)) {
							# already attempted
							if (!install.status[pkgname]) {
								# abort this (nested) install
								return(FALSE)
							}
						} else {
							cat("installing:", pkgname, "(", install.count, "of", install.total, ")", "\n")
							this.result <- install.package(pkgname)
							result <- result && this.result
							if (are.dependents && !this.result) {
								cat("aborting dependents install\n")
								return(FALSE)
							}
						}
					}
				}
			}
			install.count = install.count + 1
		}
		return(result)
	}

	if (list.versions) {
		for (i in (1:length(rownames(toinstall.pkgs)))) {
			pkg <- toinstall.pkgs[i, ]
			if (!(pkg["Package"] %in% blacklist)) {
				cat(pkg["Package"], pkg["Version"], "\n", sep=",")
			}
		}
	}

	if (is.na(testcount)) {
		# install all non-blacklisted packages in toinstall.pkgs
		test.pkgnames <- rownames(toinstall.pkgs)
	} else {
		# install testcount packages taken at random from toinstall.pkgs
		matched.toinstall.pkgs <- apply(toinstall.pkgs, 1, function(x) include.package(x, blacklist))
		test.avail.pkgs <- toinstall.pkgs[matched.toinstall.pkgs, , drop=F]
		test.avail.pkgnames <- rownames(test.avail.pkgs)
		rands <- sample(1:length(test.avail.pkgnames))
		test.pkgnames <- character(testcount)
		for (i in (1:testcount)) {
			test.pkgnames[[i]] <- test.avail.pkgnames[[rands[[i]]]]
		}
	}

	if (install) {
		cat("BEGIN package installation\n")
		install.pkgs(test.pkgnames)
		cat("END package installation\n")
		if (print.ok.installs) {
			pkgnames.i <- get.installed.pkgs()
			for (pkgname.i in pkgnames.i) {
				cat(pkgname.i, "\n")
			}
		}
	}

	if (run.tests) {
		cat("BEGIN package tests\n")
		test.count = 1
		test.total = length(test.pkgnames)
		for (pkgname in test.pkgnames) {
			if (installed.ok(pkgname)) {
				if (dry.run) {
					cat("would test:", pkgname, "\n")
				} else {
					cat("testing:", pkgname, "(", test.count, "of", test.total, ")", "\n")
					test.package(pkgname)
				}
			} else {
				cat("install failed, not testing:", pkgname, "\n")
			}
			test.count = test.count + 1
		}
		cat("END package tests\n")
	}
}

include.package <- function(x, blacklist) {
	return (!(x["Package"] %in% blacklist || x["Package"] %in% ok.pkg.filelist))
}

install.package <- function(pkgname) {
	if (run.mode == "system") {
		system.install(pkgname)
	} else if (run.mode == "internal") {
		install.packages(pkgname, contriburl=contriburl, type="source", lib=lib.install, INSTALL_opts="--install-tests")
	} else if (run.mode == "context") {
		stop("context run-mode not implemented\n")
	}
	rc <- installed.ok(pkgname)
	names(rc) <- pkgname
	install.status <- append(install.status, rc)
	return(rc)
}

system.install <- function(pkgname) {
	script <- normalizePath("com.oracle.truffle.r.test.cran/r/install.package.R")
	if (is.fastr()) {
		rscript = normalizePath("bin/Rscript")
	} else {
		rscript = "Rscript"
	}
	args <- c(script, pkgname, contriburl, lib.install)
	rc <- system2(rscript, args)
	rc
}

check.create.dir <- function(name) {
	if (!file.exists(name)) {
		if (!dir.create(name)) {
			stop(paste("cannot create: ", name))
		}
	} else {
		if(!file_test("-d", name)) {
			stop(paste(name, "exists and is not a directory"))
		}
	}
}

test.package <- function(pkgname) {
	testdir.path <- testdir
	check.create.dir(testdir.path)
	check.create.dir(file.path(testdir.path, pkgname))
	if (run.mode == "system") {
		system.test(pkgname)
	} else if (run.mode == "internal") {
		tools::testInstalledPackage(pkgname, outDir=file.path(testdir.path, pkgname), lib.loc=lib.install)
	} else if (run.mode == "context") {
		stop("context run-mode not implemented\n")
	}
}

is.fastr <- function() {
	"package:fastr" %in% search()
}

system.test <- function(pkgname) {
	script <- normalizePath("com.oracle.truffle.r.test.cran/r/test.package.R")
	if (is.fastr()) {
		rscript = normalizePath("bin/Rscript")
	} else {
		rscript = "Rscript"
	}
	args <- c(script, pkgname, file.path(testdir, pkgname), lib.install)
	rc <- system2(rscript, args)
	rc
}

get.argvalue <- function() {
	if (length(args) >= 2L) {
		value <- args[2L]
		args <<- args[-1L]
		return(value)
	} else {
		usage()
	}
}

# parse the command line arguments when run as a script
parse.args <- function() {
	while (length(args)) {
		a <- args[1L]
		if (a %in% c("-h", "--help")) {
			usage()
		} else if (a == "--contriburl") {
			contriburl <<- get.argvalue()
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
			blacklist.file <<- get.argvalue()
		} else if (a == "--initial-blacklist-file") {
			initial.blacklist.file <<- get.argvalue()
		} else if (a == "--cran-mirror") {
			cran.mirror <<- get.argvalue()
		} else if (a == "--lib") {
			lib.install <<- get.argvalue()
		} else if (a == "--testcount") {
			testcount <<- as.integer(get.argvalue())
			if (is.na(testcount)) {
				usage()
			}
		} else if (a == "--run-mode") {
			run.mode <<- get.argvalue()
			if (!(run.mode %in% c("system", "internal", "context"))) {
				usage()
			}
		} else if (a == "--pkg-filelist") {
			pkg.filelistfile <<- get.argvalue()
		} else if (a == "--ok-pkg-filelist") {
			ok.pkg.filelistfile <<- get.argvalue()
		} else if (a == "--run-tests") {
			run.tests <<- TRUE
		} else if (a == "--testdir") {
			testdir <<- get.argvalue()
		} else if (a == "--pkg-list-installed") {
			pkg.list.installed <<- T
		} else if (a == "--print-ok-installs") {
			print.ok.installs <<- T
		} else if (a == "--list-versions") {
			list.versions <<- TRUE
		} else if (a == "--install-dependents") {
			install.dependents <<- TRUE
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
		cat("initial.blacklist.file:", initial.blacklist.file, "\n")
		cat("blacklist.file:", blacklist.file, "\n")
		cat("lib.install:", lib.install, "\n")
		cat("install:", install, "\n")
		cat("install.dependents:", install.dependents, "\n")
		cat("dry.run:", dry.run, "\n")
		cat("create.blacklist.file:", create.blacklist.file, "\n")
		cat("ignore.blacklist:", ignore.blacklist, "\n")
		cat("pkg.pattern:", pkg.pattern, "\n")
		cat("contriburl:", contriburl, "\n")
		cat("testcount:", testcount, "\n")
		cat("run.mode:", run.mode, "\n")
		cat("run.tests:", run.tests, "\n")
		cat("pkg.list.installed:", pkg.list.installed, "\n")
		cat("print.ok.installs:", print.ok.installs, "\n")
		cat("testdir.path", testdir, "\n")
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

check.pkgfilelist <- function() {
	if (!is.na(pkg.filelistfile)) {
		if (file.exists(pkg.filelistfile)) {
			pkg.filelist <<- readLines(pkg.filelistfile)
		} else {
			abort(paste(pkg.filelistfile, "not found"))
		}
	}
	if (!is.na(ok.pkg.filelistfile)) {
		if (file.exists(ok.pkg.filelistfile)) {
			ok.pkg.filelist <<- readLines(ok.pkg.filelistfile)
		} else {
			abort(paste(pkg.filelistfile, "not found"))
		}
	}
}

get.initial.package.blacklist <- function() {
	if (file.exists(initial.blacklist.file)) {
		initial.blacklist <<- read.dcf(initial.blacklist.file)
		rownames(initial.blacklist) <- initial.blacklist[, "Package"]
	} else {
		abort(paste(initial.blacklist.file, "not found"))
	}
}

run <- function() {
    parse.args()
	check.libs()
	check.pkgfilelist()
	set.contriburl()
	set.initial.package.blacklist()
	set.package.blacklist()
	lib.install <<- normalizePath(lib.install)
	cat.args()
    do.it()
}

cran.mirror <- NA
contriburl <- NA
blacklist.file <- NA
initial.blacklist.file <- NA
lib.install <- NA
testdir <- "test"

pkg.pattern <- "^.*"
pkg.filelist <- character()
pkg.filelistfile <- NA
ok.pkg.filelist <- character()
ok.pkg.filelistfile <- NA
pkg.list.installed <- F
print.ok.installs <- F
verbose <- F
very.verbose <- F
install <- T
install.dependents <- F
install.status <- logical()
dry.run <- F
avail.pkgs <- NULL
avail.pkgs.rownames <- NULL
toinstall.pkgs <- NULL
create.blacklist.file <- F
ignore.blacklist <- F
testcount <- NA
run.mode <- "system"
run.tests <- FALSE
gnur <- FALSE
list.versions <- FALSE

if (!interactive()) {
    run()
}

