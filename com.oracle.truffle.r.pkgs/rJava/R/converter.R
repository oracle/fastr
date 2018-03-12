##
 # This material is distributed under the GNU General Public License
 # Version 2. You may review the terms of this license at
 # http://www.gnu.org/licenses/gpl-2.0.html
 #
 # Copyright (c) 2006 Simon Urbanek <simon.urbanek@r-project.org>
 # Copyright (c) 2018, Oracle and/or its affiliates
 #
 # All rights reserved.
##

# in: Java -> R
.conv.in <- new.env(parent=emptyenv())
.conv.in$. <- FALSE
# out: R -> Java
.conv.out <- new.env(parent=emptyenv())
.conv.out$. <- FALSE

# --- internal fns
.convert.in <- function(jobj, verify.class=TRUE) {  
  jcl <- if (verify.class) .jclass(jobj) else gsub("/",".",jobj@jclass)
  cv <- .conv.in[[jcl]]
  if (!is.null(cv)) jobj else cv$fn(jobj)
}

.convert.out <- function(robj) {
  for (cl in class(robj)) {
    cv <- .conv.out[[cl]]
    if (!is.null(cv)) return(cv$fn(robj))
  }
  robj
}

# external fns
.jsetJConvertor <- function(java.class, fn) {
  if (is.null(fn)) {
    rm(list=java.class, envir=.conv.in)
    if (!length(ls(.conv.in))) .conv.in$. <- FALSE
  } else {
    .conv.in$. <- TRUE
    .conv.in[[java.class]] <- list(fn=fn)
  }
}

.jsetRConvertor <- function(r.class, fn) {
  if (is.null(fn)) {
    rm(list=r.class, envir=.conv.out)
    if (!length(ls(.conv.out))) .conv.out$. <- FALSE
  } else {
    .conv.out$. <- TRUE
    .conv.out[[r.class]] <- list(fn=fn)
  }
}

