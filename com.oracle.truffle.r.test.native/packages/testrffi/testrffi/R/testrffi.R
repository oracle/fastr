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

rffi.createExternalPtr <- function(addr, tag, prot) {
	.Call("createExternalPtr", as.integer(addr), tag, prot, PACKAGE = "testrffi")
}

rffi.getExternalPtrAddr <- function(eptr) {
	.Call("getExternalPtrAddr", eptr)
}

rffi.TYPEOF <- function(x) {
	.Call("invoke_TYPEOF", x, PACKAGE = "testrffi")
}

rffi.error <- function() {
	.Call("invoke_error", PACKAGE = "testrffi")
}

rffi.dotExternalAccessArgs <- function(...) {
	.External("dot_external_access_args", ..., PACKAGE = "testrffi")
}

rffi.isRString <- function(s) {
	.Call("invoke_isString", s, PACKAGE = "testrffi")
}
