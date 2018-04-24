# :tabSize=4:indentSize=4:noTabs=false:folding=explicit:collapseFolds=1:

# S4 dispatch does not work for .DollarNames, so we'll use S3
# {{{ bring .DollarNames from the future if necessary
if( !exists( ".DollarNames", envir = asNamespace("utils") ) ){
	.DollarNames <- function(x, pattern)
    	UseMethod(".DollarNames")
} 
# }}}

# {{{ support function to retrieve completion names from RJavaTools
### get completion names from RJavaTools
classNamesMethod <- function (cl, static.only = TRUE ) {
	# TODO: return both from java instead of two java calls
  fieldnames <- .jcall( "RJavaTools", "[Ljava/lang/String;", 
  	"getFieldNames", cl, static.only ) 
  methodnames <- .jcall( "RJavaTools", "[Ljava/lang/String;", 
  	"getMethodNames", cl, static.only )
  c(fieldnames, methodnames)
}
# }}}

# {{{ jclassName
._names_jclassName <- function(x){
	c( "class", classNamesMethod(x@jobj, static.only = TRUE ) )
}
.DollarNames.jclassName <- function(x, pattern = "" ){
	grep( pattern, ._names_jclassName(x), value = TRUE ) 
}

setMethod("names", c(x="jclassName"), ._names_jclassName )
# }}}

# {{{ jobjRef
._names_jobjRef <- function(x){
	classNamesMethod(.jcall(x, "Ljava/lang/Class;", "getClass"), static.only = FALSE )
}
.DollarNames.jobjRef <- function(x, pattern = "" ){
	grep( pattern, ._names_jobjRef(x), value = TRUE )
}
setMethod("names", c(x="jobjRef"), ._names_jobjRef )
# }}}

# {{{ jarrayRef and jrectRef
._names_jarrayRef <- function(x ){
	c("length", classNamesMethod(.jcall(x, "Ljava/lang/Class;", "getClass"), static.only = FALSE ) )
}
.DollarNames.jarrayRef <- .DollarNames.jrectRef <- function(x, pattern = ""){
	grep( pattern, ._names_jarrayRef(x), value = TRUE )
}

setMethod("names", c(x="jarrayRef"), ._names_jarrayRef )
setMethod("names", c(x="jrectRef"), ._names_jarrayRef )


# }}}
