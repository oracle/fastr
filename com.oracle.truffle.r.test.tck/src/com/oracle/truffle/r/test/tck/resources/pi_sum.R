PiSum <- function(x) {
    t = 0.0
    for (j in 1:500) {
        t = 0.0
        for (k in 1:10000) {
            t = t + 1.0/(k*k)
        }
    }
    return(abs(t-1.644834071848065) < 1e-12)
}

function() {
    return(PiSum(c()));
}
