# :tabSize=4:indentSize=4:noTabs=false:folding=explicit:collapseFolds=1:

# {{{ rep
setGeneric("rep")
setMethod( "rep", "jobjRef", function( x, times = 1L, ... ){
	.jcall( "RJavaArrayTools", "[Ljava/lang/Object;", "rep", 
		.jcast(x), as.integer(times), evalArray = FALSE )
} )
setMethod( "rep", "jarrayRef", function(x, times = 1L, ...){
	.NotYetImplemented()
} )
setMethod( "rep", "jrectRef", function(x, times = 1L, ...){
	.NotYetImplemented()
} )
# }}}

# {{{ clone 
clone <- function( x, ... ){
	UseMethod( "clone" )
}
clone.default <- function( x, ... ){
	.NotYetImplemented()
}
setGeneric( "clone" )
setMethod( "clone", "jobjRef", function(x, ...){
	.jcall( "RJavaArrayTools", "Ljava/lang/Object;", "cloneObject", .jcast( x ) ) 
} )
setMethod( "clone", "jarrayRef", function(x, ...){ .NotYetImplemented( ) } )
setMethod( "clone", "jrectRef", function(x, ...){ .NotYetImplemented( ) } )
# }}}
