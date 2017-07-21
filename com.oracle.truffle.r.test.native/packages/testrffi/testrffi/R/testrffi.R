.onUnload <- function(libpath)
	library.dynam.unload("testrffi", libpath)

rffi.dotCModifiedArguments <- function(data) {
	.C("dotCModifiedArguments", length(data), as.integer(data), as.double(data), as.logical(data))
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

rffi.preserve_object <- function() {
	.Call("preserve_object", PACKAGE = "testrffi")
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

rffi.coerceVector <- function(x, mode) {
	.Call("test_coerceVector", x, mode)
}

rffi.ATTRIB <- function(x) {
    .Call('test_ATTRIB', x);
}

rffi.getStringNA <- function() {
    .Call("test_stringNA")
}

rffi.captureDotsWithSingleElement <- function(env) {
    .Call('test_captureDotsWithSingleElement', env)
}
