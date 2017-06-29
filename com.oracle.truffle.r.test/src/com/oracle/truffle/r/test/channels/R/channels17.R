# test passing a function (environment must stay private)

if (any(R.version$engine == "FastR")) {
    ch <- .fastr.channel.create(1L)
    code <- "ch <- .fastr.channel.get(1L); f<-.fastr.channel.receive(ch); env <- environment(f); env[['x']] <- 7; .fastr.channel.send(ch, env[['x']])"
    cx <- .fastr.context.spawn(code)
    env <- new.env()
    env[["x"]] <- 42
    f <- function() 42
    environment(f) <- env
   .fastr.channel.send(ch, f)
    y <- .fastr.channel.receive(ch)
    .fastr.context.join(cx)
    .fastr.channel.close(ch)
    print(c(env[["x"]], y))
} else {
    print(c(42, 7))
}
