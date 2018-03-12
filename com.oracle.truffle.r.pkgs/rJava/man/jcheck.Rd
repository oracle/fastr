\name{jcheck}
\alias{.jcheck}
\alias{.jthrow}
\alias{.jclear}
\alias{.jgetEx}
\title{
  Java exception handling
}
\description{
  \code{.jcheck} checks the Java VM for any pending exceptions and
  clears them.

  \code{.jthrow} throws a Java exception.

  \code{.jgetEx} polls for any pending expections and returns the exception object.

  \code{.jclear} clears a pending exception.
}
\usage{
.jcheck(silent = FALSE)

.jthrow(exception, message = NULL)
.jgetEx(clear = FALSE)
.jclear()
}
\arguments{
  \item{silent}{If set to \code{FALSE} then Java is instructed to print
    the exception on \code{stderr}. Note that Windows Rgui doesn't show
    \code{stderr} so it will not appear there (as of rJava 0.5-1 some
    errors that the JVM prints using the vfprintf callback are passed
    to R. However, some parts are printed using \code{System.err} in
    which case the ususal redirection using the \code{System} class
    can be used by the user).}
  \item{exception}{is either a class name of an exception to create or a
    throwable object reference that is to be thrown.}
  \item{message}{if \code{exception} is a class name then this parameter
    specifies the string to be used as the message of the exception. This
    parameter is ignored if \code{exception} is a reference.}
  \item{clear}{if set to \code{TRUE} then the returned exception is also
  cleared, otherwise the throwable is returned without clearing the
  cause.}
}
\value{
  \code{.jcheck} returns \code{TRUE} if an exception occurred or
  \code{FALSE} otherwise.

  \code{.jgetEx} returns \code{NULL} if there are no pending exceptions
  or an object of the class "java.lang.Throwable" representing the
  current exception.
}
\details{
  Please note that some functions (such as \code{\link{.jnew}} or
  \code{\link{.jcall}}) call \code{.jcheck} implicitly unless
  instructed to not do so. If you want to handle Java exceptions, you
  should make sure that those function don't clear the exception you may
  want to catch.
  
  The exception handling is still as a very low-level and experimental,
  because it requires polling of exceptions. A more elaboate system
  using constructs similar to \code{try} ... \code{catch} is planned for
  next major version of \code{rJava}.

  \emph{Warning:} When requesting exceptions to not be cleared
  automatically, please note that the \code{show} method (which is
  called by \code{print}) has a side-effect of making a Java call to get
  the string representation of a Java object. This implies that it will
  be impeded by any pending exceptions. Therefore exceptions obtained
  through \code{.jgetEx} can be stored, but should not be printed
  (or otherwise used in Java calls) until after the exception is
  cleared. In general, all Java calls will fail (possibly silently)
  until the exception is cleared.
}
\seealso{
  \code{\link{.jcall}}, \code{\link{.jnew}}
}
\examples{
\donttest{
# we try to create a bogus object and
# instruct .jnew to not clear the exception
# this will raise an exception
v <- .jnew("foo/bar", check=FALSE)

# you can poll for the exception, but don't try to print it
# (see details above)
if (!is.null(e<-.jgetEx())) print("Java exception was raised")

# expect TRUE result here because the exception was still not cleared
print(.jcheck(silent=TRUE))
# next invocation will be FALSE because the exception is now cleared
print(.jcheck(silent=TRUE))

# now you can print the actual expection (even after it was cleared)
print(e)
}
}
\keyword{interface}
