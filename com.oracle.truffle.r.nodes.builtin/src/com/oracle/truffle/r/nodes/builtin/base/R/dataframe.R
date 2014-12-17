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

.row_names_info <- function(x, type = 1L)
    .Internal(shortRowNames(x, type))

row.names <- function(x) UseMethod("row.names")
row.names.data.frame <- function(x) as.character(attr(x, "row.names"))
row.names.default <- function(x) if(!is.null(dim(x))) rownames(x)# else NULL

.set_row_names <- function(n)
    if(n > 0) c(NA_integer_, -n) else integer()

`row.names<-` <- function(x, value) UseMethod("row.names<-")

`row.names<-.data.frame` <- function(x, value)
{
    if (!is.data.frame(x)) x <- as.data.frame(x)
    n <- .row_names_info(x, 2L)
    if(is.null(value)) { # set automatic row.names
        attr(x, "row.names") <- .set_row_names(n)
        return(x)
    }
    ## do this here, as e.g. POSIXlt changes length when coerced.
    if( is.object(value) || !is.integer(value) )
        value <- as.character(value)
    if(n == 0L) {
        ## we have to be careful here.  This could be a
        ## 0-row data frame or an invalid one being constructed.
        if(!is.null(attr(x, "row.names")) && length(value) > 0L)
           stop("invalid 'row.names' length")
    }
    else if (length(value) != n)
        stop("invalid 'row.names' length")
    if (anyDuplicated(value)) {
        nonuniq <- sort(unique(value[duplicated(value)]))
        warning(ngettext(length(nonuniq),
                         sprintf("non-unique value when setting 'row.names': %s",
                                 sQuote(nonuniq[1L])),
                         sprintf("non-unique values when setting 'row.names': %s",
                                 paste(sQuote(nonuniq), collapse = ", "))),
                domain = NA, call. = FALSE)
        stop("duplicate 'row.names' are not allowed")
    }
    if (any(is.na(value)))
        stop("missing values in 'row.names' are not allowed")
    attr(x, "row.names") <- value
    x
}

`row.names<-.default` <- function(x, value) `rownames<-`(x, value)

print.AsIs <- function (x, ...)
{
    cl <- oldClass(x)
    oldClass(x) <- cl[cl != "AsIs"]
    NextMethod("print")
    invisible(x)
}

dim.data.frame <- function(x) c(.row_names_info(x, 2L), length(x))

dimnames.data.frame <- function(x) list(row.names(x), names(x))

`dimnames<-.data.frame` <- function(x, value)
{
	d <- dim(x)
	if(!is.list(value) || length(value) != 2L)
		stop("invalid 'dimnames' given for data frame")
	## do the coercion first, as might change length
	value[[1L]] <- as.character(value[[1L]])
	value[[2L]] <- as.character(value[[2L]])
	if(d[[1L]] != length(value[[1L]]) || d[[2L]] != length(value[[2L]]))
		stop("invalid 'dimnames' given for data frame")
	row.names(x) <- value[[1L]] # checks validity
	names(x) <- value[[2L]]
	x
}

as.data.frame <- function(x, row.names = NULL, optional = FALSE, ...)
{
    if(is.null(x)) # can't assign class to NULL
        return(as.data.frame(list()))
    UseMethod("as.data.frame")
}

as.data.frame.default <- function(x, ...)
    stop(gettextf("cannot coerce class \"%s\" to a data.frame",
                  deparse(class(x))),
		  domain = 42)

###  Here are methods ensuring that the arguments to "data.frame"
###  are in a form suitable for combining into a data frame.

as.data.frame.data.frame <- function(x, row.names = NULL, ...)
{
    cl <- oldClass(x)
    i <- match("data.frame", cl)
    if(i > 1L)
        class(x) <- cl[ - (1L:(i-1L))]
    if(!is.null(row.names)){
        nr <- .row_names_info(x, 2L)
        if(length(row.names) == nr)
            attr(x, "row.names") <- row.names
        else
            stop(sprintf(ngettext(nr,
                                  "invalid 'row.names', length %d for a data frame with %d row",
                                  "invalid 'row.names', length %d for a data frame with %d rows"),
                         length(row.names), nr), domain = NA)
    }
    x
}

## prior to 1.8.0 this coerced names - PR#3280
as.data.frame.list <-
       function(x, row.names = NULL, optional = FALSE, ...,
                stringsAsFactors = default.stringsAsFactors())
        {
            ## need to protect names in x.
            cn <- names(x)
            m <- match(c("row.names", "check.rows", "check.names", "stringsAsFactors"),
                       cn, 0L)
            if(any(m)) {
                cn[m] <- paste0("..adfl.", cn[m])
                names(x) <- cn
            }
            x <- eval(as.call(c(expression(data.frame), x, check.names = !optional,
                                stringsAsFactors = stringsAsFactors)))
            if(any(m)) names(x) <- sub("^\\.\\.adfl\\.", "", names(x))
            if(!is.null(row.names)) {
                # row.names <- as.character(row.names)
                if(length(row.names) != dim(x)[[1L]])
                    stop(sprintf(ngettext(length(row.names),
                         "supplied %d row name for %d rows",
                         "supplied %d row names for %d rows"),
                         length(row.names), dim(x)[[1L]]), domain = NA)
                attr(x, "row.names") <- row.names
            }
            x
        }

as.data.frame.vector <- function(x, row.names = NULL, optional = FALSE, ...,
                                 nm = paste(deparse(substitute(x),
                                 width.cutoff = 500L), collapse=" ")  )
{
    force(nm)
    nrows <- length(x)
    if(is.null(row.names)) {
        if (nrows == 0L) {
            row.names <- character() }
        else if(length(row.names <- names(x)) == nrows &&
            !anyDuplicated(row.names)) {}
        else row.names <- .set_row_names(nrows)
    }
    if(!is.null(names(x))) names(x) <- NULL # remove names as from 2.0.0
    value <- list(x)
    if(!optional) names(value) <- nm
    attr(value, "row.names") <- row.names
    class(value) <- "data.frame"
    value
}

as.data.frame.ts <- function(x, ...)
{
    if(is.matrix(x))
        as.data.frame.matrix(x, ...)
    else
        as.data.frame.vector(x, ...)
}

as.data.frame.raw  <- as.data.frame.vector
as.data.frame.factor  <- as.data.frame.vector
as.data.frame.ordered <- as.data.frame.vector
as.data.frame.integer <- as.data.frame.vector
as.data.frame.numeric <- as.data.frame.vector
as.data.frame.complex <- as.data.frame.vector

default.stringsAsFactors <- function()
{
    val <- getOption("stringsAsFactors")
    if(is.null(val)) val <- TRUE
    if(!is.logical(val) || is.na(val) || length(val) != 1L)
        stop('options("stringsAsFactors") not set to TRUE or FALSE')
    val
}

## in case someone passes 'nm'
as.data.frame.character <-
    function(x, ..., stringsAsFactors = default.stringsAsFactors())
{
    nm <- deparse(substitute(x), width.cutoff=500L)
    if(stringsAsFactors) x <- factor(x)
    if(!"nm" %in% names(list(...)))
        as.data.frame.vector(x, ..., nm = nm)
    else as.data.frame.vector(x, ...)
}
as.data.frame.logical <- as.data.frame.vector

as.data.frame.matrix <- function(x, row.names = NULL, optional = FALSE, ...,
                                 stringsAsFactors = default.stringsAsFactors())
{
    d <- dim(x)
    nrows <- d[1L]; ir <- seq_len(nrows)
    ncols <- d[2L]; ic <- seq_len(ncols)
    dn <- dimnames(x)
    ## surely it cannot be right to override the supplied row.names?
    ## changed in 1.8.0
    if(is.null(row.names)) row.names <- dn[[1L]]
    collabs <- dn[[2L]]
    if(any(empty <- !nzchar(collabs)))
        collabs[empty] <- paste0("V", ic)[empty]
    value <- vector("list", ncols)
    if(mode(x) == "character" && stringsAsFactors) {
        for(i in ic)
            value[[i]] <- as.factor(x[,i])
    } else {
        for(i in ic)
            value[[i]] <- as.vector(x[,i])
    }
    ## Explicitly check for NULL in case nrows==0
    if(is.null(row.names) || length(row.names) != nrows)
        row.names <- .set_row_names(nrows)
    if(length(collabs) == ncols)
        names(value) <- collabs
    else if(!optional)
        names(value) <- paste0("V", ic)
    attr(value, "row.names") <- row.names
    class(value) <- "data.frame"
    value
}

as.data.frame.model.matrix <-
		function(x, row.names = NULL, optional = FALSE, ...)
{
	d <- dim(x)
	nrows <- d[1L]
	dn <- dimnames(x)
	row.names <- dn[[1L]]
	value <- list(x)
	if(!is.null(row.names)) {
		row.names <- as.character(row.names)
		if(length(row.names) != nrows)
			stop(sprintf(ngettext(length(row.names),
									"supplied %d row name for %d rows",
									"supplied %d row names for %d rows"),
							length(row.names), nrows), domain = NA)
	}
	else row.names <- .set_row_names(nrows)
	if(!optional) names(value) <- deparse(substitute(x))[[1L]]
	attr(value, "row.names") <- row.names
	class(value) <- "data.frame"
	value
}

as.data.frame.AsIs <- function(x, row.names = NULL, optional = FALSE, ...)
{
    ## why not remove class and NextMethod here?
    if(length(dim(x)) == 2L)
     as.data.frame.model.matrix(x, row.names, optional)
    else { # as.data.frame.vector without removing names
        nrows <- length(x)
        nm <- paste(deparse(substitute(x), width.cutoff=500L), collapse=" ")
        if(is.null(row.names)) {
            if (nrows == 0L)
                row.names <- character()
            else if(length(row.names <- names(x)) == nrows &&
                    !anyDuplicated(row.names)) {}
            else row.names <- .set_row_names(nrows)
        }
        value <- list(x)
        if(!optional) names(value) <- nm
        attr(value, "row.names") <- row.names
        class(value) <- "data.frame"
        value
    }

}

###  This is the real "data.frame".
###  It does everything by calling the methods presented above.

data.frame <-
		function(..., row.names = NULL, check.rows = FALSE, check.names = TRUE,
				stringsAsFactors = default.stringsAsFactors())
{
	data.row.names <-
			if(check.rows && is.null(row.names))
				function(current, new, i) {
					if(is.character(current)) new <- as.character(new)
					if(is.character(new)) current <- as.character(current)
					if(anyDuplicated(new))
						return(current)
					if(is.null(current))
						return(new)
					if(all(current == new) || all(current == ""))
						return(new)
					stop(gettextf("mismatch of row names in arguments of 'data.frame\', item %d", i), domain = NA)
				}
			else function(current, new, i) {
					if(is.null(current)) {
						if(anyDuplicated(new)) {
							warning(gettextf("some row.names duplicated: %s --> row.names NOT used",
											paste(which(duplicated(new)), collapse=",")),
									domain = NA)
							current
						} else new
					} else current
				}
	object <- as.list(substitute(list(...)))[-1L]
	mirn <- missing(row.names) # record before possibly changing
	mrn <- is.null(row.names) # missing or NULL
	x <- list(...)
	n <- length(x)
	if(n < 1L) {
		if(!mrn) {
			if(is.object(row.names) || !is.integer(row.names))
				row.names <- as.character(row.names)
			if(any(is.na(row.names)))
				stop("row names contain missing values")
			if(anyDuplicated(row.names))
				stop(gettextf("duplicate row.names: %s",
								paste(unique(row.names[duplicated(row.names)]),
										collapse = ", ")),
						domain = NA)
		} else row.names <- integer()
		return(structure(list(), names = character(),
						row.names = row.names,
						class = "data.frame"))
	}
	vnames <- names(x)
	if(length(vnames) != n)
		vnames <- character(n)
	no.vn <- !nzchar(vnames)
	vlist <- vnames <- as.list(vnames)
	nrows <- ncols <- integer(n)
	for(i in seq_len(n)) {
		## do it this way until all as.data.frame methods have been updated
		xi <- if(is.character(x[[i]]) || is.list(x[[i]]))
					as.data.frame(x[[i]], optional = TRUE,
							stringsAsFactors = stringsAsFactors)
				else as.data.frame(x[[i]], optional = TRUE)
		
		nrows[i] <- .row_names_info(xi) # signed for now
		ncols[i] <- length(xi)
		namesi <- names(xi)
		if(ncols[i] > 1L) {
			if(length(namesi) == 0L) namesi <- seq_len(ncols[i])
			if(no.vn[i]) vnames[[i]] <- namesi
			else vnames[[i]] <- paste(vnames[[i]], namesi, sep=".")
		}
		else {
			if(length(namesi)) vnames[[i]] <- namesi
			else if (no.vn[[i]]) {
				tmpname <- deparse(object[[i]])[1L]
				if( substr(tmpname, 1L, 2L) == "I(" ) {
					ntmpn <- nchar(tmpname, "c")
					if(substr(tmpname, ntmpn, ntmpn) == ")")
						tmpname <- substr(tmpname, 3L, ntmpn - 1L)
				}
				vnames[[i]] <- tmpname
			}
		} # end of ncols[i] <= 1
		if(mirn && nrows[i] > 0L) {
			rowsi <- attr(xi, "row.names")
			## Avoid all-blank names
			nc <- nchar(rowsi, allowNA = FALSE)
			nc <- nc[!is.na(nc)]
			if(length(nc) && any(nc))
				row.names <- data.row.names(row.names, rowsi, i)
		}
		nrows[i] <- abs(nrows[i])
		vlist[[i]] <- xi
	}
	nr <- max(nrows)
	for(i in seq_len(n)[nrows < nr]) {
		xi <- vlist[[i]]
		if(nrows[i] > 0L && (nr %% nrows[i] == 0L)) {
			## make some attempt to recycle column i
			xi <- unclass(xi) # avoid data-frame methods
			fixed <- TRUE
			for(j in seq_along(xi)) {
				xi1 <- xi[[j]]
				if(is.vector(xi1) || is.factor(xi1))
					xi[[j]] <- rep(xi1, length.out = nr)
				else if(is.character(xi1) && inherits(xi1, "AsIs"))
					xi[[j]] <- structure(rep(xi1, length.out = nr),
							class = class(xi1))
				else if(inherits(xi1, "Date") || inherits(xi1, "POSIXct"))
					xi[[j]] <- rep(xi1, length.out = nr)
				else {
					fixed <- FALSE
					break
				}
			}
			if (fixed) {
				vlist[[i]] <- xi
				next
			}
		}
		stop(gettextf("arguments imply differing number of rows: %s",
						paste(unique(nrows), collapse = ", ")),
				domain = NA)
	}
	value <- unlist(vlist, recursive=FALSE, use.names=FALSE)
	## unlist() drops i-th component if it has 0 columns
	vnames <- unlist(vnames[ncols > 0L])
	noname <- !nzchar(vnames)
	if(any(noname))
		vnames[noname] <- paste("Var", seq_along(vnames), sep = ".")[noname]
	if(check.names)
		vnames <- make.names(vnames, unique=TRUE)
	names(value) <- vnames
	if(!mrn) { # non-null row.names arg was supplied
		if(length(row.names) == 1L && nr != 1L) {  # one of the variables
			if(is.character(row.names))
				row.names <- match(row.names, vnames, 0L)
			if(length(row.names) != 1L ||
					row.names < 1L || row.names > length(vnames))
				stop("'row.names' should specify one of the variables")
			i <- row.names
			row.names <- value[[i]]
			value <- value[ - i]
		} else if ( !is.null(row.names) && length(row.names) != nr )
			stop("row names supplied are of the wrong length")
	} else if( !is.null(row.names) && length(row.names) != nr ) {
		warning("row names were found from a short variable and have been discarded")
		row.names <- NULL
	}
	if(is.null(row.names))
		row.names <- .set_row_names(nr) #seq_len(nr)
	else {
		if(is.object(row.names) || !is.integer(row.names))
			row.names <- as.character(row.names)
		if(any(is.na(row.names)))
			stop("row names contain missing values")
		if(anyDuplicated(row.names))
			stop(gettextf("duplicate row.names: %s",
							paste(unique(row.names[duplicated(row.names)]),
									collapse = ", ")),
					domain = NA)
	}
	attr(value, "row.names") <- row.names
	attr(value, "class") <- "data.frame"
	value
}

### Here are the methods for rbind and cbind.

cbind.data.frame <- function(..., deparse.level = 1)
	data.frame(..., check.names = FALSE)

rbind.data.frame <- function(..., deparse.level = 1)
{
	match.names <- function(clabs, nmi)
	{
		if(identical(clabs, nmi)) NULL
		else if(length(nmi) == length(clabs) && all(nmi %in% clabs)) {
			## we need 1-1 matches here
			m <- pmatch(nmi, clabs, 0L)
			if(any(m == 0L))
				stop("names do not match previous names")
			m
		} else stop("names do not match previous names")
	}
	Make.row.names <- function(nmi, ri, ni, nrow)
	{
		if(nzchar(nmi)) {
			if(ni == 0L) character()  # PR8506
			else if(ni > 1L) paste(nmi, ri, sep = ".")
			else nmi
		}
		else if(nrow > 0L && identical(ri, seq_len(ni)))
			as.integer(seq.int(from = nrow + 1L, length.out = ni))
		else ri
	}
	allargs <- list(...)
	allargs <- allargs[vapply(allargs, length, 1L) > 0L]
	if(length(allargs)) {
		## drop any zero-row data frames, as they may not have proper column
		## types (e.g. NULL).
		nr <- vapply(allargs, function(x)
					if(is.data.frame(x)) .row_names_info(x, 2L)
					else if(is.list(x)) length(x[[1L]]) # mismatched lists are checked later
					else length(x), 1L)
		if(any(nr > 0L)) allargs <- allargs[nr > 0L]
		else return(allargs[[1L]]) # pretty arbitrary
	}
	n <- length(allargs)
	if(n == 0L)
		return(structure(list(),
						class = "data.frame",
						row.names = integer()))
	nms <- names(allargs)
	if(is.null(nms))
		nms <- character(n)
	cl <- NULL
	perm <- rows <- rlabs <- vector("list", n)
	nrow <- 0L
	value <- clabs <- NULL
	all.levs <- list()
	for(i in seq_len(n)) {
		## check the arguments, develop row and column labels
		xi <- allargs[[i]]
		nmi <- nms[i]
		## coerce matrix to data frame
		if(is.matrix(xi)) allargs[[i]] <- xi <- as.data.frame(xi)
		if(inherits(xi, "data.frame")) {
			if(is.null(cl))
				cl <- oldClass(xi)
			ri <- attr(xi, "row.names")
			ni <- length(ri)
			if(is.null(clabs))
				clabs <- names(xi)
			else {
				if(length(xi) != length(clabs))
					stop("numbers of columns of arguments do not match")
				pi <- match.names(clabs, names(xi))
				if( !is.null(pi) ) perm[[i]] <- pi
			}
			rows[[i]] <- seq.int(from = nrow + 1L, length.out = ni)
			rlabs[[i]] <- Make.row.names(nmi, ri, ni, nrow)
			nrow <- nrow + ni
			if(is.null(value)) {
				value <- unclass(xi)
				nvar <- length(value)
				all.levs <- vector("list", nvar)
				has.dim <- logical(nvar)
				facCol <- logical(nvar)
				ordCol <- logical(nvar)
				for(j in seq_len(nvar)) {
					xj <- value[[j]]
					if( !is.null(levels(xj)) ) {
						all.levs[[j]] <- levels(xj)
						facCol[j] <- TRUE # turn categories into factors
					} else facCol[j] <- is.factor(xj)
					ordCol[j] <- is.ordered(xj)
					has.dim[j] <- length(dim(xj)) == 2L
				}
			}
			else for(j in seq_len(nvar)) {
					xij <- xi[[j]]
					if(is.null(pi) || is.na(jj <- pi[[j]])) jj <- j
					if(facCol[jj]) {
						if(length(lij <- levels(xij))) {
							all.levs[[jj]] <- unique(c(all.levs[[jj]], lij))
							ordCol[jj] <- ordCol[jj] & is.ordered(xij)
						} else if(is.character(xij))
							all.levs[[jj]] <- unique(c(all.levs[[jj]], xij))
					}
				}
		}
		else if(is.list(xi)) {
			ni <- range(vapply(xi, length, 1L))
			if(ni[1L] == ni[2L])
				ni <- ni[1L]
			else stop("invalid list argument: all variables should have the same length")
			rows[[i]] <- ri <-
					as.integer(seq.int(from = nrow + 1L, length.out = ni))
			nrow <- nrow + ni
			rlabs[[i]] <- Make.row.names(nmi, ri, ni, nrow)
			if(length(nmi <- names(xi)) > 0L) {
				if(is.null(clabs))
					clabs <- nmi
				else {
					if(length(xi) != length(clabs))
						stop("numbers of columns of arguments do not match")
					pi <- match.names(clabs, nmi)
					if( !is.null(pi) ) perm[[i]] <- pi
				}
			}
		}
		else if(length(xi)) {
			rows[[i]] <- nrow <- nrow + 1L
			rlabs[[i]] <- if(nzchar(nmi)) nmi else as.integer(nrow)
		}
	}
	nvar <- length(clabs)
	if(nvar == 0L)
		nvar <- max(vapply(allargs, length, 1L)) # only vector args
	if(nvar == 0L)
		return(structure(list(), class = "data.frame",
						row.names = integer()))
	pseq <- seq_len(nvar)
	if(is.null(value)) { # this happens if there has been no data frame
		value <- list()
		value[pseq] <- list(logical(nrow)) # OK for coercion except to raw.
		all.levs <- vector("list", nvar)
		has.dim <- logical(nvar)
		facCol <- logical(nvar)
		ordCol <- logical(nvar)
	}
	names(value) <- clabs
	for(j in pseq)
		if(length(lij <- all.levs[[j]]))
			value[[j]] <-
					factor(as.vector(value[[j]]), lij, ordered = ordCol[j])
	if(any(has.dim)) {
		rmax <- max(unlist(rows))
		for(i in pseq[has.dim])
			if(!inherits(xi <- value[[i]], "data.frame")) {
				dn <- dimnames(xi)
				rn <- dn[[1L]]
				if(length(rn) > 0L) length(rn) <- rmax
				pi <- dim(xi)[2L]
				length(xi) <- rmax * pi
				value[[i]] <- array(xi, c(rmax, pi), list(rn, dn[[2L]]))
			}
	}
	for(i in seq_len(n)) {
		xi <- unclass(allargs[[i]])
		if(!is.list(xi))
			if(length(xi) != nvar)
				xi <- rep(xi, length.out = nvar)
		ri <- rows[[i]]
		pi <- perm[[i]]
		if(is.null(pi)) pi <- pseq
		for(j in pseq) {
			jj <- pi[j]
			xij <- xi[[j]]
			if(has.dim[jj]) {
				value[[jj]][ri,	 ] <- xij
				## copy rownames
				rownames(value[[jj]])[ri] <- rownames(xij)
			} else {
				## coerce factors to vectors, in case lhs is character or
				## level set has changed
				value[[jj]][ri] <- if(is.factor(xij)) as.vector(xij) else xij
				## copy names if any
				if(!is.null(nm <- names(xij))) names(value[[jj]])[ri] <- nm
			}
		}
	}
	rlabs <- unlist(rlabs)
	if(anyDuplicated(rlabs))
		rlabs <- make.unique(as.character(unlist(rlabs)), sep = "")
	if(is.null(cl)) {
		as.data.frame(value, row.names = rlabs)
	} else {
		class(value) <- cl
		attr(value, "row.names") <- rlabs
		value
	}
}

### coercion and print methods

print.data.frame <-
		function(x, ..., digits = NULL, quote = FALSE, right = TRUE,
				row.names = TRUE)
{
	n <- length(row.names(x))
	if(length(x) == 0L) {
		cat(gettextf("data frame with 0 columns and %d rows\n", n))
	} else if(n == 0L) {
		## FIXME: header format is inconsistent here
		print.default(names(x), quote = FALSE)
		cat(gettext("<0 rows> (or 0-length row.names)\n"))
	} else {
		## format.<*>() : avoiding picking up e.g. format.AsIs
		m <- as.matrix(format.data.frame(x, digits = digits, na.encode = FALSE))
		if(!isTRUE(row.names))
			dimnames(m)[[1L]] <- if(identical(row.names,FALSE))
						rep.int("", n) else row.names
		print(m, quote = quote, right = right)
	}
	invisible(x)
}

as.matrix.data.frame <- function (x, rownames.force = NA, ...)
{
	dm <- dim(x)
	rn <- if(rownames.force %in% FALSE) NULL
			else if(rownames.force %in% TRUE) row.names(x)
			else {if(.row_names_info(x) <= 0L) NULL else row.names(x)}
	dn <- list(rn, names(x))
	if(any(dm == 0L))
		return(array(NA, dim = dm, dimnames = dn))
	p <- dm[2L]
	pseq <- seq_len(p)
	n <- dm[1L]
	X <- x # will contain the result;
	## the "big question" is if we return a numeric or a character matrix
	class(X) <- NULL
	non.numeric <- non.atomic <- FALSE
	all.logical <- TRUE
	for (j in pseq) {
		if(inherits(X[[j]], "data.frame") && ncol(xj) > 1L)
			X[[j]] <- as.matrix(X[[j]])
		xj <- X[[j]]
		j.logic <- is.logical(xj)
		if(all.logical && !j.logic) all.logical <- FALSE
		if(length(levels(xj)) > 0L || !(j.logic || is.numeric(xj) || is.complex(xj))
				|| (!is.null(cl <- attr(xj, "class")) && # numeric classed objects to format:
					any(cl %in% c("Date", "POSIXct", "POSIXlt"))))
			non.numeric <- TRUE
		if(!is.atomic(xj))
			non.atomic <- TRUE
	}
	if(non.atomic) {
		for (j in pseq) {
			xj <- X[[j]]
			if(!is.recursive(xj))
				X[[j]] <- as.list(as.vector(xj))
		}
	} else if(all.logical) {
		## do nothing for logical columns if a logical matrix will result.
	} else if(non.numeric) {
		for (j in pseq) {
			if (is.character(X[[j]]))
				next
			xj <- X[[j]]
			miss <- is.na(xj)
			xj <- if(length(levels(xj))) as.vector(xj) else format(xj)
			is.na(xj) <- miss
			X[[j]] <- xj
		}
	}
	## These coercions could have changed the number of columns
	## (e.g. class "Surv" coerced to character),
	## so only now can we compute collabs.
	collabs <- as.list(dn[[2L]])
	for (j in pseq) {
		xj <- X[[j]]
		dj <- dim(xj)
		if(length(dj) == 2L && dj[2L] > 1L) { # matrix with >=2 col
			dnj <- colnames(xj)
			collabs[[j]] <- paste(collabs[[j]],
					if(length(dnj)) dnj else seq_len(dj[2L]),
					sep = ".")
		}
	}
	X <- unlist(X, recursive = FALSE, use.names = FALSE)
	dim(X) <- c(n, length(X)/n)
	dimnames(X) <- list(dn[[1L]], unlist(collabs, use.names = FALSE))
	##NO! don't copy buggy S-plus!  either all matrices have class or none!!
	##NO class(X) <- "matrix"
	X
}
	
