# test from Hadley Wickham's book

setClass("A")
setClass("A1", contains = "A")
setClass("A2", contains = "A1")
setClass("A3", contains = "A2")

setGeneric("foo", function(a, b) standardGeneric("foo")) 
setMethod("foo", signature("A1", "A2"), function(a, b) "1-2")
setMethod("foo", signature("A2", "A1"), function(a, b) "2-1")

foo(new("A2"), new("A2"))
