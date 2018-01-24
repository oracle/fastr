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
.jnew <- function (class, ..., check = TRUE, silent = !check) {
    class <- gsub("/", ".", as.character(class))
    co <- new.java.class(class)
    args <- .fromS4(co, ...)
    o <- .fastr.interop.try(function() { do.call(new.external, args) }, check)
    new("jobjRef", jobj=o, jclass=class)
}


#' @export
.jcall <- function (obj, returnSig = "V", method, ..., evalArray = TRUE, 
    evalString = TRUE, check = TRUE, interface = "RcallMethod", 
    simplify = FALSE, use.true.class = FALSE) {
    if (isS4(obj)) {
        obj <- obj@jobj
    } 
    args <- .fromS4(...)

    if (is.character(obj)) {
        obj <- gsub("/", ".", as.character(obj))
        obj <- new.java.class(obj)
    }

    extMethod <- function(...) {obj[method](...)}
    r <- .fastr.interop.try(function() { do.call(extMethod, args) }, check)
    
    if (is.null(r) && returnSig == "V") {
        return(invisible(NULL))
    }

    .toS4(r)
}

#' @export
.jfield <- function (obj, sig = NULL, name, true.class = is.null(sig), convert = TRUE) {
    if (isS4(obj)) {
        obj <- obj@jobj
    }
    if (is.character(obj)) {
        co <- new.java.class(obj)
        r <- co[name]
    } else {
        r <- obj[name]
    }
    .toS4(r)  
}

#' @export
.jarray <- function (x, contents.class = NULL, dispatch = FALSE) {
    as.java.array(x, ,TRUE)
}

#' @export
.jevalArray <- function (x, contents.class = NULL, dispatch = FALSE) {
    .fastr.interop.fromArray(x)
}

#' @export
.jbyte <- function (x) {
    x <- as.external.byte(x)
    invisible(x)
}

#' @export
.jchar <- function (x) {
    x <- as.external.char(x)
    invisible(x)
}

#' @export
.jshort <- function (x) {
    x <- as.external.short(x)
    invisible(x)
}

#' @export
.jlong <- function (x) {
    x <- as.external.long(x)
    invisible(x)
}

#' @export
.jfloat <- function (x) {
    x <- as.external.float(x)
    invisible(x)
}

#' @export
J <- function (class, method, ...) {
    class <- gsub("/", ".", as.character(class))
    javaClass <- new.java.class(class)
    if (nargs() == 1L && missing(method)) {
        javaClass
    } else {
        .jcall(javaClass, ,method, ...)
    }    
}

#' @export
.jpackage <- function (name, jars='*', morePaths='', nativeLibrary=FALSE, lib.loc=NULL) {
    classes <- system.file("java", package = name, lib.loc = lib.loc)
    if (nchar(classes)) {
        .jaddClassPath(classes)
        if (length(jars)) {
            if (length(jars) == 1 && jars == "*") {
                jars <- grep(".*\\.jar", list.files(classes, 
                  full.names = TRUE), TRUE, value = TRUE)
                if (length(jars)) 
                  .jaddClassPath(jars)
            }
            else .jaddClassPath(paste(classes, jars, sep = .Platform$file.sep))
        }
    }
    if (any(nchar(morePaths))) {
        cl <- as.character(morePaths)
        cl <- cl[nchar(cl) > 0]
        .jaddClassPath(cl)
    }
    if (is.logical(nativeLibrary)) {
        if (nativeLibrary) {
            libs <- "libs"
            if (nchar(.Platform$r_arch)) 
                lib <- file.path("libs", .Platform$r_arch)
            lib <- system.file(libs, paste(name, .Platform$dynlib.ext, 
                sep = ""), package = name, lib.loc = lib.loc)
            if (nchar(lib)) 
                .jaddLibrary(name, lib)
            else warning("Native library for `", name, "' could not be found.")
        }
    }
    else {
        .jaddLibrary(name, nativeLibrary)
    }
    invisible(TRUE)
}

#' @export
.jaddClassPath <- function (path) {
    java.addToClasspath(path)
}

#' @export
.jfindClass <- function (cl, silent = FALSE) {
    new.java.class(cl)
}

.toS4 <- function(obj) {
    res <- obj
    if (is.external(obj)) {        
        if (is.external.array(obj)) {
            res <- as.vector(obj)
        } else {
            res <- new("jobjRef", jobj=obj, jclass=java.class(obj))
        }        
    } 
    res
}

.fromS4 <- function(...) {
    l <- list(...)
    if (length(l)) {
        for (i in 1:length(l)) {
            if (isS4(l[[i]])) {
                o <- l[[i]]@jobj
                if (is.null(o)) {
                    l[i] <- list(NULL)
                } else {
                    l[[i]] <- o
                }                    
            }
        }
    }
    l
}

#' @export
.jgetEx <- function (clear = FALSE) {
    interopEx <- .fastr.interop.getTryException(clear)
    if (is.null(interopEx))
        return(NULL)
    new("jobjRef", jobj = interopEx, jclass = "java/lang/Throwable")
}

#' @export
.jclear <- function () {
     invisible(.fastr.interop.clearTryException())
}

#' @export
.jinit <- function (classpath = NULL, parameters = getOption("java.parameters"), ..., silent = FALSE, force.init = FALSE) {
    if (!is.null(classpath)) java.addToClasspath(classpath)
}

#' @export
.jnull <- function (class = "java/lang/Object") {    
    new("jobjRef", jobj=NULL, jclass=class)
}

#' @export
is.jnull <- function (x) {
    is.null(x) || is.external.null(x) || (is(x,"jobjRef") && is.null(x@jobj))
}

#' @export
.jaddLibrary <- function (name, path) {
    cat(paste0("********************************************************\n",
           "*** WARNING!!!\n",
           "*** .jaddLibrary is not yet implemented.\n",
           "*** Please ensure that all native libraries from:\n",
           "*** ", path, "\n",
           "*** are set on LD_LIBRARY_PATH or java.library.path\n",
           "********************************************************\n"))
}

#' @export
.jcast <- function(obj, new.class="java/lang/Object", check = FALSE, convert.array = FALSE) {
  if (!is(obj,"jobjRef"))
    stop("cannot cast anything but Java objects")
  # TODO implement checks  
  # if( check && !.jinstanceof( obj, new.class) ){
  #     stop( sprintf( "cannot cast object to '%s'", new.class ) ) 
  # }
  
  new.class <- gsub("\\.","/", as.character(new.class)) # allow non-JNI specifiation
  # if( convert.array && !is( obj, "jarrayRef" ) && isJavaArray( obj ) ){
  #    r <- .jcastToArray( obj, signature = new.class)
  # } else {
     r <- obj
     r@jclass <- new.class
  # }
  r
}

#' @export
.jstrVal <- function (obj) {
    if (is.character(obj)) 
        return(obj)
    r <- NULL
    if (!is(obj, "jobjRef")) 
        stop("can get value of Java objects only")
    if (!is.null(obj@jclass) && obj@jclass == "lang/java/String") 
        r <- .External(RgetStringValue, obj@jobj)
    else r <- obj@jobj["toString"]()
    r
}

#
# S4
#

setClass("truffle.object", representation(jobj="ANY"))
setClassUnion("TruffleObjectOrNull",members=c("truffle.object", "NULL"))

# jobjRef
setClass("jobjRef", representation(jobj="TruffleObjectOrNull", jclass="character"), prototype=list(jobj=NULL, jclass="java/lang/Object"))

._jobjRef_dollar <- function(x, name) {
    if(name %in% names(x@jobj)) {        
        if(is.external.executable(x@jobj[name])) {
            function(...) { .jcall(x, , name, ...) }    
        } else {
            .jfield(x, , name)                
        }
    } else if( is.character(name) && length(name) == 1L && name == "length" && is.external.array(x) ){        
        length( x@obj )
    } else {
        stop(sprintf( "no field, method or inner class called '%s' ", name)) 
    }
}
setMethod("$", c(x="jobjRef"), ._jobjRef_dollar )

._jobjRef_dollargets <- function(x, name, value) {
    if(name %in% names(x@jobj)) {
        if(!is.external.executable(x@jobj[name])) {
            if(isS4(value)) {
                value <- value@jobj
            }
            x@jobj[name] <- value
        }
    }
    x
}
setMethod("$<-", c(x="jobjRef"), ._jobjRef_dollargets )

setMethod("show", c(object="jobjRef"), function(object) {
  if (is.jnull(object)) show("Java-Object<null>") else show(paste("Java-Object{", .jstrVal(object), "}", sep=''))
  invisible(NULL)
})

#
# noop stubs
#

#' @export
.jsimplify <- function (x) {
    x
}

#' @export
.jcheck <- function(silent = FALSE) {
    FALSE
}

#' @export
.jthrow <- function (exception, message = NULL) {
    # do nothing
}
