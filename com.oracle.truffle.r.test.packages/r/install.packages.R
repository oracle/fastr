#
# Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

# A script to install and optionally test packages (typically from CRAN), with a blacklist mechanism starting
# from a known set of packages that we cannot handle, e.g. Rcpp (due to C++)
# By default all packages are candidates for installation, but this
# can be limited in the following ways:
#
# 1. by a regexp pattern which must be the last argument on the command line
# 2. by an explicit list from a file given by the --pkg-filelist option
# 3. from the set of installed packages found in the lib install directory (option --pkg-list-installed)
#    (useful primarily for testing a set of pre-installed packages

# This script can install packages from a variety of sources PROVIDED that they follow the
# structure used by CRAN, which is the main source of packages in the R world. (I.e. they support
# utils::available.packages and utils::install.packages).
# Other sources include:
#
# BioConductor
# GitHub
# FastR internal packages

# BioConductor has it's own install mechanism but it is layered on the CRAN model.

# A list of repos can be provided  with the --repos argument, which ia comma separated string of name=value pairs.
# The names "CRAN", "BIOC" and "FASTR" are understood and have default values.
# By default, we use the CRAN mirror specified in the --repos argument or env var CRAN_MIRROR.
# The default value for --repos is "CRAN=http://cloud.r-project.org/"

# Packages are installed into the directory specified by the --lib arg (or R_LIBS_USER env var)

# By default, blacklisted packages nor their dependents will not be installed. The list of blacklisted
# packages will be read from the file in the --blacklist-file arg (defaults to "package.blacklist"),
# and will be created if necessary. The initial set of blacklisted packages are read from the file specified by
# --initial-blacklist-file (defaults to "com.oracle.truffle.r.test.packages/initial.package.blacklist").
# This is a DCF file with entries of the form:
# Package: name
# Reason: reason

# The --ignore-blacklist option can be used to suppress the use of the blacklist.

# The env var R_LIBS_USER or the option --lib must be set to the directory where the install should take place.
# N.B. --lib works for installation. However, when running tests ( --run-tests), it does not and
# R_LIBS_USER must be set instead (as well) since some of the test code has explicit "library(foo)" calls
# without a "lib.loc" argument. N.B. For reasons I do not understand tools::testInstalledPackage
# explicitly sets R_LIBS to the empty string before testing the main test file (but paradoxically not when
# testing the "examples"), which is why we use R_LIBS_USER.

# A single package install can be handled in three ways, based on the run-mode argument (default system):
#   system: use a subprocess via the system2 command
#   internal: direct call to tools::install.packages
#   context: run in separate FastR context

# If --use-installed-pkgs is set the lib install directory is analyzed for existing (correctly) installed packages
# and these are not re-installed.

# By default dependents are installed implicitly by the utils::install.packages function.
# However, if --install-dependents-first is passed to this script, the dependents of a package P are installed explicitly
# in (transitive) dependency order and, if any install fails, the install for P (and any remaining
# dependents) is aborted. This also prevents re-installation when -use-installed-pkgs is set

# test output goes to a directory derived from the '--testdir dir' option (default 'test'). Each package's test output is
# stored in a subdirectory named after the package.

# There are three ways to specify the packages to be installed/tested
# --pkg-pattern a regular expression to match packages
# --pkg-filelist a file containing an explicit list of package names (not regexps), one per line
# --alpha-daily implicitly sets --pkg-pattern from the day of the year modulo 26. E.g., 0 is ^[Aa], 1 is ^[Bb]
# --ok-only implicitly sets --pkg-filelist to a list of packages known to install
# --no-install gets the list of packages from the lib install directory (evidently only useful with --run-tests)

# TODO At some point this will need to upgraded to support installation from other repos, e.g. BioConductor, github

# All fatal errors terminate with a return code of 100

# N.B. There are two unresolved problems testing some packages:
# 1. Some test files refer to packages that do not exist in the "Depends" list. Instead they
#    exists in the "Suggests" list. Unfortunately only a subset of the "Suggests" list is required and
#    there is no way to tell which. Since many of the "Suggests" packages fail to install on FastR,
#    routinely including them this can cause the entire installation to fail.
# 2. Testing vignettes requires the "knitr" and possibly the "rmarkdown" packages, which also have
#    a long list of dependents, some of which do not install on FastR.

args <- commandArgs(TRUE)

usage <- function() {
	cat(paste("usage: Rscript ",
					  "[--repos name=value,...]",
					  "[--cache-pkgs name=value,...]",
                      "[--verbose | -v] [-V] [--dryrun]",
                      "[--no-install | -n] ",
				      "[--create-blacklist] [--blacklist-file file] [--ignore-blacklist]",
					  "[--initial-blacklist-file file]",
					  "[--random count]",
					  "[--install-dependents-first]",
					  "[--run-mode mode]",
					  "[--pkg-filelist file]",
					  "[--find-top100]",
					  "[--run-tests]",
					  "[--testdir dir]",
					  "[--print-ok-installs]",
					  "[--list-versions]",
					  "[--list-canonical]",
					  "[--use-installed-pkgs]",
					  "[--invert-pkgset]",
					  "[--alpha-daily]",
					  "[--count-daily count]",
					  "[--ok-only]",
                      "[--pkg-pattern package-pattern] \n"))
	quit(status=100)
}

trim <- function (x) gsub("^\\s+|\\s+$", "", x)

strip.version <- function(x) gsub("\\s*\\(.*\\)$", "", x)

default.packages <- c("R", "base", "grid", "splines", "utils",
		"compiler", "grDevices", "methods", "stats", "stats4",
		"datasets", "graphics", "parallel", "tools", "tcltk")

choice.depends <- function(pkg, choice=c("direct","suggests")) {
	if (choice == "direct") {
		depends <- c("Depends", "Imports", "LinkingTo")
	} else {
		depends <- "Suggests"
	}
	pkgName <- pkg["Package"]
	all.deps <- character()
	for (dep in depends) {
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

# returns a vector of package names that are the direct dependents of pkg
direct.depends <- function(pkg) {
	choice.depends(pkg, "direct")
}

# returns a vector of package names that are the "Suggests" dependents of pkg
suggest.depends <- function(pkg) {
	choice.depends(pkg, "suggests")
}

# returns the transitive set of dependencies in install order
# the starting set of dependencies may be "direct" or "suggests"
# although once we start recursing, it becomes "direct"
install.order <- function(pkgs, pkg, choice, depth=0L) {

	ndup.append <- function(v, name) {
		if (!name %in% v) {
			v <- append(v, name)
		}
		v
	}

	pkgName <- pkg["Package"]
	result <- character()
	depends <- choice.depends(pkg, choice)
	for (depend in depends) {
		# check it is in avail.pkgs (cran)
		if (depend %in% avail.pkgs.rownames) {
			depend.result <- install.order(pkgs, pkgs[depend, ], "direct", depth=depth + 1)
			for (dr in depend.result) {
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
	quit("no", status=100)
}

set.repos <- function() {
	# Based on the value of repos.list we set the "repos" option
	# which is used by available.packages etc.
	repos <- character()
	for (repo in repo.list) {
		parts <- strsplit(repo, "=", fixed=T)[[1]]
		name <- parts[[1]]
		if (length(parts) > 1) {
			uri <- parts[[2]]
		} else {
			uri <- NA_character_
		}
		if (name == "BIOC") {
			# source("http://bioconductor.org/biocLite.R")
			# repos["BIOC"] <- biocinstallRepos()[1]
			# above is correct but provokes bug:
			# Error in read.table():  more columns than column names
			repos[["BIOC"]] <- "https://bioconductor.org/packages/3.4/bioc"
		} else if (name == "CRAN") {
			if (is.na(uri)) {
				# not set on command line
				cran.mirror <<- Sys.getenv("CRAN_MIRROR", unset = "http://cloud.r-project.org/")
			} else {
				cran.mirror <- uri
			}
			repos[["CRAN"]] <- cran.mirror
		} else if (name == "FASTR") {
			# set the FastR internal repo
			repos[["FASTR"]] <- paste0("file://", normalizePath("com.oracle.truffle.r.test.native/packages/repo"))
		} else {
			# User defined
			repos[[name]] <- uri
		}
	}
	options(repos = repos)
}

set.package.blacklist <- function() {
	if (is.na(blacklist.file)) {
	    # not set on command line
		blacklist.file <<- "package.blacklist"
	}
	if (!create.blacklist.file && !ignore.blacklist) {
		if (!file.exists(blacklist.file)) {
			cat(paste("blacklist file", blacklist.file, "does not exist, creating\n"))
			create.blacklist.file <<- T
		}
	}
}

this.package <- "com.oracle.truffle.r.test.packages"

set.initial.package.blacklist <- function() {
	if (is.na(initial.blacklist.file)) {
		# not set on command line
		initial.blacklist.file <<- file.path(this.package, "initial.package.blacklist")
	}

}

# Scans the package installation directory for packages that installed
# successfully or failed (indicated by leaving a 00-LOCK-pkgname file),
# depending on the value of the ok argument. Returns a vector of
# package names
get.installed.pkgs <- function(ok=T) {
	pkgs.ok <- character();
	pkgs.failed <- character()
	pkgdirs <- list.files(lib.install, no..=T)
	# find failed installs
	for (pkgname in pkgdirs) {
		if (grepl("00LOCK-", pkgname)) {
			pkg.failed <- gsub("00LOCK-", "", pkgname)
			pkgs.failed <- append(pkgs.failed, pkg.failed)
		}
	}
	# calculate ok installs
	for (pkgname in pkgdirs) {
		if (!grepl("00LOCK-", pkgname) && !pkgname %in% pkgs.failed) {
			pkgs.ok <- append(pkgs.ok, pkgname)
		}
	}
	return(if (ok) pkgs.ok else pkgs.failed)
}

get.pkgdir <- function(pkgname) {
	return(file.path(lib.install, pkgname))
}

installed.ok <- function(pkgname, initial_error_log_size) {
	# try to determine if the install was successful
	# 1. There must be a directory lib.install/pkgname
	# 2. There must not be a directory lib.install/00LOCK-pkgname
	# 3. The FastR error log must be the same size
	pkgdir <- get.pkgdir(pkgname)
	if (!file.exists(pkgdir)) {
		return(FALSE)
	}
	if (file.exists(get.pkgdir(paste0("00LOCK-", pkgname)))) {
		return(FALSE)
	}
	if (fastr_error_log_size() != initial_error_log_size) {
		# This is a really nasty case where the error happens during
		# the test load step. It is not detected by the package
		# install code and leaves no LOCK file nor does it remove
		# the faulty package, so it looks like it succeeded.
		# We delete the package dir here to reflect the failure.
		unlink(pkgdir, recursive=T)
		return(FALSE)
	}
	return(TRUE)
}

# For use with --use-installed.
# Sets up the install.status vector by scanning the package installation
# directory for OK and FAILED package installs.
# Returns the list of OK packages
check.installed.pkgs <- function() {
	pkgs.ok <- get.installed.pkgs(T)
	pkgs.failed <- get.installed.pkgs(F)
	ok <- rep_len(TRUE, length(pkgs.ok))
	failed <- rep_len(FALSE, length(pkgs.failed))
	names(ok) <- pkgs.ok
	names(failed) <- pkgs.failed
	install.status <<- c(ok, failed)
	pkgs.ok
}

# find the available packages and match those against the
# requested set of candidate packages
# sets global variables avail.pkgs and toinstall.pkgs, the latter being
# of the same type as avail.pkgs but containing only those packages to install
# returns a vector of package names to install/test
get.pkgs <- function() {
	my.warning <- function(war) {
		if (!quiet) {
			cat("Fatal error:", war$message, "\n")
		}
		quit(save="no", status=100)
	}
	tryCatch({
	    avail.pkgs <<- available.packages(type="source")
    }, warning=my.warning)

    # Owing to a FastR bug, we may not invoke the handler above, but
	# if length(avail.pkgs) == 0, that also means it failed
	if (length(avail.pkgs) == 0) {
		if (!quiet) {
		  print("Fatal error: no packages found in repo(s)")
    	}
		quit(save="no", status=100)
	}

	avail.pkgs.rownames <<- rownames(avail.pkgs)
	# get/create the blacklist
	blacklist <- get.blacklist()
	if (use.installed.pkgs) {
		installed.pkgs <- check.installed.pkgs()
	} else {
		installed.pkgs <- character()
	}

	in.blacklist <- function(x) x["Package"] %in% blacklist

	in.filelist <- function(x)  x["Package"] %in% pkg.filelist

	in.pattern <- function(x) grepl(pkg.pattern, x["Package"])

	in.installed <- function(x) x["Package"] %in% installed.pkgs

	basic.exclude <- function(x, exclude.installed = T) {
		in.blacklist(x) || ifelse(exclude.installed, in.installed(x), F)
	}

	set.match.fun <- function(exclude.installed = T) {
		# either pkg.pattern is set or pkg.filelist but not both (checked earlier)
		# if inverting, alter sense of the basic match but still exclude blacklist/installed
		if (!is.na(pkg.filelistfile)) {
			if (invert.pkgset) {
				match.fun <- function(x) !in.filelist(x) && !basic.exclude(x, exclude.installed)
			} else {
				match.fun <- function(x) in.filelist(x) && !basic.exclude(x, exclude.installed)
			}
		} else {
			if (invert.pkgset) {
				match.fun <- function(x) !in.pattern(x) && !basic.exclude(x, exclude.installed)
			} else {
				match.fun <- function(x) in.pattern(x) && !basic.exclude(x, exclude.installed)
			}
		}
	}

	match.fun <- set.match.fun()

	matched.avail.pkgs <- apply(avail.pkgs, 1, match.fun)
	toinstall.pkgs <<- avail.pkgs[matched.avail.pkgs, , drop=F]

	if (length(toinstall.pkgs) == 0 && !use.installed.pkgs) {
		print("Fatal error: requested package(s) found in repo(s)")
		quit(save="no", status=100)

	}

	if (!is.na(random.count)) {
		# install random.count packages taken at random from toinstall.pkgs
		test.avail.pkgnames <- rownames(toinstall.pkgs)
		rands <- sample(1:length(test.avail.pkgnames))
		test.pkgnames <- character(random.count)
		for (i in (1:random.count)) {
			test.pkgnames[[i]] <- test.avail.pkgnames[[rands[[i]]]]
		}
	} else {
		if (length(toinstall.pkgs) == 0) {
			# use.installed.pkgs == TRUE (see above)
			match.fun <- set.match.fun(F)
			matched.avail.pkgs <- apply(avail.pkgs, 1, match.fun)
			test.pkgnames <- rownames(avail.pkgs[matched.avail.pkgs, , drop=F])
		} else {
			test.pkgnames <- rownames(toinstall.pkgs)
			if (!is.na(count.daily)) {
				# extract count from index given by yday
				npkgs <- length(test.pkgnames)
				yday <- as.POSIXlt(Sys.Date())$yday
				chunk <- as.integer(npkgs / count.daily)
				start <- (yday %% chunk) * count.daily
				end <- ifelse(start + count.daily > npkgs, npkgs, start + count.daily - 1)
				test.pkgnames <- test.pkgnames[start:end]
			}
		}
	}

	test.pkgnames
}

# Serially install the packages in pkgnames.
# Return TRUE if the entire install succeeded, FALSE otherwise
# If dependents.install=T, this is a nested install of the dependents
# of one of the initial list. N.B. In this case pkgnames is the
# transitively computed list so this never recurses more than one level
install.pkgs <- function(pkgnames, dependents.install=F, log=T) {
	if (verbose && !dry.run) {
		cat("packages to install (+dependents):\n")
		for (pkgname in pkgnames) {
			cat(pkgname, "\n")
		}
	}
	install.count <- 1
	install.total <- length(pkgnames)
	result <- TRUE
	for (pkgname in pkgnames) {
		if (log) {
		    cat("BEGIN processing:", pkgname, "\n")
            cat("timestamp: ", Sys.time(), "\n")
		}
		dependent.install.ok <- T
		if (install.dependents.first && !dependents.install) {
			dependents <- install.order(avail.pkgs, avail.pkgs[pkgname, ], "direct")
			if (length(dependents) > 0) {
				# not a leaf package
				dep.status <- install.status[dependents]
				# three cases:
				# 1. all TRUE: nothing to do all already installed ok
				# 2. any FALSE: abort as install must fail
				# 3. a mixture of TRUE and NA: ok, but some more to install (the NAs)
				if (any(!dep.status, na.rm=T)) {
					# case 2
					cat("not installing dependents of:", pkgname, ", one or more previously failed", "\n")
					dependent.install.ok <- F
				} else {
					if (anyNA(dep.status)) {
						# case 3
						cat("installing dependents of:", pkgname, "\n")
						dependent.install.ok <- install.pkgs(dependents, dependents.install=T)
					} else {
						# case 1
					}
				}
			}
		}

		if (dry.run) {
			cat("would install:", pkgname, "\n")
		} else {
			if (!dependent.install.ok) {
				cat("not installing:", pkgname, "dependent install failure","\n")
			} else {
				should.install <- T
				if (pkgname %in% names(install.status)) {
					should.install <- F
					# already attempted
					if (!install.status[pkgname]) {
						# failed earlier
						if (dependents.install) {
							# abort this (nested) install
							return(FALSE)
						} else {
							# continue on top-level install loop
						}
					}
				}
				if (should.install) {
					cat("installing:", pkgname, "(", install.count, "of", install.total, ")", "\n")
                    cat("timestamp: ", Sys.time(), "\n")
					this.result <- install.pkg(pkgname)
					result <- result && this.result
					if (dependents.install && !this.result) {
						cat("aborting dependents install\n")
						return(FALSE)
					}
				} else {
					msg <- if (install.status[pkgname]) "already installed" else "failed earlier"
					cat("not installing:", pkgname, "(", install.count, "of", install.total, ")", msg, "\n")
				}
			}
		}
		if (log) {
		    cat("END processing:", pkgname, "\n")
		}

		install.count = install.count + 1
	}
	return(result)
}

install.suggests <- function(pkgnames) {
	for (pkgname in pkgnames) {
		suggests <- install.order(avail.pkgs, avail.pkgs[pkgname, ], "suggests")
		if (length(suggests) > 0) {
			if (is.fastr() && !ignore.blacklist) {
				# no point in trying to install blacklisted packages (which are likely)
				blacklist <- get.blacklist()
				nsuggests <- suggests[!suggests %in% blacklist]
				if (length(nsuggests) != length(suggests)) {
					cat("not installing Suggests of:", pkgname, ", one or more is blacklisted: ", paste(suggests[suggests %in% blacklist], sep=", "), "\n")
					return()
				}
			}
			dep.status <- install.status[suggests]
			# three cases:
			# 1. all TRUE: nothing to do all already installed ok
			# 2. any FALSE: ignore; tests will fail but that's ok
			# 3. a mixture of TRUE and NA: ok, but some more to install (the NAs)
			if (any(!dep.status, na.rm=T)) {
				# case 2
				cat("not installing Suggests of:", pkgname, ", one or more previously failed", "\n")
			} else {
				if (anyNA(dep.status)) {
					# case 3
					cat("installing Suggests of:", pkgname,":",paste(suggests[is.na(dep.status)], sep=", "), "\n")
					dependent.install.ok <- install.pkgs(suggests[is.na(dep.status)], dependents.install=T, log=F)
				} else {
					# case 1
				}
			}
		}
	}
}

get.blacklist <- function() {
	if (create.blacklist.file) {
		get.initial.package.blacklist()
		blacklist <- create.blacklist()
		writeLines(sort(blacklist), blacklist.file)
	} else {
		if (ignore.blacklist) {
			blacklist <- character()
		} else {
			blacklist <- readLines(blacklist.file)
		}
	}
	blacklist
}

show.install.status <- function(test.pkgnames) {
	if (print.install.status) {
		cat("BEGIN install status\n")
		for (pkgname.i in test.pkgnames) {
			cat(paste0(pkgname.i, ":"), ifelse(install.status[pkgname.i], "OK", "FAILED"), "\n")
		}
		cat("END install status\n")
	}
}

# performs the installation, or logs what it would install if dry.run = T
do.it <- function() {
	test.pkgnames <- get.pkgs()

	if (list.versions) {
		for (pkgname in test.pkgnames) {
			pkg <- toinstall.pkgs[pkgname, ]
			# pretend we are accessing CRAN if list.canonical
			list.contriburl = ifelse(list.canonical, "https://cran.r-project.org/src/contrib", pkg["Repository"])
			cat(pkg["Package"], pkg["Version"], paste0(list.contriburl, "/", pkgname, "_", pkg["Version"], ".tar.gz"), "\n", sep=",")
		}
	}

	if (install) {
		cat("BEGIN package installation\n")
        cat("timestamp: ", Sys.time(), "\n")
		install.pkgs(test.pkgnames)
		cat("END package installation\n")
		show.install.status(test.pkgnames)
	}

	if (run.tests) {
		if (!install) {
			# The starting set is just what is installed
			test.pkgnames = check.installed.pkgs()
			if (!is.na(pkg.filelistfile)) {
				match.fun <- function(x)  x %in% pkg.filelist
			} else {
				match.fun <- function(x) grepl(pkg.pattern, x)
			}
			matched.pkgnames <- sapply(test.pkgnames, match.fun)
			test.pkgnames <- test.pkgnames[matched.pkgnames]
			# fake the install
			show.install.status(test.pkgnames)
		}

		# need to install the Suggests packages as they may be used
		cat('BEGIN suggests install\n')
        cat("timestamp: ", Sys.time(), "\n")
		install.suggests(test.pkgnames)
		cat('END suggests install\n')

		cat("BEGIN package tests\n")
        cat("timestamp: ", Sys.time(), "\n")
		test.count = 1
		test.total = length(test.pkgnames)
		for (pkgname in test.pkgnames) {
			if (install.status[pkgname]) {
				if (dry.run) {
					cat("would test:", pkgname, "\n")
				} else {
					cat("BEGIN testing:", pkgname, "(", test.count, "of", test.total, ")", "\n")
					test.package(pkgname)
					cat("END testing:", pkgname, "\n")
				}
			} else {
				cat("install failed, not testing:", pkgname, "\n")
			}
			test.count = test.count + 1
		}
		cat("END package tests\n")
	}
}

# Should package "x" be included in the install?
# No, if it is in the blacklist set (what about --ignore-blacklist?)
# Nor if it is in ok.pkg.filelist (what does this achieve)
include.package <- function(x, blacklist) {
	return (!(x["Package"] %in% blacklist || x["Package"] %in% ok.pkg.filelist))
}

fastr_error_log_size <- function() {
	size <- file.info("fastr_errors.log")$size
	if (is.na(size)) {
		return(0)
	} else {
		return(size)
	}
}

# installs a single package or retrieves it from the cache
install.pkg <- function(pkgname) {
	error_log_size <- fastr_error_log_size()
	if (run.mode == "system") {
        pkg.cache.install(pkgname, function() system.install(pkgname))
	} else if (run.mode == "internal") {
        pkg.cache.install(pkgname, function() install.packages(pkgname, type="source", lib=lib.install, INSTALL_opts="--install-tests"))
	} else if (run.mode == "context") {
		stop("context run-mode not implemented\n")
	}
	rc <- installed.ok(pkgname, error_log_size)
	names(rc) <- pkgname
	install.status <<- append(install.status, rc)
	return(rc)
}

pkg.cache.install <- function(pkgname, install.cmd) {
    is.cached <- pkg.cache.get(pkgname, lib=lib.install)
    if (!is.cached) {
        install.cmd()
        pkg.cache.insert(pkgname, lib.install)
    }
}

pkg.cache.get <- function(pkgname, lib) {
    # check if caching is enabled
    if (!pkg.cache$enabled) {
        return (FALSE)
    }

    # check if package cache directory can be accessed
    if (dir.exists(pkg.cache$dir) && any(file.access(pkg.cache$dir, mode = 6) == -1)) {
        log.message("package cache error: cannot access package cache dir ", pkg.cache$dir, level=1)
        return (FALSE)
    }

    # check cache directory has valid structure
    if (!is.valid.cache.dir(pkg.cache$dir)) {
        pkg.cache.init(pkg.cache$dir, pkg.cache$version)
    }

    # get version sub-directory
    version.dir <- pkg.cache.get.version(pkg.cache$dir, pkg.cache$version)
    if (is.null(version.dir)) {
        log.message("package cache error: cannot access or create version subdir for ", pkg.cache$version, level=1)
        return (FALSE)
    }

    log.message("using package cache directory ", version.dir, level=1)

    # lookup package dir
    pkg.dirs <- list.dirs(version.dir, full.names=FALSE, recursive=FALSE)
    if (!is.na(match(pkgname, pkg.dirs))) {
        # cache hit
        fromPath <- file.path(version.dir, pkgname)
        toPath <- lib

        # copy from cache to package library
        if(!pkg.cache.import.dir(fromPath, toPath)) {
            log.message("could not copy/link package dir from ", fromPath , " to ", toPath, level=1)
            return (FALSE)
        }
        log.message("package cache hit, using package from ", fromPath)
        return (TRUE)
    } 
    log.message("cache miss for package ", pkgname, level=1)
    
    FALSE
}

pkg.cache.import.dir <- function(fromPath, toPath) {
    if (any(as.logical(pkg.cache$link), na.rm=T)) {
        file.symlink(fromPath, toPath)
    } else {
        file.copy(fromPath, toPath, recursive=TRUE)
    }
}

pkg.cache.insert <- function(pkgname, lib) {
    # check if caching is enabled
    if (!pkg.cache$enabled) {
        return (FALSE)
    }

    # check if package cache directory can be accessed
    if (dir.exists(pkg.cache$dir) && any(file.access(pkg.cache$dir, mode = 6) == -1)) {
        log.message("cannot access package cache dir ", pkg.cache$dir, level=1)
        return (FALSE)
    }

    # check cache directory has valid structure
    if (!is.valid.cache.dir(pkg.cache$dir)) {
        pkg.cache.init(pkg.cache$dir, as.character(pkg.cache$version))
    }

    # get version sub-directory
    version.dir <- pkg.cache.get.version(pkg.cache$dir, as.character(pkg.cache$version))
    if (is.null(version.dir)) {
        log.message("cannot access or create version subdir for ", as.character(pkg.cache$version), level=1)
        return (FALSE)
    }

    tryCatch({
        # Create version directory if inexisting
        if (!dir.exists(version.dir)) {
            log.message("creating version directory ", version.dir, level=1)
            dir.create(version.dir)
        }

        fromPath <- file.path(lib, pkgname)
        toPath <- version.dir

        # copy from cache to package library
        if(!file.copy(fromPath, toPath, recursive=TRUE)) {
            log.message("could not copy dir ", fromPath , " from package cache to ", toPath, level=1)
            return (FALSE)
        }
        log.message("successfully inserted package ", pkgname , " to package cache (", toPath, ")")
        return (TRUE)
    }, error = function(e) {
        log.message("could not insert package '", pkgname, "' because: ", e$message)
    })
    FALSE
}

is.valid.cache.dir <- function(cache.dir) {
    if (!dir.exists(cache.dir)) {
        return (FALSE)
    }

    # look for the version table
    version.table.name <- file.path(cache.dir, pkg.cache$table.file.name)
    if (any(file.access(version.table.name, mode = 6) == -1)) {
        return (FALSE)
    }

    tryCatch({
        version.table <- read.csv(version.table.name)
        # TODO: check if versions have appropriate subdirs
        TRUE
    }, error = function(e) {
        log.message("could not read package cache's version table: ", e$message, level=1)
        FALSE
    })
}

# Generates a package cache API version directory using the first 20 characters (if available) from the version.
pkg.cache.gen.version.dir.name <- function(version) {
    paste0("library", substr(version, 1, max(20,length(version))))
}

pkg.cache.init <- function(cache.dir, version) {
    if (is.null(version)) {
        # This has been logged during argument parsing.
        return (NULL)
    }

    if (!dir.exists(cache.dir)) {
        log.message("creating cache directory ", cache.dir, level=1)

        tryCatch({
            dir.create(cache.dir)
        }, error = function(e) {
            log.message("could create package cache dir '", cache.dir, "' because: ", e$message)
        })
    }

    version.table.name <- file.path(cache.dir, pkg.cache$table.file.name)

    # create package lib dir for this version (if not existing)
    version.table <- pkg.cache.create.version(cache.dir, version, data.frame(version=character(0),dir=character(0),ctime=double(0)))
    tryCatch({
        write.csv(version.table, version.table.name, row.names=FALSE)
    }, error = function(e) {
        log.message("could not write version table to file ", version.table.name, " because: ", e$message)
    })
    NULL
}

# creates package lib dir for this version (if not existing)
pkg.cache.create.version <- function(cache.dir, version, version.table) {
    version.table.name <- file.path(cache.dir, pkg.cache$table.file.name)
    version.subdir <- pkg.cache.gen.version.dir.name(version)
    version.dir <- file.path(cache.dir, version.subdir)

    # We do not create the version directory here because we cannot guarantee that this will stay in sync
    # with the version table anyway.

    # Do cleanup if cache dir size exceeds
    while (pkg.cache$size > 0 && nrow(version.table) >= pkg.cache$size) {
        # Remove oldest version (if any)
        order <- order(version.table$ctime)
        if (length(order) > 0) {
            oldest.entry <- version.table[order[[1]],]
            oldest.dir <- file.path(cache.dir, as.character(oldest.entry$dir))
            log.message("removing oldest version ", as.character(oldest.entry$version), " with dir ", oldest.dir, level=1)
            version.table <- version.table[-order[[1]],]

            # delete directory
            tryCatch({
                unlink(oldest.dir, recursive=TRUE)
            }, error = function(e) {
                log.message("could not remove directory ", oldest.dir, " from cache", level=1)
            })
        } else {
            # just to be sure
            break ()
        }
    }

    # Add entry to version table
    log.message("adding entry for ", version, level=1)
    rbind(version.table, data.frame(version=version,dir=version.subdir,ctime=as.double(Sys.time())))
}

pkg.cache.get.version <- function(cache.dir, cache.version) {
    if (is.null(cache.version)) {
        return (NULL)
    }

    # look for 'version.table'
    version.table.name <- file.path(cache.dir, pkg.cache$table.file.name)
    if (any(file.access(version.table.name, mode = 6) == -1)) {
        return (NULL)
    }

    tryCatch({
        version.table <- read.csv(version.table.name)
        version.subdir <- as.character(version.table[version.table$version == cache.version, "dir"])
        updated.version.table <- NULL
        if (length(version.subdir) == 0L) {
            updated.version.table <- pkg.cache.create.version(cache.dir, cache.version, version.table)
        }
        if (!is.null(updated.version.table)) {
            version.subdir <- as.character(updated.version.table[updated.version.table$version == cache.version, "dir"])
            write.csv(updated.version.table, version.table.name, row.names=FALSE)
        }

        # return the version directory
        file.path(cache.dir, version.subdir)
    }, error = function(e) {
        log.message("error reading/writing 'version.table': ", e$message, level=1)
        NULL
    })
}

# when testing under graalvm, fastr is not built so we must use the (assumed) sibling gnur repo
check_graalvm <- function() {
	if (!is.na(Sys.getenv('FASTR_GRAALVM', unset=NA)) || !is.na(Sys.getenv('GRAALVM_FASTR', unset=NA))) {
		normalizePath(Sys.glob(file.path("..", 'gnur', 'gnur', 'R-*')))
	} else {
		NA
	}
}

gnu_rscript <- function() {
	gnur_dir <- check_graalvm()
	if (!is.na(gnur_dir)) {
		file.path(gnur_dir, 'bin', 'Rscript')
	} else {
		rv <- R.Version()
		dirv <- paste0('R-', rv$major, '.', rv$minor)
		gnurHome <- Sys.getenv("GNUR_HOME_BINARY")
		if (gnurHome == "") {
			gnurHome <- "libdownloads"
		}
		file.path(gnurHome, dirv, 'bin', 'Rscript')
	}
}

system.install <- function(pkgname) {
	script <- normalizePath("com.oracle.truffle.r.test.packages/r/install.package.R")
	if (is.fastr()) {
		rscript = file.path(R.home(), "bin", "Rscript")
	} else {
		rscript = gnu_rscript()
	}
	args <- c(script, pkgname, paste0(contrib.url(getOption("repos"), "source"), collapse=","), lib.install)
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
	start.time <- proc.time()[[3]]
	if (run.mode == "system") {
		system.test(pkgname)
	} else if (run.mode == "internal") {
		tools::testInstalledPackage(pkgname, outDir=file.path(testdir.path, pkgname), lib.loc=lib.install)
	} else if (run.mode == "context") {
		stop("context run-mode not implemented\n")
	}
	end.time <- proc.time()[[3]]
	cat("TEST_TIME:", pkgname, end.time - start.time, "\n")
}

is.fastr <- function() {
	exists(".fastr.context.get", baseenv())
}

system.test <- function(pkgname) {
	script <- normalizePath("com.oracle.truffle.r.test.packages/r/test.package.R")
	if (is.fastr()) {
		rscript = file.path(R.home(), "bin", "Rscript")
	} else {
		rscript = gnu_rscript()
	}
	args <- c(script, pkgname, file.path(testdir, pkgname), lib.install)
	# we want to stop tests that hang, but some packages have many tests
	# each of which spawns a sub-process (over which we have no control)
	# so we time out the entire set after 20 minutes.
	rc <- system2(rscript, args, env=c("FASTR_PROCESS_TIMEOUT=20", paste0("R_LIBS_USER=",shQuote(lib.install)),"R_LIBS="))
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

# parse the (command line) arguments
parse.args <- function() {
	while (length(args)) {
		a <- args[1L]
		if (a %in% c("-h", "--help")) {
			usage()
		} else if (a == "--verbose" || a == "-v") {
			verbose <<- T
		} else if (a == "-V") {
			verbose <<- T
			very.verbose <<- T
		} else if (a == "--quiet") {
			quiet <<- T
		} else if (a == "--no-install" || a == "-n") {
			install <<- F
		} else if (a == "--dryrun" || a == "--dry-run") {
			dry.run <<- T
		} else if (a == "--create-blacklist") {
			create.blacklist.file <<- T
		} else if (a == "--ignore-blacklist") {
			ignore.blacklist <<- T
		} else if (a == "--blacklist-file") {
			blacklist.file <<- get.argvalue()
		} else if (a == "--initial-blacklist-file") {
			initial.blacklist.file <<- get.argvalue()
		} else if (a == "--repos") {
			repo.list <<- strsplit(get.argvalue(), ",")[[1]]
		} else if (a == "--cache-pkgs") {
            pkg.cache$enabled <- TRUE
            svalue <- strsplit(get.argvalue(), ",")[[1]]
	        for (s in svalue) {
                arg <- strsplit(s, "=", fixed=T)[[1]]
                assign(arg[[1]], arg[[2]], envir=pkg.cache)
            }
		} else if (a == "--random") {
			random.count <<- as.integer(get.argvalue())
			if (is.na(random.count)) {
				usage()
			}
		} else if ( a == "--alpha-daily") {
			day.index <- as.POSIXlt(Sys.Date())$yday %% 26
			l <- letters[day.index]
			ul <- toupper(l)
			pkg.pattern <<- paste0("^[", ul, l, "]")
		} else if ( a == "--count-daily") {
			count.daily <<- as.integer(get.argvalue())
			if (is.na(count.daily)) {
				usage()
			}
		} else if ( a == "--ok-only") {
			pkg.filelistfile <<- file.path(this.package, "ok.packages")
		} else if (a == "--run-mode") {
			run.mode <<- get.argvalue()
			if (!(run.mode %in% c("system", "internal", "context"))) {
				usage()
			}
		} else if (a == "--pkg-filelist") {
			pkg.filelistfile <<- get.argvalue()
		} else if (a == "--pkg-pattern") {
			pkg.pattern <<- get.argvalue()
		} else if (a == "--run-tests") {
			run.tests <<- TRUE
		} else if (a == "--testdir") {
			testdir <<- get.argvalue()
		} else if (a == "--print-install-status" || a == "--print-ok-installs") {
			print.install.status <<- T
		} else if (a == "--list-versions") {
			list.versions <<- TRUE
		} else if (a == "--list-canonical") {
			list.canonical <<- TRUE
		} else if (a == "--install-dependents-first") {
			install.dependents.first <<- TRUE
		} else if (a == "--use-installed-pkgs") {
			use.installed.pkgs <<- TRUE
		} else if (a == "--invert-pkgset") {
			invert.pkgset <<- TRUE
		} else if (a == "--find-top100") {
			find.top100 <<- TRUE
		} else {
			if (grepl("^-.*", a)) {
				usage()
			}
			# backwards compatibility
			pkg.pattern <<- a
		}

		args <<- args[-1L]
	}
	if (!is.na(pkg.pattern) && !is.na(pkg.filelistfile)) {
		stop("--pkg.pattern and --pkg.filelist are mutually exclusive")
	}
	if (is.na(pkg.pattern) && is.na(pkg.filelistfile)) {
	    pkg.pattern <<- "^.*"
	}
	if (!install) {
		use.installed.pkgs <<- T
	}
	# list.versions is just that
    if (list.versions) {
		install <<- F
		run.tests <<- F
	}
}

cat.args <- function() {
	if (verbose) {
		cat("cran.mirror:", cran.mirror, "\n")
		cat("initial.blacklist.file:", initial.blacklist.file, "\n")
		cat("blacklist.file:", blacklist.file, "\n")
		cat("lib.install:", lib.install, "\n")
		cat("install:", install, "\n")
		cat("install.dependents.first:", install.dependents.first, "\n")
		cat("dry.run:", dry.run, "\n")
		cat("create.blacklist.file:", create.blacklist.file, "\n")
		cat("ignore.blacklist:", ignore.blacklist, "\n")
		cat("pkg.pattern:", pkg.pattern, "\n")
		cat("random.count:", random.count, "\n")
		cat("count.daily:", count.daily, "\n")
		cat("run.mode:", run.mode, "\n")
		cat("run.tests:", run.tests, "\n")
		cat("print.install.status:", print.install.status, "\n")
		cat("use.installed.pkgs:", use.installed.pkgs, "\n")
		cat("invert.pkgset:", invert.pkgset, "\n")
		cat("testdir.path", testdir, "\n")
		cat("pkg.cache:", pkg.cache$enabled, "\n")
	}
}

log.message <- function(..., level=0) {
    if(level == 0 || verbose) {
        cat(..., "\n")
    }
}

check.libs <- function() {
    lib.install <<- Sys.getenv("R_LIBS_USER", unset=NA)
	if (is.na(lib.install)) {
		abort("R_LIBS_USER must be set")
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
}

get.initial.package.blacklist <- function() {
	if (file.exists(initial.blacklist.file)) {
		initial.blacklist <<- read.dcf(initial.blacklist.file)
		rownames(initial.blacklist) <- initial.blacklist[, "Package"]
	} else {
		abort(paste(initial.blacklist.file, "not found"))
	}
}

do.find.top100 <- function() {
	avail.pkgs <- available.packages(type="source");
	if (!require('cranlogs', quietly = T)) {
		install.packages('cranlogs', quiet = T)
		library('cranlogs', quietly = T)
	}
	top100 <- cran_top_downloads(when = c("last-day", "last-week", "last-month"), count = 100)
	names <- top100[['package']]
	l = length(names)
	for (i in 1:l) {
		pkgname <- names[[i]]
		pkg <- avail.pkgs[pkgname, ]
		list.contriburl = ifelse(list.canonical, "https://cran.r-project.org/src/contrib", pkg["Repository"])
		cat(pkg["Package"], pkg["Version"], paste0(list.contriburl, "/", pkgname, "_", pkg["Version"], ".tar.gz"), "\n", sep = ",")
	}
}

run.setup <- function() {
	check.libs()
	check.pkgfilelist()
	set.repos()
	set.initial.package.blacklist()
	set.package.blacklist()
	lib.install <<- normalizePath(lib.install)
	cat.args()
}

run <- function() {
	parse.args()
	if (find.top100) {
		set.repos()
		do.find.top100()
	} else {
	    run.setup()
        do.it()
    }
}

quiet <- F
repo.list <- c("CRAN")
pkg.cache <- as.environment(list(enabled=FALSE, table.file.name="version.table", size=2L,link=FALSE))
cran.mirror <- NA
blacklist.file <- NA
initial.blacklist.file <- NA
lib.install <- NA
testdir <- "test"

pkg.pattern <- NA
pkg.filelist <- character()
pkg.filelistfile <- NA
print.install.status <- F
use.installed.pkgs <- F
verbose <- F
very.verbose <- F
install <- T
install.dependents.first <- F
install.status <- logical()
dry.run <- F
avail.pkgs <- NULL
avail.pkgs.rownames <- NULL
toinstall.pkgs <- NULL
create.blacklist.file <- F
ignore.blacklist <- F
random.count <- NA
count.daily <- NA
run.mode <- "system"
run.tests <- FALSE
gnur <- FALSE
list.versions <- FALSE
list.canonical <- FALSE
invert.pkgset <- F
find.top100 <- F

if (!interactive()) {
    run()
}

