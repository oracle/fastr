setClass("A")
setClass("B", contains = "A")
setClass("C", contains = "A")

setClass("D", representation(name = "character"))

setGeneric("foo.bar", function(x, y) {
   standardGeneric("foo.bar")
 })
 
 setMethod("foo.bar", 
  signature(x = "C", y = "D"),
  function(x, y) {  
   callNextMethod()
   message("foo.bar(C, D)")
  })
  
 setMethod("foo.bar", 
  signature(x = "A", y = "D"), 
  function(x, y) {
    message("foo.bar(A, D)")
  })

foo.bar(new("C"), new("D"))
