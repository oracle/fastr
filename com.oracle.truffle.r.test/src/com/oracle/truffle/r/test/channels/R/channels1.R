# test remote updated of atomic vector (must stay private)

if (length(grep("FastR", R.Version()$version.string)) == 1) {
    ch <- .fastr.channel.create(1L)
    cx <- .fastr.context.create("SHARED_NOTHING")
    code <- "ch <- .fastr.channel.get(1L); x<-.fastr.channel.receive(ch); x[1]<-7; .fastr.channel.send(ch, x)"
    .fastr.context.spawn(cx, code)
    y<-c(42)
    .fastr.channel.send(ch, y)
    x<-.fastr.channel.receive(ch)
    .fastr.context.join(cx)
    .fastr.channel.close(ch)
    print(c(x,y))
} else {
    print(c(7L, 42L))
}
