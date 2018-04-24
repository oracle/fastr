\name{jengine}
\alias{.jengine}
\title{
  Java callback engineCast a Java object to another class
}
\description{
  \code{.jengine} obtains the current callback engine or starts it.
}
\usage{
.jengine(start=FALSE, silent=FALSE)
}
\arguments{
  \item{start}{if set to \code{TRUE} then the callback engine is started
    if it is not yet active}
  \item{silent}{if set to \code{TRUE} then NULL is returned if there is
    no engine available. Otherwise an error is raised}
}
\value{
  Returns a Java object reference (\code{jobjRef}) to the current Java
  callback engine.
}
\details{
  \code{.jengine} can be used to detect whether the engine was started
  or to start the engine.

  Before any callbacks from Java into R can be performed, the Java
  callback engine must be initialized, loading Java/R Interface
  (JRI). If JRI was not started and \code{start} is set to \code{TRUE}
  then \code{.jengine} will load necessary classes and start
  it.

  Note that JRI is an optional part of rJava and requires R shared
  library at the moment. By default rJava will continue with
  installation even if JRI cannot be built.
}
\seealso{
  \code{\link{.jcall}}
}
\examples{
\dontrun{
.jengine(TRUE)
}
}
\keyword{interface}
