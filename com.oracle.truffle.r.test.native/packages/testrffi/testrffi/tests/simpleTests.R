# Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 3 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 3 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 3 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
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
# See issue GR-9928
# x <- "12345"; rffi.char_length(x)

rffi.test_duplicate(quote(a[,3])[[3]], 1L) # try duplicating empty symbol

strVec <- rffi.getStringNA();
stopifnot(anyNA(strVec))
stopifnot(rffi.isNAString(strVec))
rffi.LENGTH(strVec)
# See issue GR-9928
# this will call CHAR(x) on the NA string, which materializes it to native pointer...
# rffi.char_length(strVec)
strVec <- rffi.setStringElt(c('hello'), as.character(NA))
stopifnot(anyNA(strVec))
stopifnot(rffi.isNAString(as.character(NA)))

# See issue GR-9928
	# Encoding tests
	# rffi.getBytes('\u1F602\n')
	# ignored: FastR does not support explicit encoding yet
	# latinEncStr <- '\xFD\xDD\xD6\xF0\n'
	# Encoding(latinEncStr) <- "latin1"
	# rffi.getBytes(latinEncStr)
	#rffi.getBytes('hello ascii')

x <- list(1)
attr(x, 'myattr') <- 'hello';
attrs <- rffi.ATTRIB(x)
stopifnot(attrs[[1]] == 'hello')
attr <- rffi.getAttrib(x, 'myattr')
stopifnot(attr == 'hello')

# Enable when GR-9876 is fixed
if (Sys.getenv("FASTR_RFFI") != "llvm") {
	# loess invokes loess_raw native function passing in string value as argument and that is what we test here.
	loess(dist ~ speed, cars);
}

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

# length tests
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

# Enable when GR-10914 is fixed
if (Sys.getenv("FASTR_RFFI") != "llvm") {
	testLength <- function(type) {
    	s <- api.Rf_allocVector(type, 1000) 
	    print(api.LENGTH(s))
	    print(api.TRUELENGTH(s))

    	api.SETLENGTH(s, 10)
	    print(api.LENGTH(s))
	    print(api.TRUELENGTH(s))

	    api.SET_TRUELENGTH(s, 1000)
	    print(api.LENGTH(s))
	    print(api.TRUELENGTH(s))
	}
	testLength(10) # LGLSXP
	testLength(13) # INTSXP
	testLength(14) # REALSXP
	testLength(15) # CPLXSXP
	testLength(16) # STRSXP
	testLength(19) # VECSXP

	svec <- c("a")
	charsxp <- api.STRING_ELT(svec, 0)
	api.LENGTH(charsxp)
	# gnur returns different value
	# api.TRUELENGTH(charsxp)
	api.SET_TRUELENGTH(charsxp, 1000)
	api.LENGTH(charsxp)
	api.TRUELENGTH(charsxp)

	# gnur returns different value
	# api.LEVELS(charsxp)

	identical(charsxp, api.STRING_ELT(c("a"), 0))
}

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

api.ATTRIB(data.frame(1, 2, 3))

invisible(rffi.testDATAPTR('hello', testSingleString = T));
# See issue GR-9928
# rffi.testDATAPTR(c('hello', 'world'), testSingleString = F);

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

## The following set/get semantics does not work in FastR as the scalar value is
## always transformed into a NEW string vector before passing it to the native function.
#svec <- "a"
#api.SETLEVELS(svec, 1)
#api.LEVELS(svec)

svec <- c("a", "b")
api.SETLEVELS(svec, 1)
api.LEVELS(svec)

env <- new.env()
env2 <- new.env()
env2$id <- "enclosing"
api.SET_ENCLOS(env, env2)
api.ENCLOS(env)$id == "enclosing"

rffi.test_R_nchar("ffff")

f1 <- function(x,y) { print("f1"); x^y }
f2 <- function(z) { print("f2"); z }
ll <- quote(f1(2, f2(3)))
rffi.test_forceAndCall(ll, 0, .GlobalEnv)
rffi.test_forceAndCall(ll, 2, .GlobalEnv)

f1 <- function(x, y, ...) { print("f1"); vars <- list(...); print(vars); x^y }
f2 <- function(z) { print("f2"); z }
f3 <- function(s) { print("f3"); s }
ll <- quote(f1(2, f2(3), ...))
testForceAndCallWithVarArgs <- function (n, ...) {
	rffi.test_forceAndCall(ll, n, environment())
}
testForceAndCallWithVarArgs(0, f3("aaa"))
testForceAndCallWithVarArgs(3, f3("aaa"))

x <- c(1)
api.Rf_isObject(x)
class(x) <- "c1"
api.Rf_isObject(x)

# prints R types of C constants like R_NamesSymbol
rffi.test_constantTypes()

# findVarInFrame for "..." that is empty gives symbol for missing, i.e. ""
foo <- function(...) rffi.findvar('...', environment())
typeof(foo())
foo()

# findVarInFrame for empty argument gives symbol for missing, i.e. ""
foo <- function(x) rffi.findvar('x', environment())
typeof(foo())
foo()

# active bindings
f <- local( {
	x <- 1
    function(v) {
    	if (missing(v))
        	cat("get\n")
		else {
        	cat("set\n")
            x <<- v
		}
		x
    }
})
api.R_MakeActiveBinding(as.symbol("fred"), f, .GlobalEnv)
bindingIsActive("fred", .GlobalEnv)
fred
fred <- 2

# sharing elements in native data
x <- c("abc")
y <- c("xyz")
# x[0] = y[0]
rffi.shareStringElement(x, 1L, y, 1L) 

l1 <- list(1:2, c("a", "b"))
l2 <- list(3:4, c("c", "d"))
rffi.shareListElement(l1, 1L, l2, 1L)
rffi.shareListElement(l1, 1L, l2, 2L)

i1 <- c(1L, 2L)
i2 <- c(3L, 4L)
rffi.shareIntElement(i1, 1L, i2, 2L)

d1 <- c(1, 2)
d2 <- c(3, 4)
rffi.shareDoubleElement(d1, 1L, d2, 2L)

# setVar
e <- new.env()
e$x <- 1
rffi.test_setVar(as.symbol('x'), 42, e)
stopifnot(identical(e$x, 42))
rffi.test_setVar(as.symbol('y'), 42, e)
stopifnot(identical(e$y, NULL))
stopifnot(identical(globalenv()$y, 42))

v <- c(1:6)
d <- c(2.0, 3.0)
rffi.test_setAttribDimDoubleVec(v, d)
print(dim(v))

# Complex vectors
x <- c(4+3i,2+1i)
rffi.test_sort_complex(x)
