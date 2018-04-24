\name{clone}
\alias{clone}
\alias{clone,jobjRef-method}
\alias{clone,jarrayRef-method}
\alias{clone,jrectRef-method}

\title{
Object cloner
}
\description{
Generic function to clone objects
}
\usage{
clone(x, ...)
}
\section{Methods}{
  \describe{
    \item{clone}{\code{signature(x = "jobjRef")}: clone a java object reference (must implement Cloneable) }
    \item{clone}{\code{signature(x = "jarrayRef")}: clone a java rugged array (not yet implemented) }
    \item{clone}{\code{signature(x = "jrectRef")}: clone a java rectangular array (not yet implemented) }
  }
}

\arguments{
  \item{x}{An object to clone}
  \item{\dots}{Further arguments, ignored}
}
\value{
A clone of the object
}
\section{Warning}{
	The implementation of clone for java object references uses
	the clone method of the Object class. The reading of its description
	in the java help page is \emph{strongly} recommended. 
}

\examples{
\dontshow{.jinit()}

  p1 <- .jnew("java/awt/Point" )
  p2 <- clone( p1 )
  p2$move( 10L, 10L )
  p1$getX() 

  # check that p1 and p2 are not references to the same java object
  stopifnot( p1$getX() == 0 )
  stopifnot( p2$getX() == 10 )

}
\keyword{ programming }

