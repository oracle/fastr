#' GraalVM R package is supposed to demonstrate FastR performance and polyglot capabilities 
#' of GraalVM to GNUR users.
#'
#' @name graalvm
#' @examples
#' # Loading and setting up
#' 
#' library(graalvm)
#' graalvm.setup("~/work/graalvm-0.25")
#' 
#' # Code execution
#' g(v <- runif(1e3))
#' g(f <- function(x) { s <- 0; for (i in seq_along(x)) s <- s + x[[i]]; s })
#' g(system.time(f(v)))
#' 
#' g.js("1 < 2")
#' g.rb("$a = 2")
#' 
#' 
#' # Coupled variables
#' 
#' # Create and initialize coupled variables:
#' gset.r(a1, TRUE) 
#' gset.js(a2, c(1,2))
#' gset.rb(a3, list(a=1,b="2")) 
#' a1
#' a2
#' a3
#' g.r("a1")
#' g.js("a2")
#' g.rb("$a3")
#' 
#' g(a1 <- FALSE)
#' # Sync the local a1 with the remote a1
#' gget(a1)
#' a1
#' 
#' # Coupled functions
#' 
#' # Create a coupled function
#' gset.r(measure, function(n) { system.time(runif(n)) })
#' # Execute the local version of the coupled function
#' measure(1e8)
#' # Execute the remote version of the coupled function
#' g(measure(1e8)) # Try a couple of times
#' 
#' # Executing a script file
#' 
#' tmp <- tempfile()
#' writeLines(con = tmp, c("x<-10", "y<-x^2", "y"))
#' g.r(readLines(con=tmp))
#'
NULL
