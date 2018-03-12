\name{jclassName-class}
\docType{class}
\alias{jclassName-class}
\alias{as.character,jclassName-method}

\title{Class "jclassName" - a representation of a Java class name }
\description{ This class holds a name of a class in Java. }
\section{Objects from the Class}{
Objects of this class should *not* be created directly. Instead, the
function \code{\link{J}} should be used to create new objects of this class.
}
\section{Slots}{
  \describe{
    \item{\code{name}:}{Name of the class (in source code notation)}
    \item{\code{jobj}:}{Object representing the class in Java}
  }
}
\section{Methods}{
  The objects of class \code{jclassName} are used indirectly to be able
  to create new Java objects via \code{new} such as
  \code{new(J("java.lang.String"), "foo")} or to use the \code{$}
  convenience operator on static classes, such as
  \code{J("java.lang.Double")$parseDouble("10.2")}.

  \describe{
    \item{\code{as.character}}{\code{signature(x = "jclassName")}:
      returns the class name as a string vector of length one.
    }
  }
}
%\references{ ~put references to the literature/web site here ~ }
\author{ Simon Urbanek }
%\note{ ~~further notes~~ }
% ~Make other sections like Warning with \section{Warning }{....} ~
\seealso{
  \code{\link{J}}, \code{\link{new}}
}
%\examples{
%##---- Should be DIRECTLY executable !! ----
%}
\keyword{classes}
