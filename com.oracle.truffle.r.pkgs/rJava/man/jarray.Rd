\name{jarray}
\alias{.jarray}
\alias{.jevalArray}
\title{
  Java array handling functions
}
\description{
  \code{.jarray} takes a vector (or a list of Java references) as its
  argument, creates a Java array containing the elements of the vector
  (or list) and returns a reference to such newly created array.

  \code{.jevalArray} takes a reference to a Java array and returns its
  contents (if possible).
}
\usage{
.jarray(x, contents.class = NULL, dispatch = FALSE)
.jevalArray(obj, rawJNIRefSignature = NULL, silent = FALSE, simplify = FALSE)
}
\arguments{
  \item{x}{vector or a list of Java references}
  \item{contents.class}{common class of the contained objects, see
    details}
  \item{obj}{Java object reference to an array that is to be evaluated}
  \item{rawJNIRefSignature}{JNI signature that whould be used for
    conversion. If set to \code{NULL}, the signature is detected
    automatically.}
  \item{silent}{if set to true, warnings are suppressed}
  \item{dispatch}{logical. If \code{TRUE} the code attemps to dispatch
  to either a \code{jarrayRef} object for rugged arrays and
  \code{jrectRef} objects for rectangular arrays, creating possibly a
  multi-dimensional object in Java (e.g., when used with a matrix).}
  \item{simplify}{if set to \code{TRUE} more than two-dimensional arrays
  are converted to native obejcts (e.g., matrices) if their type and
  size matches (essentially the inverse for objects created with
  \code{dispatch=TRUE}).}
}
\value{
  \code{.jarray} returns a Java array reference (\code{jarrayRef} or \code{jrectRef}) to an
  array created with the supplied contents.

  \code{.jevalArray} returns the contents of the array object.
}
\details{
  \code{.jarray}: The input can be either a vector of some sort (such as
  numeric, integer, logical, ...) or a list of Java references. The
  contents is pushed to the Java side and a corresponding array is
  created. The type of the array depends on the input vector type. For
  example numeric vector creates \code{double[]} array, integer vector
  creates \code{int[]} array, character vector \code{String[]} array and
  so on. If \code{x} is a list, it must contain Java references only (or
  \code{NULL}s which will be treated as \code{NULL} references).

  The \code{contents.class} parameter is used only if \code{x} is a list
  of Java object references and it can specify the class that will be
  used for all objects in the array. If set to \code{NULL} no assumption
  is made and \code{java/lang/Object} will be used. Use with care and
  only if you know what you're doing - you can always use
  \code{\link{.jcast}} to cast the entire array to another type even if
  you use a more general object type. One typical use is to construct
  multi-dimensional arrays which mandates passing the array type as
  \code{contents.class}.

  The result is a reference to the newly created array.

  The inverse function which fetches the elements of an array reference
  is \code{.jevalArray}.

  \code{.jevalArray} currently supports only a subset of all possible
  array types. Recursive arrays are handled by returning a list of
  references which can then be evaluated separately. The only exception
  is \code{simplify=TRUE} in which case \code{.jevalArray} arrempts to
  convert multi-dimensional arrays into native R type if there is a
  such. This only works for rectangular arrays of the same basic type
  (i.e. the length and type of each referenced array is the same -
  sometimes matrices are represented that way in Java).
}
\examples{
\dontshow{.jinit()}
a <- .jarray(1:10)
print(a)
.jevalArray(a)
b <- .jarray(c("hello","world"))
print(b)
c <- .jarray(list(a,b))
print(c)
# simple .jevalArray will return a list of references
print(l <- .jevalArray(c))
# to convert it back, use lapply
lapply(l, .jevalArray)

# two-dimensional array resulting in int[2][10]
d <- .jarray(list(a,a),"[I")
print(d)
# use dispatch to convert a matrix to [[D
e <- .jarray(matrix(1:12/2, 3), dispatch=TRUE)
print(e)
# simplify it back to a matrix
.jevalArray(e, simplify=TRUE)
}
\keyword{interface}
