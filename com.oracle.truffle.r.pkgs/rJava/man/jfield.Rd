\name{jfield}
\alias{.jfield}
\alias{.jfield<-}
\title{
  Obtains the value of a field 
}
\description{
  \code{.jfield} returns the value of the specified field on an object.
}
\usage{
.jfield(o, sig = NULL, name, true.class = is.null(sig), convert = TRUE)
`.jfield<-`(o, name, value)
}
\arguments{
  \item{o}{Class name or object (Java reference) whose field is to be
    accessed. Static fields are supported both by specifying the class
    name or using an instance.}
  \item{sig}{signature (JNI type) of the field. If set to \code{NULL}
    rJava attempts to determine the signature using reflection. For
    efficiency it is recommended to specify the signature, because
    the reflection lookup is quite expensive.}
  \item{name}{name of the field to access}
  \item{true.class}{by default the class of the resulting object matches
    the siganture of the field. Setting this flag to \code{TRUE} causes
    \code{.jfield} to use true class name of the resulting object
    instead. (this flag has no effect on scalar fields)}
  \item{convert}{when set to \code{TRUE} all references are converted to
    native types (where possible). Otherwise Java references are
    returned directly.}
  \item{value}{value to assign into the field. The field signature is
    determined from the value in the same way that parameter signatures
    are determined in \code{\link{.jcall}} - be sure to cast the value
    as necessary, no automatic conversion is done.}
}
\value{
  \code{.jfield}: contents of the field, \code{.jfield<-}: modified object.
}
\details{
  The detection of a field signature in \code{.jfield} using reflection
  is considerably expensive (more than 3 additional method calls have to
  be performed), therefore it is recommended for time-critical code to
  specify the field signature beforehand.

  NOTE: The sequence of arguments in \code{.jfield} has been changed
  since rJava 0.5 to be more consistent and match the sequence in
  \code{.jcall}. Also \code{.jsimplify} is no longer needed as primitive
  types are obtained directly.
}
\seealso{
  \code{\link{.jcall}}
}
\examples{
\dontrun{
.jfield("java/lang/Boolean",, "TYPE")
}
}
\keyword{interface}
