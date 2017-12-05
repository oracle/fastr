RandMatStat = function(t) {
    set.seed(10)
    n = 5
    v = matrix(0, nrow=t)
    w = matrix(0, nrow=t)
    for (i in 1:t) {
        a = matrix(rnorm(n*n), ncol=n, nrow=n)
        b = matrix(rnorm(n*n), ncol=n, nrow=n)
        c = matrix(rnorm(n*n), ncol=n, nrow=n)
        d = matrix(rnorm(n*n), ncol=n, nrow=n)
        P = cbind(a,b,c,d)
        Q = rbind(cbind(a,b),cbind(c,d))
        v[i] = sum(diag((t(P)%*%P)^4))
        w[i] = sum(diag((t(Q)%*%Q)^4))
    }
    s1 = apply(v,2,sd)/mean(v)
    s2 = apply(w,2,sd)/mean(w)
    return (round(s1, digits=7) == 0.8324299 && round(s2, digits=7) == 0.7440433)
}

function() {
    return (RandMatStat(1000))
}
