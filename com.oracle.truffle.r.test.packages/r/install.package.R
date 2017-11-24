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

# A script to do a single package installation (+dependents), called from install.packages.R.
# It exists as a separate script only to avoid internal FastR errors from killing the
# entire installation process for multiple package installation tests.

# args:
# pkgname, contriburl, lib, pkg.cache.enabled [, api.version, cache.dir ]


log.message <- function(..., level=0L) {
    # TODO: verbosity
    if (level == 0L) {
        cat(..., "\n")
    }
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
    pkg.cache.internal.install(pkg.cache, pkgname, contriburl, lib.install)
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
