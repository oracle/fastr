\name{loader}
\alias{.jaddClassPath}
\alias{.jclassPath}
\title{
  Java class loader
}
\description{
  \code{.jaddClassPath} adds directories or JAR files to the class
  path.

  \code{.jclassPath} returns a vector containg the current entries in
  the class path
}
\usage{
.jaddClassPath(path)
.jclassPath()
}
\arguments{
  \item{path}{character string vector listing the paths to add to the
    class path}
}
\value{
  \code{.jclassPath} returns a character vector listing the class path sequence.
}
%\details{
% 
%}
%\seealso{
%  \code{\link{.jpackage}}
%}
\examples{
\dontrun{
.jaddClassPath("/my/jars/foo.jar","/my/classes/")
print(.jclassPath())
}
}
\keyword{interface}
