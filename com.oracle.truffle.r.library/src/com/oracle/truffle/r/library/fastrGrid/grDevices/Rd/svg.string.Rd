\name{svg.string}
\alias{svg.string}
\title{Returns SVG code of the current plot.}
\usage{
svg.string()
}
\value{
Character vector with a single element.
}
\description{
Returns SVG code of the current plot.
\code{svg.string} can be invoked only when SVG device has been opened using \code{svg}.
Unlike \code{svg.off} this function does not close the SVG device.
}
\examples{
library(grid)
svg()
grid.rect()
print(svg.string())
}
\seealso{
\code{svg.off}
}

