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

# A script to do a single package installation (+dependents), called from install.packages.R.
# It exists as a separate script only to avoid internal FastR errors from killing the
# entire installation process for multiple package installation tests.

# args:
# pkgname, contriburl, lib

log.message <- function(..., level=0) {
    cat(..., "\n")
}

ignored.packages <- c("boot", "class", "cluster", "codetools", "foreign", "KernSmooth", "lattice", "MASS", "Matrix", "mgcv", "nlme", "nnet", "rpart", "spatial", "survival", "base", "compiler", "datasets", "grDevices", "graphics", "grid", "methods", "parallel", "splines", "stats", "stats4", "tools", "utils")

package.dependencies <- function(pkg, lib, dependencies = c("Depends", "Imports", "LinkingTo"), pl = available.packages()) {
    if (!(pkg %in% rownames(pl))) {
        # TODO: logging
        cat("Package", pkg, "not on CRAN\n")
        return (NULL)
    }
    fields <- pl[pkg, dependencies]
    fields <- fields[!is.na(fields)]

    # remove newline artefacts '\n' and split by ','
    deps <- unlist(strsplit(gsub("\\n", " ", fields), ","))

    # remove version
    deps <- trimws(sub("\\(.*\\)", "", deps))

    # ignore dependency to "R" and ignore already installed packages
    installed.packages <- tryCatch({
        # query base and recommended packages
        ip <- available.packages(lib.loc=lib)
        ip[as.logical(match(ip[,"Priority"], c("base", "recommended"), nomatch=0L)),"Package"]
        installed.pacakges(lib.loc=lib)
    }, error = function(e) {
        character(0)
    }, warning = function(e) {
        character(0)
    })
    setdiff(deps, c("R", installed.packages, ignored.packages))
}

transitive.dependencies <- function(pkg, lib, pl = available.packages(), deptype=c("Depends", "Imports", "LinkingTo"), suggests=FALSE) {
    deps <- c()
    more <- pkg

    # Also add "Suggests" to dependencies but do not recurse
    if (suggests) {
        this.suggests <- package.dependencies(pkg, dependencies = "Suggests", pl = pl)
        if (!is.null(this.suggests)) {
            more <- c(more, this.suggests)
        }
    }

    #  TODO: improve list operations for better performance
    processed <- character(0)

    # the loop can't have more iterations then available packages
    max.iterations <- nrow(pl)
    iteration <- 0L
    while (length(more) > 0) {
        if (iteration >= max.iterations) {
            stop("Maximum number of iterations exceeded")
        }
        this <- head(more, 1)
        more <- tail(more, -1)

        if (!(this %in% processed)) {
            cat("processing ", this, "\n")
            processed <- unique(c(processed, this))
            this.deps <- package.dependencies(this, lib, dependencies = deptype, pl = pl)
            if (!is.null(this.deps)) {
                deps <- c(deps, this.deps)
                more <- c(more, this.deps[!(this.deps %in% processed)])
            }
        }

        iteration <- iteration + 1L
    }
    unique(deps)
}

args <- commandArgs(TRUE)

parse.args <- function() {
	if (length(args)) {
		pkgname <<- args[[1]]
		contriburl <<- strsplit(args[[2]], ",")[[1]]
		lib.install <<- args[[3]]

        pkg.cache <<- as.environment(list(enabled=FALSE, table.file.name="version.table", size=2L))
        pkg.cache$enabled <- as.logical(args[[4]])
        cat("system.install, cache enabled: ", pkg.cache$enabled, "\n")
        if (pkg.cache$enabled) {
		    pkg.cache$version <- args[[5]]
		    pkg.cache$dir <- args[[6]]
        }
	}
}

# return code: sucess == 0L, error == 1L
run <- function() {
    parse.args()

    tryCatch({
        # determine available packages
        pkg.list <- available.packages(contriburl=contriburl)

        # compute transitive dependencies of the package to install
        cat("Computing transitive package hull for ", pkgname, "\n")
        transitive.pkg.list <- c(transitive.dependencies(pkgname, lib=lib.install, pl=pkg.list), pkgname)
        cat("transitive deps: ", transitive.pkg.list, "\n")

        # apply pkg cache to fetch cached packages first
        cat("Fetching from cache if possible\n")
        cached.pkgs <- sapply(transitive.pkg.list, function(pkgname) pkg.cache.get(pkg.cache, pkgname, lib.install))
        cat("Number of cached pkgs: ", length(cached.pkgs), "\n")

        # if there was at least one non-cached package
        if (any(!cached.pkgs) || length(cached.pkgs) == 0L) {
            # install the package (and the transitive dependencies implicitly)
            res <- install.packages(pkgname, contriburl=contriburl, type="source", lib=lib.install, INSTALL_opts="--install-tests")
            if (res == NULL) {
                # cache packages that were not in the cache before
                lapply(transitive.pkg.list[!cached.pkgs], function(pkgname) pkg.cache.insert(pkg.cache, pkgname, lib.install))
            } else {
                return (1L)
            }
        }

        # if we reach here, installation was a success
        0L
    }, error = function(e) {
        cat(e$message, "\n")
        return (1L)
    }, warning = function(e) {
        cat(e$message, "\n")
        return (1L)
    })
}

# Determines the directory of the script assuming that there is a "--file=" argument on the command line.
getCurrentScriptDir <- function() {
     cmdArgs <- commandArgs()
     res <- startsWith(cmdArgs, '--file=')
     fileArg <- cmdArgs[res]
     if (length(fileArg) > 0L) {
         p <- strsplit(fileArg, "=")[[1]][[2]]
         dirname(p)
     } else {
        NULL
     }
}

# load package cache code
curScriptDir <- getCurrentScriptDir()
if (!is.null(curScriptDir)) {
    source(file.path(curScriptDir, "install.cache.R"))
} else {
    log.message("Cannot use package cache since script directory cannot be determined")
    pkg.cache.get <<- function(...) FALSE
    pkg.cache.insert <<- function(...) FALSE
}

if (!interactive()) {
	status.code <- run()
    quit(status = status.code)
}
