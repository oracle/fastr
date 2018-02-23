
runTwice <- function() {
    cat("getDLLRegisteredRoutines('(embedding)'):\n")
    print(getDLLRegisteredRoutines("(embedding)"))
    .Call(getDLLRegisteredRoutines("(embedding)")[[".Call"]][[1]], 1:5);
}