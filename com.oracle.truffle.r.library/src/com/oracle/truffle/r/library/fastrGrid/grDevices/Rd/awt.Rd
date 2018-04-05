\name{awt}
\alias{awt}
\title{Opens the AWT device, which is an interactive window.}
\usage{
awt(width = NULL, height = NULL, graphicsObj = NULL)
}
\arguments{
\item{width}{window width in AWT units (~pixel).}
\item{height}{window height in AWT units (~pixel).}
\item{java.awt.graphicsObj}{Java object of type \code{java.awt.Graphics2D}.}
}
\value{
Invisible NULL.
}
\description{
The AWT device draws into given Java \code{java.awt.Graphics2D} object.
If \code{NULL} is given as \code{graphicsObj}, then the function creates a
window whose \code{Graphics2D} is used, i.e. plots will be drawn into the window.
In FastR \code{X11} is aliased to \code{awt}.
}
\examples{
awt()
}
\seealso{
\code{X11}
}
