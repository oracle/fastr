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

#' converts a java class name to jni notation
tojni <- function( cl = "java.lang.Object" ){
	gsub( "[.]", "/", cl )
}

tojniSignature <- function( cl ){
	sig <- tojni( cl )

        # TODO FASTR how comes that sig %in% c("boolean", "byte", "char", "double", "float", "int", "long", "short")
	if( isPrimitiveTypeName(sig) || isPrimitiveArraySignature(sig) || sig %in% c("boolean", "byte", "char", "double", "float", "int", "long", "short")){
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

