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
        obj <- gsub("/", ".", as.character(obj))
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

#' @export
J <- function (class, method, ...) 
{    
    class <- gsub("/", ".", as.character(class))
    javaClass <- .fastr.java.class(class)
    if (nargs() == 1L && missing(method)) {
        javaClass
    } else {
        .jcall(javaClass, ,method, ...)
    }    
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

#' @export
.jpackage <- function (name, jars='*', morePaths='', nativeLibrary=FALSE, lib.loc=NULL)
{    
    javalibs <- system.file("java", package = name, lib.loc = lib.loc)
    if(javalibs == "") {
        javalibs = paste0("library/", name, "/java")
    }
    cat(paste0("********************************************************\n",
               "*** WARNING!!!\n",
               "*** .jpackage is not yet implemented.\n",
               "*** Please ensure that all java libraries from:\n",
               "*** ", javalibs, "\n",
               "*** are on FastR classpath\n",
               "********************************************************\n"))
}

#' @export
.jaddClassPath <- function (path)
{
    cat(paste0("********************************************************\n",
               "*** WARNING!!!\n",
               "*** .jaddClasPath is not yet implemented.\n",
               "*** Please ensure that \n",
               "*** ", path, "\n",
               "*** is on FastR classpath\n",
               "********************************************************\n"))
}