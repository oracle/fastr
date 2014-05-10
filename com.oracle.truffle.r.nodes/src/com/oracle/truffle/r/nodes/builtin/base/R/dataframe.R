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
# TODO: remove default parameter values for rownames once snippet default args are handled properly
row.names.default <- function(x) if(!is.null(dim(x))) rownames(x, TRUE, "row")# else NULL

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
# TODO: implement anyDuplicated and sprintf
#    if (anyDuplicated(value)) {
#        nonuniq <- sort(unique(value[duplicated(value)]))
#        warning(ngettext(length(nonuniq),
#                         sprintf("non-unique value when setting 'row.names': %s",
#                                 sQuote(nonuniq[1L])),
#                         sprintf("non-unique values when setting 'row.names': %s",
#                                 paste(sQuote(nonuniq), collapse = ", "))),
#                domain = NA, call. = FALSE)
#        stop("duplicate 'row.names' are not allowed")
#    }
    if (any(is.na(value)))
        stop("missing values in 'row.names' are not allowed")
    attr(x, "row.names") <- value
    x
}

`row.names<-.default` <- function(x, value) `rownames<-`(x, value)


as.data.frame <- function(x, row.names = NULL, optional = FALSE, ...)
{
    if(is.null(x)) # can't assign class to NULL
        return(as.data.frame(list()))
    UseMethod("as.data.frame")
}

as.data.frame.default <- function(x, ...)
# TODO: implement deparse (sprintf inside of gettextf does not work here either)
#    stop(gettextf("cannot coerce class \"%s\" to a data.frame",
#                  deparse(class(x))),
    stop(sprintf("cannot coerce class to a data.frame"), domain = NA)

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


# TODO: handle parameters correctly
#as.data.frame.numeric <- function(x, row.names = NULL, optional = FALSE, ...,
#                                 nm = paste(deparse(substitute(x),
#                                 width.cutoff = 500L), collapse=" ")  )
as.data.frame.vector <- function(x, row.names = NULL, nm = NULL, optional = FALSE)
{
    force(nm)
    nrows <- length(x)
    if(is.null(row.names)) {
	if (nrows == 0L)
	    row.names <- character()
# TODO implement anyDuplicated
#	else if(length(row.names <- names(x)) == nrows &&
#		!anyDuplicated(row.names)) {}
	else if(length(row.names <- names(x)) == nrows) {}
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

# TODO: "properly" evaluate these .R files (for some reason assignments do not work at this point)
#as.data.frame.raw  <- as.data.frame.vector
as.data.frame.raw <- function(x, row.names = NULL, nm = NULL, optional = FALSE) { as.data.frame.vector(x, row.names, nm, optional); }
#as.data.frame.factor  <- as.data.frame.vector
#as.data.frame.ordered <- as.data.frame.vector
#as.data.frame.integer <- as.data.frame.vector
as.data.frame.integer <- function(x, row.names = NULL, nm = NULL, optional = FALSE) { as.data.frame.vector(x, row.names, nm, optional); }
#as.data.frame.numeric <- as.data.frame.vector
as.data.frame.numeric <- function(x, row.names = NULL, nm = NULL, optional = FALSE) { as.data.frame.vector(x, row.names, nm, optional); }
#as.data.frame.complex <- as.data.frame.vector
as.data.frame.complex <- function(x, row.names = NULL, nm = NULL, optional = FALSE) { as.data.frame.vector(x, row.names, nm, optional); }

default.stringsAsFactors <- function()
{
# TODO: implement getOption
#    val <- getOption("stringsAsFactors")
#    if(is.null(val)) val <- TRUE
#    if(!is.logical(val) || is.na(val) || length(val) != 1L)
#        stop('options("stringsAsFactors") not set to TRUE or FALSE')
#    val
    FALSE
}

# TODO: implement deparse (on the main execution path)
## in case someone passes 'nm'
#as.data.frame.character <-
#    function(x, ..., stringsAsFactors = default.stringsAsFactors())
#{
#    nm <- deparse(substitute(x), width.cutoff=500L)
#    if(stringsAsFactors) x <- factor(x)
#    if(!"nm" %in% names(list(...)))
#        as.data.frame.vector(x, ..., nm = nm)
#    else as.data.frame.vector(x, ...)
#}
as.data.frame.character <- function(x, row.names = NULL, nm = NULL, optional = FALSE) { as.data.frame.vector(x, row.names, nm, optional); }

# TODO: "properly" evaluate these .R files (for some reason assignments do not work at this point)
#as.data.frame.logical <- as.data.frame.vector
as.data.frame.logical <- function(x, row.names = NULL, nm = NULL, optional = FALSE) { as.data.frame.vector(x, row.names, nm, optional); }

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
