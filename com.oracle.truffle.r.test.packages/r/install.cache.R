#
# Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

pkg.cache.install <- function(pkg.cache.env, pkgname, lib.install, install.cmd) {
    is.cached <- pkg.cache.get(pkg.cache.env, pkgname, lib.install)
    if (!is.cached) {
        res <- install.cmd()

        # 0L stands for success
        if (res == 0L) {
            pkg.cache.insert(pkg.cache.env, pkgname, lib.install)
        }
    }
}

pkg.cache.get <- function(pkg.cache.env, pkgname, lib) {
    version.dir <- pkg.cache.check(pkg.cache.env)
    if(is.null(version.dir)) {
        return (FALSE)
    }

    log.message("using package cache directory ", version.dir, level=1)
    cache.entry.name <- paste0(pkgname, ".gz")

    # lookup package dir
    pkg.dirs <- list.files(version.dir, full.names=FALSE, recursive=FALSE)
    if (!is.na(match(cache.entry.name, pkg.dirs))) {
        # cache hit
        fromPath <- file.path(version.dir, cache.entry.name)
        toPath <- lib

        # extract cached package to library directory
        tryCatch({
            unzip(fromPath, exdir=toPath, unzip = getOption("unzip"))
            log.message("package cache hit, using package from ", fromPath)
            return (TRUE)
        }, error = function(e) {
            log.message("could not extract cached package from ", fromPath , " to ", toPath, level=1)
            return (FALSE)
        })
    } 
    log.message("cache miss for package ", pkgname, level=1)
    
    FALSE
}

pkg.cache.insert <- function(pkg.cache.env, pkgname, lib) {
    version.dir <- pkg.cache.check(pkg.cache.env)
    if(is.null(version.dir)) {
        return (FALSE)
    }

    tryCatch({
        # Create version directory if inexisting
        if (!dir.exists(version.dir)) {
            log.message("creating version directory ", version.dir, level=1)
            dir.create(version.dir)
        }

        fromPath <- file.path(lib, pkgname)
        toPath <- file.path(version.dir, paste0(pkgname, ".gz"))

        # to produce a TAR with relative paths, we need to change the working dir
        prev.wd <- getwd()
        setwd(lib)
        if(zip(toPath, pkgname) != 0L) {
            log.message("could not compress package dir ", fromPath , " and store it to ", toPath, level=1)
            return (FALSE)
        }
        setwd(prev.wd)

        log.message("successfully inserted package ", pkgname , " to package cache (", toPath, ")")
        return (TRUE)
    }, error = function(e) {
        log.message("could not insert package '", pkgname, "' because: ", e$message)
    })
    FALSE
}

pkg.cache.check <- function(pkg.cache.env) {
    # check if caching is enabled
    if (!pkg.cache.env$enabled) {
        return (NULL)
    }

    # check if package cache directory can be accessed
    if (dir.exists(pkg.cache.env$dir) && any(file.access(pkg.cache.env$dir, mode = 6) == -1)) {
        log.message("cannot access package cache dir ", pkg.cache.env$dir, level=1)
        return (NULL)
    }

    # check cache directory has valid structure
    if (!is.valid.cache.dir(pkg.cache.env$dir, pkg.cache.env$table.file.name)) {
        pkg.cache.init(pkg.cache.env$dir, as.character(pkg.cache.env$version), pkg.cache.env$table.file.name, pkg.cache.env$size)
    }

    # get version sub-directory
    version.dir <- pkg.cache.get.version(pkg.cache.env$dir, as.character(pkg.cache.env$version), pkg.cache.env$table.file.name, pkg.cache.env$size)
    if (is.null(version.dir)) {
        log.message("cannot access or create version subdir for ", as.character(pkg.cache.env$version), level=1)
    }

    version.dir
}

is.valid.cache.dir <- function(cache.dir, table.file.name) {
    if (!dir.exists(cache.dir)) {
        return (FALSE)
    }

    # look for the version table
    version.table.name <- file.path(cache.dir, table.file.name)
    if (any(file.access(version.table.name, mode = 6) == -1)) {
        return (FALSE)
    }

    tryCatch({
        version.table <- read.csv(version.table.name)
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

pkg.cache.init <- function(cache.dir, version, table.file.name, cache.size) {
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

    version.table.name <- file.path(cache.dir, table.file.name)

    # create package lib dir for this version (if not existing)
    version.table <- pkg.cache.create.version(cache.dir, version, table.file.name, cache.size, data.frame(version=character(0),dir=character(0),ctime=double(0)))
    tryCatch({
        write.csv(version.table, version.table.name, row.names=FALSE)
    }, error = function(e) {
        log.message("could not write version table to file ", version.table.name, " because: ", e$message)
    })
    NULL
}

# creates package lib dir for this version (if not existing)
pkg.cache.create.version <- function(cache.dir, version, table.file.name, cache.size, version.table) {
    version.table.name <- file.path(cache.dir, table.file.name)
    version.subdir <- pkg.cache.gen.version.dir.name(version)
    version.dir <- file.path(cache.dir, version.subdir)

    # We do not create the version directory here because we cannot guarantee that this will stay in sync
    # with the version table anyway.

    # Do cleanup if cache dir size exceeds
    while (cache.size > 0 && nrow(version.table) >= cache.size) {
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

pkg.cache.get.version <- function(cache.dir, cache.version, table.file.name, cache.size) {
    if (is.null(cache.version)) {
        return (NULL)
    }

    # look for 'version.table'
    version.table.name <- file.path(cache.dir, table.file.name)
    if (any(file.access(version.table.name, mode = 6) == -1)) {
        return (NULL)
    }

    tryCatch({
        version.table <- read.csv(version.table.name)
        version.subdir <- as.character(version.table[version.table$version == cache.version, "dir"])
        updated.version.table <- NULL
        if (length(version.subdir) == 0L) {
            updated.version.table <- pkg.cache.create.version(cache.dir, cache.version, table.file.name, cache.size, version.table)
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

log.message <- function(..., level=0) {
    cat(..., "\n")
}

# list of recommended and base packages
recommended.base.packages <- c("boot", "class", "cluster", "codetools", "foreign", "KernSmooth", "lattice", "MASS", "Matrix", "mgcv", "nlme", "nnet", "rpart", "spatial", "survival", "base", "compiler", "datasets", "grDevices", "graphics", "grid", "methods", "parallel", "splines", "stats", "stats4", "tools", "utils")

# list of base packages
base.packages <- c("base", "compiler", "datasets", "grDevices", "graphics", "grid", "methods", "parallel", "splines", "stats", "stats4", "tools", "utils")

# the list of packages that will be excluded in the transitive dependecies
ignored.packages <- base.packages

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

pkg.cache.internal.install <- function(pkg.cache.env, pkgname, contriburl, lib.install) {
    tryCatch({
        # determine available packages
        pkg.list <- available.packages(contriburl=contriburl)

        # compute transitive dependencies of the package to install
        log.message("Computing transitive package dependencies for ", pkgname, level=1)
        transitive.pkg.list <- c(transitive.dependencies(pkgname, lib=lib.install, pl=pkg.list), pkgname)
        log.message("transitive deps: ", transitive.pkg.list, level=1)

        # apply pkg cache to fetch cached packages first
        cached.pkgs <- sapply(transitive.pkg.list, function(pkgname) pkg.cache.get(pkg.cache.env, pkgname, lib.install))

        # if there was at least one non-cached package
        if (any(!cached.pkgs) || length(cached.pkgs) == 0L) {
            # install the package (and the transitive dependencies implicitly)
            res <- install.packages(pkgname, contriburl=contriburl, type="source", lib=lib.install, INSTALL_opts="--install-tests")
            if (res == NULL) {
                # cache packages that were not in the cache before
                lapply(transitive.pkg.list[!cached.pkgs], function(pkgname) pkg.cache.insert(pkg.cache.env, pkgname, lib.install))
            } else {
                return (1L)
            }
        }

        # if we reach here, installation was a success
        0L
    }, error = function(e) {
        log.message(e$message)
        return (1L)
    }, warning = function(e) {
        log.message(e$message)
        return (1L)
    })
}
