#
# Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
#

lock.file.name <- ".lock"

pkg.cache.max.retries <- 3L

# expiration time of a lock in seconds (default: 1 hour)
pkg.cache.lock.expiration <- 3600

pkg.cache.lock <- function(pkg.cache.env, version.dir) {
    if (!(as.logical(pkg.cache.env$sync) && pkg.cache.mode.local(pkg.cache.env))) {
        return (TRUE)
    }

    tries <- 0L
    version.lock.file <- file.path(version.dir, lock.file.name)
    log.message("try to lock: ", version.lock.file, level=1)
    pkg.cache.lock.cleanup.expired(version.lock.file)
    while (file.exists(version.lock.file)) {

        Sys.sleep(1)
        tries <- tries + 1L

        if (tries >= pkg.cache.max.retries) {
            log.message("cannot lock ", version.dir, " because it is already locked")
            return (FALSE)
        }
        pkg.cache.lock.cleanup.expired(version.lock.file)
    }
    log.message("locking: ", version.lock.file, level=1)
    tryCatch({
             # creates a file with permissions "-r--r--r--"
             umask.bak <- Sys.umask("333")
             # this will fail if the file already exists (due to permissions)
             cat(paste0(as.double(Sys.time() + pkg.cache.lock.expiration), "\n"), file=version.lock.file)
             Sys.umask(umask.bak)
             return (TRUE)
    }, error = function(e) {
        log.message("error when creating lock file ", version.lock.file, ": ", e$message)
        Sys.umask(umask.bak)
        return (FALSE)
    }, warning = function(e) {
        log.message("warning when creating lock file ", version.lock.file, ": ", e$message, level=1)
        Sys.umask(umask.bak)
    })
    TRUE
}

pkg.cache.lock.cleanup.expired <- function(lock.file) {
    # see if lock is expired
    log.message("check expiration of lock file: ", lock.file, level=1)
    if (file.exists(lock.file)) {
        cur.time <- Sys.time()
        v <- as.double(readLines(lock.file, n=1))
        if (length(v) == 0 || is.na(v)) {
            log.message("removing lock file without expiration: ", lock.file, level=1)
            tryCatch({
                unlink(lock.file, force=TRUE)
            }, error = function(e) {
                log.message(paste0("Failed to remove lock file ", lock.file, " because: ", as.character(e)), level=1)
            })
        } else {
            class(v) <- class(cur.time)
            if (v - cur.time < 0) {
                # this also works if the write permission is missing
                log.message("removing expired lock file: ", lock.file, " (expired on: ", as.character(v), ")", level=1)
                tryCatch({
                    unlink(lock.file, force=TRUE)
                }, error = function(e) {
                    log.message(paste0("Failed to remove lock file ", lock.file, " because: ", as.character(e)))
                })
            } else {
                log.message("lock file not expired (expires on ", as.character(v), ")", level=1)
            }
        }
    } else {
        log.message("nothing to remove (", lock.file, " does not exist)", level=1)
    }
}

pkg.cache.unlock <- function(pkg.cache.env, version.dir) {
    if (!(as.logical(pkg.cache.env$sync) && pkg.cache.mode.local(pkg.cache.env))) {
        return (TRUE)
    }

    version.lock.file <- file.path(version.dir, lock.file.name)
    log.message("releasing: ", version.lock.file, level=1)
    tryCatch({
        unlink(version.lock.file, force=TRUE)
    }, errors = function(e) { log.message(as.character(e), level=1) })
    TRUE
}


is.fastr <- function() {
    length(grep('FastR', R.Version()$version.string))
}

get.vm <- function() if (is.fastr()) "fastr" else "gnur"

pkg.cache.is.enabled <- function(pkg.cache.env) {
    pkg.cache.env$enabled && (!is.character(pkg.cache.env$vm) || grepl(get.vm(), pkg.cache.env$vm))
}

# A simple log function; to be replaced by a user of this file.
log.message <- if(!exists("log.message")) {
    function(..., level=0) {
        if(level == 0 || verbose) {
            cat(paste0(..., "\n"))
        }
    }
} else {
    log.message
}

pkg.cache.install <- function(pkg.cache.env, pkgname, pkg.version, lib.install, install.cmd) {
    pkg <- list(Package=pkgname, Version=pkg.version)
    is.cached <- pkg.cache.get(pkg.cache.env, pkg, lib.install)
    if (!is.cached) {
        res <- install.cmd()

        # 0L stands for success
        if (res == 0L) {
            pkg.cache.insert(pkg.cache.env, pkg, lib.install)
        }
    }
}

pkg.cache.entry.filename <- function(pkg) {
    paste0(as.character(pkg["Package"]), "_", as.character(pkg["Version"]), ".zip")
}

pkg.cache.file.path <- function(pkg.cache.env, version.dir, cache.entry.name) {
    if (pkg.cache.mode.oci(pkg.cache.env)) {
        # In mode="os", we temporarily store the archive file to the package cache dir 
        # which is in this case just a working directory for transferring the files.
        file.path(pkg.cache.env$dir, cache.entry.name)
    } else {
        file.path(version.dir, cache.entry.name)
    }
}

pkg.cache.get <- function(pkg.cache.env, pkg, lib) {
    version.dir <- pkg.cache.check(pkg.cache.env)
    if(is.null(version.dir)) {
        return (FALSE)
    }

    # lock version directory
    if (!pkg.cache.lock(pkg.cache.env, version.dir)) {
        log.message("could not fetch: version dir ", version.dir, " is locked", level=1)
        return (FALSE)
    }

    pkg.name <- as.character(pkg["Package"])
    pkg.version <- as.character(pkg["Version"])

    log.message("using package cache directory ", version.dir, level=1)
    cache.entry.name <- pkg.cache.entry.filename(pkg)

    fromPath <- pkg.cache.file.path(pkg.cache.env, version.dir, cache.entry.name)
    if (pkg.cache.mode.oci(pkg.cache.env)) {
        dest.file <- fromPath
        object.name <- paste0(version.dir, "/", cache.entry.name)
        if (!pkg.cache.os.get(pkg.cache.env, object.name, dest.file)) {
            pkg.cache.unlock(pkg.cache.env, version.dir)
            log.message("cache miss for package ", pkg.name, level=1)
            return (FALSE)
        }
    } 


    toPath <- lib
    # extract cached package to library directory
    tryCatch({
        if (file.exists(fromPath)) {
            unzip(fromPath, exdir=toPath, unzip = getOption("unzip"))
            log.message("package cache hit, using package from ", fromPath)
            pkg.cache.unlock(pkg.cache.env, version.dir)
            return (TRUE)
        } else {
            log.message("cache miss for package ", pkg.name, " (path=", fromPath, ")", level=1)
        }
    }, error = function(e) {
        log.message("could not extract cached package from ", fromPath , " to ", toPath, level=1)
    })
    pkg.cache.unlock(pkg.cache.env, version.dir)
    
    FALSE
}

pkg.cache.create.version.dir <- function(pkg.cache.env, version.dir) {
    # we don't need to create anything in mode="os"
    if (pkg.cache.mode.local(pkg.cache.env)) {
        tryCatch({
            # Create version directory if inexisting
            if (!dir.exists(version.dir)) {
                log.message("creating version directory ", version.dir, level=1)
                dir.create(version.dir, recursive=T)
            }
        }, error = function(e) {
            log.message("could not insert package '", pkgname, "' because: ", e$message)
            return (FALSE)
        })
    }
    TRUE
}

pkg.cache.insert <- function(pkg.cache.env, pkg, lib) {
    pkgname <- as.character(pkg["Package"])
    if (pkgname %in% ignored.packages) {
        log.message("reject to insert ignored package '", pkgname, "'", level=1)
        return (FALSE)
    }

    version.dir <- pkg.cache.check(pkg.cache.env)
    if(is.null(version.dir)) {
        return (FALSE)
    }

    # create the version directory
    if (!pkg.cache.create.version.dir(pkg.cache.env, version.dir)) {
        return (FALSE)
    }

    # lock version directory
    if (!pkg.cache.lock(pkg.cache.env, version.dir)) {
        log.message("could not insert: version dir ", version.dir, " is locked", level=1)
        return (FALSE)
    }

    tryCatch({
        pkg.version <- as.character(pkg["Version"])
        fromPath <- file.path(lib, pkgname)
        cache.entry.name <- pkg.cache.entry.filename(pkg)
        toPath <- pkg.cache.file.path(pkg.cache.env, version.dir, cache.entry.name)

        # to produce a ZIP with relative paths, we need to change the working dir
        prev.wd <- getwd()
        setwd(lib)

        # cleanup older package versions
        pkg.cache.cleanup.pkg.versions(pkg.cache.env, version.dir, pkgname)

        if(zip(toPath, pkgname, flags="-r9Xq") != 0L) {
            pkg.cache.unlock(pkg.cache.env, version.dir)
            log.message("could not compress package dir ", fromPath , " and store it to ", toPath, level=1)
            return (FALSE)
        }

        # transfer archive file to object store
        if (pkg.cache.mode.oci(pkg.cache.env)) {
            object.name <- paste0(version.dir, "/", cache.entry.name)
            pkg.cache.os.put(pkg.cache.env, object.name, toPath)
            dest.name <- object.name

            # delete temporary file (in any case)
            unlink(toPath)
        } else {
            dest.name <- toPath
        }
        setwd(prev.wd)
        log.message("successfully inserted package ", pkgname , " to package cache (", dest.name, ")")
        pkg.cache.unlock(pkg.cache.env, version.dir)
        return (TRUE)
    }, error = function(e) {
        pkg.cache.unlock(pkg.cache.env, version.dir)
        log.message("could not insert package '", pkgname, "' because: ", e$message)
    })
    FALSE
}

pkg.cache.cleanup.pkg.versions <- function(pkg.cache.env, version.dir, pkgname) {
    if (pkg.cache.mode.local(pkg.cache.env)) {
        tryCatch({
            fs <- list.files(version.dir, full.names=TRUE, recursive=FALSE)
            pkg.cached.versions.idxs <- grepl(pkgname, fs)
            if (length(pkg.cached.versions.idxs) != 0L) {
                log.message("cleaning up old package versions '", fs[pkg.cached.versions.idxs], "'", level=1)
                unlink(fs[pkg.cached.versions.idxs], recursive=FALSE)
            }
        }, error = function(e) {
            log.message("could not cleanup old package versions of '", pkgname, "' because: ", e$message)
        })
    } else if(pkg.cache.mode.oci(pkg.cache.env)) {
        # TODO
    }
}

pkg.cache.os.run.client <- function(os.cmd, os.object.name, os.object.dest="") {
    # consider env var 'FASTR_OS_PKG_CACHE_PREFIX'
    pkg.cache.os.prefix <- Sys.getenv("FASTR_OS_PKG_CACHE_PREFIX")

    # assemble final object name
    os.object.qname <- paste0(pkg.cache.os.prefix, os.object.name)

    # consider env var 'FASTR_OCI_CLIENT_ARGS'
    os.client.args <- Sys.getenv("FASTR_OCI_CLIENT_ARGS")

    # consider env var 'FASTR_OS_BUCKET_NAME'
    os.bucket.name <- Sys.getenv("FASTR_OS_BUCKET_NAME")
    os.bucket.option <- if (os.bucket.name != "") paste("-bn", os.bucket.name) else ""

    # optional: 'os.object.dest'
    os.file.option <- if (os.object.dest != "") paste("--file", os.object.dest) else ""

    # assemble command line
    cmd.line <- paste("oci", os.client.args, "os object", os.cmd, os.bucket.option, "--name", os.object.qname, "--file", os.object.dest)
    log.message("OCI get command line: ", cmd.line, level=1)

    rc <- system(cmd.line, show.output.on.console=verbose)
    if (rc != 0L) {
        log.message("object store ", os.cmd, " failed with status code=", rc)
        FALSE
    }
    TRUE
}

# get an object from the object store; TRUE on success, FALSE otherwise
pkg.cache.os.get <- function(pkg.cache.env, os.object.name, os.object.dest) {
    pkg.cache.os.run.client("get", os.object.name, os.object.dest)
}

# put an object to the object store; TRUE on success, FALSE otherwise
pkg.cache.os.put <- function(pkg.cache.env, os.object.name, os.object.dest) {
    pkg.cache.os.run.client("put", os.object.name, os.object.dest)
}

# delete a single object from the object storage (not recursive)
pkg.cache.os.delete <- function(pkg.cache.env, os.object.name) {
    pkg.cache.os.run.client("delete", os.object.name)
}

pkg.cache.mode.oci <- function(pkg.cache.env) {
    pkg.cache.env$mode == "os"
}

pkg.cache.mode.local <- function(pkg.cache.env) {
    !pkg.cache.mode.oci(pkg.cache.env)
}

pkg.cache.check <- function(pkg.cache.env) {
    cat("##### FOO\n")
    # check if caching is enabled
    if (!pkg.cache.is.enabled(pkg.cache.env)) {
        return (NULL)
    }

    if (pkg.cache.mode.oci(pkg.cache.env)) {
        if (!pkg.cache.os.initialized(pkg.cache.env)) {
            pkg.cache.init(pkg.cache.env, as.character(pkg.cache.env$version), pkg.cache.env$table.file.name, pkg.cache.env$size)
        }
    } else {
        # check if package cache directory can be accessed
        if (dir.exists(pkg.cache.env$dir) && any(file.access(pkg.cache.env$dir, mode = 6) == -1)) {
            log.message("cannot access package cache dir ", pkg.cache.env$dir, level=1)
            return (NULL)
        }

        # check cache directory has valid structure
        if (!is.valid.cache.dir(pkg.cache.env$dir, pkg.cache.env$table.file.name)) {
            pkg.cache.init(pkg.cache.env, as.character(pkg.cache.env$version), pkg.cache.env$table.file.name, pkg.cache.env$size)
        }
    }
    # get version sub-directory
    version.dir <- pkg.cache.get.version(pkg.cache.env, pkg.cache.env$dir, as.character(pkg.cache.env$version), pkg.cache.env$table.file.name, pkg.cache.env$size)
    if (is.null(version.dir)) {
        log.message("cannot access or create version subdir for ", as.character(pkg.cache.env$version), level=1)
    }

    if (pkg.cache.mode.local(pkg.cache.env)) {
        file.path(pkg.cache.env$dir, version.dir)
    } else {
        version.dir
    }
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

# checks if the package cache structure is initialized on the object storage
pkg.cache.os.initialized <- function(pkg.cache.env) {
    table.file.name <- pkg.cache.env$table.file.name
    dst.file <- file.path(pkg.cache.env$dir, table.file.name)

    # download version table file to temporary location
    rc <- pkg.cache.os.get(pkg.cache.env, table.file.name, dst.file)
    if (rc != 0L) {
        # error already logged
        return (FALSE)
    }


    if (any(file.access(dst.file, mode = 6) == -1)) {
        # delete temporary file
        unlink(dst.file)
        return (FALSE)
    }

    tryCatch({
        version.table <- read.csv(dst.file)
        # delete temporary file
        unlink(dst.file)
        TRUE
    }, error = function(e) {
        log.message("could not read package cache's version table: ", e$message, level=1)
        # delete temporary file
        unlink(dst.file)
        FALSE
    })
}

# Generates a package cache API version directory using the first 20 characters (if available) from the version.
pkg.cache.gen.version.dir.name <- function(version) {
    paste0("library", substr(version, 1, max(20,length(version))))
}

pkg.cache.init <- function(pkg.cache.env, version, table.file.name, cache.size) {
    if (is.null(version)) {
        # This has been logged during argument parsing.
        return (NULL)
    }

    log.message("initializing package cache (mode=", pkg.cache.env$mode, ")")

    if (pkg.cache.mode.local(pkg.cache.env)) {
        cache.dir <- pkg.cache.env$dir
        if (!dir.exists(cache.dir)) {
            log.message("creating cache directory ", cache.dir, level=1)

            tryCatch({
                dir.create(cache.dir, recursive=T)
            }, error = function(e) {
                log.message("could create package cache dir '", cache.dir, "' because: ", e$message)
            })
        }
    }

    # create package lib dir for this version (if not existing)
    initial.version.table <- data.frame(version=character(0),dir=character(0),ctime=double(0))
    version.table <- pkg.cache.create.version(pkg.cache.env, version, initial.version.table)

    version.table.name <- file.path(cache.dir, table.file.name)
    tryCatch({
        write.csv(version.table, version.table.name, row.names=FALSE)
    }, error = function(e) {
        log.message("could not write version table to file ", version.table.name, " because: ", e$message)
    })

    # in mode="os", we need to upload the file to the object store
    if (pkg.cache.mode.oci(pkg.cache.env)) {
        pkg.cache.os.put(pkg.cache.env, table.file.name, version.table.name)
    }

    NULL
}

pkg.cache.delete.version <- function(pkg.cache.env, version.dir.basename) {

    log.message("removing oldest version dir ", version.dir.basename, level=1)
    if (pkg.cache.mode.oci(pkg.cache.env)) {
        # TODO implement; use 'bulk-delete'
        TRUE
    } else {
        # delete directory
        tryCatch({
            oldest.dir <- file.path(pkg.cache.env$dir, version.dir.basename)
            version.table <- version.table[-order[[1]],]
            unlink(oldest.dir, recursive=TRUE)
        }, error = function(e) {
            log.message("could not remove directory ", oldest.dir, " from cache", level=1)
        })
    }
}


# creates package lib dir for this version (if not existing)
pkg.cache.create.version <- function(pkg.cache.env, version, version.table) {
    version.subdir <- pkg.cache.gen.version.dir.name(version)

    # We do not create the version directory here because we cannot guarantee that this will stay in sync
    # with the version table anyway.

    # Do cleanup if cache dir size exceeds
    cache.size <- pkg.cache.env$size
    while (cache.size > 0 && nrow(version.table) >= cache.size) {
        # Remove oldest version (if any)
        order <- order(version.table$ctime)
        if (length(order) > 0) {
            oldest.entry <- version.table[order[[1]],]
            version.table <- version.table[-order[[1]],]

            # delete directory
            pkg.cache.delete.version(pkg.cache.env, as.character(oldest.entry$dir))
        } else {
            # just to be sure
            break ()
        }
    }

    # Add entry to version table
    log.message("adding entry for ", version, level=1)
    rbind(version.table, data.frame(version=version,dir=version.subdir,ctime=as.double(Sys.time())))
}

pkg.cache.read.version.from.table <- function(pkg.cache.env, cache.version, version.table.file) {
    if (is.null(cache.version)) {
        return (NULL)
    }

    if (any(file.access(version.table.file, mode = 6) == -1)) {
        return (NULL)
    }

    tryCatch({
        # read the version table file
        version.table <- read.csv(version.table.file)

        # get subdir for given version (create if not exists)
        version.subdir <- as.character(version.table[version.table$version == cache.version, "dir"])
        updated.version.table <- NULL
        if (length(version.subdir) == 0L) {
            updated.version.table <- pkg.cache.create.version(pkg.cache.env, cache.version, version.table)
        }
        if (!is.null(updated.version.table)) {
            version.subdir <- as.character(updated.version.table[updated.version.table$version == cache.version, "dir"])

            if (!pkg.cache.lock(pkg.cache.env, cache.dir)) {
                return (NULL)
            }

            tryCatch({
                write.csv(updated.version.table, version.table.file, row.names=FALSE)
                pkg.cache.unlock(pkg.cache.env, cache.dir)
            }, error = function(e) {
                pkg.cache.unlock(pkg.cache.env, cache.dir)
            })

            # send updated file to object store
            if (pkg.cache.mode.oci(pkg.cache.env)) {
                if (!pkg.cache.os.put(pkg.cache.env, pkg.cache.env$table.file.name, version.table.file)) {
                    return (NULL)
                }
            }

        }

        # return the version directory
        version.subdir
    }, error = function(e) {
        log.message("error reading/writing 'version.table': ", e$message, level=1)
        NULL
    })
}

pkg.cache.get.version <- function(pkg.cache.env, cache.dir, cache.version, table.file.name, cache.size) {

    version.table.file <- file.path(cache.dir, table.file.name)
    if (pkg.cache.mode.oci(pkg.cache.env)) {
        pkg.cache.os.get(pkg.cache.env, table.file.name, version.table.file)
    }

    pkg.cache.read.version.from.table(pkg.cache.env, cache.version, version.table.file)
}

# list of recommended and base packages
recommended.base.packages <- c("boot", "class", "cluster", "codetools", "foreign", "KernSmooth", "lattice", "MASS", "Matrix", "mgcv", "nlme", "nnet", "rpart", "spatial", "survival", "base", "compiler", "datasets", "grDevices", "graphics", "grid", "methods", "parallel", "splines", "stats", "stats4", "tools", "utils")

# list of base packages
base.packages <- c("base", "compiler", "datasets", "grDevices", "graphics", "grid", "methods", "parallel", "splines", "stats", "stats4", "tools", "utils")

# the list of packages that will be excluded in the transitive dependecies
ignored.packages <- if (is.fastr()) recommended.base.packages else base.packages

# Computes the direct dependencies of a package.
# Returns a data frame containing the with rows c("Package", "Version")
package.dependencies <- function(pkg, lib, dependencies = c("Depends", "Imports", "LinkingTo"), pl = as.data.frame(available.packages(), stringAsFactors=FALSE)) {
    if (!(pkg %in% rownames(pl))) {
        log.message("Package", as.character(pkg), "not on CRAN\n", level=1)
        return (NULL)
    }
    fields <- pl[pkg, dependencies]
    fields <- fields[!is.na(fields)]

    # remove newline artefacts '\n' and split by ','
    deps <- unlist(strsplit(gsub("\\n", " ", fields), ","))

    # remove version constraints like '(>= 3.4.0)'
    deps <- trimws(sub("\\(.*\\)", "", deps))

    # ignore dependency to "R" and ignore already installed packages
    installed.pkgs.table <- tryCatch({
        as.data.frame(installed.packages(lib.loc=lib)[,c("Package", "Version")], stringAsFactors=FALSE)
    }, error = function(e) {
        data.frame(Package=character(0), Version=character(0))
    }, warning = function(e) {
        data.frame(Package=character(0), Version=character(0))
    })
    # Remove ignored packages from dependencies vector
    non.ignored.names <- setdiff(deps, c("R", ignored.packages))

    # Convert vector to data frame (query from package list data frame)
    non.ignored.deps <- pl[pl$Package %in% non.ignored.names,]

    # Remove any installed packages
    non.ignored.deps[!(non.ignored.deps$Package %in% installed.pkgs.table$Package & non.ignored.deps$Version %in% installed.pkgs.table$Version),c("Package", "Version")]
}

# Computes the transitive dependencies of a package by ignoring installed packages and 'ignored.packages'.
# The result is a data frame with columns named "Package" and "Version".
# Every row represents a package by the name and its version.
transitive.dependencies <- function(pkg, lib, pl = as.data.frame(available.packages(), stringAsFactors=FALSE), deptype=c("Depends", "Imports", "LinkingTo"), suggests=FALSE) {
    deps <- data.frame(Package=character(0), Version=character(0))
    more <- pkg

    # Also add "Suggests" to dependencies but do not recurse
    if (suggests) {
        this.suggests <- package.dependencies(pkg, dependencies = "Suggests", pl = pl)
        if (!is.null(this.suggests)) {
            more <- c(more, as.character(this.suggests$Package))
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
                deps <- rbind(deps, this.deps)
                more <- c(more, as.character(this.deps[!(this.deps$Package %in% processed), "Package"]))
            }
        }

        iteration <- iteration + 1L
    }
    unique(deps)
}

# Fetches the package from the cache or installs it. This is also done for all transitive dependencies.
pkg.cache.internal.install <- function(pkg.cache.env, pkgname, contriburl, lib.install) {
    tryCatch({
        if (pkg.cache.is.enabled(pkg.cache.env)) {
            # determine available packages
            pkg.list <- as.data.frame(available.packages(contriburl=contriburl), stringAsFactors=FALSE)

            # query version of the package
            pkg <- pkg.list[pkgname, c("Package", "Version")]

            # compute transitive dependencies of the package to install
            log.message("Computing transitive package dependencies for ", paste0(pkgname, "_", as.character(pkg$Version)), level=1)
            transitive.pkg.list <- rbind(transitive.dependencies(pkgname, lib=lib.install, pl=pkg.list), pkg)
            log.message("transitive deps: ", as.character(transitive.pkg.list$Package), level=1)

            # apply pkg cache to fetch cached packages first
            cached.pkgs <- apply(transitive.pkg.list, 1, function(pkg) pkg.cache.get(pkg.cache.env, pkg, lib.install))
            log.message("Number of uncached packages:", nrow(transitive.pkg.list[!cached.pkgs, ]), level=1)

            # if there was at least one non-cached package
            if (any(!cached.pkgs) || length(cached.pkgs) == 0L) {
                # install the package (and the transitive dependencies implicitly)
                install.packages(as.character(transitive.pkg.list[!cached.pkgs, "Package"]), contriburl=contriburl, type="source", lib=lib.install, INSTALL_opts="--install-tests")

                # cache packages that were not in the cache before
                log.message("Caching uncached dependencies:", as.character(transitive.pkg.list[!cached.pkgs, "Package"]), level=1)
                apply(transitive.pkg.list[!cached.pkgs, ], 1, function(pkg) pkg.cache.insert(pkg.cache.env, pkg, lib.install))
            }
        } else {
            install.packages(pkgname, contriburl=contriburl, type="source", lib=lib.install, INSTALL_opts="--install-tests")
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
