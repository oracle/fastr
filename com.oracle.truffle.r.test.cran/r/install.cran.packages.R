# A script to install CRAN packages, with a blacklist mechanism starting from a known
# set of packages that we cannot handle, e.g. Rcpp (due to C++)
# By default all packages are candidates for installation, but this
# can be limited by a regexp pattern

args <- commandArgs(TRUE)

usage <- function() {
	cat("usage: Rscript [--contriburl url] [--verbose | -v] [-dryrun] [--save-blacklist file] [--blacklist file] [package-pattern\n")
	quit(status=1)
}

# blacklist is a vector of package (names) that are known to be bad, i.e. uninstallable.
# the result is a vector of new packages that depend/import/suggest/linkto any package on blacklist
create.blacklist.with <- function(blacklist) {
	this.blacklist <- vector()

	trim <- function (x) gsub("^\\s+|\\s+$", "", x)

	strip.version <- function(x) gsub("\\s+\\(.*\\)$", "", x)

	for (i in (1:length(rownames(avail.pkgs)))) {
		pkg <- avail.pkgs[i, ]
		if (!(pkg["Package"] %in% blacklist)) {
			all.deps <- vector()
			for (dep in c("Depends", "Imports", "LinkingTo")) {
				deps <- pkg[dep]
				if (!is.na(dep)) {
					if (very.verbose) {
						cat(dep, " deps for: ", pkg["Package"], " ", deps, "\n")
					}
					all.deps <-  append(all.deps, strip.version(trim(unlist(strsplit(deps, fixed=T, ", ")))))
				}
			}

			match.result <- match(blacklist, all.deps, nomatch=0)
			in.result <- match.result > 0
			if (any(in.result)) {
				if (verbose) {
					names(all.deps) <- NULL
					cat("adding: ", pkg["Package"], " to blacklist (", all.deps[match.result], ")\n")
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
	while (length(v) > 0) {
		v <- create.blacklist.with(result)
		result <- append(result, v)
	}
	result
}

# known to be uninstallable
initial.blacklist <- c("Rcpp")

create.blacklist <- function() {
	create.blacklist.iter(initial.blacklist)
}

abort <- function(msg) {
	print(msg)
	quit("no", 1)
}

# find the available packages from contriburl and macth those against pkg.pattern
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

	if (read.blacklist) {
		if (is.na(blacklist.file) || !file.exists(blacklist.file)) {
			abort("blacklist file not set or does not exist")
		} else {
			blacklist <- readLines(con=file(blacklist.file))
		}
	} else {
		blacklist <- create.blacklist()
	}

	if (save.blacklist) {
		if (is.na(blacklist.file)) {
			abort("blacklist file not set")
		} else {
			writeLines(sort(blacklist), con=blacklist.file)
		}
	}

	pkgnames <- rownames(toinstall.pkgs)
	for (pkgname in pkgnames) {
		if (pkgname %in% blacklist) {
			cat("not installing: ", pkgname, " - blacklisted\n")
		} else {
			if (dry.run) {
				cat("would install: ", pkgname, "\n")
			} else {
				cat("installing: ", pkgname, "\n")
				install.packages(pkgname, contriburl=contriburl, type="source", INSTALL_opts="--install-tests")
			}
		}
	}
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
			very.verbose <- T
		} else if (a == "--dryrun") {
			dry.run <<- T
		} else if (a == "--save-blacklist") {
			save.blacklist <<- T
		} else if (a == "--read-blacklist") {
			read.blacklist <<- T
		} else if (a == "--blacklist") {
			if (length(args) >= 2L) {
				blacklist.file <<- args[2L]
				args <<- args[-1L]
			} else {
				usage()
			}
		} else {
			pkg.pattern <<- a
			break
		}

		args <<- args[-1L]
	}
}

# global variables used by the installation
contriburl <- Sys.getenv("LOCAL_CRAN_REPO", unset=NA)
if (is.na(contriburl)) {
	contriburl <- paste("file://", getwd(), "/cran/LOCAL_REPO/src/contrib", sep="")
}
blacklist.file <- Sys.getenv("PACKAGE_BLACKLIST", unset=NA)

pkg.pattern <- "^.*"
verbose <- F
very.verbose <- F
dry.run <- F
avail.pkgs <- NULL
toinstall.pkgs <- NULL
save.blacklist <- F
read.blacklist <- F

if (!interactive()) {
	parse.args()
	do.install()
}

#tryCatch(url(contriburl, open="r"), error=abort)

