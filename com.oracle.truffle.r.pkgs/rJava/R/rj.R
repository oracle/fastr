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
    args <- .ellipsisToJObj(co, ...)
    o <- .fastr.interop.try(function() { do.call(new.external, args) }, check)
    new("jobjRef", jobj=o, jclass=class)
}


#' @export
.jcall <- function (obj, returnSig = "V", method, ..., evalArray = TRUE, 
    evalString = TRUE, check = TRUE, interface = "RcallMethod", 
    simplify = FALSE, use.true.class = FALSE) {
    obj <- .toJObj(obj)    

    if (is.character(obj)) {
        obj <- gsub("/", ".", as.character(obj))
        obj <- new.java.class(obj)
    }

    args <- .ellipsisToJObj(...)
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
    storage.mode( x ) <- "integer"
    new("jbyte", x)
}

#' @export
.jchar <- function (x) {
    storage.mode( x ) <- "character"
    new("jchar", x)
}

#' @export
.jshort <- function (x) {
    storage.mode( x ) <- "integer"
    new("jshort", x)
}

#' @export
.jlong <- function (x) {
    storage.mode( x ) <- "double"
    new("jlong", x)
}

#' @export
.jfloat <- function (x) {    
    storage.mode( x ) <- "double"
    new("jfloat", x )
}

#' @export
J <- function (class, method, ...) {         
    if (nargs() == 1L && missing(method)) {
        if(inherits(class, "jclassName")) {
            return(class)
        } else if(is.character(class)) {
            className <- class
            jobj <- .jfindClass(class)
        } else {            
            className <- as.character(class)
            jobj <- .jfindClass(className)
        }
        new("jclassName", name=className, jobj=jobj)
    } else {
        # .jrcall(class, method, ...)
        .jcall(class, , method, ...)
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
    if (inherits(cl, "jclassName")) return(cl@jobj)
    if (!is.character(cl) || length(cl)!=1) {
        stop("invalid class name")
    }

    cl <- gsub("/", ".", as.character(cl))
    javaClass <- new.java.class(cl)
    cls <- new('jobjRef', jobj=javaClass, jclass='java.lang.Class', stringValue=paste0("class ", cl))
    .jcheck(silent)
    if (!silent && is.jnull(cls)) stop("class not found")
    cls
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

.ellipsisToJObj <- function(...) {
    lapply(list(...), function(x) .toJObj(x))
}

.toJObj <- function(x) {
    if (is(x, "jobjRef")) {
        x@jobj
    } else if (is(x, "jclassName")) {
        x@jobj@jobj
    } else if (is(x, "jbyte")) {
        as.external.byte(x)    
    } else if (is(x, "jchar")) {
        as.external.char(x)    
    } else if (is(x, "jfloat")) {
        as.external.float(x)
    } else if (is(x, "jlong")) {
        as.external.long(x)
    } else if (is(x, "jshort")) {
        as.external.short(x)        
    } else {
        x
    } 
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
    if (is.character(obj)) {
        return(obj)
    }    
    r <- NULL
    if (!is(obj, "jobjRef")) {
        stop("can get value of Java objects only")
    }
    if(!is.null(obj@stringValue)) {
        obj@stringValue
    } else {
        obj@jobj["toString"]()
    }
}

.isJavaArray <- function(o){
    is.external.array(o) && java.class(o) != NULL
}

#
# S4
#

setClass("truffle.object", representation(jobj="ANY"))
setClassUnion("TruffleObjectOrNull",members=c("truffle.object", "NULL"))
setClassUnion("characterOrNull",members=c("character", "NULL"))

#
# jobjRef
#
setClass("jobjRef", representation(jobj="TruffleObjectOrNull", jclass="character", stringValue="characterOrNull"), prototype=list(jobj=NULL, jclass="java/lang/Object", stringValue=NULL))

setMethod("$", c(x="jobjRef"), function(x, name) {
    if(name %in% names(x@jobj)) {        
        if(is.external.executable(x@jobj[name])) {
            function(...) { .jcall(x, , name, ...) }    
        } else {
            .jfield(x, , name)                
        }
    } else if( is.character(name) && length(name) == 1L && name == "length" && is.external.array(x) ) {
        length( x@obj )
    } else {
        stop(sprintf( "no field, method or inner class called '%s' ", name)) 
    }
})

setMethod("$<-", c(x="jobjRef"), function(x, name, value) {
    if(name %in% names(x@jobj)) {
        if(!is.external.executable(x@jobj[name])) {
            value <- .toJObj(value)
            x@jobj[name] <- value
        }
    }
    x
})

setMethod("show", c(object="jobjRef"), function(object) {
    if (is.jnull(object)) {
        show("Java-Object<null>") 
    } else {
        show(paste("Java-Object{", .jstrVal(object), "}", sep=''))
    }
    invisible(NULL)
})

#
# jclassName
#

setClass("jclassName", representation(name="character", jobj="jobjRef"))
setMethod("show", c(object="jclassName"), function(object) {
    invisible(show(paste("Java-Class-Name:", object@name)))
})
setMethod("as.character", c(x="jclassName"), function(x, ...) x@name)
setMethod("$", c(x="jclassName"), function(x, name) {
    if(name == "class") {
        x@jobj
    } 
    obj <- x@jobj@jobj
    if(name %in% names(obj)) {
        if(is.external.executable(obj[name])) {
            function(...) { .jcall(obj, , name, ...) }    
        } else {
            .jfield(obj, , name)
        }
    } else {
        stop("no static field, method or inner class called `", name, "' in `", x@name, "'")
    } 
})
setMethod("$<-", c(x="jclassName"), function(x, name, value) {
    value <- .toJObj(value)
    x@jobj@jobj[name] <- value
    x
})

# TODO makes CMD INSTALL complain
# setGeneric("new")
# setMethod("new", signature(Class="jclassName"), function(Class, ...) .jnew(Class, ...))

setClass("jfloat", representation("array"))
setClass("jlong", representation("array"))
setClass("jbyte", representation("array"))
setClass("jshort", representation("array"))
setClass("jchar", representation("array"))

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
