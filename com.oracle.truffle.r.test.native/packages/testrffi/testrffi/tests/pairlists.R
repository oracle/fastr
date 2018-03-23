# Some more tests that work with pairlists and related objects like '...' or language
stopifnot(require(testrffi))

# Note: GNU R returns the promise, FastR the value, we check that at least there are not exceptions
foo <- function(...) api.CAR(get('...'))
invisible(foo(a=3))
invisible(foo(a=4, b=6))

foo <- function(...) api.CDR(get('...'))
is.null(foo(a=3))
names(foo(a=4, b=6))

someFunction <- function(a,b,c,d=100) a + b + d
l <- quote(someFunction(a=1,122,c=x+foo))
api.TYPEOF(l)
api.CAR(l)
api.CDR(l)
l
eval(l)
api.SET_TYPEOF(l, 2) # LISTSXP
l
eval(l)
api.SET_TYPEOF(l, 6) # LANGSXP

l <- pairlist(a=as.symbol("sum"), foo=123L,1233)
api.TYPEOF(l)
api.CAR(l)
api.CDR(l)
l
api.SET_TYPEOF(l, 6) # LANGSXP
l
eval(l)
