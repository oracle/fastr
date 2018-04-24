\name{J}
\alias{J}
\title{
High level API for accessing Java
}
\description{
\code{J} creates a Java class reference or calls a Java method
}
\usage{
J(class, method, ...)
}
\arguments{
  \item{class}{
    java object reference or fully qualified class name in JNI
    notation (e.g "java/lang/String" ) or standard java notation (e.g
    "java.lang.String")
  }
  \item{method}{
    if present then \code{J} results in a method call, otherwise it
    just creates a class name reference.
  }
  \item{\dots}{
    optional parameters that will be passed to the method (if the
    \code{method} argument is present)
  }
}
\details{
\code{J} is the high-level access to Java.

If the \code{method} argument is missing then \code{code} must be a
class name and \code{J} creates a class name reference that can be
used either in a call to \code{new} to create a new Java object
(e.g. \code{new(J("java.lang.String"), "foo")}) or with \code{$}
operator to call a static method
(e.g. \code{J("java.lang.Double")$parseDouble("10.2")}.)

If the \code{method} argument is present then it must be a string
vector of length one which defines the method to be called on the
object.
}
\value{
  If \code{method} is missing the the returned value is an object of
  the class \code{jclassName}. Otherwise the value is the result of
  the method invocation. In the latter case Java exceptions may be
  thrown and the function doesn't return.
}
\note{
\code{J} is a high-level API which is slower than \code{\link{.jnew}}
or \code{\link{.jcall}} since it has to use reflection to find the
most suitable method.
}
\seealso{
\code{\link{.jcall}}, \code{\link{.jnew}}
}
\examples{
\dontshow{.jinit()}

if (!nzchar(Sys.getenv("NOAWT"))) {
  f <- new(J("java.awt.Frame"), "Hello")
  f$setVisible(TRUE)
}

J("java.lang.Double")$parseDouble("10.2")
J("java.lang.Double", "parseDouble", "10.2" )

Double <- J("java.lang.Double")
Double$parseDouble( "10.2")

# String[] strings = new String[]{ "string", "array" } ;
  strings <- .jarray( c("string", "array") )
# this uses the JList( Object[] ) constructor 
# even though the "strings" parameter is a String[] 
  l <- new( J("javax.swing.JList"), strings)

}
\keyword{interface}
