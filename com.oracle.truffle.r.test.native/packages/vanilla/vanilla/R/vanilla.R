vanilla <-
function() print("A vanilla R package")

functionTest <- function(x, y) {
    x[5] <- 1
    y[2] <- x[5]
    (1+y[2]):10
}