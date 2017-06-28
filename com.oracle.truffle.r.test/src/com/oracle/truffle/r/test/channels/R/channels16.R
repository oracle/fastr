# test recursion - sending environment containing vector with same environment attribute

if (any(R.version$engine == "FastR")) {
    ch <- .fastr.channel.create(1L)
    code <- "ch <- .fastr.channel.get(1L); env<-.fastr.channel.receive(ch); .fastr.channel.send(ch, get('v', envir=env))"
    cx <- .fastr.context.spawn(code)
    env <- new.env(parent = .GlobalEnv)
    v <- c(7)
    attr(v, "env") <- env
    assign("v", v, envir=env)
    .fastr.channel.send(ch, env)
    x<-.fastr.channel.receive(ch)
    .fastr.context.join(cx)
    .fastr.channel.close(ch)
    print(c(v, x))
} else {
    print(c(7, 7))
}
