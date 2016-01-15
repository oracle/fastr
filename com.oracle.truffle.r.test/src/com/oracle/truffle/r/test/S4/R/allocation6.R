# test from Hadley Wickham's book

setClass("Person", representation(name = "character", age = "numeric"), prototype(name = NA_character_, age = NA_real_))
hadley <- new("Person", name = "Hadley")
hadley@age
