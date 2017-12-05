fib_fun <- function(n) {
    if (n < 2) {
        return(n)
    } else {
        return(fib_fun(n - 1) + fib_fun(n - 2))
    }
}

function() {
    return (fib_fun(20))
}
