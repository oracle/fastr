##
 # This material is distributed under the GNU General Public License
 # Version 2. You may review the terms of this license at
 # http://www.gnu.org/licenses/gpl-2.0.html
 #
 # Copyright (c) 2006 Simon Urbanek <simon.urbanek@r-project.org>
 # Copyright (c) 2017, Oracle and/or its affiliates
 #
 # All rights reserved.
##

#' @export
.jnew <- function (class, ..., check = TRUE, silent = !check) 
{
    class <- gsub("/", ".", as.character(class))
    co <- .fastr.java.class(class)
    o <- .fastr.interop.new(co, ...)
    invisible(o)
}

#' @export
.jcall <- function (obj, returnSig = "V", method, ..., evalArray = TRUE, 
    evalString = TRUE, check = TRUE, interface = "RcallMethod", 
    simplify = FALSE, use.true.class = FALSE) 
{
    if(is.character(obj)) {
        co <- .fastr.java.class(obj)
        r <- co[method](...)
    } else {
        r <- obj[method](...)
    }
    r
}

#' @export
.jfield <- function (obj, sig = NULL, name, true.class = is.null(sig), convert = TRUE) 
{
    if(is.character(obj)) {
        co <- .fastr.java.class(obj)
        r <- co[name]
    } else {
        r <- obj[name]
    }
    r    
}

#' @export
.jarray <- function (x, contents.class = NULL, dispatch = FALSE) 
{
    .fastr.java.toArray(x, ,TRUE)
}

#' @export
.jevalArray <- function (x, contents.class = NULL, dispatch = FALSE) 
{
    .fastr.java.fromArray(x)
}

#' @export
.jbyte <- function (x) 
{
    x <- .fastr.interop.toByte(x)
    invisible(x)
}

#' @export
.jchar <- function (x) 
{
    x <- .fastr.interop.toChar(x)
    invisible(x)
}

#' @export
.jshort <- function (x) 
{
    x <- .fastr.interop.toShort(x)
    invisible(x)
}

#' @export
.jlong <- function (x) 
{
    x <- .fastr.interop.toLong(x)
    invisible(x)
}

#' @export
.jfloat <- function (x) 
{    
    x <- .fastr.interop.toFloat(x)
    invisible(x)
}

#
# noop stubs
#

#' @export
.jinit <- function ()
{    
    # do nothing
}

#' @export
.jsimplify <- function (x) 
{    
    x
}

#' @export
.jcheck <- function(silent = FALSE) {
    FALSE
}

#' @export
.jnull <- function (class)
{    
    # do nothing
}
