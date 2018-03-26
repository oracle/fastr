\name{jnew}
\alias{.jnew}
\title{
  Create a Java object
}
\description{
	\code{.jnew} create a new Java object.
}
\usage{
.jnew(class, ..., check=TRUE, silent=!check, class.loader=NULL)
}
\arguments{
  \item{class}{fully qualified class name in JNI notation (e.g. \code{"java/lang/String"}).}
  \item{...}{
    Any parameters that will be passed to the corresponding
    constructor. The parameter types are determined automatically and/or
    taken from the \code{jobjRef} object. For details see
    \code{\link{.jcall}}. Note that all named parameters are discarded.}
  \item{check}{
    If set to \code{TRUE} then \code{\link{.jcheck}} is invoked before
    and after the call to the constructor to clear any pending Java
    exceptions.}
  \item{silent}{
    If set to \code{FALSE} then \code{.jnew} will fail with an error if
    the object cannot be created, otherwise a null-reference is returned
    instead. In addition, this flag is also passed to final
    \code{.jcheck} if \code{check} above is set to \code{TRUE}. Note
    that the error handling also clears exceptions, so
    \code{check=FALSE, silent=FALSE} is usually not a meaningful
    combination.
  }
  \item{class.loader}{optional class loader to force for loading the
    class. If not set, the rJava class loader is used first. The default
    Java class loader is always used as a last resort. This is for
    expert use only! If you set the class loader, the class loading
    behavior changes - use only in very special circumstances.}
}
\value{
  Returns the reference (\code{jobjRef}) to the newly created object or
  \code{null}-reference (see \code{\link{.jnull}}) if something went wrong.
}
\seealso{
  \code{\link{.jcall}}, \code{\link{.jnull}}
}
\examples{
\dontrun{
f <- .jnew("java/awt/Frame","Hello")
.jcall(f,,"setVisible",TRUE)
}
}
\keyword{interface}
