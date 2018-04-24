\name{jequals}
\alias{.jequals}
\alias{.jcompare}
\alias{!=,ANY,jobjRef-method}
\alias{!=,jobjRef,jobjRef-method}
\alias{!=,jobjRef,ANY-method}
\alias{==,ANY,jobjRef-method}
\alias{==,jobjRef,jobjRef-method}
\alias{==,jobjRef,ANY-method}
\alias{<,ANY,jobjRef-method}
\alias{<,jobjRef,jobjRef-method}
\alias{<,jobjRef,ANY-method}
\alias{>,ANY,jobjRef-method}
\alias{>,jobjRef,jobjRef-method}
\alias{>,jobjRef,ANY-method}
\alias{<=,ANY,jobjRef-method}
\alias{<=,jobjRef,jobjRef-method}
\alias{<=,jobjRef,ANY-method}
\alias{>=,ANY,jobjRef-method}
\alias{>=,jobjRef,jobjRef-method}
\alias{>=,jobjRef,ANY-method}

\title{
  Comparing Java References
}
\description{
  \code{.jequals} function can be used to determine whether two objects
  are equal. In addition, it allows mixed comparison of non-Java object
  for convenience, unless strict comparison is desired.

  The binary operators \code{==} and \code{!=} are mapped to
  (non-strict) call to \code{.jequals} for convenience.
  
  \code{.jcompare} compares two objects in the sense of the 
  \code{java.lang.Comparable} interface. 
  
  The binary operators \code{<}, \code{>}, \code{<=}, \code{>=} are mapped 
  to calls to \code{.jcompare} for convenience
}
\usage{
.jequals(a, b, strict = FALSE)
.jcompare( a, b )
}
\arguments{
  \item{a}{first object}
  \item{b}{second object}
  \item{strict}{when set to \code{TRUE} then non-references save for
    \code{NULL} are always treated as different, see details.}
}
\value{
 \code{.jequals} returns \code{TRUE} if both object 
 	are considered equal, \code{FALSE} otherwise.
 	
 \code{.jcompare} returns the result of the \code{compareTo} java method
 of the object a applied to b
}
\section{Methods}{
  \describe{
    \item{!=}{\code{signature(e1 = "ANY", e2 = "jobjRef")}: ... }
    \item{!=}{\code{signature(e1 = "jobjRef", e2 = "jobjRef")}: ... }
    \item{!=}{\code{signature(e1 = "jobjRef", e2 = "ANY")}: ... }
    \item{==}{\code{signature(e1 = "ANY", e2 = "jobjRef")}: ... }
    \item{==}{\code{signature(e1 = "jobjRef", e2 = "jobjRef")}: ... }
    \item{==}{\code{signature(e1 = "jobjRef", e2 = "ANY")}: ... }
    \item{<}{\code{signature(e1 = "ANY", e2 = "jobjRef")}: ... }
    \item{<}{\code{signature(e1 = "jobjRef", e2 = "jobjRef")}: ... }
    \item{<}{\code{signature(e1 = "jobjRef", e2 = "ANY")}: ... }
    \item{>}{\code{signature(e1 = "ANY", e2 = "jobjRef")}: ... }
    \item{>}{\code{signature(e1 = "jobjRef", e2 = "jobjRef")}: ... }
    \item{>}{\code{signature(e1 = "jobjRef", e2 = "ANY")}: ... }
    \item{>=}{\code{signature(e1 = "ANY", e2 = "jobjRef")}: ... }
    \item{>=}{\code{signature(e1 = "jobjRef", e2 = "jobjRef")}: ... }
    \item{>=}{\code{signature(e1 = "jobjRef", e2 = "ANY")}: ... }
    \item{<=}{\code{signature(e1 = "ANY", e2 = "jobjRef")}: ... }
    \item{<=}{\code{signature(e1 = "jobjRef", e2 = "jobjRef")}: ... }
    \item{<=}{\code{signature(e1 = "jobjRef", e2 = "ANY")}: ... }

	 }
}
\details{
	\code{.jequals} compares two Java objects by calling \code{equals}
  method of one of the objects and passing the other object as its
  argument. This allows Java objects to define the `equality' in
  object-dependent way.

  In addition, \code{.jequals} allows the comparison of Java object to
  other scalar R objects. This is done by creating a temporary Java
  object that corresponds to the R object and using it for a call to the
  \code{equals} method. If such conversion is not possible a warning is
  produced and the result it \code{FALSE}. The automatic conversion
  will be avoided if \code{strict} parameter is set to \code{TRUE}.

  \code{NULL} values in \code{a} or \code{b} are replaced by Java
  \code{null}-references and thus \code{.jequals(NULL,NULL)} is \code{TRUE}.

  If neither \code{a} and \code{b} are Java objects (with the exception
  of both being \code{NULL}) then the result is identical to that of
  \code{all.equal(a,b)}.
  
  Neither comparison operators nor \code{.jequals} supports vectors and
  returns \code{FALSE} in that case. A warning is also issued unless
  strict comparison was requested.
}
\note{
  Don't use \code{x == NULL} to check for
  \code{null}-references, because \code{x} could be \code{NULL} and thus
  the result would be an empty vector. Use \code{\link{is.jnull}}
  instead.
  (In theory \code{is.jnull} and \code{x == .jnull()} are the the same,
  but \code{is.jnull} is more efficient.)
}
\seealso{
  \code{\link{is.jnull}}
}
\examples{
\dontshow{.jinit()}
s <- .jnew("java/lang/String", "foo")
.jequals(s, "foo") # TRUE
.jequals(s, "foo", strict=TRUE) # FALSE - "foo" is not a Java object
t <- s
.jequals(s, t, strict=TRUE) # TRUE

s=="foo" # TRUE

\dontshow{ 
	stopifnot( 
		.jequals(s, "foo"), 
		!.jequals(s, "foo", strict=TRUE), 
		.jequals(s, t, strict=TRUE), 
		s == "foo"
	)
}

Double <- J("java.lang.Double")
d1 <- new( Double, 0.0 ) 
d2 <- new( Double, 1.0 )
d3 <- new( Double, 0.0 )

d1 < d2
d1 <= d3
d1 >= d3
d1 > d2

# cannot compare a Double and a String
try( d1 < "foo" )

# but can compare a Double and an Integer
d1 < 10L

\dontshow{
	stopifnot( 
		d1 < d2       ,
		d1 <= d3      ,
		d1 >= d3      ,
		! (d1 > d2 )  , 
		inherits( try( d1 < "foo", silent = TRUE ), "try-error" ), 
		d1 < 10L )
}


}
\keyword{interface}
