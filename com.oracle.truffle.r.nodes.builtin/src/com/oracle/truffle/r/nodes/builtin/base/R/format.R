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

format <- function(x, ...) UseMethod("format")

format.default <-
    function(x, trim = FALSE, digits = NULL, nsmall = 0L,
                justify = c("left", "right", "centre", "none"),
                width = NULL, na.encode = TRUE, scientific = NA,
                big.mark = "", big.interval = 3L,
                small.mark = "", small.interval = 5L, decimal.mark = ".",
                zero.print = NULL, drop0trailing = FALSE, ...)
{
# TODO: implement deparse in match.arg
#    justify <- match.arg(justify)
    justify <- match.arg(justify, c("left", "right", "centre", "none"))
        adj <- match(justify, c("left", "right", "centre", "none")) - 1L
    if(is.list(x)) {
        ## do it this way to force evaluation of args
        if(missing(trim)) trim <- TRUE
        if(missing(justify)) justify <- "none"
        res <- lapply(X = x,
                      FUN = function(xx, ...) format.default(unlist(xx),...),
                      trim = trim, digits = digits, nsmall = nsmall,
                      justify = justify, width = width, na.encode = na.encode,
                      scientific = scientific,
                      big.mark = big.mark, big.interval = big.interval,
                      small.mark = small.mark, small.interval = small.interval,
                      decimal.mark = decimal.mark, zero.print = zero.print,
                      drop0trailing = drop0trailing, ...)
        sapply(res, paste, collapse = ", ")
    } else {
        switch(mode(x),
# TODO: for some reason, our parser does not handle this case
#               NULL = "NULL",
               character = .Internal(format(x, trim, digits, nsmall, width,
                                            adj, na.encode, scientific)),
# TODO: implement deparse
#               call=, expression=, "function"=, "(" = deparse(x),
               raw = as.character(x),
           {
                ## else: logical, numeric, complex, .. :
                prettyNum(.Internal(format(x, trim, digits, nsmall, width,
                          3L, na.encode, scientific)),
                          big.mark = big.mark, big.interval = big.interval,
                          small.mark = small.mark,
                          small.interval = small.interval,
                          decimal.mark = decimal.mark,
                          zero.print = zero.print, drop0trailing = drop0trailing,
                          is.cmplx = is.complex(x),
                          preserve.width = if (trim) "individual" else "common")
           })
    }
}

format.data.frame <- function(x, ..., justify = "none")
{
    nr <- .row_names_info(x, 2L)
    nc <- length(x)
    rval <- vector("list", nc)
    for(i in 1L:nc)
        rval[[i]] <- format(x[[i]], ..., justify = justify)
	lens <- sapply(rval, NROW)
    if(any(lens != nr)) { # corrupt data frame, must have at least one column
		warning("corrupt data frame: columns will be truncated or padded with NAs")
        for(i in 1L:nc) {
            len <- NROW(rval[[i]])
            if(len == nr) next
			if(length(dim(rval[[i]])) == 2L) {
                rval[[i]] <- if(len < nr)
                    rbind(rval[[i]], matrix(NA, nr-len, ncol(rval[[i]])))
                else rval[[i]][1L:nr,]
            } else {
                rval[[i]] <- if(len < nr) c(rval[[i]], rep.int(NA, nr-len))
                    else rval[[i]][1L:nr]
            }
        }
    }
	for(i in 1L:nc) {
        if(is.character(rval[[i]]) && inherits(rval[[i]], "character"))
            oldClass(rval[[i]]) <- "AsIs"
    }
    cn <- names(x)
    m <- match(c("row.names", "check.rows", "check.names", ""), cn, 0L)
    if(any(m)) cn[m] <- paste0("..dfd.", cn[m])
    ## This requires valid symbols for the columns, so we need to
    ## truncate any of more than 256 bytes.
    long <- nchar(cn, "bytes") > 256L
    cn[long] <- paste(substr(cn[long], 1L, 250L), "...")
    names(rval) <- cn
    rval$check.names <- FALSE
    rval$row.names <- row.names(x)
    x <- do.call("data.frame", rval)
	## x will have more cols than rval if there are matrix/data.frame cols
    if(any(m)) names(x) <- sub("^..dfd.", "", names(x))
    x
}

format.AsIs <- function(x, width = 12, ...)
{
    if(is.character(x)) return(format.default(x, ...))
    if(is.null(width)) width = 12L
    n <- length(x)
    rvec <- rep.int(NA_character_, n)
    for(i in 1L:n) {
        y <- x[[i]]
        ## need to remove class AsIs to avoid an infinite loop.
        cl <- oldClass(y)
        if(m <- match("AsIs", cl, 0L)) oldClass(y) <- cl[-m]
        rvec[i] <- toString(y, width = width, ...)
    }
    ## AsIs might be around a matrix, which is not a class.
    dim(rvec) <- dim(x)
    dimnames(rvec) <- dimnames(x)
    format.default(rvec, justify = "right")
}

prettyNum <-
    function(x,
             big.mark = "", big.interval = 3L,
             small.mark = "", small.interval = 5L,
             decimal.mark = ".",
             preserve.width = c("common", "individual", "none"),
             zero.print = NULL, drop0trailing = FALSE, is.cmplx = NA, ...)
{
    if(!is.character(x)) {
        is.cmplx <- is.complex(x)
        x <- sapply(X = x, FUN = format, ...)
    }
    ## be fast in trivial case (when all options have their default):
    nMark <- big.mark== "" && small.mark== "" && decimal.mark== "."
    nZero <- is.null(zero.print) && !drop0trailing
    if(nMark && nZero)
        return(x)

    ## else
    if(!is.null(zero.print) && any(i0 <- as.numeric(x) == 0)) {
        ## print zeros according to 'zero.print' (logical or string):
        if(length(zero.print) > 1L) stop("'zero.print' has length > 1")
        if(is.logical(zero.print))
            zero.print <- if(zero.print) "0" else " "
        if(!is.character(zero.print))
            stop("'zero.print' must be character, logical or NULL")
        blank.chars <- function(no) # as in formatC()
            vapply(no+1L, function(n) paste(character(n), collapse=" "), "")
        nz <- nchar(zero.print, "c")
        nc <- nchar(x[i0], "c")
        ind0 <- regexpr("0", x[i0], fixed = TRUE)# first '0' in string
        substr(x[i0],ind0, (i1 <- ind0+nz-1L)) <- zero.print
        substr(x[i0],ind0+nz, nc) <- blank.chars(nc - i1)
    }
    if(nMark && !drop0trailing)# zero.print was only non-default
        return(x)

    ## else
    if(is.na(is.cmplx)) { ## find if 'x' is format from a *complex*
        ina <- is.na(x) | x == "NA"
        is.cmplx <-
            if(all(ina)) FALSE
            else length(grep("[0-9].*[-+][0-9].*i$", x)) > 0
    }
    if(is.cmplx) {
        ## should be rare .. taking an easy route
        z.sp <- strsplit(sub("([0-9] *)([-+])( *[0-9])",
                         "\\1::\\2::\\3", x), "::", fixed=TRUE)
        ## be careful, if x had an  "NA":
        i3 <- vapply(z.sp, length, 0L) == 3L # those are re + im *i
        if(any(i3)) {
            z.sp <- z.sp[i3]
            z.im <- sapply(z.sp, `[[`, 3L)
            ## drop ending 'i' (and later re-add it)
            has.i <- grep("i$", z.im)
            z.im[has.i] <- sub("i$", '', z.im[has.i])
            r <- lapply(list(sapply(z.sp, `[[`, 1L), z.im),
                        function(.)
                            prettyNum(.,
                                big.mark=big.mark, big.interval=big.interval,
                                small.mark=small.mark, small.interval=small.interval,
                                decimal.mark=decimal.mark, preserve.width=preserve.width,
                                zero.print=zero.print, drop0trailing=drop0trailing,
                                is.cmplx=FALSE, ...))
            r[[2]][has.i] <- paste0(r[[2]][has.i], "i")
            x[i3] <- paste0(r[[1]], sapply(z.sp, `[[`, 2L), r[[2]])
        }
        return(x)
    }
    preserve.width <- match.arg(preserve.width)
    x.sp <- strsplit(x, ".", fixed=TRUE)
    revStr <- function(cc)
        sapply(lapply(strsplit(cc,NULL), rev), paste, collapse="")
    B. <- sapply(x.sp, `[`, 1L)     # Before "."
    A. <- sapply(x.sp, `[`, 2)      # After  "." ; empty == NA
    if(any(iN <- is.na(A.))) A.[iN] <- ""

    if(nzchar(big.mark) &&
       length(i.big <- grep(paste0("[0-9]{", big.interval + 1L,",}"), B.))
       ) { ## add 'big.mark' in decimals before "." :
        B.[i.big] <-
            revStr(gsub(paste0("([0-9]{",big.interval,"})\\B"),
                   paste0("\\1",revStr(big.mark)), revStr(B.[i.big])))
    }
    if(nzchar(small.mark) &&
       length(i.sml <- grep(paste0("[0-9]{", small.interval + 1L,",}"), A.))
       ) { ## add 'small.mark' in decimals after "."  -- but *not* trailing
        A.[i.sml] <- gsub(paste0("([0-9]{",small.interval,"}\\B)"),
                          paste0("\\1",small.mark), A.[i.sml])
    }
    if(drop0trailing) {
        a <- A.[!iN]
        if(length(hasE <- grep("e", a, fixed=TRUE))) {
            a[ hasE] <- sub("e[+-]0+$", '', a[ hasE]) # also drop "e+00"
            a[-hasE] <- sub("0+$",      '', a[-hasE])
        } else a <- sub("0+$", '', a)
        A.[!iN] <- a
        ## iN := TRUE for those A.[]  which are ""
        iN <- !nzchar(A.)
    }
    ## extraneous trailing dec.marks: paste(B., A., sep = decimal.mark)
    A. <- paste0(B., c(decimal.mark, "")[iN+ 1L], A.)
    if(preserve.width != "none") {
        nnc <- nchar(A., "c")
        d.len <- nnc - nchar(x, "c") # extra space added by 'marks' above
        if(any(ii <- d.len > 0L)) {
            switch(preserve.width,
                   "individual" = {
                       ## drop initial blanks preserving original width
                       ## where possible:
                       A.[ii] <- sapply(which(ii), function(i)
                                        sub(sprintf("^ {1,%d}", d.len[i]), "",
                                        A.[i]))
                   },
                   "common" = {
                       A. <- format(A., justify = "right")
                   })
        }
    }
    attributes(A.) <- attributes(x)
    class(A.) <- NULL
    A.
}
