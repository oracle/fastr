# test access to global environment with multiple context instantiations varying context number

if (length(grep("FastR", R.Version()$version.string)) == 1) {
    ch1 <- .fastr.channel.create(1L)
    code <- "ch2 <- .fastr.channel.get(1L); x <- .fastr.channel.receive(ch2); .fastr.channel.send(ch2, x)"
    x <- 7
    # create one child context
    cx <- .fastr.context.spawn(code)
    .fastr.channel.send(ch1, 7)
    y <- .fastr.channel.receive(ch1)
    .fastr.context.join(cx)
    # create two child contexts
    cx <- .fastr.context.spawn(rep(code, 2))
    .fastr.channel.send(ch1, 42)
    .fastr.channel.send(ch1, 24)
    y <- .fastr.channel.receive(ch1)
    z <- .fastr.channel.receive(ch1)
    
    .fastr.channel.close(ch1)
    print(sort(c(y, z)))
} else {
    print(c(24L, 42L))
}
