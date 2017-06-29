# test remote updated of function with shareable attribute (attribute must stay private)

if (any(R.version$engine == "FastR")) {
    ch <- .fastr.channel.create(1L)
    code <- "ch <- .fastr.channel.get(1L); f<-.fastr.channel.receive(ch); attr(f, 'foo') <- c('baz', 'bar'); .fastr.channel.send(ch, attr(f, 'foo'))"
    cx <- .fastr.context.spawn(code)
    f<-function() 42
    attr(f, "foo") <- c("foo", "bar")
    .fastr.channel.send(ch, f)
    x<-.fastr.channel.receive(ch)
    .fastr.context.join(cx)
    .fastr.channel.close(ch)
    print(list(x, attr(f, "foo")))
} else {
    print(list(c("baz", "bar"), c("foo", "bar")))
}
