\name{toJava}
\alias{toJava}
\title{
Convert R objects to REXP references in Java
}
\description{
\code{toJava} takes an R object and creates a reference to that object
in Java. This reference can then be passed to Java methods such taht
they can refer to it back in R. This is commonly used to pass functions
to Java such that Java code can call those functions later.
}
\usage{
toJava(x, engine = NULL)
}
\arguments{
  \item{x}{R object to reference. It can be any R object and it will be
    retained at least for the duration of the reference on the Java side.}
  \item{engine}{REngine in which the reference is to be created. If
    <code>null</code> then the last created engine is used. This must be
    a Java object and a subclass of org.rosuda.REngine (and NOT the old
    org.rosuda.JRI.Rengine!).
    }
}
%\details{
%}
\value{
  There result is a Java reference (\code{jobjRef}) of the Java class
  \code{REXPReference}.
}
\examples{
\dontrun{
  .jinit()
  # requires JRI and REngine classes
  .jengine(TRUE)
  f <- function() { cat("Hello!\n"); 1 }
  fref <- toJava(f)
  # to use this in Java you would use something like:
  # public static REXP call(REXPReference fn) throws REngineException, REXPMismatchException {
  #	 return fn.getEngine().eval(new REXPLanguage(new RList(new REXP[] { fn })), null, false);
  # }
  # .jcall("Call","Lorg/rosuda/REngine/REXP;","call", fref)
  }
}
\keyword{interface}
