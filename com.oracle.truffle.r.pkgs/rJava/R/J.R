setClass("jclassName", representation(name="character", jobj="jobjRef"))
jclassName <- function(class){
	if( is( class, "jobjRef" ) && .jinherits(class, "java/lang/Class" ) ){
		jobj <- class
		name <- .jcall( class, "Ljava/lang/String;", "getName", evalString = TRUE )
	} else{
		name <- gsub("/",".",as.character(class))
		jobj <- .jfindClass(as.character(class))
	}
	new("jclassName", name=name, jobj=jobj)
}

setGeneric("new")
setMethod("new", signature(Class="jclassName"), function(Class, ...) .J(Class@name, ...))

setMethod("$", c(x="jclassName"), function(x, name) {
	if( name == "class" ){
		x@jobj
	} else if (classHasField(x@jobj, name, TRUE)){
		.jfield(x@name, , name) 
	} else if (classHasMethod(x@jobj, name, TRUE)){
		function(...) .jrcall(x@name, name, ...) 
	} else if( classHasClass(x@jobj, name, FALSE) ){
		inner.cl <- .jcall( "RJavaTools", "Ljava/lang/Class;", "getClass", x@jobj, name, FALSE ) 
		new("jclassName", name=.jcall(inner.cl, "S", "getName"), jobj=inner.cl)
	} else {
		stop("no static field, method or inner class called `", name, "' in `", x@name, "'")
	}
})
setMethod("$<-", c(x="jclassName"), function(x, name, value) .jfield(x@name, name) <- value)
setMethod("show", c(object="jclassName"), function(object) invisible(show(paste("Java-Class-Name:",object@name))))
setMethod("as.character", c(x="jclassName"), function(x, ...) x@name)

## the magic `J'
J<-function(class, method, ...) if (nargs() == 1L && missing(method)) jclassName(class) else .jrcall(class, method, ...)
