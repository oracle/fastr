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

eval(expression({
makeLazyLoading <-
    function(package, lib.loc = NULL, compress = TRUE,
             keep.source = getOption("keep.source.pkgs"))
{
    if(!is.logical(compress) && ! compress %in% c(2,3))
        stop("invalid value for 'compress': should be FALSE, TRUE, 2 or 3")
    options(warn = 1L)
    findpack <- function(package, lib.loc) {
        pkgpath <- find.package(package, lib.loc, quiet = TRUE)
        if(!length(pkgpath))
            stop(gettextf("there is no package called '%s'", package),
                 domain = NA)
        pkgpath
    }

    if (package == "base")
        stop("this cannot be used for package 'base'")

    loaderFile <- file.path(R.home("share"), "R", "nspackloader.R")
    pkgpath <- findpack(package, lib.loc)
    codeFile <- file.path(pkgpath, "R", package)

    if (!file.exists(codeFile)) {
        warning("package contains no R code")
        return(invisible())
    }
    if (file.size(codeFile) == file.size(loaderFile))
        warning("package seems to be using lazy loading already")
    else {
        code2LazyLoadDB(package, lib.loc = lib.loc,
                        keep.source = keep.source, compress = compress)
        #file.copy(loaderFile, codeFile, TRUE)
    }

    invisible()
}
}), asNamespace("tools"))
