\name{Exceptions}
\alias{Exceptions}
\alias{$.Throwable}
\alias{$<-.Throwable}
\title{Exception handling}
\description{R handling of java exception}
\usage{
	\S3method{$}{Throwable}(x, name )
	\S3method{$}{Throwable}(x, name ) <- value
}
\arguments{
	\item{x}{condition}
	\item{name}{...}
	\item{value}{...}
}
\details{
	Java exceptions are mapped to R conditions that are relayed by the
	\code{\link{stop}} function. 
	
	The R condition contains the actual exception object as the 
	\code{jobj} item.
	
	The class name of the R condition is made of a vector 
	of simple java class names, the class names without their package
	path. This allows the R code to use direct handlers similar to 
	direct exception handlers in java. See the example below. 
}
\examples{
\dontshow{.jinit()}

Integer <- J("java.lang.Integer")
tryCatch( Integer$parseInt( "10.." ), NumberFormatException = function(e){
	e$jobj$printStackTrace() 
} )

# the dollar method is also implemented for Throwable conditions, 
# so that syntactic sugar can be used on condition objects
# however, in the example below e is __not__ a jobjRef object reference
tryCatch( Integer$parseInt( "10.." ), NumberFormatException = function(e){
	e$printStackTrace() 
} )


\dontshow{
tryCatch( Integer$parseInt( "10.." ), NumberFormatException = function(e){
	classes <- class( e )
	stopifnot( "NumberFormatException" \%in\% classes )
	stopifnot( "Exception" \%in\% classes )
	stopifnot( "Object" \%in\% classes )
	stopifnot( "error" \%in\% classes )
	stopifnot( "condition" \%in\% classes )
} )
}

}
