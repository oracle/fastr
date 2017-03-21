# 'do.call' needs to be a function for S4 but we do not use 'enquote' for performance reasons
do.call <- function(what, args, quote = FALSE, envir = parent.frame())
{
    if (!is.list(args))
		stop("second argument must be a list")
    .Internal(.fastr.do.call(what, args, quote, envir))
}