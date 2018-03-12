\name{jmemprof}
\alias{.jmemprof}
\title{
  rJava memory profiler
}
\description{
  \code{.jmemprof} enables or disables rJava memory profiling. If rJava
  was compiled without memory profiling support, then a call to this
  function always causes an error.
}
\usage{
.jmemprof(file = "-")
}
\arguments{
  \item{file}{file to write profiling information to or \code{NULL} to
    disable profiling}
}
\value{
  Returns \code{NULL}.
}
\details{
  The \code{file} parameter must be either a filename (which will be
  opened in append-mode) or "-" to use standard output or \code{NULL} to
  disable profiling. An empty string "" is equivalent to \code{NULL} in
  this context.

  Note that lots of finalizers are run only when R exists, so usually
  you want to enable profiling early and let R exit to get a sensible
  profile. Runninng gc may be helpful to get rid of references that can
  be collected in R.

  A simple perl script is provided to analyze the result of the
  profiler. Due to its simple text format, it is possible to capture
  entire stdout including the profiler information to have both the
  console context for the allocations and the profile. Memory profiling
  is also helful if rJava debug is enabled.

  Note that memory profiling support must be compiled in rJava and it is
  by default compiled only if debug mode is enabled (which is not the
  case by default).
}
\examples{
\donttest{
.jmemprof("rJava.mem.profile.txt")
}
}
\keyword{interface}
