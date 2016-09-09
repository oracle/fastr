# test remote updated of atomic vector with shareable attribute (must stay private)

if (length(grep("FastR", R.Version()$version.string)) == 1) {
    ch <- .fastr.channel.create(1L)
    code <- "ch <- .fastr.channel.get(1L); x<-.fastr.channel.receive(ch); x[1]<-7; .fastr.channel.send(ch, x)"
    cx <- .fastr.context.spawn(code)
    y<-c(42)
    attr(y, "foo") <- c("foo", "bar")
    .fastr.channel.send(ch, y)
    x<-.fastr.channel.receive(ch)
    .fastr.context.join(cx)
    .fastr.channel.close(ch)
    print(c(x,y))
} else {
    print(c(7L, 42L))
}
