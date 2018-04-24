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

