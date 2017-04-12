# test subsequent remote updates in global space - second time value should have been reset

if (length(grep("FastR", R.Version()$version.string)) == 1) {
    ch1 <- .fastr.channel.create(1L)
    code <- "ch2 <- .fastr.channel.get(1L); x <- 7; .fastr.channel.send(ch2, x)"
    x <- 42
    cx <- .fastr.context.spawn(code)
    y <- .fastr.channel.receive(ch1)
    .fastr.context.join(cx)
    code <- "ch2 <- .fastr.channel.get(1L); res <- tryCatch(z <- x, error=function(e) e); .fastr.channel.send(ch2, res[[1]])"
    cx <- .fastr.context.spawn(code)
    y <- .fastr.channel.receive(ch1)
    .fastr.context.join(cx)    
    .fastr.channel.close(ch1)
    print(y)
} else {
    print("object 'x' not found")
}
