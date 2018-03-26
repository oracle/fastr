\name{javaImport}
\alias{javaImport}
\title{
Attach mechanism for java packages
}
\description{
The \code{javaImport} function creates an item on R's
search that maps names to class names references found in 
one or several "imported" java packages.
}
\usage{
javaImport(packages = "java.lang")
}
\arguments{
  \item{packages}{character vector containing java package paths}
}
\value{
An external pointer to a java specific \code{UserDefinedDatabase} object
}
\references{
	\emph{User-Defined Tables in the R Search Path}. Duncan Temple Lang. December 4, 2001
	\url{http://www.omegahat.org/RObjectTables/}
}
\author{
Romain Francois <francoisromain@free.fr>
}
\note{
Currently the list of objects in the imported package is populated
as new objects are found, \emph{not} at creation time.
}
\section{Warning}{
This feature is experimental. Use with caution, and don't forget to
detach.
}
\seealso{
\code{\link{attach}}
}
\examples{
\dontrun{
	attach( javaImport( "java.util" ), pos = 2 , name = "java:java.util" )
	
	# now we can just do something like this 
	v <- new( Vector )
	v$add( "foobar" )
	ls( pos = 2 )
	
	# or this
	m <- new( HashMap )
	m$put( "foo", "bar" )
	ls( pos = 2 )
	
	# or even this :
	Collections$EMPTY_MAP
}
}
\keyword{ programming }
