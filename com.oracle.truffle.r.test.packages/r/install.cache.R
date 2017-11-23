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

