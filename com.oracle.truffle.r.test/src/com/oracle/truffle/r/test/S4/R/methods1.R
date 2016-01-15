# test from Hadley Wickham's book

setGeneric("sides", function(object) {
  standardGeneric("sides")
})

setClass("Shape")
setClass("Polygon", representation(sides = "integer"), contains = "Shape")
setClass("Triangle", contains = "Polygon")
setClass("Square", contains = "Polygon")
setClass("Circle", contains = "Shape")

setMethod("sides", signature(object = "Polygon"), function(object) {
  object@sides
})

setMethod("sides", signature("Triangle"), function(object) 3)
setMethod("sides", signature("Square"),   function(object) 4)
setMethod("sides", signature("Circle"),   function(object) Inf)

showMethods("sides")
