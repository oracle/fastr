# test passing a promise (environment must stay private)

if (any(R.version$engine == "FastR")) {
    ch <- .fastr.channel.create(1L)
    code <- "ch <- .fastr.channel.get(1L); x<-.fastr.channel.receive(ch); x[['y']]<-7; .fastr.channel.send(ch, x[['y']])"
    cx <- .fastr.context.spawn(code)
    y<-c(42)
    f <- function(ch, y) {
        .fastr.channel.send(ch, environment())
    }
    f(ch, y + 1)
    x<-.fastr.channel.receive(ch)
    .fastr.context.join(cx)
    .fastr.channel.close(ch)
    print(c(y, x))
} else {
    print(c(42, 7))
}
