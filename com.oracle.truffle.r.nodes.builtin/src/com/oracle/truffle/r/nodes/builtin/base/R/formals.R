formals <- function(fun = sys.function(sys.parent())) {
	if(is.character(fun))
		fun <- get(fun, mode = "function", envir = parent.frame())
	.Internal(formals(fun))
}

body <- function(fun = sys.function(sys.parent())) {
	if(is.character(fun))
		fun <- get(fun, mode = "function", envir = parent.frame())
	.Internal(body(fun))
}

alist <- function (...) as.list(sys.call())[-1L]

`body<-` <- function (fun, envir = environment(fun), value) {
	if (is.expression(value)) {
		if (length(value) > 1L)
			warning("using the first element of 'value' of type \"expression\"")
		value <- value[[1L]]
	}
	as.function(c(as.list(formals(fun)), list(value)), envir)
}

`formals<-` <- function (fun, envir = environment(fun), value)
{
	bd <- body(fun)
	as.function(c(value,
					if(is.null(bd) || is.list(bd)) list(bd) else bd),
			envir)
}
