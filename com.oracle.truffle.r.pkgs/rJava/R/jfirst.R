# this part is common to all platforms and must be invoked
# from .First.lib after library.dynam

# actual namespace environment of this package
.env <- environment()

# variables in the rJava environment that will be initialized *after* the package is loaded
# they need to be pre-created at load time and populated later by .jinit
.delayed.export.variables <- c(".jniInitialized", ".jclassObject", ".jclassString", ".jclassClass",
                               ".jclass.int", ".jclass.double", ".jclass.float", ".jclass.boolean",
                               ".jclass.void", ".jinit.merge.error")
# variables that are delayed but not exported are added here
.delayed.variables <- c(.delayed.export.variables, ".rJava.class.loader")

.jfirst <- function(libname, pkgname) {
  # FASTR - no more C entry points
  .registerFastrFunctions()
  # register all C entry points
  # addr <- getNativeSymbolInfo(.register.addr, pkgname)
  # for (name in .register.addr)
  #    .env[[name]] <- addr[[name]]$address
  
  # previously set in C
  assign(".rJava_initialized", FALSE, .env)  
  
  assign(".rJava.base.path", paste(libname, pkgname, sep=.Platform$file.sep), .env)
  assign(".jzeroRef", .createZeroRef(), .env)

  for (x in .delayed.variables) assign(x, NULL, .env)
  assign(".jniInitialized", FALSE, .env)

  # FASTR - no JVM params
  # # default JVM initialization parameters
  # if (is.null(getOption("java.parameters")))
  #   options("java.parameters"="-Xmx512m")

  # assign(".rJava.debug", T, .env)  

  ## S4 classes update - all classes are created earlier in classes.R, but jobjRef's prototype is only valid after the dylib is loaded
  setClass("jobjRef", representation(jobj="externalptr", jclass="character"), prototype=list(jobj=.jzeroRef, jclass="java/lang/Object"), where=.env, validity=.jobjRef.validity)
}

# FASTR <<<<<

.registerFastrFunctions <- function() {
  # .C
  .fastr.register.functions("rJava", .env, 0, 
        list(RJavaCheckExceptions = .RJavaCheckExceptions,
             RclearException=.RclearException))
  # .External  
  .fastr.register.functions("rJava", .env, 3, 
        list(RcreateObject = .RcreateObject, 
             RgetStringValue = .RgetStringValue, 
             RinitJVM = .RinitJVM, 
             RtoString = .RtoString, 
             RcallMethod = .RcallMethod))
  # .Call
  .fastr.register.functions("rJava", .env, 1, 
        list(RpollException = .RpollException,
            RthrowException = .RthrowException,                                               
            RidenticalRef = .RidenticalRef,
            RisAssignableFrom = .RisAssignableFrom,
            RJava_checkJVM = .RJava_checkJVM,
            RJava_needs_init = .RJava_needs_init,
            initRJavaTools = .initRJavaTools,            
            RJava_set_memprof = .RJava_set_memprof,
            RgetStringArrayCont = .RgetStringArrayCont,
            RgetIntArrayCont = .RgetIntArrayCont,                 
            RgetBoolArrayCont = .RgetBoolArrayCont,
            RgetCharArrayCont = .RgetCharArrayCont,
            RgetShortArrayCont = .RgetShortArrayCont,
            RgetByteArrayCont = .RgetByteArrayCont,
            RgetDoubleArrayCont = .RgetDoubleArrayCont,
            RgetFloatArrayCont = .RgetFloatArrayCont,
            RgetLongArrayCont = .RgetLongArrayCont,
            RgetObjectArrayCont = .RgetObjectArrayCont,
            RcreateArray = .RcreateArray,
            RgetField = .RgetField,
            RsetField = .RsetField))
}

.createZeroRef <- function() {
    zr <- methods:::.newExternalptr()    
    attr(zr, ".rJava.zeroRef") <- T
    zr
}

.jobjRef.validity <- function(object) { 
    object # force args

    o <- attr(object@jobj, "external.object", exact=TRUE)
    if(is.null(o)) {
        zr <- attr(object@jobj, ".rJava.zeroRef", exact=TRUE)
        if(is.null(zr)) {
            return(FALSE)
        }
    } else if(!is.external(o)) {
        # truffle unboxes first and fastr then converts primitive types into equivalent R values
        # and the external.object attr in such a case isn't a truffle object 
        # in e.g. .jnew and .jcall rJava knows and stores the return value class name in the jclass slot,
        # but it is only the externalptr, that is passed into functions like RcallMethod 
        # and we might need the class name for calls like getClass or isInstance
        attr(object@jobj, "external.classname") <- object@jclass
    }
    return(TRUE)
}

# TODO issues:
# - bytes converted to int
# - chars converted to int


.RJavaCheckExceptions <- function(silent) {
    silent # force args

    .fastr.interop.checkException(silent, ".jcheck")
}

.RpollException <- function() {    
    e <- .fastr.interop.getTryException(FALSE)
    if(is.null(e)) {
        return(NULL)        
    }
    .fromJ(e)
}

.RclearException <- function() {    
    .fastr.interop.clearTryException()
}

.RthrowException <- function(silent) {    
    # TODO do we need this? not used directly from rJava code
}

.RidenticalRef <- function(x1, x2) {
    x1; x2 # force args

    if(!inherits(x1, "externalptr") || !inherits(x2, "externalptr")) {
        return(NULL)
    }
    o1 <- attr(x1, "external.object", exact=TRUE)
    o2 <- attr(x2, "external.object", exact=TRUE)
    if(!(is.external(o1) && is.external(o2))) {
        return(identical(o1, o2))
    }
    if(!is.external(o1) || !is.external(o2)) {
        return(NULL)
    }
    .fastr.interop.isIdentical(o1, o2)
}

.RisAssignableFrom <- function(cl1, cl2) {
    cl1; cl2 # force args

    if(!inherits(cl1, "externalptr") || !inherits(cl2, "externalptr")) {
        stop("invalid type")
    }
    # should be already ensured that both args are class extpointer-s
    .fastr.interop.isAssignableFrom(attr(cl1, "external.object", exact=TRUE), attr(cl2, "external.object", exact=TRUE))  
}

.RcallMethod <- function(obj, returnSig, method, ...) {
    obj; returnSig; method # force args

    if(is.null(obj)) {
        stop("RcallMethod: call on a NULL object")
    }

    o <- NULL
    clnam <- NULL
    if(inherits(obj, "externalptr")) {
        o <- attr(obj, "external.object", exact=TRUE)
    } else if(is.character(obj) && length(obj) == 1) {
        clnam <- obj
    } else {
        stop("RcallMethod: invalid object parameter")
    }
    if(is.null(o) && is.null(clnam)) {
        stop("RcallMethod: attempt to call a method of a NULL object.")
    }
  
    # <<<<<< j.l.Class HACKs <<<<<<
    # truffle provides no access to j.l.Class methods
    if (method == "forName") {
        if (!is.null(clnam) && clnam %in% c("java/lang/Class", "java.lang.Class")) {
            res <- .fastr.interop.try(function() { new.java.class(list(...)[[1]]) }, FALSE)            
            return(.fromJ(res))
        }
    } else if (method == "getName" && !is.null(o)) {
        if (java.class(o) %in% c("java/lang/Class", "java.lang.Class")) {
            res <- java.class(o, T)
            return(.fromJ(res))
        }           
    } else if (method == "isInstance") {
        if (!is.external(o)) {
            o <- new.java.class(attr(obj, "external.classname", exact=TRUE))
        }
        o2 <- .toJ(list(...)[[1]])
        res <- .fastr.interop.isInstance(o, o2)
        return(.fromJ(res))        
    } else if (method == "getClass") {
        if(is.external(o)) {            
            res <- .fastr.interop.getJavaClass(o)
            return(.fromJ(res))
        } else {
            extClName <- attr(obj, "external.classname", exact=TRUE)
            if(!is.null(extClName)) {
                res <- new.java.class(extClName)  
                return(.fromJ(res))
            }                        
        }
    } 
    # >>>>>> j.l.Class HACKs >>>>>>
    
    if (!is.null(o) && !is.external(o)) {        
        o <- .asTruffleObject(o, attr(obj, "external.classname", exact=TRUE))
    } 

    if(!is.character(returnSig) || length(returnSig) != 1) {
        stop("RcallMethod: invalid return signature parameter")
    }

    if(!is.character(method) || length(method) != 1) {
        stop("RcallMethod: invalid method name")
    }

    # if((!is.null(o) && !(method %in% names(o))) ||        
    #    (!is.null(cls) && !(method %in% names(cls))) ) {
    #     stop(paste0("method ", method, " with signature ", returnSig, " not found"))
    # }

    if(is.null(o)) { 
        cls <- NULL
        if (!is.null(clnam)) {
            cls <- .fastr.interop.try(function() { new.java.class(clnam) }, FALSE)
        } 
        if (is.null(cls)) {
            stop("RcallMethod: cannot determine object class")
        }

        extMethod <- function(...) cls[method](...)
    } else {
        extMethod <- function(...) o[method](...)
    }

    args <- .ellipsisToJ(...)
    res <- .fastr.interop.try(function() { do.call(extMethod, args) }, FALSE)
    if(is.null(res)) {
        return(NULL)
    }

    if(substr(returnSig, 1, 1) %in% c("L", "[")) {        
        .fromJ(res, toExtPointer=TRUE)
    } else {
        .fromJ(res, toExtPointer=FALSE)    
    }
}

.RgetField <- function(obj, sig, name, trueclass) {
    obj; sig; name; trueclass # force args

    if(is.null(obj)) {
        return(NULL)
    }

    if (!is.character(name) || length(name) != 1) {
        stop("invalid field name")
    }

    if (!is.null(sig) && (!is.character(sig) || length(sig) != 1)) {
      stop("invalid signature parameter")
    }
    
    if(.IS_JOBJREF(obj)) {
        obj <- obj@jobj
    }
    clnam <- NULL
    externalClassName <- NULL
    o <- NULL
    if(inherits(obj, "externalptr")) {
        o <- .toJ(obj)
        externalClassName <- attr(obj, "external.classname", exact=TRUE)
    } else if(is.character(obj) && length(obj) == 1) {
        clnam <- obj
    } else {
        stop("invalid object parameter")
    }
    if (is.null(o) && is.null(clnam)) {
        stop("cannot access a field of a NULL object")
    }       
        
    if(!is.null(o)) {
        if(!is.external(o)) {
            o <- .asTruffleObject(o, externalClassName)
        }
        res <- o[name]
    } else {
        cls <- new.java.class(clnam)
        if (is.null(cls)) {
            stop("cannot determine object class")
        }
        res <- cls[name]
    }    
    if(is.null(res)) {
        return(.jnull())
    }
    if(!is.external(res)) {
        # TODO there are cases when the passed signature is NULL - e.g. rJavaObject$someField 
        # with truffle we have no way to defferenciate if the unboxed return value relates to an Object or primitive field
        # but the original field.c RgetField implementation checks the return type and 
        # if field not primitive an "jobjRef" S4 object is returned.
        # rjava:
        # > rJavaObject$fieldIntegerObject
        # [1] "Java-Object{2147483647}"
        # vs fastr:
        # > rJavaObject$fieldIntegerObject
        # [1] 2147483647
        # Note that his is not the case in rjava with method calls:
        # > rJavaObject$methodIntegerObject()
        # [1] 2147483647
        return(res)
    }
    
    # as opposed to RcallMethod, we have to return a S4 objects at this place
    if(trueclass) {
        clsname <- java.class(res)
    } else {
        clsname <- .signatureToClassName(sig)
    }
    res <- .fromJ(res)
    return(new("jobjRef", jobj=res, jclass=clsname))
}

.RsetField <- function(ref, name, value) {
    ref; name; value # force args

    obj <- ref
    if (is.null(obj)) {
        stop("cannot set a field of a NULL object")
    }

    if (!is.character(name) && length(name) != 1) {
        stop("invalid field name")
    }
    
    if(.IS_JOBJREF(obj)) {
        obj <- obj@jobj
    }
    clnam <- NULL
    o <- NULL
    if(inherits(obj, "externalptr")) {
        o <- .toJ(obj)
    } else if(is.character(obj) && length(obj) == 1) {
        clnam <- obj
    } else {
        stop("invalid object parameter")
    }
    if (is.null(o) && is.null(clnam)) {
        stop("cannot set a field of a NULL object")
    }

    value <- .toJ(value)
    if(!is.null(o)) {
        o[name] <- value
    } else {
        cls <- new.java.class(clnam)
        cls[name] <- value
    }
    ref
}    

.RcreateObject <-function(class, ..., silent) {
    class; silent; # force args

    if(!is.character(class) || length(class) != 1) {
        stop("RcreateObject: invalid class name")
    }

    co <- .fastr.interop.try(function() { new.java.class(class, silent) }, FALSE)
    if(is.null(co)) {
        return(NULL)
    }
    args <- .ellipsisToJ(co, ...)
    res <- .fastr.interop.try(function() { do.call(new.external, args) }, FALSE)

    # create an external pointer even for java.lang.String & co    
    .fromJ(res, toExtPointer=TRUE)
}

.RgetStringArrayCont <- function(obj) {
    obj # force args

    .getArrayCont(obj, as.character)
}

.RgetIntArrayCont <- function(obj) {
    obj # force args

    .getArrayCont(obj, as.integer)
}

.RgetBoolArrayCont <- function(obj) {
    obj # force args

    .getArrayCont(obj, as.logical)
}

.RgetCharArrayCont <- function(obj) {
    obj # force args

    # TODO as.integer might cause trouble?
    .getArrayCont(obj, as.integer)
}

.RgetShortArrayCont <- function(obj) {
    obj # force args

    .getArrayCont(obj, as.integer)
}

.RgetByteArrayCont <- function(obj) {
    obj # force args

    .getArrayCont(obj, as.raw)
}

.RgetDoubleArrayCont <- function(obj) {
    obj # force args

    .getArrayCont(obj, as.double)
}

.RgetFloatArrayCont <- function(obj) {
    obj # force args

    .getArrayCont(obj, as.double)
}

.RgetLongArrayCont <- function(obj) {
    obj # force args

    .getArrayCont(obj, as.double)
}

.RgetObjectArrayCont <- function(obj) {
    obj # force args

    if (is.null(obj)) {
        return(obj)
    }

    if(!(inherits(obj, "externalptr"))) {
        error("invalid object parameter")
    }
    obj <- attr(obj, "external.object", exact=TRUE)
    # we rely on this being called only if obj is an external array
    obj <- .fastr.interop.fromArray(obj)
    lapply(obj, function(e) {.fromJ(e, toExtPointer=TRUE)})
}

.getArrayCont <- function(obj, toVectorFun) {
    obj; toVectorFun # force args

    if(is.null(obj)) {
        return(obj)
    } else if(!(inherits(obj, "externalptr"))) {
        stop("invalid object parameter")
    }
    obj <- attr(obj, "external.object", exact=TRUE)
    if(is.null(obj)) {
        stop("invalid object parameter") 
    }

    if(missing(toVectorFun)) {
        res <- as.vector(obj)
    } else {
       res <- toVectorFun(obj)    
    }
    res
}

.RcreateArray <- function(ar, cl) {
    ar; cl # force args

    if(is.null(ar)) {
        return(NULL)
    }

    type <- typeof(ar)
    if(type %in% c("integer", "double", "character", "logical", "raw")) {
        ar <- .vectorToJArray(ar)
        sig <- java.class(ar)
        return(new("jarrayRef", jobj=.fromJ(ar), jclass=sig, jsig=sig))
    } else if(is.list(ar)) {

        lapply(ar, function(e) {
            if(!is.null(e) && !.IS_JOBJREF(e)) {
                stop("Cannot create a Java array from a list that contains anything other than Java object references.")
            }
        })
        
        if(length(cl) > 0) {
            clsName <- cl[1]            
        } else {
            clsName <- "java.lang.Object"    
        }
        ar <- .listToJ(ar)
        ar <- as.java.array(ar, clsName)
        sig <- java.class(ar)
        return(new("jarrayRef", jobj=.fromJ(ar), jclass=sig, jsig=sig))
    } 
    stop("Unsupported type to create Java array from.")
}

.RtoString <- function(obj) {
    obj # force args

    if(is.null(obj)) {
        return(obj)
    }
    
    if(!inherits(obj, "externalptr")) {
        stop("RtoString: invalid object parameter")
    }
    
    if(!is.null(attr(obj, "external.object", exact=TRUE))) {
        obj <- .toJ(obj)
        if(is.external(obj)) {
            if(java.class(obj) == "java.lang.Class") {
                res <- paste0("class ", java.class(obj, T))
            } else {
                res <- obj["toString"]()
            }        
        } else {
            res <- obj
        }
    } else {
        stop("RtoString: invalid object parameter")        
    } 
    res
}


.RgetStringValue <- function(obj) {
    obj # force args

    if (inherits(obj, "externalptr")) {
        attr(obj, "external.object", exact=TRUE)
    } else {
        obj
    }
}

.RinitJVM <- function(...) {    
    .rJava_initialized <- TRUE
}

.RJava_needs_init <- function(...) {
    !.rJava_initialized
}

.RJava_set_memprof <- function(...) {
    stop("memory profiling not enabled")
}        

.RJava_checkJVM <- function(...) {
    # do nothing
}

.initRJavaTools <- function(...) {
    # do nothing
}

.fromJ <- function(x, toExtPointer=FALSE) {
    x; toExtPointer # force args

    if(is.null(x)) {
        return(.jzeroRef)
    } else {        
        if(toExtPointer || is.external(x)) {
            ep <- methods:::.newExternalptr() 
            attr(ep, "external.object") <- x            
            ep
        } else {
            x
        }    
    }
}

.ellipsisToJ <- function(...) {
    lapply(list(...), function(x) .toJ(x))
}

.listToJ <- function(l) {
    l # force args

    lapply(l, function(x) .toJ(x))
}

.toJ <- function(x) {
    x # force args

    if (is(x, "jobjRef")) {
        x <- x@jobj
    } else if (is(x, "jclassName")) {
        x <- x@jobj@jobj
    }
    if(inherits(x, "externalptr")) {
        if(.jidenticalRef(x, .jzeroRef)) {
            x <- NULL
        } else {
            xo <- attr(x, "external.object", exact=TRUE)
            if (is.null(xo)) {
                stop(paste0("missing 'external' attribute on: ", x))
            }
            if (is.external(xo)) {
                return(xo)
            } else {                
                return(.asTruffleObject(xo, attr(x, "external.classname", exact=TRUE)))
            }
        }
    }
    if(length(x) > 1) {
        .vectorToJArray(x)
    } else {
        if (inherits(x, "jbyte")) {
            x <- as.external.byte(x)    
        } else if (inherits(x, "jchar")) {
            x <- as.external.char(x)
        } else if (inherits(x, "jfloat")) {
            x <- as.external.float(x)
        } else if (inherits(x, "jlong")) {
            x <- as.external.long(x)
        } else if (inherits(x, "jshort")) {
            x <- as.external.short(x)        
        }
        x
    }
}

.vectorToJArray <- function(x) {
    x # force args

    switch(class(x),
        "jbyte" = as.java.array(x, "byte"),
        "jchar" = as.java.array(x, "char"),
        "jfloat" = as.java.array(x, "float"),
        "jlong" = as.java.array(x, "long"),
        "jshort" = as.java.array(x, "short"),
        as.java.array(x)
    )
}

.asTruffleObject <- function(x, className=NULL) {
    x; className # force args

    if(!is.null(className)) {
        x <- switch(gsub("/", ".", className),
            "java.lang.Byte" = as.external.byte(x),
            "java.lang.Character" = as.external.char(x),
            "java.lang.Float" = as.external.float(x),
            "java.lang.Long" = as.external.long(x),
            "java.lang.Short" = as.external.short(x),        
            x
        )        
    }
    .fastr.interop.asJavaTruffleObject(x)
}

.signatureToClassName <- function(sig) {
    sig # force args

    if(startsWith(sig, "L")) {
        if(endsWith(sig, ";")) {
            substr(sig, 2, nchar(sig) - 1)
        } else {
            substr(sig, 2, nchar(sig))
        }    
    } else {
        sig
    }
}

.IS_JOBJREF <- function(obj) {
    obj # force args

    inherits(obj, "jobjRef") || inherits(obj, "jarrayRef") || inherits(obj,"jrectRef")
}

.IS_JARRAYREF <- function(obj) { 
    obj # force args

    inherits(obj, "jobjRef") || inherits(obj, "jarrayRef") || inherits(obj, "jrectRef") 
}

.IS_JRECTREF <- function(obj) { 
    obj # force args

    inherits(obj,"jrectRef")
}
