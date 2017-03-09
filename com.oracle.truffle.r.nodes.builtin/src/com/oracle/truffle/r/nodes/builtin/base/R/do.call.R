do.call <- function(what, args, quote = FALSE, envir = parent.frame())
{
    if (!is.list(args))
		stop("second argument must be a list")
    .Internal(.fastr.do.call(what, args, quote, envir))
}