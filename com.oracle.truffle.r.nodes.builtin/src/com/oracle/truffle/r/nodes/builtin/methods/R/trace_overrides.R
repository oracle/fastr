trace <- function(what, tracer, exit = NULL, at = numeric(), print = TRUE, signature = NULL, where = topenv(parent.frame()), edit = FALSE)
{
    if (nargs() == 1L) {
        return(.primTrace(what))
    } else {
        return(fastr::fastr.trace(what, tracer, exit, at, print, signature, where, edit))
    }
 }
