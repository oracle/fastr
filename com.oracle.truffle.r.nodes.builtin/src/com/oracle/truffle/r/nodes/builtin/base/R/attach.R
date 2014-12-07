#  File src/library/base/R/attach.R
#  Part of the R package, http://www.R-project.org
#
#  Copyright (C) 1995-2013 The R Core Team
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation; either version 2 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  A copy of the GNU General Public License is available at
#  http://www.r-project.org/Licenses/

# Temporarily massively cut down from GnuR

attach <- function(what, pos = 2L, name = "",
        warn.conflicts = TRUE)
{
    .Internal(attach(what, pos, name, warn.conflicts))
}

detach <- function(name, pos = 2L, unload = FALSE, character.only = FALSE,
        force = FALSE)
{
    .Internal(detach(name, pos, unload, character.only, force))
}

ls <- objects <-
    function (name, pos = -1L, envir = as.environment(pos), all.names = FALSE,
              pattern)
{
    if (!missing(name)) {
        pos <- tryCatch(name, error = function(e)e)
        if(inherits(pos, "error")) {
            name <- substitute(name)
            if (!is.character(name))
                name <- deparse(name)
            warning(gettextf("%s converted to character string", sQuote(name)),
                    domain = NA)
            pos <- name
        }
    }
    all.names <- .Internal(ls(envir, all.names))
    if (!missing(pattern)) {
        if ((ll <- length(grep("[", pattern, fixed = TRUE))) &&
            ll != length(grep("]", pattern, fixed = TRUE))) {
            if (pattern == "[") {
                pattern <- "\\["
                warning("replaced regular expression pattern '[' by  '\\\\['")
            }
            else if (length(grep("[^\\\\]\\[<-", pattern))) {
                pattern <- sub("\\[<-", "\\\\\\[<-", pattern)
                warning("replaced '[<-' by '\\\\[<-' in regular expression pattern")
            }
        }
        grep(pattern, all.names, value = TRUE)
    }
    else all.names
}
