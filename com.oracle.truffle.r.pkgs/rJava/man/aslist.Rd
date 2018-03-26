\name{aslist}
\alias{as.list.jobjRef}
\alias{as.list.jarrayRef}
\alias{as.list.jrectRef}
\title{
Converts java objects or arrays to R lists
}
\description{
	\code{as.list} is implemented for java objects and java arrays
	to facilitate using \code{lapply} calls over elements of a java array
	or items of an Iterator associated with an Iterable object
	
	For java array references, \code{as.list} is mapped to
	\code{\link{.jevalArray}}
	
	For java objects that implement the Iterable interface, 
	the list is created by iterating over the associated iterator
}
\usage{
\S3method{as.list}{jobjRef}(x, ...)
\S3method{as.list}{jarrayRef}(x, ...)
}
\arguments{
  \item{x}{java array or Iterable java object}
  \item{\dots}{ignored}
}
\value{
An R list, or vector.
}
\note{
The function is not intended to be called directly. It is implemented
so that java arrays or Iterable java objects can be used as the first 
argument of \code{\link{lapply}}
}
\seealso{
	\code{\link{.jevalArray}}, \code{\link{lapply}}
}
\examples{
\dontshow{.jinit()}
  # lapplying over a java array
  a <- .jarray( list( 
	.jnew( "java/awt/Point", 10L, 10L ), 
	.jnew( "java/awt/Point", 30L, 30L )
  ) )
  lapply( a, function(point){ 
	with(point, { 
		(x + y ) ^ 2
	} )
  } )

# lapply over a Vector (implements Iterable)
v <- .jnew("java/util/Vector")
v$add( "foo" )
v$add( .jnew("java/lang/Double", 10.2 ) )
sapply( v, function(item) item$getClass()$getName() )

}
\keyword{ programming }

