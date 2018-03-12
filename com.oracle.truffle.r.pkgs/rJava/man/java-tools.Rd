\name{java-tools}
\alias{java-tools}
\title{java tools used internally in rJava}
\description{java tools used internally in rJava}
\examples{
\dontshow{
# running the java unit tests from the R examples
.jinit()
J("RJavaTools_Test")$runtests()
J("RJavaArrayTools_Test")$runtests()
J("ArrayWrapper_Test")$runtests()
J("RectangularArrayBuilder_Test")$runtests()


  p <- .jnew( "java/awt/Point" )
  classes <- .Call( "RgetSimpleClassNames", p@jobj, TRUE, PACKAGE = "rJava" )
  stopifnot( all( c( "Point", "Point2D", "Object", "error", "condition" ) \%in\% classes ) )
  classes <- .Call( "RgetSimpleClassNames", p@jobj, FALSE, PACKAGE = "rJava" )
  stopifnot( all( c( "Point", "Point2D", "Object" )  \%in\% classes ) )

}
}
