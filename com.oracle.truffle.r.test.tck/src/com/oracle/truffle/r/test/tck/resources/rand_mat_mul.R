RandMatMul <- function(n) {
    A <- matrix(runif(n*n), ncol=n, nrow=n)
    B <- matrix(runif(n*n), ncol=n, nrow=n)
    result <- A %*% B
    return (all(result >= 0))
}

function() {
    return (RandMatMul(100))
}
