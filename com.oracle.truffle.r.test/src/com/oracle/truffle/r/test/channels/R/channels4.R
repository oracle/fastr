# test sending global environment and assigning it remotely (should assign remotely but not locally)

if (length(grep("FastR", R.Version()$version.string)) == 1) {
    ch <- .fastr.channel.create(1L)
    code <- "ch <- .fastr.channel.get(1L); env<-.fastr.channel.receive(ch); assign('y', 7, pos=env); .fastr.channel.send(ch, y)"
    cx <- .fastr.context.spawn(code)
    .fastr.channel.send(ch, .GlobalEnv)
    x<-.fastr.channel.receive(ch)
    .fastr.context.join(cx)
    .fastr.channel.close(ch)
    print(list(x, exists('y')))
} else {
    print(list(7, FALSE))
}
