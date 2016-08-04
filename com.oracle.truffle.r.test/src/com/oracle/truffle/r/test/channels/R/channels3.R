# test sending a function

if (length(grep("FastR", R.Version()$version.string)) == 1) {
    ch <- .fastr.channel.create(1L)
    code <- "ch <- .fastr.channel.get(1L); f<-.fastr.channel.receive(ch); x<-f(7); .fastr.channel.send(ch, x)"
    cx <- .fastr.context.spawn(code)
    mul<-function(y) y*y
    .fastr.channel.send(ch, mul)
    x<-.fastr.channel.receive(ch)
    .fastr.context.join(cx)
    .fastr.channel.close(ch)
    print(x)
} else {
    print(49)
}
