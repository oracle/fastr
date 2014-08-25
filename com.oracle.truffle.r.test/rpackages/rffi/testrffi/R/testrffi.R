addint <- function(a, b) {
	.Call("addint", as.integer(a), as.integer(b), PACKAGE = "testrffi")
}
