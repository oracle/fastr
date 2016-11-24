# test remote update of a list containing atomic vector (must stay private)

if (length(grep("FastR", R.Version()$version.string)) == 1) {
    ch <- .fastr.channel.create(1L)
    code <- "ch <- .fastr.channel.get(1L); x<-.fastr.channel.receive(ch); x[1][1]<-7; .fastr.channel.send(ch, x)"
    cx <- .fastr.context.spawn(code)
    y<-list(c(42))
    .fastr.channel.send(ch, y)
    x<-.fastr.channel.receive(ch)
    .fastr.context.join(cx)
    .fastr.channel.close(ch)
    print(c(x,y))
} else {
    print(list(7L, 42L))
}
