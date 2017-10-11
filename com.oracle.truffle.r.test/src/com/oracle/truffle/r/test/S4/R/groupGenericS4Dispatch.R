setClass("Foo1234", slots = c(a = "numeric"))
setMethod("%*%", signature(x = "ANY", y = "Foo1234"), function(x, y) { "s4 dispatched" })

obj <- new("Foo1234")
x <- matrix(1.1:16.1, 4, 4)
obj@a <- runif(10)
res <- x %*% obj
print(res)