## This file is part of the rJava package - low-level R/Java interface
## (C)2006 Simon Urbanek <simon.urbanek@r-project.org>
## For license terms see DESCRIPTION and/or LICENSE
##
## $Id$

# create a new object
.jnew <- function(class, ..., check=TRUE, silent=!check) {
  class <- gsub("\\.", "/", as.character(class)) # allow non-JNI specifiation
  # TODO: should this do "S" > "java/lang/String", ... like .jcall
  
  if (check) .jcheck(silent=TRUE)
  o<-.External(RcreateObject, class, ..., silent=silent)
  if (check) .jcheck(silent=silent)
  if (is.null(o)) {
  	  if (!silent) {
    	  stop("Failed to create object of class `",class,"'")
      } else {
    	  o <- .jzeroRef
      }
  }
  new("jobjRef", jobj=o, jclass=class)
}

# create a new object reference manually (avoid! for backward compat only!) 
# the problem with this is that you need a valid `jobj' which 
# is implementation-dependent so it is undefined outside rJava internals
# it is now used by JRI.createRJavaRef, though
.jmkref <- function(jobj, jclass="java/lang/Object") {
  new("jobjRef", jobj=jobj, jclass=gsub('\\.','/',as.character(jclass)))
}

# evaluates an array reference. If rawJNIRefSignature is set, then obj is not assumed to be
# jarrayRef, but rather direct JNI reference with the corresponding signature
.jevalArray <- function(obj, rawJNIRefSignature=NULL, silent=FALSE, simplify=FALSE) {
  jobj<-obj
  sig<-rawJNIRefSignature
  if (is.null(rawJNIRefSignature)) {
    if(!inherits(obj,"jarrayRef")) {
      if (!inherits(obj,"jobjRef"))
        stop("object is not a Java object reference (jobjRef/jarrayRef).")
      cl <- gsub("\\.","/",.jclass(obj))
      if (is.null(cl) || !isJavaArraySignature(cl) )
        stop("object is not a Java array.")
      sig <- cl
    } else sig <- obj@jsig
    jobj<-obj@jobj
  } else if (is(obj, "jobjRef")) jobj<-obj@jobj
  if (sig=="[I")
    return(.Call(RgetIntArrayCont, jobj))
  else if (sig=="[J")
    return(.Call(RgetLongArrayCont, jobj))
  else if (sig=="[Z")
    return(.Call(RgetBoolArrayCont, jobj))
  else if (sig=="[B")
    return(.Call(RgetByteArrayCont, jobj))
  else if (sig=="[D")
    return(.Call(RgetDoubleArrayCont, jobj))
  else if (sig=="[S")
    return(.Call(RgetShortArrayCont, jobj))
  else if (sig=="[C")
    return(.Call(RgetCharArrayCont, jobj))
  else if (sig=="[F")
    return(.Call(RgetFloatArrayCont, jobj))
  else if (sig=="[Ljava/lang/String;")
    return(.Call(RgetStringArrayCont, jobj))
  else if (sig=="[Ljava/lang/Double;" && simplify) {
    obj@jclass <- sig; return(.jcall("RJavaArrayTools", "[D", "unboxDoubles", obj)) }
  else if (sig=="[Ljava/lang/Integer;" && simplify) {
    obj@jclass <- sig; return(.jcall("RJavaArrayTools", "[I", "unboxIntegers", obj)) }
  else if (sig=="[Ljava/lang/Boolean;" && simplify) {
    obj@jclass <- sig; return(as.logical(.jcall("RJavaArrayTools", "[I", "unboxBooleans", obj))) }
  else if (substr(sig,1,2)=="[L")
    return(lapply(.Call(RgetObjectArrayCont, jobj),
                  function(x) new("jobjRef", jobj=x, jclass=substr(sig, 3, nchar(sig)-1)) ))
  else if (substr(sig,1,2)=="[[") {
    if (simplify) { # try to figure out if this is a rectangular array in which case we can do better
      o <- newArray(simplify=TRUE, jobj=jobj, signature=sig)
      # if o is not a reference then we were able to simplify it
      if (!is(o, "jobjRef")) return(o)
    }
    # otherwise simplify has no effect
    return(lapply(.Call(RgetObjectArrayCont, jobj),
                  function(x) newArray(jobj=x, signature=substr(sig, 2, 999), simplify=simplify)))
  }
  # if we don't know how to evaluate this, issue a warning and return the jarrayRef
  if (!silent)
    warning(paste("I don't know how to evaluate an array with signature",sig,". Returning a reference."))
  newArray(jobj = jobj, signature = sig, simplify = FALSE)
}

.jcall <- function(obj, returnSig="V", method, ..., evalArray=TRUE, 
	evalString=TRUE, check=TRUE, interface="RcallMethod", 
	simplify=FALSE, use.true.class = FALSE) {
  if (check) .jcheck()
  iaddr <- .env[[interface]]
  interface <- if (is.null(iaddr)) getNativeSymbolInfo(interface, "rJava", TRUE, FALSE)$address else iaddr
  r<-NULL
  # S is a shortcut for Ljava/lang/String;
  if (returnSig=="S")
    returnSig<-"Ljava/lang/String;"
  if (returnSig=="[S")
    returnSig<-"[Ljava/lang/String;"
  # original S (short) is now mapped to T so we need to re-map it (we don't really support short, though)
  if (returnSig=="T") returnSig <- "S"
  if (returnSig=="[T") returnSig <- "[S"
  
  if (inherits(obj,"jobjRef") || inherits(obj,"jarrayRef") || inherits(obj,"jrectRef") )
    r<-.External(interface, obj@jobj, returnSig, method, ...)
  else
    r<-.External(interface, as.character(obj), returnSig, method, ...)
  if (returnSig=="V") return(invisible(NULL))
  
  if( use.true.class && !is.null( r ) ){
  	  if( ! ( isPrimitiveTypeName(returnSig) || isArraySignature(returnSig) ) ){
  	  	  # avoid calling .jcall since we work on external pointers directly here
  	  	  clazz     <- .External(interface, r    , "Ljava/lang/Class;", "getClass")
  	  	  clazzname <- .External(interface, clazz, "Ljava/lang/String;", "getName")
  	  	  clazzname <- .External(RgetStringValue, clazzname)
  	  	  returnSig <- tojniSignature( clazzname ) 
  	  }
  }
  
  if (isJavaArraySignature(returnSig)) {
      # eval or return a reference
      r <- if (evalArray) .jevalArray(r, rawJNIRefSignature=returnSig, simplify=simplify) else newArray(jobj = r, signature = returnSig, simplify = FALSE)
  } else if ( substr(returnSig,1,1)=="L") {
  	  if (is.null(r)){
  	  	  if( check ) .jcheck( silent = FALSE )
  	  	  return(r)
  	  }
    
  	if (returnSig=="Ljava/lang/String;" && evalString){
      if( check ) .jcheck( silent = FALSE )
  	  return(.External(RgetStringValue, r))
    }
    r <- new("jobjRef", jobj=r, jclass=substr(returnSig,2,nchar(returnSig)-1))
  }
  if (check) .jcheck()
  if (.conv.in$.) .convert.in(r) else r
}

.jstrVal <- function(obj) {
  # .jstrVal(.jstrVal(...)) = .jstrVal(...)
  if (is.character(obj))
    return(obj)
  r<-NULL
  if (!is(obj,"jobjRef"))
    stop("can get value of Java objects only")
  if (!is.null(obj@jclass) && obj@jclass=="lang/java/String")
    r<-.External(RgetStringValue, obj@jobj)
  else
    r<-.External(RtoString, obj@jobj)
  r
}

#' casts java object into new.class
#' 
#' @param obj a java object reference
#' @param new.class the new class (in JNI or Java)
#' @param check logical. If TRUE the cast if checked
#' @param convert.array logical. If TRUE and the new class represents an array, then a jarrayRef object is made
.jcast <- function(obj, new.class="java/lang/Object", check = FALSE, convert.array = FALSE) {
  if (!is(obj,"jobjRef"))
    stop("cannot cast anything but Java objects")
  if( check && !.jinstanceof( obj, new.class) ){
  	  stop( sprintf( "cannot cast object to '%s'", new.class ) ) 
  }
  
  new.class <- gsub("\\.","/", as.character(new.class)) # allow non-JNI specifiation
  if( convert.array && !is( obj, "jarrayRef" ) && isJavaArray( obj ) ){
  	 r <- .jcastToArray( obj, signature = new.class)
  } else {
  	 r <- obj
  	 r@jclass <- new.class
  }
  r
}

# makes sure that a given object is jarrayRef 
.jcastToArray <- function(obj, signature=NULL, class="", quiet=FALSE) {
  if (!is(obj, "jobjRef"))
    return(.jarray(obj))
  if (is.null(signature)) {
  	  # TODO: factor out these two calls into a separate function
    cl <- .jcall(obj, "Ljava/lang/Class;", "getClass")
    cn <- .jcall(cl, "Ljava/lang/String;", "getName")
    if ( !isJavaArraySignature(cn) ) {
      if (quiet)
        return(obj)
      else
        stop("cannot cast to array, object signature is unknown and class name is not an array")
    }
    signature <- cn
  } else{
  	  if( !isJavaArraySignature(signature) ){
  	  	  if( quiet ) {
  	  	  	  return( obj )
  	  	  } else{
  	  	  	  stop( "cannot cast to array, signature is not an array signature" )
  	  	  }
  	  }
  }
  signature <- gsub('\\.', '/', signature)
  if (inherits(obj, "jarrayRef")) {
    obj@jsig <- signature
    return(obj)
  }
  newArray(obj, simplify=FALSE)
}

# creates a new "null" object of the specified class
# although it sounds weird, the class is important when passed as
# a parameter (you can even cast the result)
.jnull <- function(class="java/lang/Object") { 
  new("jobjRef", jobj=.jzeroRef, jclass=as.character(class))
}

.jcheck <- function(silent=FALSE) invisible(.Call(RJavaCheckExceptions, silent))

.jproperty <- function(key) {
  if (length(key)>1)
    sapply(key, .jproperty)
  else
    .jcall("java/lang/System", "S", "getProperty", as.character(key)[1])
}

#' gets the dim of an array, or its length if it is just a vector
getDim <- function(x){
	dim <- dim(x)
	if( is.null( dim ) ) dim <- length(x)
	dim
}

.jarray <- function(x, contents.class = NULL, dispatch = FALSE) {
	# this already is an array, so don't bother
	if( isJavaArray( x ) ) return( newArray( x, simplify = FALSE) ) 
	
	# this is a two stage process, first we need to convert into 
	# a flat array using the jni code
	# TODO: but this needs to move to the internal jni world to avoid 
	#       too many copies
	
	# common mistake is to not specify a list but just a single Java object
	# but, well, people just keep doing it so we may as well support it 
	dim <- if (inherits(x,"jobjRef")) {
		x <- list(x)
		1L
	} else getDim(x)

	# the jni call
	array <- .Call(RcreateArray, x, contents.class)
	
	if (!dispatch) return( array )
	
	if( is.list( x ) ){
		# if the input of RcreateArray was a list, we need some more care
		# because we cannot be sure the array is rectangular so we have to 
		# check it 
		newArray( array, simplify = FALSE )
	} else {
	
		# then we transform this to a rectangular array of the proper dimensions
		if( length( dim ) == 1L ) {
			# single dimension array
			new( "jrectRef", jobj = array@jobj, jsig = array@jsig, 
					jclass = array@jclass, dimension = dim )
		} else {
			builder <- .jnew( "RectangularArrayBuilder", .jcast(array), dim )
			clazz <- .jcall( builder, "Ljava/lang/String;", "getArrayClassName" )
			
			# we cannot use .jcall here since it will try to simplify the array
			# or go back to java to calculate its dimensions, ...
			r <- .External( "RcallMethod", builder@jobj, 
				"Ljava/lang/Object;", "getArray", PACKAGE="rJava")
			
			new( "jrectRef", jobj = r, dimension = dim, 
				jclass = clazz, jsig = tojni( clazz ) ) 
		}  
	}
}

# works on EXTPTR or jobjRef or NULL. NULL is always silently converted to .jzeroRef
.jidenticalRef <- function(a,b) {        
  if (is(a,"jobjRef")) a<-a@jobj
  if (is(b,"jobjRef")) b<-b@jobj
  if (is.null(a)) a <- .jzeroRef
  if (is.null(b)) b <- .jzeroRef
  if (!inherits(a,"externalptr") || !inherits(b,"externalptr")) stop("Invalid argument to .jidenticalRef, must be a pointer or jobjRef")
  .Call(RidenticalRef,a,b)
}

# returns TRUE only for NULL or jobjRef with jobj=0x0
is.jnull <- function(x) {
  (is.null(x) || (is(x,"jobjRef") && .jidenticalRef(x@jobj,.jzeroRef)))
}

# should we move this to C?
.jclassRef <- function(x, silent=FALSE) {
  if (is.jnull(x)) {
    if (silent) return(NULL) else stop("null reference has no class")
  }
  if (!is(x, "jobjRef")) {
    if (silent) return(NULL) else stop("invalid object")
  }
  cl <- NULL
  try(cl <- .jcall(x, "Ljava/lang/Class;", "getClass", check=FALSE))
  .jcheck(silent=TRUE)
  if (is.jnull(cl) && !silent) stop("cannot get class object")
  cl
}

# return class object for a given class name; silent determines whether
# an error should be thrown on failure (FALSE) or just null reference (TRUE)
.jfindClass <- function(cl, silent=FALSE) {
  if (inherits(cl, "jclassName")) return(cl@jobj)
  if (!is.character(cl) || length(cl)!=1)
    stop("invalid class name")
  cl<-gsub("/",".",cl)
  a <- NULL
  if (!is.jnull(.rJava.class.loader))
    try(a <- .jcall("java/lang/Class","Ljava/lang/Class;","forName",cl,TRUE,.jcast(.rJava.class.loader,"java.lang.ClassLoader"), check=FALSE))
  else
    try(a <- .jcall("java/lang/Class","Ljava/lang/Class;","forName",cl,check=FALSE))
  # this is really .jcheck but we don't want it to appear on the call stack
  .C(RJavaCheckExceptions, silent, FALSE, PACKAGE = "rJava")
  if (!silent && is.jnull(a)) stop("class not found")
  a
}

# Java-side inheritance check; NULL inherits from any class, because 
# it can be cast to any class type; cl can be a class name or a jobjRef to a class object
.jinherits <- function(o, cl) {
  if (is.jnull(o)) return(TRUE)
  if (!is(o, "jobjRef")) stop("invalid object")
  if (is.character(cl)) cl <- .jfindClass(cl) else if (inherits(cl, "jclassName")) cl <- cl@jobj
  if (!is(cl, "jobjRef")) stop("invalid class object")  
  ocl <- .jclassRef(o)
  .Call(RisAssignableFrom, ocl@jobj, cl@jobj)
}

# compares two things which may be Java objects. invokes Object.equals if applicable and thus even different pointers can be equal. if one parameter is not Java object, but scalar string/int/number/boolean then a corresponding Java object is created for comparison
# strict comparison returns FALSE if Java-reference is compared with non-reference. otherwise conversion into Java scalar object is attempted
.jequals <- function(a, b, strict=FALSE) {
  if (is.null(a)) a <- new("jobjRef")
  if (is.null(b)) b <- new("jobjRef")
  if (is(a,"jobjRef")) o <- a else
    if (is(b,"jobjRef")) { o <- b; b <- a } else
    return(all.equal(a,b))
  if (!is(b,"jobjRef")) {
    if (strict) return(FALSE)
    if (length(b)!=1) { warning("comparison of non-scalar values is always FALSE"); return(FALSE) }
    if (is.character(b)) b <- .jnew("java/lang/String",b) else
    if (is.integer(b)) b <- .jnew("java/lang/Integer",b) else
    if (is.numeric(b)) b <- .jnew("java/lang/Double",b) else
    if (is.logical(b)) b <- .jnew("java/lang/Boolean", b) else
    { warning("comparison of non-trivial values to Java objects is always FALSE"); return(FALSE) }
  }
  if (is.jnull(a))
    is.jnull(b)
  else
    .jcall(o, "Z", "equals", .jcast(b, "java/lang/Object"))
}

.jfield <- function(o, sig=NULL, name, true.class=is.null(sig), convert=TRUE) {
  if (length(sig)) {
    if (sig=='S') sig<-"Ljava/lang/String;"
    if (sig=='T') sig<-"S"
    if (sig=='[S') sig<-"[Ljava/lang/String;"
    if (sig=='[T') sig<-"[S"
  }
  r <- .Call(RgetField, o, sig, as.character(name), as.integer(true.class))
  if (inherits(r, "jobjRef")) {
    if (isJavaArraySignature(r@jclass)) {
    	r <- if (convert) .jevalArray(r, rawJNIRefSignature=r@jclass, simplify=TRUE) else newArray(r, simplify=FALSE)
    }
    if (convert && inherits(r, "jobjRef")) {
      if (r@jclass == "java/lang/String")
        return(.External(RgetStringValue, r@jobj))
      if (.conv.in$.) return(.convert.in(r))
    }
  }
  r
}

".jfield<-" <- function(o, name, value)
  .Call(RsetField, o, name, value)

