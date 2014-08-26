add_int <- function(a, b) {
	.Call("add_int", as.integer(a), as.integer(b), PACKAGE = "testrffi")
}

add_double <- function(a, b) {
	.Call("add_double", as.double(a), as.double(b), PACKAGE = "testrffi")
}

createIntVector <- function(n) {
	.Call("createIntVector", as.integer(n), PACKAGE = "testrffi")
}
