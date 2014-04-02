#  File src/library/base/R/connections.R
#  Part of the R package, http://www.R-project.org
#
#  Copyright (C) 1995-2012 The R Core Team
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

# Partial copy of GnuR equivalent

file <- function(description = "", open = "", blocking = TRUE,
                 encoding = getOption("encoding"), raw = FALSE)
    .Internal(file(description, open, blocking, encoding, raw))

readLines <- function(con = stdin(), n = -1L, ok = TRUE, warn = TRUE,
                      encoding = "unknown")
{
    if(is.character(con)) {
        con <- file(con, "r")
        on.exit(close(con))
    }
    .Internal(readLines(con, n, ok, warn, encoding))
}

