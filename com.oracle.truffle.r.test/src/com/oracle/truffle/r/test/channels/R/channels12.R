# test sending environment with global parent and assigning it remotely (should assign remotely but not locally)

if (length(grep("FastR", R.Version()$version.string)) == 1) {
    ch <- .fastr.channel.create(1L)
    code <- "ch <- .fastr.channel.get(1L); env<-.fastr.channel.receive(ch); assign('y', 7, pos=env); .fastr.channel.send(ch, get('x', envir=env))"
    cx <- .fastr.context.spawn(code)
    env <- new.env(parent = .GlobalEnv)
    assign("x", 7, envir=env)
    .fastr.channel.send(ch, env)
    x<-.fastr.channel.receive(ch)
    .fastr.context.join(cx)
    .fastr.channel.close(ch)
    print(list(x, exists('y', envir=env)))
} else {
    print(list(7, FALSE))
}
