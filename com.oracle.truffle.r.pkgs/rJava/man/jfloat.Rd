\name{jfloat}
\alias{.jfloat}
\alias{.jlong}
\alias{.jbyte}
\alias{.jchar}
\alias{.jshort}
\alias{jfloat}
\alias{jlong}
\alias{jbyte}
\alias{jchar}
\alias{jshort}
\title{
  Wrap numeric vector as flat Java parameter
}
\description{
  \code{.jfloat} marks a numeric vector as an object that can be used
  as parameter to Java calls that require \code{float} parameters.
  Similarly, \code{.jlong} marks a numeric vector as \code{long} parameter.
}
\usage{
.jfloat(x)
.jlong(x)
.jbyte(x)
.jchar(x)
.jshort(x)
}
\arguments{
  \item{x}{numeric vector}
}
\value{
  Returns a numeric vector of the class \code{jfloat}, \code{jlong},
  \code{jbyte}, \code{jshort} or \code{jchar}
  that can be used as parameter to Java calls that require
  \code{float}, \code{long}, \code{byte}, \code{short} or \code{char}
  parameters respectively.
}
\details{
  R has no native \code{float} or \code{long} type. Numeric vectors are
  stored as \code{double}s, hence there is no native way to pass float
  numbers to Java methods. The \code{.jfloat} call marks a numeric
  vector as having the Java type \code{float} by wrapping it in the
  \code{jfloat} class. The class is still a subclass of \code{numeric},
  therefore all regular R operations are unaffected by this.

  Similarly, \code{.jlong} is used to mark a numeric vector as a
  parameter of the \code{long} Java type. Please note that in general R
  has no native type that will hold a \code{long} value, so conversion
  between Java's \code{long} type and R's numeric is potentially lossy.

  \code{.jbyte} is used when a scalar byte is to be passed ot Java. Note
  that byte arrays are natively passed as RAW vectors, not as
  \code{.jbyte} arrays.
  
  \code{jchar} is strictly experimental and may be based on
  \code{character} vectors in the future.
}
\seealso{
  \code{\link{.jcall}}, \code{\link{jfloat-class}}
}
%\examples{
%\dontrun{
%v <- .jnew("java/util/Vector")
%.jcall("java/lang/System","I","identityHashCode",.jcast(v, "java/lang/Object"))
%}
%}
\keyword{interface}
