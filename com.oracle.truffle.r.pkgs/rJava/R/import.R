
IMPORTER <- ".__rjava__import"

java_class_importers <- new.env()
assign( ".namespaces", NULL, envir = java_class_importers )

getImporterFromNamespace <- function( nm, create = TRUE ){
	.namespaces <- get(".namespaces", envir = java_class_importers )
	if( !is.null( .namespaces ) ){
		for( item in .namespaces ){
			if( identical( item$nm, nm ) ){
				return( item$importer )
			}
		}
	}
	if( create ){
		addImporterNamespace(nm)
	}
	
}
addImporterNamespace <- function( nm ){
	importer <- .jnew( "RJavaImport", .jcast( .rJava.class.loader, "java/lang/ClassLoader" ) )
	assign( ".namespaces",	
		append( list( list( nm = nm, importer = importer ) ), get(".namespaces", envir = java_class_importers ) ), 
		envir = java_class_importers )
	importer
}

getImporterFromEnvironment <- function(env, create = TRUE){
	if( isNamespace( env ) ){
		getImporterFromNamespace( env )
	} else if( exists(IMPORTER, envir = env ) ){
		get( IMPORTER, envir = env )
	} else if( create ){
		addImporterNamespace(env)
	}
}

getImporterFromGlobalEnv <- function( ){
	if( exists( "global", envir = java_class_importers ) ){
		get( "global", envir = java_class_importers ) 
	} else{
		initGlobalEnvImporter()
	}
}
initGlobalEnvImporter <- function(){
	importer <- .jnew( "RJavaImport", .jcast( .rJava.class.loader, "java/lang/ClassLoader" ) )
	assign( "global", importer , envir = java_class_importers )
	importer
}

import <- function( package = "java.util", env = sys.frame(sys.parent()) ){
	
	if( missing(env) ){
		caller <- sys.function(-1)
		env <- environment( caller ) 
		if( isNamespace( env ) ){
			importer <- getImporterFromNamespace( env )
		}
	} else{
		force(env)
	
		if( !is.environment( env ) ){
			stop( "env is not an environment" ) 
		}
		
		if( ! exists( IMPORTER, env ) || is.jnull( get( IMPORTER, envir = env ) ) ){
			importer <- .jnew( "RJavaImport", .jcast( .rJava.class.loader, "java/lang/ClassLoader" ) )
			if( isNamespace(env) ){
				unlockBinding( IMPORTER, env = env )
				assignInNamespace( IMPORTER, importer, envir = env ) 
			}
			assign( IMPORTER, importer, envir = env ) 
		} else{
			importer <- get( IMPORTER, envir = env )
		}
	}
	mustbe.importer( importer )
	.jcall( importer, "V", "importPackage", package )
	
}

is.importer <- function(x){
	is( x, "jobjRef" ) && .jinherits( x, "RJavaImport" )
}
mustbe.importer <- function(x){
	if( !is.importer(x) ){
		stop( "object not a suitable java package importer" )
	}
}

#' collect importers
getAvailableImporters <- function( frames = TRUE, namespace = TRUE, 
	global = TRUE, caller = sys.function(-1L) ){
	
	importers <- .jnew( "java/util/HashSet" )
	
	addImporter <- function( importer ){
		if( is.importer( importer ) ){
			.jcall( importers, "Z", "add", .jcast(importer) )
		}
	}
	if( isTRUE( global ) ){
		addImporter( getImporterFromGlobalEnv() )
	}
	
	if( isTRUE( frames ) ){
		frames <- sys.frames()
		if( length(frames) > 1L ){
			sapply( head( frames, -1L ), function(env) {
				if( !identical( env, .GlobalEnv ) ){
					addImporter( getImporterFromEnvironment( env ) )
				}
			} )
		}
	}
	
	if( isTRUE( namespace ) ){
		force(caller)
		env <- environment( caller ) 
		if( isNamespace( env ) ){
			addImporter( getImporterFromNamespace( env ) )
		}
	}
	
	importers
}

#' lookup for a class name in the available importers
lookup <- function( name = "Object", ..., caller = sys.function(-1L) ){
	force(caller)
	importers <- getAvailableImporters(..., caller = caller)
	.jcall( "RJavaImport", "Ljava/lang/Class;", "lookup", 
		name, .jcast( importers, "java/util/Set" )  )
}


javaImport <- function( packages = "java.lang" ){
	importer <- .jnew( "RJavaImport", .jcast( .rJava.class.loader, "java/lang/ClassLoader" ) )
	.jcall( importer, "V", "importPackage", packages )
	.Call( "newRJavaLookupTable" , importer, 
		PACKAGE = "rJava" )
}

