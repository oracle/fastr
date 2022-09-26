# Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

# Contains unit-tests of the individual R API functions

printCallPreffix <- function() {
    width <- 80L
    name <- substr(deparse(sys.call(-1), width)[[1L]], 1, width)
    cat(name, paste(rep('.', width + 3L - nchar(name)), collapse=''))
}

assertEquals <- function(expected, actual) {
    printCallPreffix()
    cat(if (identical(expected, actual)) 'pass' else 'fail', '\n')
}

assertTrue <- function(value) {
    printCallPreffix()
    cat(if (value) 'pass' else 'fail', '\n')
}

ignore <- function(...) {}

library(testrffi)


# ---------------------------------------------------------------------------------------
# Rf_allocSExp

assertTrue(is.environment(api.Rf_allocSExp(4))) # ENVSXP
assertTrue(is.pairlist(api.Rf_allocSExp(2))) # LISTSXP
assertTrue(is.function(api.Rf_allocSExp(3))) # CLOSXP
assertTrue(is.language(api.Rf_allocSExp(6))) # LANGSXP

# combined with SET_BODY, SET_FORMALS, SET_CLOENV
q <- 42
f2 <- function(a,b=2) a+b+q

f <- api.Rf_allocSExp(3)
# assertEquals("closure", typeof(f))
api.SET_BODY(f, api.BODY(f2))
api.SET_FORMALS(f, api.FORMALS(f2))
api.SET_CLOENV(f, api.CLOENV(f2))
assertEquals(42, f(-2))


# ---------------------------------------------------------------------------------------
# SET_ATTRIB

x <- c(1,3,10)
assertEquals(NULL, api.SET_ATTRIB(x, pairlist(names=c('a','b','q'))))
assertEquals(c('a','b','q'), names(x))

# there is no validation
x <- c(1,3,10)
assertEquals(NULL, api.SET_ATTRIB(x, as.pairlist(list(names=c('a','b')))))
assertEquals(c('a','b'), names(x))
# note: printing x on GNU-R causes segfault

# ---------------------------------------------------------------------------------------
# Rf_mkCharLenCE, note: last arg is encoding and 0 ~ native encoding

assertEquals("hello world", api.Rf_mkCharLenCE("hello world", 11, 0))
ignore("FastR bug", assertEquals("hello", api.Rf_mkCharLenCE("hello this will be cut away", 5, 0)))


# ----------------------------------------------------------------------------------------
# Rf_eval

bar <- function(x) typeof(x)
lang <- quote(bar(x))
lang[[2L]] <- as.pairlist(1)
# lang will be call to "bar" with a pairlist as the argument -- the pairlist is interpreted as the value
assertEquals("pairlist", api.Rf_eval(lang, new.env()))

lang[[2L]] <- quote(1)
# lang will be call to "bar" with "language(1)" as the argument -- the language object is "executed"
assertEquals("double", api.Rf_eval(lang, new.env()))

# ----------------------------------------------------------------------------------------
# Rf_asS4
v <- c(1,2,3)
assertEquals(0L, api.IS_S4_OBJECT(v))
v <- api.Rf_asS4(v, TRUE, 0)
# GnuR returns 16, FastR returns 1
assertTrue(api.IS_S4_OBJECT(v) > 0)

# ----------------------------------------------------------------------------------------
# PRENV: gives environment associated with given promise object
# tricky thing in FastR is that we attempt to optimize promises

getDotsPrenvC <- load.Call(function(env) '
    SEXP dots = Rf_findVar(install("..."), env);
    assert_equal_i(TYPEOF(CAR(dots)), 5); // PROMSXP
    return PRENV(CAR(dots));
')

getDotsPrenv <- function(...) getDotsPrenvC(environment())

assertEquals(globalenv(), getDotsPrenv(2L + 1L)) # expression
assertEquals(globalenv(), getDotsPrenv(var))     # variable lookup
assertEquals(globalenv(), getDotsPrenv(2L))      # constant
assertEquals(globalenv(), getDotsPrenv(bar()))   # call

# ----------------------------------------------------------------------------------------
# PRIMFUN: dispatches a call to a primitive function. This test uses the fact that FastR 
# accepts any R function, not only primite ones. There is no GNUR counterpart of this test 
# as it would be quite difficult to use PRIMFUN that is defined as a macro in Defn.h that
# is not available for non-internal packages, such as testrffi.

if (!is.null(version$engine) && version$engine=="FastR") {
	f1 <- function(x) x + 1
	res <- rffi.testPRIMFUN(f1, pairlist(1))
	assertEquals(2, res)
} else {
	# A fake assertion in GNU-R
	res <- 2
	assertEquals(2, res)
}

# ----------------------------------------------------------------------------------------
# .C downcall interface

dotC <- load.C(function(a = 'int*', b = 'double*') '
    assert_equal_i(a[0], 33);
    assert_equal_i(b[0], 2);
    a[0] = 42;
    b[0] = 42;
')

nontempA <- 33L
nontempB <- 2.0
res <- dotC(a = nontempA, b = nontempB)
assertEquals(list(a = 42L, b = 42), res) # result has names and correct values
assertEquals(33L, nontempA) # original vectors were not modified
assertEquals(2.0, nontempB) # original vectors were not modified

# non-vectors are passed as SEXPs to the .C and .Fortran interfaces
# some seem to be supported: environments, closures
# some produce deprecation warning: external pointers, symbols, expressions, language, ...
dotC <- load.C(function(x = 'void*', res = 'int*') 'res[0] = TYPEOF((SEXP) x);')
dummy <- 1L

res <- dotC(new.env(), res = dummy)
assertEquals(4L, res$res) # environment

res <- dotC(function() 42L, res = dummy)
assertEquals(3L, res$res) # function

# lists: data pointer is passed to the native function
dotC <- load.C(function(list = 'void*', res = 'int*') '
    res[0] = TYPEOF(((SEXP*) list)[0]); // type of the first SEXP in the list
')

dummy <- 1L
assertEquals(13L, dotC(list(2L), res = dummy)$res)
assertEquals(14L, dotC(list(0.5), res = dummy)$res)

# ----------------------------------------------------------------------------------------
# .Call downcall interface
dotCall <- load.Call(function(intv, realv, fun, str) '
    assert_equal_i(TYPEOF(intv), 13);
    assert_equal_i(TYPEOF(realv), 14);
    assert_equal_i(TYPEOF(fun), 3);
    assert_equal_i(TYPEOF(str), 16);
    if (INTEGER(intv)[0] == 42) return realv;
    else return str;
')

res <- dotCall(42L, 3.14, function() 42L, 'foo')
assertEquals(3.14, res)

res <- dotCall(1L, 3.14, function() 42L, 'foo')
assertEquals('foo', res)

# ----------------------------------------------------------------------------------------
# .External downcall interface
fun <- load.External('
    assert_equal_i(TYPEOF(args), 2 /*pairlist*/);
    assert_equal_i(TYPEOF(CAR(args)), 16 /*character: name of the C function*/);
    return args; // checked on the R side...
')
actual_args <- fun('bar', arg1 = 'foo', arg2 = 42L)
assertEquals('pairlist', typeof(actual_args))
assertEquals('character', typeof(actual_args[[1L]]))    # 1st argument should be the name of the native function
assertEquals(list('bar', arg1 = 'foo', arg2 = 42L), as.list(actual_args[-1L]))

# ----------------------------------------------------------------------------------------
# .External2 downcall interface
fun <- load.External2('
    SEXP res;
    PROTECT(res = allocVector(VECSXP, 4));
    SET_VECTOR_ELT(res, 0, call);
    SET_VECTOR_ELT(res, 1, op);
    SET_VECTOR_ELT(res, 2, args);
    SET_VECTOR_ELT(res, 3, rho);
    UNPROTECT(1);
    return res; // checked on the R side...
')

all_args <- fun('bar', arg1 = 'foo', arg2 = 42L)

# call: should be language object .External2(name, ...)
# ignored: FastR passes string "call" relying on that fact that it is useless anyone and no-one relies on that
ignore(assertEquals('language', typeof(all_args[[1L]])))
ignore(assertEquals(as.symbol('.External2'), all_args[[1L]][[1L]]))

assertEquals('builtin', typeof(all_args[[2L]]))  # op

assertEquals('pairlist', typeof(all_args[[3L]])) # args: the same as with .External
actual_args <- all_args[[3L]]
assertEquals('character', typeof(actual_args[[1L]]))    # 1st argument should be the name of the native function
assertEquals(list('bar', arg1 = 'foo', arg2 = 42L), as.list(actual_args[-1L]))

assertEquals('environment', typeof(all_args[[4L]])) # rho: calling environment

# ----------------------------------------------------------------------------------------
# Downcall interfaces: call via external ptr and via the NativeSymbolInfo -- requires native functions declated in a package
# Note: in case of GNU-R the external ptr is actually a pointer to R_RegisteredNativeSymbol, not to the C function itself
# In case of External and External2, the first object in the args pair-list is the handle: i.e. external ptr or list of class NativeSymbolInfo

fun <- function() 42L
for (handle in list(testrffi:::C_rapi_dotC$address, testrffi:::C_rapi_dotC)) {
    res <- .C(handle, arg1 = 42L, arg2 = 3.14, fun)
    assertEquals(list(arg1 = 1L, arg2 = 0.5, fun), res)
}

for (handle in list(testrffi:::C_rapi_dotCall$address, testrffi:::C_rapi_dotCall)) {
    res <- .Call(handle, arg1 = 42L, new.env())
    assertEquals(13L + 4L, res) # TYPEOF(int) + TYPEOF(env)
}

for (handle in list(testrffi:::C_rapi_dotExternal$address, testrffi:::C_rapi_dotExternal)) {
    args <- .External(handle, 'bar', arg1 = 'foo', arg2 = 42L)
    assertEquals('pairlist', typeof(args))
    assertEquals(list(handle, 'bar', arg1 = 'foo', arg2 = 42L), as.list(args))
}

for (handle in list(testrffi:::C_rapi_dotExternal2$address, testrffi:::C_rapi_dotExternal2)) {
    args <- .External2(handle, 'bar', arg1 = 'foo', arg2 = 42L)
    assertEquals('pairlist', typeof(args))
    assertEquals(list(handle, 'bar', arg1 = 'foo', arg2 = 42L), as.list(args))
}

# ----------------------------------------------------------------------------------------
# Rf_match
assertEquals(rffi.test_RfMatch(c("x", "y"), "x"), 1L)
assertEquals(rffi.test_RfMatch(c("x", "y"), "y"), 2L)
assertEquals(rffi.test_RfMatch(c("x", "y"), "foo"), NA_integer_)
assertEquals(rffi.test_RfMatch(c(), "foo"), NA_integer_)
assertEquals(rffi.test_RfMatch(c(), c("foo", "bar")), c(NA_integer_, NA_integer_))

# ----------------------------------------------------------------------------------------
# Rf_mkChar should not be garbage collected
gctorture(on = TRUE)
assertEquals(rffi.test_mkCharDoesNotCollect(), list("XX_YY", "XX_YY"))
gctorture(on = FALSE)

# ----------------------------------------------------------------------------------------
# Rf_allocArray
arr <- api.Rf_allocArray(13L, c(2L, 2L)) # INTSXP
assertEquals(4, length(arr))
assertEquals(c(2L, 2L), dim(arr))

# ----------------------------------------------------------------------------------------
# Set raw vector
assertEquals(rffi.test_setRRawVector(), as.raw(c(10, 20, 30)))
# Testing specific values of the raw vector returned by test_setRRawVector2 does not make sense,
# because it contains a pointer.
assertTrue(length(rffi.test_setRRawVector2()) > 0)
