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
stopifnot(rffi.isNAString(strVec))
rffi.LENGTH(strVec)
# this will call CHAR(x) on the NA string, which materializes it to native pointer...
rffi.char_length(strVec)
strVec <- rffi.setStringElt(c('hello'), as.character(NA))
stopifnot(anyNA(strVec))
stopifnot(rffi.isNAString(as.character(NA)))

# Encoding tests
rffi.getBytes('\u1F602\n')
# ignored: FastR does not support explicit encoding yet
# latinEncStr <- '\xFD\xDD\xD6\xF0\n'
# Encoding(latinEncStr) <- "latin1"
# rffi.getBytes(latinEncStr)
rffi.getBytes('hello ascii')

x <- list(1)
attr(x, 'myattr') <- 'hello';
attrs <- rffi.ATTRIB(x)
stopifnot(attrs[[1]] == 'hello')
attr <- rffi.getAttrib(x, 'myattr')
stopifnot(attr == 'hello')

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

# parent.frame call in Rf_eval. Simulates pattern from rlang package
getCurrEnv <- function(r = parent.frame()) r
fn <- function(eval_fn) {
  list(middle(eval_fn), getCurrEnv())
}
middle <- function(eval_fn) {
  deep(eval_fn, getCurrEnv())
}
deep <- function(eval_fn, eval_env) {
  # the result value of rffi.tryEval is list, first element is the actual result
  eval_fn(quote(parent.frame()), eval_env)[[1]]
}
res <- fn(rffi.tryEval)
stopifnot(identical(res[[1]], res[[2]]))

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

# Stack introspection after Rf_eval
# Apparently parent.frame does not always give what sys.frame(sys.parent()) if the Rf_eval gets explicit environment != global env

testStackIntro <- function(doSysParents) {
    if (doSysParents) {
        cat("sys.parents(): ", paste0(sys.parents(), collapse=","), "\n")
    }
    cat("sys.frame(2):", paste0(ls(sys.frame(2)), collapse=","), "\n")
    cat("parent.frame():", paste0(ls(parent.frame()), collapse=","), "\n")
    cat("sys.nframe():", sys.nframe(), "\n")
    4242
}
rfEval <- function(expr, env, evalWrapperVar = 4422) .Call(testrffi:::C_api_Rf_eval, expr, env)
rfEval(quote(testStackIntro(T)), list2env(list(myenv=42)))
rfEval(quote(testStackIntro(T)), .GlobalEnv)

eval(quote(testStackIntro(T)), list2env(list(myenv=42)))
# TODO: sys.parents() give 0,1,2 in FastR instead of 0,1,0 in GNUR, but parent.frame works
eval(quote(testStackIntro(F)), .GlobalEnv)

# TODO: fix do.call in the same way
# testStackIntro <- function(doSysParents) {
#     cat("sys.parents(): ", paste0(sys.parents(), collapse=","), "\n")
#     cat("sys.frame(2):", paste0(ls(sys.frame(2)), collapse=","), "\n")
#     cat("parent.frame():", paste0(ls(parent.frame()), collapse=","), "\n")
#     cat("sys.nframe():", sys.nframe(), "\n")
#     4242
# }
#
# do.call(testStackIntro, list(T))
# do.call(testStackIntro, list(T), envir = list2env(list(myenv=42)))

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
preserved_objects <- list()
for(i in seq(5000)) {
    preserved_objects[[i]] <- rffi.preserve_object(i)
}

for(i in seq(5000)) {
    obj <- preserved_objects[[i]]
    stopifnot(obj == i)
    rffi.release_object(obj)
}

# Note: runif must not be used before this test so that it is still a promise!!!
# Following code calls Rf_eval with a language object that contains a promise instead of the expected function
set.seed(42)
rffi.RfEvalWithPromiseInPairList()

# CAR/CDR tests
rffi.CAR(NULL)
rffi.CDR(NULL)
invisible(rffi.CAR(as.symbol('a'))) # TODO: printing CHARSEXP not implemented in FastR

set.seed(42)
rffi.RfRandomFunctions()

rffi.RfRMultinom()
rffi.RfFunctions()

setAttrTarget <- c(1,2,3)
attr(setAttrTarget, 'myattr2') <- 'some value';
api.SET_ATTRIB(setAttrTarget, as.pairlist(list(myattr=42)))
setAttrTarget

typeof(api.ATTRIB(mtcars))
api.ATTRIB(structure(c(1,2,3), myattr3 = 33))

invisible(rffi.testDATAPTR('hello', testSingleString = T));
rffi.testDATAPTR(c('hello', 'world'), testSingleString = F);

# SET_OBJECT
# FastR does not fully support the SET_OBJECT fully,
# the test is left here in case there is a need to actually implement it.
x <- structure(3, class='abc')
# just to make sure tirivial SET_OBJECT examples work
api.SET_OBJECT(x, 1)
api.SET_OBJECT(c(1,2,3), 0)

## before SET_OBJECT(x,0), S3 dispatching works as expected:
# foo <- function(x) UseMethod('foo')
# foo.default <- function(x) cat("foo.default\n")
# foo.abc <- function(x) cat("foo.abc\n")
# as.character.abc <- function(...) "42"
# paste(x) # "42"
# foo(x) # "foo.abc"

# api.SET_OBJECT(x, 0) # FastR throws error saying that this is not implemented

## after SET_OBJECT(x,0), S3 dispatching does not work for internals
# paste(x) # "3" -- as.character.abc not called
# inherits(x, 'abc') # TRUE
# foo(x) # "foo.abc"

