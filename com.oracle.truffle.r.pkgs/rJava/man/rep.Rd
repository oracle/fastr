\name{rep}
\alias{rep,jarrayRef-method}
\alias{rep,jobjRef-method}
\alias{rep,jrectRef-method}

\title{Creates java arrays by cloning}
\description{ 
	Creates a java array by cloning a reference several times
}
\section{Methods}{
  \describe{
    \item{rep}{\code{signature(object = "jobjRef")}: ... }
    \item{rep}{\code{signature(object = "jarrayRef")}: ... }
    \item{rep}{\code{signature(object = "jrectRef")}: ... }
  }
}

\seealso{
  \code{\link[base]{rep}} or \code{\link{.jarray}}
}

\examples{
\dontshow{.jinit()}
if (!nzchar(Sys.getenv("NOAWT"))) {
  p <- .jnew( "java.awt.Point" )
  a <- rep( p, 10 )

  stopifnot( dim(a) == c(10L ) )
  a[[1]]$move( 10L, 50L )
  stopifnot( a[[2]]$getX() == 0.0 )
}
}
