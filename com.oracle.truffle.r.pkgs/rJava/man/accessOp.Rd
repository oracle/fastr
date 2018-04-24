\name{JavaAccess}
\alias{$,jobjRef-method}
\alias{$,jclassName-method}
\alias{$<-,jobjRef-method}
\alias{$<-,jclassName-method}
\alias{names,jobjRef-method}
\alias{names,jclassName-method}
\alias{names,jarrayRef-method}
\alias{names,jrectRef-method}
\alias{.DollarNames.jobjRef}
\alias{.DollarNames.jclassName}
\alias{.DollarNames.jarrayRef}
\alias{.DollarNames.jrectRef}

\title{
  Field/method operator for Java objects
}
\description{
  The \code{$} operator for \code{jobjRef} Java object references provides convenience access to object attributes and calling Java methods.
}
\usage{
	\S3method{.DollarNames}{jobjRef}   (x, pattern = "" )
	\S3method{.DollarNames}{jarrayRef} (x, pattern = "" )
	\S3method{.DollarNames}{jrectRef}  (x, pattern = "" )
	\S3method{.DollarNames}{jclassName}(x, pattern = "" )
}
\arguments{
	\item{x}{object to complete}
	\item{pattern}{pattern}
}
\section{Methods}{
  \describe{
    \item{\code{$}}{\code{signature(x = "jobjRef")}: ... }
    \item{\code{$}}{\code{signature(x = "jclassName")}: ... }
    \item{\code{$<-}}{\code{signature(x = "jobjRef")}: ... }
    \item{\code{$<-}}{\code{signature(x = "jclassName")}: ... }
    \item{\code{names}}{\code{signature(x = "jobjRef")}: ... }
    \item{\code{names}}{\code{signature(x = "jarrayRef")}: ... }
    \item{\code{names}}{\code{signature(x = "jrectRef")}: ... }
    \item{\code{names}}{\code{signature(x = "jclassName")}: ... }
	 }
}
\details{
  rJava provies two levels of API: low-level JNI-API in the form of \code{\link{.jcall}} function and high-level reflection API based on the \code{$} operator. The former is very fast, but inflexible. The latter is a convenient way to use Java-like programming at the cost of performance. The reflection API is build around the \code{$} operator on \code{\link{jobjRef-class}} objects that allows to access Java attributes and call object methods.

 \code{$} returns either the value of the attribute or calls a method, depending on which name matches first.

 \code{$<-} assigns a value to the corresponding Java attribute.

 \code{names} and \code{.DollarNames} returns all fields and methods associated with the object. 
 Method names are followed by \code{(} or \code{()} depending on arity. 
 This use of names is mainly useful for code completion, it is not intended to be used programmatically.

 This is just a convenience API. Internally all calls are mapped into \code{\link{.jcall}} calls, therefore the calling conventions and returning objects use the same rules. For time-critical Java calls \code{\link{.jcall}} should be used directly.
}
\seealso{
  \code{\link{J}}, \code{\link{.jcall}}, \code{\link{.jnew}}, \code{\link{jobjRef-class}}
}
\examples{
\dontshow{.jinit()}

v <- new(J("java.lang.String"), "Hello World!")
v$length()
v$indexOf("World")
names(v)

\dontshow{
stopifnot( v$length() == 12L )
stopifnot( v$indexOf("World") == 6L ) 
}

J("java.lang.String")$valueOf(10)

Double <- J("java.lang.Double")
# the class pseudo field - instance of Class for the associated class
# similar to java Double.class
Double$class 
\dontshow{
	stopifnot( Double$class$getName() == "java.lang.Double" )
}

}
\keyword{interface}
