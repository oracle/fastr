#  File src/library/base/R/dataframe.R
#  Part of the R package, http://www.R-project.org
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

# Statlib code by John Chambers, Bell Labs, 1994
# Changes Copyright (C) 1998-2013 The R Core Team


## As from R 2.4.0, row.names can be either character or integer.
## row.names() will always return character.
## attr(, "row.names") will return either character or integer.
##
## Do not assume that the internal representation is either, since
## 1L:n is stored as the integer vector c(NA, n) to save space (and
## the C-level code to get/set the attribute makes the appropriate
## translations.
##
## As from 2.5.0 c(NA, n > 0) indicates deliberately assigned row names,
## and c(NA, n < 0) automatic row names.

## We cannot allow long vectors as elements until we can handle
## duplication of row names.

# Until parsing problem fixed

`[.data.frame` <-
		function(x, i, j, drop = if(missing(i)) TRUE else length(cols) == 1)
{

	mdrop <- missing(drop)
	# TODO: parsing problem?
#	Narg <- nargs() - !mdrop  # number of arg from x,i,j that were specified
	Narg <- nargs() - (!mdrop)  # number of arg from x,i,j that were specified
	has.j <- !missing(j)
	if(!all(names(sys.call()) %in% c("", "drop"))
			&& !isS4(x)) # at least don't warn for callNextMethod!
		warning("named arguments other than 'drop' are discouraged")

	if(Narg < 3L) {  # list-like indexing or matrix indexing
		if(!mdrop) warning("'drop' argument will be ignored")
		if(missing(i)) return(x)
		if(is.matrix(i))
			return(as.matrix(x)[i])  # desperate measures
		## zero-column data frames prior to 2.4.0 had no names.
		nm <- names(x); if(is.null(nm)) nm <- character()
		## if we have NA names, character indexing should always fail
		## (for positive index length)
		if(!is.character(i) && anyNA(nm)) { # less efficient version
			names(nm) <- names(x) <- seq_along(x)
			y <- NextMethod("[")
			cols <- names(y)
			if(anyNA(cols)) stop("undefined columns selected")
			cols <- names(y) <- nm[cols]
		} else {
			y <- NextMethod("[")
			cols <- names(y)
			if(!is.null(cols) && anyNA(cols))
				stop("undefined columns selected")
		}
		## added in 1.8.0
		if(anyDuplicated(cols)) names(y) <- make.unique(cols)
		## since we have not touched the rows, copy over the raw row.names
		## Claimed at one time at least one fewer copies: PR#15274
		attr(y, "row.names") <- .row_names_info(x, 0L)
		attr(y, "class") <- oldClass(x)
		return(y)
	}

	if(missing(i)) { # df[, j] or df[ , ]
		## not quite the same as the 1/2-arg case, as 'drop' is used.
		if(drop && !has.j && length(x) == 1L) return(.subset2(x, 1L))
		nm <- names(x); if(is.null(nm)) nm <- character()
		if(has.j && !is.character(j) && anyNA(nm)) {
			## less efficient version
			names(nm) <- names(x) <- seq_along(x)
			y <- .subset(x, j)
			cols <- names(y)
			if(anyNA(cols)) stop("undefined columns selected")
			cols <- names(y) <- nm[cols]
		} else {
			y <- if(has.j) .subset(x, j) else x
			cols <- names(y)
			if(anyNA(cols)) stop("undefined columns selected")
		}
		if(drop && length(y) == 1L) return(.subset2(y, 1L))
		if(anyDuplicated(cols)) names(y) <- make.unique(cols)
		nrow <- .row_names_info(x, 2L)
		if(drop && !mdrop && nrow == 1L)
			return(structure(y, class = NULL, row.names = NULL))
		else {
			## Claimed at one time at least one fewer copies: PR#15274
			attr(y, "class") <- oldClass(x)
			attr(y, "row.names") <- .row_names_info(x, 0L)
			return(y)
		}
	}

	### df[i, j] or df[i , ]
	## rewritten for R 2.5.0 to avoid duplicating x.
	xx <- x
	cols <- names(xx)  # needed for computation of 'drop' arg
	## make a shallow copy
	x <- vector("list", length(x))
	## attributes(x) <- attributes(xx) expands row names
	x <- .Internal(copyDFattr(xx, x))
	oldClass(x) <- attr(x, "row.names") <- NULL

	if(has.j) { # df[i, j]
		nm <- names(x); if(is.null(nm)) nm <- character()
		if(!is.character(j) && anyNA(nm))
			names(nm) <- names(x) <- seq_along(x)
		x <- x[j]
		cols <- names(x)  # needed for 'drop'
		if(drop && length(x) == 1L) {
			## for consistency with [, <length-1>]
			if(is.character(i)) {
				rows <- attr(xx, "row.names")
				i <- pmatch(i, rows, duplicates.ok = TRUE)
			}
			## need to figure which col was selected:
			## cannot use .subset2 directly as that may
			## use recursive selection for a logical index.
			xj <- .subset2(.subset(xx, j), 1L)
			return(if(length(dim(xj)) != 2L) xj[i] else xj[i, , drop = FALSE])
		}
		if(anyNA(cols)) stop("undefined columns selected")
		## fix up names if we altered them.
		if(!is.null(names(nm))) cols <- names(x) <- nm[cols]
		## sxx <- match(cols, names(xx)) fails with duplicate names
		nxx <- structure(seq_along(xx), names=names(xx))
		sxx <- match(nxx[j], seq_along(xx))
	} else sxx <- seq_along(x)

	rows <- NULL # placeholder: only create row names when needed
	# as this can be expensive.
	if(is.character(i)) {
		rows <- attr(xx, "row.names")
		i <- pmatch(i, rows, duplicates.ok = TRUE)
	}
	for(j in seq_along(x)) {
		xj <- xx[[ sxx[j] ]]
		## had drop = drop prior to 1.8.0
		x[[j]] <- if(length(dim(xj)) != 2L) xj[i] else xj[i, , drop = FALSE]
	}

	if(drop) {
		n <- length(x)
		if(n == 1L) return(x[[1L]]) # drops attributes
		if(n > 1L) {
			xj <- x[[1L]]
			nrow <- if(length(dim(xj)) == 2L) dim(xj)[1L] else length(xj)
			## for consistency with S: don't drop (to a list)
			## if only one row, unless explicitly asked for
			drop <- !mdrop && nrow == 1L
		} else drop <- FALSE ## for n == 0
	}

	if(!drop) { # not else as previous section might reset drop
		## row names might have NAs.
		if(is.null(rows)) rows <- attr(xx, "row.names")
		rows <- rows[i]
		if((ina <- anyNA(rows)) | (dup <- anyDuplicated(rows))) {
			## both will coerce integer 'rows' to character:
			if (!dup && is.character(rows)) dup <- "NA" %in% rows
			if(ina)
				rows[is.na(rows)] <- "NA"
			if(dup)
				rows <- make.unique(as.character(rows))
		}
		## new in 1.8.0  -- might have duplicate columns
		if(has.j && anyDuplicated(nm <- names(x)))
			names(x) <- make.unique(nm)
		if(is.null(rows)) rows <- attr(xx, "row.names")[i]
		attr(x, "row.names") <- rows
		oldClass(x) <- oldClass(xx)
	}
	x
}

`[[.data.frame` <- function(x, ..., exact=TRUE)
{
	## use in-line functions to refer to the 1st and 2nd ... arguments
	## explicitly. Also will check for wrong number or empty args
	# TODO: parsing problem?
#	na <- nargs() - !missing(exact)
	na <- nargs() - (!missing(exact))
	if(!all(names(sys.call()) %in% c("", "exact")))
		warning("named arguments other than 'exact' are discouraged")

	if(na < 3L)
		(function(x, i, exact)
				if(is.matrix(i)) as.matrix(x)[[i]]
				else .subset2(x, i, exact=exact))(x, ..., exact=exact)
	else {
		col <- .subset2(x, ..2, exact=exact)
		i <- if(is.character(..1))
					pmatch(..1, row.names(x), duplicates.ok = TRUE)
				else ..1
		## we do want to dispatch on methods for a column.
		## .subset2(col, i, exact=exact)
		col[[i, exact = exact]]
	}
}
