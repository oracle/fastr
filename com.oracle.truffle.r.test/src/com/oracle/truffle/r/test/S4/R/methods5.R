setClass("Vehicle")
setClass("Truck", contains = "Vehicle")
setClass("Car", contains = "Vehicle")

setClass("Inspector", representation(name = "character"))
setClass("StateInspector", contains = "Inspector")

setGeneric("inspect.vehicle", function(v, i) {
   standardGeneric("inspect.vehicle")
 })

setMethod("inspect.vehicle",
 signature(v = "Vehicle", i = "Inspector"),
 function(v, i) {
   message("Looking for rust")
 })

setMethod("inspect.vehicle",
 signature(v = "Car", i = "Inspector"),
 function(v, i) {
   callNextMethod() # perform vehicle inspection
   message("Checking seat belts")
 })

inspect.vehicle(new("Car"), new("Inspector"))
