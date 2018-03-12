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

`%instanceof%` <- .jinstanceof <- function( o, cl ){
	
	if( !inherits( o, "jobjRef" ) ){
		stop( "o is not a java object" )
	}
	
	# first get the class object that represents cl
	if( inherits( cl, "jobjRef" ) ){
		if( .jclass( cl ) == "java.lang.Class" ){
			clazz <- cl
		} else {
			clazz <- .jcall( cl, "Ljava/lang/Class;", "getClass" ) 
		}
	} else if( inherits( cl, "jclassName" ) ) {
		clazz <- cl@jobj
	} else if( inherits( cl, "character" ) ){
		clazz <- .jfindClass(cl)
	} else {
		return(FALSE)
	}
	
	# then find out if o is an instance of the class
	.jcall( clazz , "Z", "isInstance", .jcast(o, "java/lang/Object" ) )
}

