setClass("A")
setClass("B", contains = "A")
setClass("C", contains = "A")

setClass("D", representation(id = "numeric"))
setClass("E", contains = "D")


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
  
  setMethod("foo.bar", 
  signature(x = "B", y = "D"),
  function(x, y) {
    callNextMethod()
    message("foo.bar(B, D)")
  })

foo.bar(new("B"), new("D"))

setMethod("foo.bar", 
  signature(x = "C", y = "E"),
  function(x, y) {
    callNextMethod()
    message("foo.bar(C, E)")
})

foo.bar(new("C"), new("E"))

foo.bar(new("B"), new("E"))
