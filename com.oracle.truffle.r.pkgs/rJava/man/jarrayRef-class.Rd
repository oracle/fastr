\name{jarrayRef-class}
\docType{class}
\alias{jarrayRef-class}
\alias{[,jarrayRef-method}
\alias{[[,jarrayRef-method}
\alias{[[<-,jarrayRef-method}
\alias{head,jarrayRef-method}
\alias{tail,jarrayRef-method}
\alias{length,jarrayRef-method}
\alias{str,jarrayRef-method}
\alias{unique,jarrayRef-method}
\alias{duplicated,jarrayRef-method}
\alias{anyDuplicated,jarrayRef-method}
\alias{sort,jarrayRef-method}
\alias{rev,jarrayRef-method}
\alias{min,jarrayRef-method}
\alias{max,jarrayRef-method}
\alias{range,jarrayRef-method}

\title{Class "jarrayRef" Reference to an array Java object }
\description{ This class is a subclass of \link{jobjRef-class} 
and represents a reference to an array Java object. }
\section{Objects from the Class}{
Objects cannot be created directly, but only as the return
value of \code{\link{.jcall}} function.
}
\section{Slots}{
  \describe{
    \item{\code{jsig}:}{JNI signature of the array type}
    \item{\code{jobj}:}{Internal identifier of the object}
    \item{\code{jclass}:}{Inherited from \code{jobjRef}, but unspecified}
  }
}
\section{Methods}{
  \describe{
  	  \item{[}{\code{signature(x = "jarrayRef")}: \emph{not yet implemented} }
    \item{[[}{\code{signature(x = "jarrayRef")}: R indexing of java arrays }
    \item{[[<-}{\code{signature(x = "jarrayRef")}: replacement method }
    \item{\code{head}}{\code{signature(x = "jarrayRef")}: head of the java array }
    \item{\code{tail}}{\code{signature(x = "jarrayRef")}: tail of the java array }
    \item{length}{\code{signature(object = "jarrayRef")}: Number of java objects in the java array }
    \item{str}{\code{signature(object = "jarrayRef")}: ... }
    \item{unique}{\code{signature(x = "jarrayRef")}: \emph{not yet implemented} }
    \item{duplicated}{\code{signature(x = "jarrayRef")}: \emph{not yet implemented} }
    \item{anyDuplicated}{\code{signature(x = "jarrayRef")}: \emph{not yet implemented} }
    \item{sort}{\code{signature(x = "jarrayRef")}: \emph{not yet implemented} }
    \item{rev}{\code{signature(x = "jarrayRef")}: \emph{not yet implemented} }
    \item{min}{\code{signature(x = "jarrayRef")}: \emph{not yet implemented} }
    \item{max}{\code{signature(x = "jarrayRef")}: \emph{not yet implemented} }
    \item{range}{\code{signature(x = "jarrayRef")}: \emph{not yet implemented} }
  }
}
\section{Extends}{
Class \code{"\linkS4class{jobjRef}"}, directly.
}
\author{ Simon Urbanek }
\seealso{
  \code{\link{.jcall}} or \code{\linkS4class{jobjRef}}
  \code{\linkS4class{jrectRef}} for rectangular arrays
}
% need to find examples of rugged arrays
% \examples{
% \dontshow{.jinit()}
% }
\keyword{classes}
