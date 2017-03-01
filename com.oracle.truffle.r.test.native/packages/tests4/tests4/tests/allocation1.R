# test from Hadley Wickham's book

stopifnot(require(methods))
stopifnot(require(tests4))
setClass("Person", representation(name = "character", age = "numeric"))
setClass("Employee", representation(boss = "Person"), contains = "Person")
print(new("Person", name = "Hadley", age = 31))

