stopifnot(require(testrffi))

rffi.addInt(2L, 3L)
rffi.addDouble(2, 3)
rffi.populateIntVector(5)
rffi.populateLogicalVector(5)
rffi.mkStringFromChar()
rffi.mkStringFromBytes()
rffi.null()
try(rffi.null.E())
rffi.null.C()
rffi.isRString(character(0))
a <- c(1L,2L,3L); rffi.iterate_iarray(a)
a <- c(1L,2L,3L); rffi.iterate_iptr(a)
rffi.dotCModifiedArguments(c(0,1,2,3))
rffi.dotExternalAccessArgs(1L, 3, c(1,2,3), c('a', 'b'), 'b', TRUE, as.raw(12))
rffi.dotExternalAccessArgs(x=1L, 3, c(1,2,3), y=c('a', 'b'), 'b', TRUE, as.raw(12))
rffi.invoke12()
rffi.TYPEOF(3L)
rffi.isRString("hello")
rffi.isRString(NULL)
rffi.interactive()
x <- 1; rffi.findvar("x", globalenv())
x <- "12345"; rffi.char_length(x)

strVec <- rffi.getStringNA();
stopifnot(anyNA(strVec))

x <- list(1)
attr(x, 'myattr') <- 'hello';
attrs <- rffi.ATTRIB(x)
stopifnot(attrs[[1]] == 'hello')

# loess invokes loess_raw native function passing in string value as argument and that is what we test here.
loess(dist ~ speed, cars);

# code snippet that simulates work with promises ala rlang package
tmp <- c(1,2,4)
some_unique_name <- TRUE
foo <- function(...) { tmp <- 'this is not the right tmp'; bar(); }
bar <- function() rffi.captureDotsWithSingleElement(parent.frame())
promiseInfo <- foo(tmp)
stopifnot('some_unique_name' %in% ls(promiseInfo[[2]]))
eval(promiseInfo[[1]], promiseInfo[[2]])

# fiddling the pointers to the native arrays: we get data pointer to the first SEXP argument (vec),
# then put value 42/TRUE directly into it at index 0,
# value of symbol 'myvar' through Rf_eval at index 1,
# value of Rf_eval('max(vec)') at the last index (note that the upcall now should take max from the updated vector!)
env <- new.env()
env$myvar <- 44L;
rffi.evalAndNativeArrays(c(1L, 2L, 3L, 4L, 5L), as.symbol('myvar'), env);

env$myvar <- 3.14
rffi.evalAndNativeArrays(c(1.1, 2.2, 3), as.symbol('myvar'), env);

env$myvar <- T
rffi.evalAndNativeArrays(c(F, F, F, F), as.symbol('myvar'), env);

env$myvar <- 20L
rffi.evalAndNativeArrays(as.raw(c(1, 3, 2)), as.symbol('myvar'), env);

# legth tests
env <- new.env(); env$a <- 42; env$b <- 44;
rffi.inlined_length(env)
rffi.inlined_length(c(1,2,3))
rffi.inlined_length(list(a = 1, b = 42))
rffi.inlined_length(as.pairlist(c(1,2,3,4,5)))
expr <- expression(x + y, 3)
rffi.inlined_length(expr)
rffi.inlined_length(expr[[1]])

# fails in FastR because DotCall class cannot recognize that the RArgsValuesAndNames
# are not meant to be extracted into individual arguments, but instead send as is
# to the native function as SEXP
#
# foo <-function(...) rffi.inlined_length(get('...'))
# foo(a = 1, b = 2, c = 3, d = 42)

rffi.parseVector('1+2')
rffi.parseVector('.*/-')
rffi.parseVector('1+')

# preserve and release object
# using loop to trigger compilation
for(i in seq(5000)) {
    rffi.preserve_object()
}
for(i in seq(5000)) {
    rffi.release_object()
}