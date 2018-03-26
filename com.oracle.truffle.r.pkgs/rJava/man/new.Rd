\name{new}
\alias{new,jclassName-method}
\title{
  Create a new Java object
}
\description{
  Creates a new Java object and invokes the constructor with given arguments.
}
\section{Methods}{
  \describe{
    \item{\code{new}}{\code{signature(Class = "jclassName")}: ... }
  }
}
\details{
  The \code{new} method is used as the high-level API to create new
  Java objects (for low-level access see \code{\link{.jnew}}). It
  returns the newly created Java object.

  \code{...} arguments are passed to the constructor of the class
  specified as \code{J("class.name")}.
}
\seealso{
  \code{\link{.jnew}}, \code{\link{jclassName-class}}
}
\examples{
\dontrun{
v <- new(J("java.lang.String"), "Hello World!")
v$length()
v$indexOf("World")
names(v)
}
}
\keyword{interface}
