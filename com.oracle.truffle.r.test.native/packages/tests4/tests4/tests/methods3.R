# test from Hadley Wickham's book

stopifnot(require(methods))
stopifnot(require(tests4))
setGeneric("sides", valueClass = "numeric", function(object) {
  standardGeneric("sides")
})

setClass("Shape")
setClass("Polygon", representation(sides = "integer"), contains = "Shape")
setClass("Triangle", contains = "Polygon")
setClass("Square", contains = "Polygon")
# setClass("Circle", contains = "Shape")

setMethod("sides", signature("Triangle"), function(object) "three")
try(tryCatch({sides(new("Triangle"))}, error = function(e) { removeGeneric("sides"); stop(e) }))
