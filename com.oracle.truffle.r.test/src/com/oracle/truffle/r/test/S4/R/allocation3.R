# IgnoreErrorContext
# test from Hadley Wickham's book

setClass("Person", representation(name = "character", age = "numeric"))
setClass("Employee", representation(boss = "Person"), contains = "Person")
print(new("Person", name = "Hadley", sex = "male"))
