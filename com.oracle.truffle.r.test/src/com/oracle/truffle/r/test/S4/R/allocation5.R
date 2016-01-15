# test from Hadley Wickham's book

setClass("Person", representation(name = "character", age = "numeric"))
setClass("Employee", representation(boss = "Person"), contains = "Person")
hadley <- new("Person", name = "Hadley")
slot(hadley, "age")
