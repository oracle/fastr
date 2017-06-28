# test remote list transfer for read - should use the same vector elements

if (any(R.version$engine == "FastR")) {
    ch <- .fastr.channel.create(1L)
    code <- "ch <- .fastr.channel.get(1L); x<-.fastr.channel.receive(ch); y<-x[[1]][1]; z<-.fastr.identity(x[[1]]); .fastr.channel.send(ch, z)"
    cx <- .fastr.context.spawn(code)
    y<-list(c(7, 42), 1)
    z<-.fastr.identity(y[[1]])
    .fastr.channel.send(ch, y)
    x<-.fastr.channel.receive(ch)
    .fastr.context.join(cx)
    .fastr.channel.close(ch)
    print(x == z)
} else {
    print(TRUE)
}
