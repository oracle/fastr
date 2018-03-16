# Some more tests that work with pairlists and related objects like '...' or language
stopifnot(require(testrffi))

# Note: GNU R returns the promise, FastR the value, we check that at least there are not exceptions
foo <- function(...) api.CAR(get('...'))
invisible(foo(a=3))
invisible(foo(a=4, b=6))

foo <- function(...) api.CDR(get('...'))
is.null(foo(a=3))
names(foo(a=4, b=6))
