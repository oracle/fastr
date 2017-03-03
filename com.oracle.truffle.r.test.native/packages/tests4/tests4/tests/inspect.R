stopifnot(require(methods))
stopifnot(require(tests4))

tests4:::inspect.vehicle(new("Car"), new("Inspector"))
tests4:::inspect.vehicle(new("Truck"), new("Inspector"))
tests4:::inspect.vehicle(new("Car"), new("StateInspector"))
tests4:::inspect.vehicle(new("Truck"), new("StateInspector"))
