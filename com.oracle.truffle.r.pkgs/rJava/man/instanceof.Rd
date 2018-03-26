\name{.jinstanceof}
\Rdversion{1.1}
\alias{\%instanceof\%}
\alias{.jinstanceof}
\title{
Is a java object an instance of a given java class
}
\description{
Is a java object an instance of a given java class
}
\usage{
o \%instanceof\% cl
.jinstanceof( o, cl )
}
\arguments{
  \item{o}{java object reference}
  \item{cl}{java class. This can be a character vector of length one
  giving the name of the class, or another java object, or an instance
  of the Class class, or a object of class \code{jclassName}.}
}
\value{
TRUE if o is an instance of cl
}
\author{
Romain Francois <francoisromain@free.fr>
}
\examples{
\dontshow{
.jinit()
}
Double <- J("java.lang.Double")
d <- new( Double, "10.2" )

# character
d \%instanceof\% "java.lang.Double"
d \%instanceof\% "java.lang.Number"

# jclassName
d \%instanceof\% Double

# instance of Class
Double.class <- Double@jobj
d \%instanceof\% Double.class

# other object
other.double <- new( Double, 10.2 )
d \%instanceof\% other.double

\dontshow{
% simple unit tests
stopifnot( d \%instanceof\% "java.lang.Double" )
stopifnot( d \%instanceof\% "java.lang.Number" )
stopifnot( d \%instanceof\% "java.lang.Object" )
stopifnot( d \%instanceof\% Double.class )
stopifnot( d \%instanceof\% other.double )
stopifnot( d \%instanceof\% Double )
}

}
\keyword{ interface }

