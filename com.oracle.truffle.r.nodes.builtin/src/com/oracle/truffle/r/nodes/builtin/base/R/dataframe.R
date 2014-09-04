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

# TODO: implement NA_integer_
.set_row_names <- function(n)
    if(n > 0) c(as.integer(NA), -n) else integer()

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
            x <- data.frame(x, check.names = !optional,
                            stringsAsFactors = stringsAsFactors)
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
                    new
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
		print(m, ..., quote = quote, right = right)
	}
	invisible(x)
}
