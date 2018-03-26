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

## functions for some basic exception handling

# FIXME: should all these actually be deprecated or defunct

## poll for an exception
.jgetEx <- function(clear=FALSE) {
  exo <- .Call(RpollException)
  if (is.null(exo)) return(NULL)
  x <- new("jobjRef", jobj=exo, jclass="java/lang/Throwable")
  if (clear) .jclear()
  x
}

## explicitly clear any pending exceptions
.jclear <- function() {
  .C(RclearException)
  invisible(NULL)
}

## throw an exception
.jthrow <- function(exception, message=NULL) {
  if (is.character(exception))
    exception <- .jnew(exception, as.character(message))
  if (is(exception, "jobjRef"))
    .Call(RthrowException, exception)
  else
    stop("Invalid exception.")
}


"$.Throwable" <- function( x, name ){
	if( name %in% names(c(x)) ){
		c(x)[[ name ]]
	} else{
		._jobjRef_dollar( x[["jobj"]], name )
	}
}

"$<-.Throwable" <- function( x, name, value ){
	if( name %in% names(x) ){
		x[[ name ]] <- value
	} else{
		._jobjRef_dollargets( x[["jobj"]], name, value )
	}
	x
	
}

