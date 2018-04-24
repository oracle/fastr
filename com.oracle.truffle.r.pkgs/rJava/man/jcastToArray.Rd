\name{jcastToArray}
\alias{.jcastToArray}
\title{
  Ensures that a given object is an array reference
}
\description{
  \code{.jcastToArray} takes a Java object reference of any kind and
  returns Java array reference if the given object is a reference to an
  array.
}
\usage{
.jcastToArray(obj, signature=NULL, class="", quiet=FALSE)
}
\arguments{
  \item{obj}{Java object reference to cast or a scalar vector}
  \item{signature}{array signature in JNI notation (e.g. \code{"[I"} for
    an array of integers). If set to \code{NULL} (the default),
    the signature is automatically determined from the object's class.}
  \item{class}{force the result to pose as a particular Java
    class. This has the same effect as using \code{\link{.jcast}} on the
    result and is provided for convenience only.}
  \item{quiet}{if set to \code{TRUE}, no failures are reported and the
    original object is returned unmodified.}
}
\value{
  Returns a Java array reference (\code{jarrayRef}) on success. If
  \code{quiet} is \code{TRUE} then the result can also be the original
  object in the case of failure.
}
\details{
  Sometimes a result of a method is by definition of the class
  \code{java.lang.Object}, but the acutal referenced object may be an
  array. In that case the method returns a Java object reference instead
  of an array reference. In order to obtain an array reference, it is
  necessary to cast such an object to an array reference - this is done
  using the above \code{.jcastToArray} function.

  The input is an object reference that points to an array. Ususally the
  signature should be left at \code{NULL} such that it is determined
  from the object's class. This is also a check, because if the object's
  class is not an array, then the functions fails either with an error
  (when \code{quiet=FALSE}) or by returing the original object (when
  \code{quiet=TRUE}). If the signature is set to anything else, it is
  not verified and the array reference is always created, even if it may
  be invalid and unusable.

  For convenience \code{.jcastToArray} also accepts non-references in
  which case it simply calls \code{\link{.jarray}}, ignoring all other
  parameters.
}
\examples{
\dontrun{
a <- .jarray(1:10)
print(a)
# let's create an array containing the array
aa <- .jarray(list(a))
print(aa)
ba <- .jevalArray(aa)[[1]]
# it is NOT the inverse, because .jarray works on a list of objects
print(ba)
# so we need to cast the object into an array
b <- .jcastToArray(ba)
# only now a and b are the same array reference
print(b)
# for convenience .jcastToArray behaves like .jarray for non-references
print(.jcastToArray(1:10/2))
}
}
\keyword{interface}
