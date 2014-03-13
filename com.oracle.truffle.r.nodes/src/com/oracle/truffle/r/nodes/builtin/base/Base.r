#
# This material is distributed under the GNU General Public License
# Version 2. You may review the terms of this license at
# http://www.gnu.org/licenses/gpl-2.0.html
#
# Copyright (c) 1995-2012, The R Core Team
# Copyright (c) 2003, The R Foundation
# Copyright (c) 2013, 2014, Oracle and/or its affiliates
#
# All rights reserved.
#
# builtins for the Base package

nrow <- function(x) dim(x)[1L]
NROW <- function(x) if (length(d <- dim(x))) d[1L] else length(x)

ncol <- function(x) dim(x)[2L]
NCOL <- function(x) if (length(d <- dim(x))) d[2L] else 1L

`%in%` <- function(x, table) match(x, table, nomatch=0L) > 0L

`xor` <- function(x, y) (x | y) & !(x & y)

Sys.getenv <- function(x = NULL, unset = "", names = NA)
{
    if (is.null(x)) {
        ## This presumes that '=' does not appear as part of the name
        ## of an environment variable.  That used to happen on Windows.
        x <- strsplit(.Internal.Sys.getenv(character(), ""), "=", fixed=TRUE)
        v <- n <- character(LEN <- length(x))
        for (i in 1L:LEN) {
            n[i] <- x[[i]][1L]
            v[i] <- paste(x[[i]][-1L], collapse = "=")
        }
        if (!identical(names, FALSE)) v <- structure(v, names = n)
        v[sort.list(n)]
    } else {
        v <- .Internal.Sys.getenv(as.character(x), as.character(unset))
        if (isTRUE(names) || (length(x) > 1L && !identical(names, FALSE)))
            structure(v, names = x)
        else v
    }
}

as.list <- function(x, ...) if (typeof(x) == "list") x else as.vector(x, "list")

`%o%` <- function(x, y) outer(x, y)

tempfile <- function(pattern = "file", tmpdir = tempdir(), fileext = "")
{
    .Internal.tempfile(pattern, tmpdir, fileext)
}

identical <- function(x, y, num.eq = TRUE, single.NA = TRUE,
                      attrib.as.set = TRUE, ignore.bytecode = TRUE,
                      ignore.environment = FALSE)
    .Internal.identical(x,y, num.eq, single.NA, attrib.as.set,
                        ignore.bytecode, ignore.environment)
