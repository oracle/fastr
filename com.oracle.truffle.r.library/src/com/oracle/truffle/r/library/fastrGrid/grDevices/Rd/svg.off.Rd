\name{svg.off}
\alias{svg.off}
\title{Closes the SVG device and returns the SVG code as a character vector.}
\usage{
svg.off(which = dev.cur())
}
\arguments{
\item{which}{device number.}
}
\value{
Character vector with a single element.
}
\description{
The semantics is the same as for \code{dev.off} function from the graphics package,
except that \code{svg.off} does not write into any file and returns the SVG code as
a character vector. \code{svg.off} can be invoked only when SVG device has been opened
with \code{svg}.
}
\examples{
library(grid)
svg()
grid.rect()
print(svg.off())
}
\seealso{
\code{dev.off}
}

