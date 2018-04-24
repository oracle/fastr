\name{jpackage}
\alias{.jpackage}
\title{
  Initialize an R package containing Java code
}
\description{
  \code{.jpackage} initializes the Java Virtual Machine (JVM) for an R
  package. In addition to starting the JVM it also registers Java
  classes and native code contained in the package with the JVM.
  function must be called before any rJava functions can be used.
}
\usage{
.jpackage(name, jars='*', morePaths='', nativeLibrary=FALSE, lib.loc=NULL)
}
\arguments{
  \item{name}{name of the package. It should correspond to the
    \code{pkgname} parameter of \code{.onLoad} or \code{.First.lib}
    function.}
  \item{jars}{Java archives in the \code{java} directory of the package
    that should be added to the class path. The paths must be relative
    to package's \code{java} directory. A special value of
    \code{'*'} adds all \code{.jar} files form the \code{java} the
    directory.}
  \item{morePaths}{vector listing any additional entries that should
    be added to the class path.}
  \item{nativeLibrary}{a logical determining whether rJava should look
    for native code in the R package's shared object or not.}
  \item{lib.loc}{a character vector with path names of R libraries, or
    \code{NULL} (see \code{\link{system.file}} and examples below).}

}
\value{
  The return value is an invisible TRUE if the initialization was successful.  
}
\details{
  \code{.jpackage} initializes a Java R package as follows: first the
  JVM is initialized via \code{\link{.jinit}} (if it is not running
  already). Then the \code{java} directory of the package is added to
  the class path. Then \code{.jpackage} prepends \code{jars} with the
  path to the \code{java} directory of the package and adds them to the
  class path (or all \code{.jar} files if \code{'*'} was specified).
  Finally the \code{morePaths} parameter (if set) is passed to a call
  to \code{\link{.jaddClassPath}}.

  Therefore the easiest way to create a Java package is to add
  \code{.jpackage(pkgname, lib.loc=libname)} in \code{.onLoad} or
  \code{.First.lib}, and copy all necessary classes to a JAR file(s)
  which is placed in the \code{inst/java/} directory of the source
  package.

  If a package needs special Java parameters, \code{"java.parameters"}
  option can be used to set them on initialization. Note, however, that
  Java parameters can only be used during JVM initialization and other
  package may have intialized JVM already.
}
\seealso{
  \code{\link{.jinit}}
}
\examples{
\dontrun{
.onLoad <- function(libname, pkgname) {
  .jpackage(pkgname, lib.loc=libname)
}
}
}
\keyword{interface}
