\name{jnull}
\alias{.jnull}
\alias{is.jnull}
\title{
  Java null object reference
}
\description{
  \code{.jnull} returns a \code{null} reference of a specified class
  type. The resulting object is of the class \code{jobjRef}.

  \code{is.jnull} is an extension of \code{is.null} that also returns
  \code{TRUE} if the supplied object is a \code{null} Java reference.
}
\usage{
.jnull(class = "java/lang/Object")
is.jnull(x)
}
\arguments{
  \item{class}{fully qualified target class name in JNI notation
    (e.g. \code{"java/lang/String"}).}
  \item{x}{object to check}
}
\value{
  \code{.jnull} returns a Java object reference (\code{jobjRef}) of a
  \code{null} object having the specified object class.

  \code{is.jnull} returns \code{TRUE} if \code{is.null(x)} is
  \code{TRUE} or if \code{x} is a Java \code{null} reference.
}
\details{
  \code{.jnull} is necesary if \code{null} is to be passed as an
  argument of \code{\link{.jcall}} or \code{\link{.jnew}}, in order to be
  able to find the correct method/constructor.

  Example: given the following method definitions of the class \code{A}:
  \itemize{
    \item{o}{public static void run(String a);}
    \item{o}{public static void run(Double n);}
  }
  Calling \code{.jcall("A",,"run",NULL)} is ambiguous, because it is
  unclear which method is to be used. Therefore rJava requires class
  information with each argument to \code{\link{.jcall}}. If we wanted
  to run the String-version, we could use
  \code{.jcall("A",,"run",.jnull("java/lang/String"))}.

  \code{is.jnull} is a test that should be used to determine whether a
  given Java reference is a \code{null} reference.
}
\seealso{
  \code{\link{.jcall}}, \code{\link{.jcast}}
}
\examples{
\dontrun{
.jcall("java/lang/System","I","identityHashCode",.jnull())
}
}
\keyword{interface}
