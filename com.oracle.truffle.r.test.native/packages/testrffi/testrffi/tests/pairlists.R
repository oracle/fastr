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

# language objects treated as pairlists:
b <- ~fun(arg = val, arg2)

api.CAR(b) # symbol: `~`
api.TAG(b) # NULL
api.CDR(api.CDR(b)) # NULL
api.TAG(api.CDR(b)) # NULL

api.TAG(api.CDR(api.CDR(api.CAR(api.CDR(b))))) # TAG of arg2 -> NULL
api.CAR(api.CDR(api.CDR(api.CAR(api.CDR(b))))) # CAR of arg2 -> symbol arg2
api.TAG(api.CDR(api.CDR(api.CDR(b)))) # TAG(NULL) -> NULL

api.CAR(api.CDR(api.CAR(api.CDR(b)))) # symbol: val
api.TAG(api.CDR(api.CAR(api.CDR(b)))) # symbol: arg

api.TAG(as.symbol('a')) # TAG(symbol) -> NULL

g <- quote(fun(,42,'hello',3L,T,FALSE,...))

api.CAR(g) # symbol: fun
argspl <- api.CDR(g) # pairlist with the arguments
while (!is.null(argspl)) {
    print(api.TAG(argspl))
    print(api.CAR(argspl))
    print(typeof(api.CAR(argspl)))
    argspl <- api.CDR(argspl)
}

