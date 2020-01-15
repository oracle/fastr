# Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
    setBreakpoint <- function (srcfile, line, nameonly = TRUE, envir = parent.frame(),
        lastenv, verbose = TRUE, tracer, print = FALSE, clear = FALSE,
        ...) {
        res <- .fastr.setBreakpoint(srcfile, line, clear)
        if(is.null(res))
            res <- structure(list(), class="findLineNumResult")
        if (verbose)
            print(res, steps = !clear)
    }

    index.search.orig <- utils:::index.search
    index.search <- function (topic, paths, firstOnly = FALSE) {
        res <- index.search.orig(topic, paths, firstOnly)

        if(length(res) == 0) {
            fastrHelpRd <- .fastr.helpPath(topic)
            if(!is.null(fastrHelpRd)) {
                res <- fastrHelpRd
            }
        }
        res
    }

    .getHelpFile.orig <- utils:::.getHelpFile
    .getHelpFile <- function (file) {
        fastrHelpRd <- .fastr.helpRd(file)
        if(!is.null(fastrHelpRd)) {
            return(tools::parse_Rd(textConnection(fastrHelpRd)))
        }

        .getHelpFile.orig(file)
    }

    untar.orig <- untar
    untar <- function (tarfile, files = NULL, list = FALSE, exdir = ".", 
                       compressed = NA, extras = NULL, verbose = FALSE, 
                       restore_times = TRUE, 
                       support_old_tars = Sys.getenv("R_SUPPORT_OLD_TARS", FALSE), 
                       tar = Sys.getenv("TAR")) 
    {

        # if the argument "tar" is "Sys.getenv(R_INSTALL_TAR, ...)" we know we're decompressing package sources
        tarArg <- substitute(tar);
        patching <- tarArg[[1]] == 'Sys.getenv' && tarArg[[2]] == 'R_INSTALL_TAR'
        if (patching) {
            # in order to find out to which directory the tarball decompresses
            # note: GNU-R does the same trick to find this out
            of <- dir(exdir, full.names = TRUE)
        }

        result <- if (missing(compressed)) { 
            untar.orig(tarfile, files=files, list=list, exdir=exdir, extras=extras, verbose=verbose, restore_times=restore_times, support_old_tars=support_old_tars, tar=tar) 
        } else { 
            untar.orig(tarfile, files=files, list=list, exdir=exdir, compressed=compressed, extras=extras, verbose=verbose, restore_times=restore_times, support_old_tars=support_old_tars, tar=tar) 
        }

        if (patching) {
            nf <- dir(exdir, full.names = TRUE)
            new <- setdiff(nf, of)
            # if it decompressed to 0 or more than 1 directories, GNU-R will handle the error reporting
            if (length(new) == 1L) {
                .fastr.patch.package(new)
            }
        }
        result
    }

}), asNamespace("utils"))
