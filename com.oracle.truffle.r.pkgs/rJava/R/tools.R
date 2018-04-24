#' converts a java class name to jni notation
tojni <- function( cl = "java.lang.Object" ){
	gsub( "[.]", "/", cl )
}

tojniSignature <- function( cl ){
	sig <- tojni( cl )
	
	if( isPrimitiveTypeName(sig) || isPrimitiveArraySignature(sig) ){
		return( sig ) 
	}
	
	n <- nchar( sig )
	last <- substr( sig, n, n )
	add.semi <- last != ";"
	
	first <- substr( sig, 1, 1 )
	add.L <- ! first %in% c("L", "[" )
	
	sig <- if( !add.L && !add.semi) sig else sprintf( "%s%s%s", if( add.L ) "L" else "", sig, if( add.semi ) ";" else "" )
	sig
}

#' converts jni notation to java notation
tojava <- function( cl = "java/lang/Object" ){
	gsub( "/", ".", cl )
}

