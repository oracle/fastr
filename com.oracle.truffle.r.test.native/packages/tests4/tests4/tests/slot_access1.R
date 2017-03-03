# test from Hadley Wickham's book

stopifnot(require(methods))
stopifnot(require(tests4))
setClass("Person", representation(name = "character", age = "numeric"), prototype(name = NA_character_, age = NA_real_))
hadley <- new("Person", name = "Hadley")
print(getSlots("Person"))

