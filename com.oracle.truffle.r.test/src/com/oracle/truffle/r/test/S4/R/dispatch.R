foo.bar <- function(x, y, z) { print(x); print(y); print(z) }

setClass("A", representation(a = "numeric"))
setClass("B", representation(b = "logical"))
setMethod("foo.bar", signature = list(y = "A", z = "B"), function(x, y, z) { print("primitive, A, B") })
setMethod("foo.bar", signature = list(y = "B", z = "A"), function(x, y, z) { print("primitive, B, A") })

foo.bar(1, 2, 3)
foo.bar(1, new("A"), new("B"))
foo.bar(1, new("B"), new("A"))
