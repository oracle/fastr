\name{with.jobjRef}
\alias{with.jobjRef}
\alias{within.jobjRef}
\alias{with.jarrayRef}
\alias{within.jarrayRef}
\alias{with.jclassName}
\alias{within.jclassName}
\title{
with and within methods for Java objects and class names
}
\description{
Convenience wrapper that allow calling methods of 
Java object and classes from within the object (or class).
}
\usage{
\S3method{with}{jobjRef}(data, expr, ...)
\S3method{within}{jobjRef}(data, expr, ...)

\S3method{with}{jarrayRef}(data, expr, ...)
\S3method{within}{jarrayRef}(data, expr, ...)

\S3method{with}{jclassName}(data, expr, ...)
\S3method{within}{jclassName}(data, expr, ...)
}
\arguments{
 \item{data}{
 A Java object reference or a java class name. See \code{\link{J}}
}
 \item{expr}{
R expression to evaluate
}
 \item{\dots}{
ignored
}
}
\details{
The expression is evaluated in an environment 
that contains a mapping between the public fields 
and methods of the object. 

The methods of the object are mapped to standard R functions
in the environment. In case of classes, only static methods
are used.

The fields of the object are mapped to active bindings
(see \link{makeActiveBinding}) so that they can be accessed
and modified from within the environment. For classes, only 
static fields are used.
}
\value{
	\code{with} returns the value of the expression and 
	\code{within} returns the \code{data} argument
}
\author{
Romain Francois <francoisromain@free.fr>
}
\references{
	the \code{java.lang.reflect} package: 
	\url{http://java.sun.com/j2se/1.5.0/docs/api/java/lang/reflect/package-summary.html}
}
\examples{
\dontshow{.jinit()}

if (!nzchar(Sys.getenv("NOAWT"))) {
  p <- .jnew( "java/awt/Point", 0L, 0L )
  with( p, {
	# x and y and now 0
	move( 10L, 10L )
	# x and y are now 10
	x <- x + y
  } )

  f <- within( .jnew( "javax/swing/JFrame" ) , {
	layout <- .jnew( "java/awt/BorderLayout" )
	setLayout( layout )
	add( .jnew( "javax/swing/JLabel", "north" ), layout$NORTH )
	add( .jnew( "javax/swing/JLabel", "south" ), layout$SOUTH )
	add( .jnew( "javax/swing/JLabel", "west" ), layout$WEST )
	add( .jnew( "javax/swing/JLabel", "east" ), layout$EAST )
	setSize( .jnew( "java/awt/Dimension", 400L, 400L ) )
	setVisible( TRUE )
  } )
}

Double <- J("java.lang.Double")
with( Double, MIN_VALUE )
with( Double, parseDouble( "10.2" ) )

\dontrun{
# inner class example
% TODO: find a better example
HashMap <- J("java.util.HashMap")
with( HashMap, new( SimpleEntry, "key", "value" ) )
with( HashMap, SimpleEntry )
}

with( J("java.lang.System"), getProperty("java.home") )

\dontshow{
stopifnot( with( Double, parseDouble("10.0") ) == 10.0 )
d <- new( Double, "10.0") 
stopifnot( with( d, doubleValue() ) == 10.0 )
}

}
\keyword{ classes }
