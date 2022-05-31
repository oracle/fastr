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

.onUnload <- function(libpath)
	library.dynam.unload("testrffi", libpath)

rffi.dotCModifiedArguments <- function(data) {
	.C("dotCModifiedArguments", length(data), as.integer(data), as.double(data), as.logical(data), as.character(data))
}

rffi.addInt <- function(a, b) {
	.Call("addInt", as.integer(a), as.integer(b), PACKAGE = "testrffi")
}

rffi.addDouble <- function(a, b) {
	.Call("addDouble", as.double(a), as.double(b), PACKAGE = "testrffi")
}

rffi.populateIntVector <- function(n) {
	.Call("populateIntVector", as.integer(n), PACKAGE = "testrffi")
}

rffi.populateCharacterVector <- function(n) {
	.Call("populateCharacterVector", as.integer(n), PACKAGE = "testrffi")
}

rffi.populateDoubleVector <- function(n) {
	.Call("populateDoubleVector", as.integer(n), PACKAGE = "testrffi")
}

rffi.populateComplexVector <- function(n) {
	.Call("populateComplexVector", as.integer(n), PACKAGE = "testrffi")
}

rffi.populateRawVector <- function(n) {
	.Call("populateRawVector", as.integer(n), PACKAGE = "testrffi")
}

rffi.populateLogicalVector <- function(n) {
	.Call("populateLogicalVector", as.integer(n), PACKAGE = "testrffi")
}

rffi.createExternalPtr <- function(addr, tag, prot) {
	.Call("createExternalPtr", as.integer(addr), tag, prot, PACKAGE = "testrffi")
}

rffi.getExternalPtrAddr <- function(eptr) {
	.Call("getExternalPtrAddr", eptr)
}

rffi.TYPEOF <- function(x) {
	.Call("invoke_TYPEOF", x, PACKAGE = "testrffi")
}

rffi.error <- function(msg = "invoke_error in testrffi") {
	.Call("invoke_error", msg, PACKAGE = "testrffi")
}

rffi.dotExternalAccessArgs <- function(...) {
	.External("dot_external_access_args", ..., PACKAGE = "testrffi")
}

rffi.invokeFun <- function(vec, fun) {
	.C('invoke_fun', vec, length(vec), fun)
}
    
rffi.isRString <- function(s) {
	.Call("invoke_isString", s, PACKAGE = "testrffi")
}

rffi.invoke12 <- function() {
	.Call("invoke12", 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, PACKAGE = "testrffi")
}

rffi.interactive <- function() {
	.Call("interactive", PACKAGE = "testrffi");
}

rffi.tryEval <- function(expr, env) {
	.Call("tryEval", expr, env, PACKAGE = "testrffi")
}

rffi.rhome_dir <- function() {
	.Call("rHomeDir", PACKAGE = "testrffi")
}

rffi.upcalled <- function(v) {
	gc()
	.Call("nestedCall2", PACKAGE = "testrffi", v)
}

rffi.nested.call1 <- function() {
	upcall <- quote(rffi.upcalled(v))
	v <- c(10L, 20L, 30L)
	env <- new.env()
	assign("v", v, env)
	.Call("nestedCall1", PACKAGE = "testrffi", upcall, env)
}

rffi.r_home <- function() {
	.Call("r_home", PACKAGE = "testrffi")
}

rffi.char_length <- function(x) {
	.Call("char_length", PACKAGE = "testrffi", x)
}

rffi.mkStringFromChar <- function() {
	.Call("mkStringFromChar", PACKAGE = "testrffi")
}

rffi.mkStringFromBytes <- function() {
	.Call("mkStringFromBytes", PACKAGE = "testrffi")
}

rffi.iterate_iarray <- function(x) {
	.Call("iterate_iarray", x, PACKAGE = "testrffi")
}

rffi.iterate_iptr <- function(x) {
	.Call("iterate_iptr", x, PACKAGE = "testrffi")
}

rffi.preserve_object <- function(v) {
	.Call("preserve_object", v, PACKAGE = "testrffi")
}

rffi.release_object <- function(x) {
	invisible(.Call("release_object", x, PACKAGE = "testrffi"))
}

rffi.findvar <- function(x, env) {
	if (is.character(x)) {
		x = as.symbol(x)
	}
	.Call("findvar", x, env, PACKAGE = "testrffi")
}

rffi.null <- function() {
	.Call("null", PACKAGE = "testrffi")
}

rffi.null.E <- function() {
	.Call("null", PACKAGE = "foo")
}

rffi.null.C <- function() {
	.Call(C_null)
}

rffi.asReal <- function(x) {
	.Call("test_asReal", x)
}

rffi.asInteger <- function(x) {
	.Call("test_asInteger", x)
}

rffi.asLogical <- function(x) {
	.Call("test_asLogical", x)
}

rffi.asChar <- function(x) {
	.Call("test_asChar", x)
}

rffi.CAR <- function(x) {
	.Call("test_CAR", x)
}

rffi.CDR <- function(x) {
	.Call("test_CDR", x)
}

rffi.LENGTH <- function(x) {
	.Call("test_LENGTH", x)
}

rffi.inlined_length <- function(x) {
    .Call("test_inlined_length", x)
}

rffi.coerceVector <- function(x, mode) {
	.Call("test_coerceVector", x, mode)
}

rffi.ATTRIB <- function(x) {
    .Call('test_ATTRIB', x);
}

rffi.getAttrib <- function(source, name) {
    .Call('test_getAttrib', source, name);
}

rffi.getStringNA <- function() {
    .Call("test_stringNA")
}

rffi.setStringElt <- function(x,y) {
	.Call("test_setStringElt", x, y)
}

rffi.captureDotsWithSingleElement <- function(env) {
    .Call('test_captureDotsWithSingleElement', env)
}

rffi.evalAndNativeArrays <- function(vec, expr, env) {
    .Call('test_evalAndNativeArrays', vec, expr, env)
}

rffi.writeConnection <- function(connection) {
    .Call('test_writeConnection', connection);
}

rffi.readConnection <- function(connection) {
    .Call('test_readConnection', connection);
}

rffi.createNativeConnection <- function() {
    .Call('test_createNativeConnection');
}

rffi.parseVector <- function(x) {
    .Call('test_ParseVector', x);
}

rffi.test_lapply <- function(a, fn, env) {
	.Call("test_lapply", a, fn, env)
}

rffi.test_RfFindFunAndRfEval <- function(x, y) {
    .Call('test_RfFindFunAndRfEval', x, y)
}

rffi.RfEvalWithPromiseInPairList <- function() {
    .Call('test_RfEvalWithPromiseInPairList')
}

rffi.isNAString <- function(x) {
	.Call('test_isNAString', x)
}

rffi.getBytes <- function(x) {
	.Call('test_getBytes', x)
}

rffi.RfRandomFunctions <- function() {
	.Call('test_RfRandomFunctions')
}

rffi.RfRMultinom <- function() {
	.Call('test_RfRMultinom')
}

rffi.RfFunctions <- function() {
	.Call('test_RfFunctions')
}

rffi.testDATAPTR <- function(strings, testSingleString) {
	.Call('test_DATAPTR', strings, testSingleString)
}

rffi.test_duplicate <- function(val, deep) {
	.Call('test_duplicate', val, deep)
}

rffi.test_R_nchar <- function(x) {
	.Call('test_R_nchar', x)
}

rffi.test_forceAndCall <- function(call, args, rho) {
	.Call('test_forceAndCall', call, args, rho)
}

rffi.test_constantTypes <- function(env) {
    .Call('test_constant_types')
}

rffi.shareIntElement <- function(x, xi, y, yi) {
	.Call('shareIntElement', x, xi, y, yi)
}

rffi.shareDoubleElement <- function(x, xi, y, yi) {
	.Call('shareDoubleElement', x, xi, y, yi)
}

rffi.shareStringElement <- function(x, xi, y, yi) {
	.Call('shareStringElement', x, xi, y, yi)
}

rffi.shareListElement <- function(x, xi, y, yi) {
	.Call('shareListElement', x, xi, y, yi)
}

rffi.test_setVar <- function(symbol, value, env) {
    .Call('test_Rf_setVar', symbol, value, env)
}

rffi.test_setAttribDimDoubleVec <- function(vec, dimDoubleVec) {
    .Call('test_Rf_setAttribDimDoubleVec', vec, dimDoubleVec)
}

rffi.test_sort_complex <- function(complexVec) {
    .Call('test_sort_complex', complexVec)
}

rffi.testMultiSetProtection <- function() {
    .Call('testMultiSetProtection')
}

rffi.get_dataptr <- function(x) .Call('get_dataptr', x)

rffi.benchRf_isNull <- function(n) {
	.C("benchRf_isNull", as.integer(n))
}

rffi.benchMultipleUpcalls <- function(x) {
    .Call('benchMultipleUpcalls', x)
}

rffi.benchProtect <- function(x, n) {
    .Call('benchProtect', x, n)
}

rffi.test_lapplyWithForceAndCall <- function(list, fn, fa, ...) {
    .Call('test_lapplyWithForceAndCall', list, fn, fa, environment())
}

rffi.testMissingArgWithATTRIB <- function() {
    .Call('testMissingArgWithATTRIB')
}

rffi.testPRIMFUN <- function(fun, args) {
    .Call('testPRIMFUN', fun, args)
}

rffi.test_trace <- function(a, b) {
    .Call("testTrace")
}

rffi.div_zero <- function() {
    .Call("testdiv", 0)
}

rffi.serialize <- function(object) {
    .Call("serialize", object)
}

rffi.testInstallTrChar <- function(strvec, envir) {
	.Call("testInstallTrChar", strvec, envir, PACKAGE = "testrffi")
}

rffi.test_RfMatch <- function(x, y) {
	.Call("test_RfMatch", x, y)
}

rffi.test_mkCharDoesNotCollect <- function() {
	.Call("test_mkCharDoesNotCollect")
}

rffi.test_setRRawVector <- function() {
	.Call("test_setRRawVector")
}

rffi.test_setRRawVector2 <- function() {
	.Call("test_setRRawVector2")
}
