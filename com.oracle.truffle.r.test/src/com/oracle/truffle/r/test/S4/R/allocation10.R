# test from Hadley Wickham's book

check_person <- function(object) {
  errors <- character()
  length_age <- length(object@age)
  if (length_age != 1) {
    msg <- paste("Age is length ", length_age, ".  Should be 1", sep = "")
    errors <- c(errors, msg)
  }

  length_name <- length(object@name)
  if (length_name != 1) {
    msg <- paste("Name is length ", length_name, ".  Should be 1", sep = "")
    errors <- c(errors, msg)
  }
  
  if (length(errors) == 0) TRUE else errors
}
setClass("Person", representation(name = "character", age = "numeric"), validity = check_person)

hadley <- new("Person", name = "Hadley", age = 31)
hadley@age <- 1:10
print(validObject(hadley))
