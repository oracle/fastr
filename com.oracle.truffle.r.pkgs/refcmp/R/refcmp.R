#' The refcmp packages is supposed to compare intermediate values to a dedicated reference run.
#'
#' @name refcmp
#' @examples
#' # Loading and setting up
#' 
#' library(refcmp)
#' snapshot.init()
#' 
#' # Taking a snapshot of variables or comparing the values to the reference run
#' a <- 1
#' b <- 2
#' snapshot(a, b)
#' 
#' # Taking a snapshot of expressions or comparing the values
#' # We need to name them
#' snapshot.named(c = 2 +3, d = a + b)
#' 
#' # Show the contents of the latest snapshot
#' snapshot.show()
#' 
#' # Show the contents of a snapshot using the ID (a sequence number)
#' snapshot.show(0)
#'
NULL
