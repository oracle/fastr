\name{jsimplify}
\alias{.jsimplify}
\title{
  Converts Java object to a simple scalar if possible
}
\description{
  \code{.jsimplify} attempts to convert Java objects that represent
  simple scalars into corresponding scalar representation in R.
}
\usage{
.jsimplify(o, promote=FALSE)
}
\arguments{
  \item{o}{arbitrary object}
  \item{promote}{logical, if \code{TRUE} then an ambiguous conversion
  where the native type value would map to \code{NA} (e.g., Java
  \code{int} type with value -2147483648) will be taken
  to represent an actual value and will be promoted to a larger type
  that can represent the value (in case of \code{int} promoted to
  \code{double}). If \code{FALSE} then such values are assumed to
  represent \code{NA}s.}
}
\value{
  Simple scalar or \code{o} unchanged.
}
\details{
  If \code{o} is not a Java object reference, \code{o} is returned
  as-is. If \code{o} is a reference to a scalar object (such as single
  integer, number, string or boolean) then the value of that object is
  returned as R vector of the corresponding type and length one.

  This function is used by \code{\link{.jfield}} to simplify the results
  of field access if required.

  Currently there is no function inverse to this, the usual way to wrap
  scalar values in Java references is to use \code{\link{.jnew}} as the
  corresponding constructor.
}
\seealso{
  \code{\link{.jfield}}
}
\examples{
\dontrun{
i <- .jnew("java/lang/Integer", as.integer(10))
print(i)
print(.jsimplify(i))
}
}
\keyword{interface}
