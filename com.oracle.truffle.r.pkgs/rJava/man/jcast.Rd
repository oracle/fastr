\name{jcast}
\alias{.jcast}
\title{
  Cast a Java object to another class
}
\description{
  \code{.jcast} returns a Java object reference cast to another Java class.
}
\usage{
.jcast(obj, new.class = "java/lang/Object", check = FALSE, convert.array = FALSE)
}
\arguments{
  \item{obj}{a Java object reference}
  \item{new.class}{fully qualified class name in JNI notation
    (e.g. \code{"java/lang/String"}). }
  \item{check}{logical. If \code{TRUE}, it is checked that the object 
effectively is an instance of the new class. See \code{\link{\%instanceof\%}}.
Using FALSE (the default) for this argument, rJava does not perform type check and this 
will cause an error on the first use if the cast is illegal.}
\item{convert.array}{logical. If \code{TRUE} and the object is an array, 
it is converted into a \code{jarrayRef} reference. }
}
\value{
  Returns a Java object reference (\code{jobjRef}) to the object
  \code{obj}, changing the object class.
}
\details{
  This function is necessary if a argument of \code{\link{.jcall}} or
  \code{\link{.jnew}} is defined as the superclass of the object to be
  passed (see \code{\link{.jcall}}). The original object is not modified.
  
  The default values for the arguments \code{check} and \code{convert.array}
  is \code{FALSE} in order to guarantee backwards compatibility, 
  but it is recommended to set the arguments to \code{TRUE}
}
\seealso{
  \code{\link{.jcall}}
}
\examples{
\dontrun{
v <- .jnew("java/util/Vector")
.jcall("java/lang/System","I","identityHashCode",.jcast(v, "java/lang/Object"))
}
}
\keyword{interface}
