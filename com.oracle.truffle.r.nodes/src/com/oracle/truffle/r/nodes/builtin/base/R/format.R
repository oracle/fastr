#  File src/library/base/R/format.R
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

format.data.frame <- function(x, ..., justify = "none")
{
"dummy"
#    nr <- .row_names_info(x, 2L)
#    nc <- length(x)
#    rval <- vector("list", nc)
#    for(i in 1L:nc)
#	rval[[i]] <- format(x[[i]], ..., justify = justify)
#    lens <- sapply(rval, NROW)
#    if(any(lens != nr)) { # corrupt data frame, must have at least one column
#	warning("corrupt data frame: columns will be truncated or padded with NAs")
#	for(i in 1L:nc) {
#	    len <- NROW(rval[[i]])
#	    if(len == nr) next
#	    if(length(dim(rval[[i]])) == 2L) {
#		rval[[i]] <- if(len < nr)
#		    rbind(rval[[i]], matrix(NA, nr-len, ncol(rval[[i]])))
#		else rval[[i]][1L:nr,]
#	    } else {
#		rval[[i]] <- if(len < nr) c(rval[[i]], rep.int(NA, nr-len))
#		else rval[[i]][1L:nr]
#	    }
#	}
#    }
#    for(i in 1L:nc) {
#	if(is.character(rval[[i]]) && inherits(rval[[i]], "character"))
#	    oldClass(rval[[i]]) <- "AsIs"
#    }
#    cn <- names(x)
#    m <- match(c("row.names", "check.rows", "check.names", ""), cn, 0L)
#    if(any(m)) cn[m] <- paste0("..dfd.", cn[m])
#    ## This requires valid symbols for the columns, so we need to
#    ## truncate any of more than 256 bytes.
#    long <- nchar(cn, "bytes") > 256L
#    cn[long] <- paste(substr(cn[long], 1L, 250L), "...")
#    names(rval) <- cn
#    rval$check.names <- FALSE
#    rval$row.names <- row.names(x)
#    x <- do.call("data.frame", rval)
#    ## x will have more cols than rval if there are matrix/data.frame cols
#    if(any(m)) names(x) <- sub("^..dfd.", "", names(x))
#    x
}
