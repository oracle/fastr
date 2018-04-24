
#' if a and b are compatable, 
#' in the sense of the java.util.Comparable interface
#' then the result of the compareTo method is returned
#' otherwise an error message is generated
.jcompare <- function(a, b) {
  if (is.null(a)) a <- new("jobjRef")
  if (is.null(b)) b <- new("jobjRef")
  
  if( isJavaArray(a) || isJavaArray(b) ){
  	  stop( "comparison (<,>,<=,>=) is not implemented for java arrays yet" )
  }
  
  if( !is(a, "jobjRef" ) ) a <- ._java_valid_object( a )
  if( !is(b, "jobjRef" ) ) b <- ._java_valid_object( b )
  
  .jcall( "RJavaComparator", "I", "compare", .jcast(a), .jcast(b) )
  
}
._lower <- function(e1, e2){
	.jcompare( e1, e2 ) <= 0L
}
._greater <- function(e1, e2 ){
	.jcompare( e1, e2 ) >= 0L
}
._strictly_lower <- function(e1, e2 ){
	.jcompare( e1, e2 ) < 0L
}
._strictly_greater <- function(e1, e2 ){
	.jcompare( e1, e2 ) > 0L
}

setMethod("<" , c(e1="jobjRef",e2="jobjRef"), ._strictly_lower )
setMethod("<" , c(e1="jobjRef")             , ._strictly_lower )
setMethod("<" , c(e2="jobjRef")             , ._strictly_lower )
              
setMethod(">" , c(e1="jobjRef",e2="jobjRef"), ._strictly_greater )
setMethod(">" , c(e1="jobjRef")             , ._strictly_greater )
setMethod(">" , c(e2="jobjRef")             , ._strictly_greater )

setMethod("<=", c(e1="jobjRef",e2="jobjRef"), ._lower )
setMethod("<=", c(e1="jobjRef")             , ._lower )
setMethod("<=", c(e2="jobjRef")             , ._lower )
                                               
setMethod(">=", c(e1="jobjRef",e2="jobjRef"), ._greater )
setMethod(">=", c(e1="jobjRef")             , ._greater )
setMethod(">=", c(e2="jobjRef")             , ._greater )


