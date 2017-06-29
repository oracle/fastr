if (any(R.version$engine == "FastR")) {
    ch0 <- .fastr.channel.create(1L)
    ch1 <- .fastr.channel.create(2L)
    code0 <- "ch <- .fastr.channel.get(1L); .fastr.channel.send(ch, 7L)"
    code1 <- "ch <- .fastr.channel.get(2L); .fastr.channel.send(ch, 42L)"
    cx0 <- .fastr.context.spawn(code0)
    cx1 <- .fastr.context.spawn(code1)
    x<-.fastr.channel.receive(ch0)
    y<-.fastr.channel.receive(ch1)
    .fastr.context.join(cx0)
    .fastr.context.join(cx1)
    .fastr.channel.close(ch0)
    .fastr.channel.close(ch1)
    print(c(x,y))
} else {
    print(c(7L, 42L))
}
