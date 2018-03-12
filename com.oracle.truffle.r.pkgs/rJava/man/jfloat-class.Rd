\name{jfloat-class}
\docType{class}
\alias{jfloat-class}
\alias{jlong-class}
\alias{jbyte-class}
\alias{jchar-class}
\title{Classes "jfloat", "jlong", "jbyte" and "jchar" specify Java
  native types that are not native in R}
\description{ These classes wrap a numeric vector to be treated as
  \code{float} or \code{long} argument when passed to Java and an
  integer vector to be treated as \code{byte} or \code{char}. R doesn't
  distinguish between \code{double} and \code{float}, but Java
  does. In order to satisfy object types, numeric vectors that should be
  converted to floats or long on the Java side must be wrapped in this
  class. In addition \code{jbyte} must be used when passing scalar byte
  (but not byte arrays, those are mapped into RAW vectors). Finally
  \code{jchar} it used when mapping integer vectors into unicode Java
  character vectors.}
\section{Objects from the Class}{
  Objects can be created by calling \code{\link{.jfloat}},
  \code{\link{.jlong}}, \code{\link{.jbyte}} or \code{\link{.jchar}}
  respectively.
}
\section{Slots}{
  \describe{
    \item{\code{.Data}:}{Payload}
  }
}
\section{Extends}{
  "jfloat" and "jlong":
  Class \code{"numeric"}, from data part.
  Class \code{"vector"}, by class \code{"numeric"}.

  "jbyte" and "jchar":
  Class \code{"integer"}, from data part.
  Class \code{"vector"}, by class \code{"integer"}.
}
\section{Methods}{
  "jfloat" and "jlong" have no methods other than those inherited from "numeric".
  "jbyte" and "jchar" have no methods other than those inherited from "integer".
}
%\references{ ~put references to the literature/web site here ~ }
\author{ Simon Urbanek }
%\note{ ~~further notes~~ }

% ~Make other sections like Warning with \section{Warning }{....} ~

\seealso{
  \code{\link{.jfloat}}, \code{\link{.jlong}}, \code{\link{.jbyte}}, \code{\link{.jchar}} and \code{\link{.jcall}}
}
%\examples{
%##---- Should be DIRECTLY executable !! ----
%}
\keyword{classes}
