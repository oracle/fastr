# test sending environment with a parent and global grandparent and assigning it remotely (should assign remotely but not locally)

if (length(grep("FastR", R.Version()$version.string)) == 1) {
    ch <- .fastr.channel.create(1L)
    code <- "ch <- .fastr.channel.get(1L); env<-.fastr.channel.receive(ch); assign('y', 70, pos=env); assign('z', 420, pos=parent.env(env)); .fastr.channel.send(ch, c(get('w', envir=env), get('x', envir=parent.env(env))))"
    cx <- .fastr.context.spawn(code)
    parentEnv <- new.env(parent = .GlobalEnv)
    env <- new.env(parent = parentEnv)
    assign("w", 7, envir=env)
    assign("x", 42, envir=parentEnv)
    .fastr.channel.send(ch, env)
    v<-.fastr.channel.receive(ch)
    .fastr.context.join(cx)
    .fastr.channel.close(ch)
    print(list(v, c(exists('y', envir=env), exists('z', envir=env))))
} else {
    print(list(c(7, 42), c(FALSE, FALSE)))
}
