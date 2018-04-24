## This program is free software: you can redistribute it and/or modify
## it under the terms of the GNU General Public License as published by
## the Free Software Foundation, either version 2 of the License, or
## (at your option) any later version.
##
## This program is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
## GNU General Public License for more details.
##
## You should have received a copy of the GNU General Public License
## along with this program. If not, see <http://www.gnu.org/licenses/>.
##
## Author: Romain Francois <francoisromain@free.fr>

._populate_with_fields_and_methods <- function( env, fields, methods, classes, data, only.static = FALSE ){
	object <- if( only.static ) .jnull() else .jcast( data )
	
	# fields 
	if( !is.jnull(fields) ) {
		lapply( fields, function(x ){
  		  n <- .jcall( x, "S", "getName" )
  		  type <- .jcall( .jcall( x, "Ljava/lang/Class;", "getType"), "Ljava/lang/String;" , "getName" )
  		  suffix <- switch( type, 
  		  	"boolean" = "Boolean", 
  		  	"byte" = "Byte", 
  		  	"char" = "Char",
  		  	"double" = "Double", 
  		  	"float" = "Float", 
  		  	"int" = "Int", 
  		  	"long" = "Long", 
  		  	"short" = "Short",
  		  	"" )
  		  target <- switch( type,
  		  	"boolean" = "Z", 
  		  	"byte" = "B", 
  		  	"char" = "C",
  		  	"double" = "D", 
  		  	"float" = "F", 
  		  	"int" = "I", 
  		  	"long" = "L", 
  		  	"short" = "S",
  		  	"Ljava/lang/Object;" )
  		  set_method <- sprintf( "set%s", suffix)
  		  get_method <- sprintf( "get%s", suffix )
  		  	
  		  makeActiveBinding( n, function(v){
  		    if( missing(v) ){
  		      ## get
  		      .jcall( x, target, get_method, object )
  		    } else {
  		      ## set
  		      .jcall( x, "V", set_method , object, v )
  		    }
  		  }, env )
  		} )
  	}
  	
  	# methods
  	if( !is.jnull(methods) ){
  		done.this <- NULL
  		lapply( methods, function(m){
  			n <- .jcall( m, "S", "getName" )
  			if( n %in% done.this ) return()
  		  fallback <- tryCatch( match.fun( n ), error = function(e) NULL )
  		  assign( n, function(...) {
  		    tryCatch( .jrcall( if(only.static) data@name else data , n, ...), error = function(e){
  		      if( !is.null(fallback) && inherits(fallback, "function") ){
  		        fallback( ... )
  		      }
  		    } )
  		  }, env )
  		  done.this <<- c( done.this, n )
  		} )
  	}
  	
  	# classes
  	if( !is.jnull( classes ) ){
  		lapply( classes, function( cl ){
  			name <- .jcall( cl, "S", "getSimpleName" )
  			assign( name, new("jclassName", name=.jcall(cl, "S", "getName"), jobj=cl), env )
  		} )
  	}
}
grabDots <- function( env, ...){
  dots <- list(...)
  if( length( dots ) ){
  	dots.names <- names(dots)
  	sapply( dots.names, function( name ){
  		if( name != "" ){
  			assign( name, dots[[ name ]], env )
  		}
  	} )
  	
  }
}

with.jobjRef <- function( data, expr, ...){
  env <- new.env( parent = parent.frame() )
  clazz <- .jcall( data, "Ljava/lang/Class;", "getClass")
  
  fields  <- .jcall( clazz, "[Ljava/lang/reflect/Field;", "getFields" )
  methods <- .jcall( clazz, "[Ljava/lang/reflect/Method;", "getMethods" )
  classes <- .jcall( clazz, "[Ljava/lang/Class;" , "getClasses" )
  ._populate_with_fields_and_methods( env, fields, methods, classes, data, only.static = FALSE )

  assign( "this", data, env )

  grabDots( env, ... )
  
  eval( substitute( expr ), env )
}

within.jobjRef <- function(data, expr, ... ){
  call <- match.call()
  call[[1]] <- as.name("with.jobjRef")
  eval( call, parent.frame() )
  data
}

with.jarrayRef <- function( data, expr, ...){
  env <- new.env( parent = environment() )
  clazz <- .jcall( data, "Ljava/lang/Class;", "getClass")
  
  fields  <- .jcall( clazz, "[Ljava/lang/reflect/Field;", "getFields" )
  methods <- .jcall( clazz, "[Ljava/lang/reflect/Method;", "getMethods" )
  classes <- .jcall( clazz, "[Ljava/lang/Class;" , "getClasses" )
  ._populate_with_fields_and_methods( env, fields, methods, classes, data, only.static = FALSE )

  assign( "this", data, env )

  # add "length" pseudo field
  makeActiveBinding( "length", function(v){
  	if( missing( v ) ){
  		._length_java_array( data )
  	} else{
  		stop( "cannot modify length of java array" ) 
  	}
  }, env )
  
  grabDots( env, ... )
  
  eval( substitute( expr ), env )
}

within.jarrayRef <- function(data, expr, ... ){
  call <- match.call()
  call[[1]] <- as.name("with.jarrayRef")
  eval( call, parent.frame() )
  data
}

with.jclassName <- function( data, expr, ... ){
	env <- new.env( parent = environment() )
	clazz <- data@jobj
	
	static_fields  <- .jcall( "RJavaTools", "[Ljava/lang/reflect/Field;",  "getStaticFields",  clazz )
	static_methods <- .jcall( "RJavaTools", "[Ljava/lang/reflect/Method;",  "getStaticMethods",  clazz )
	static_classes <- .jcall( clazz, "[Ljava/lang/Class;",  "getClasses" )
	
	._populate_with_fields_and_methods( env, static_fields, 
		static_methods, static_classes, data, only.static = TRUE )
	
	grabDots( env, ... )
	eval( substitute( expr ), env )
}

within.jclassName <- function(data, expr, ... ){
  call <- match.call()
  call[[1]] <- as.name("with.jclassName")
  eval( call, parent.frame() )
  data
}


