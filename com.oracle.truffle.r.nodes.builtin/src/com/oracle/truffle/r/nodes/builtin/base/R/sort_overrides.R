#  File src/library/base/R/sort.R
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

sort.list <- function(x, partial = NULL, na.last = TRUE, decreasing = FALSE,
                      method = c("shell", "quick", "radix"))
{
	# TODO: implement radix sort
	if (missing(method) && is.factor(x) && nlevels(x) < 100000) method <-"shell"
#    if (missing(method) && is.factor(x) && nlevels(x) < 100000) method <-"radix"
    method <- match.arg(method)
    if(!is.atomic(x))
        stop("'x' must be atomic for 'sort.list'\nHave you called 'sort' on a list?")
    if(!is.null(partial))
        .NotYetUsed("partial != NULL")
    if(method == "quick") {
        if(is.factor(x)) x <- as.integer(x) # sort the internal codes
        if(is.numeric(x))
            return(sort(x, na.last = na.last, decreasing = decreasing,
                        method = "quick", index.return = TRUE)$ix)
        else stop("method = \"quick\" is only for numeric 'x'")
    }
    if(method == "radix") {
        if(!typeof(x) == "integer") # we do want to allow factors here
            stop("method = \"radix\" is only for integer 'x'")
        if(is.na(na.last))
            return(.Internal(radixsort(x[!is.na(x)], TRUE, decreasing)))
        else
            return(.Internal(radixsort(x, na.last, decreasing)))
    }
    ## method == "shell"
    if(is.na(na.last)) .Internal(order(TRUE, decreasing, x[!is.na(x)]))
    else .Internal(order(na.last, decreasing, x))
}
