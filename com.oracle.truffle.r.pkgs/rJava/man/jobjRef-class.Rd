\name{jobjRef-class}
\docType{class}
\alias{jobjRef-class}

\title{Class "jobjRef" - Reference to a Java object }
\description{ This class describes a reference to an object held in a JavaVM. }
\section{Objects from the Class}{
Objects of this class should *not* be created directly. Instead, the function \code{\link{.jnew}} should be use to create new Java objects. They can also be created as results of the \code{\link{.jcall}} function.
}
\section{Slots}{
  \describe{
    \item{\code{jobj}:}{Internal identifier of the object (external pointer to be precise)}
    \item{\code{jclass}:}{Java class name of the object (in JNI notation)}
  }
  Java-side attributes are not accessed via slots, but the \code{$} operator instead.
}
\section{Methods}{
This object's Java methods are not accessed directly. Instead, \code{\link{.jcall}} JNI-API should be used for invoking Java methods. For convenience the \code{$} operator can be used to call methods via reflection API.
}
\author{ Simon Urbanek }
\seealso{
  \code{\link{.jnew}}, \code{\link{.jcall}}  or \code{\link{jarrayRef-class}}
}
\keyword{classes}
