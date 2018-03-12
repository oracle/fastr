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
