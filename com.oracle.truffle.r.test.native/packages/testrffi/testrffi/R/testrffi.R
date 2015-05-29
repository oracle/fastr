add_int <- function(a, b) {
	.Call("add_int", as.integer(a), as.integer(b), PACKAGE = "testrffi")
}

add_double <- function(a, b) {
	.Call("add_double", as.double(a), as.double(b), PACKAGE = "testrffi")
}

createIntVector <- function(n) {
	.Call("createIntVector", as.integer(n), PACKAGE = "testrffi")
}

createExternalPtr <- function(addr, tag, prot) {
	.Call("createExternalPtr", as.integer(addr), tag, prot, PACKAGE = "testrffi")
}

getExternalPtrAddr <- function(eptr) {
	.Call("getExternalPtrAddr", eptr)
}

test_TYPEOF <- function(x) {
	.Call("test_TYPEOF", x, PACKAGE = "testrffi")
}
