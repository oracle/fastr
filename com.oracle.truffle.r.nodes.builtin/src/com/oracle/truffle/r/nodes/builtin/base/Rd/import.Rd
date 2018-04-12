% Generated by roxygen2: do not edit by hand
% Please edit documentation in R/fastrInteropt.R
\name{import}
\alias{import}
\title{Explicitly imports a polyglot symbol. Reading a symbol that does not exist will return NULL.
The returned symbol value can either be a polyglot object (e.g. a native
object from the other language) to support interoperability between languages, 
or a value converted into a native R type - e.g. integer, double, logical, character, etc.}
\usage{
import(symbolName)
}
\arguments{
\item{symbolName}{character string. The name of the symbol to search for.}
}
\description{
Explicitly imports a polyglot symbol. Reading a symbol that does not exist will return NULL.
The returned symbol value can either be a polyglot object (e.g. a native
object from the other language) to support interoperability between languages, 
or a value converted into a native R type - e.g. integer, double, logical, character, etc.
}
\examples{
import('foo')
}
\seealso{
\code{\link{export}}
}