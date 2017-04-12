# test remote update in global space - values should remain distinct

if (length(grep("FastR", R.Version()$version.string)) == 1) {
    ch1 <- .fastr.channel.create(1L)
    code <- "ch2 <- .fastr.channel.get(1L); x <- 7; .fastr.channel.send(ch2, x)"
    x <- 42
    cx <- .fastr.context.spawn(code)
    y <- .fastr.channel.receive(ch1)
    .fastr.context.join(cx)
    .fastr.channel.close(ch1)
    print(c(x, y))
} else {
    print(c(42L, 7L))
}
