# test sending global environment as an attribute and assigning it remotely (should assign remotely but not locally)

if (length(grep("FastR", R.Version()$version.string)) == 1) {
    ch <- .fastr.channel.create(1L)
    cx <- .fastr.context.create("SHARED_NOTHING")
    code <- "ch <- .fastr.channel.get(1L); msg<-.fastr.channel.receive(ch); env<-attr(msg, 'GLOBAL'); assign('y', 7, pos=env); .fastr.channel.send(ch, y)"
    .fastr.context.spawn(cx, code)
    l<-list(c(42))
    attr(l, 'GLOBAL')<-.GlobalEnv
    .fastr.channel.send(ch, l)
    x<-.fastr.channel.receive(ch)
    .fastr.context.join(cx)
    .fastr.channel.close(ch)
    print(list(x, exists('y')))
} else {
    print(list(7, FALSE))
}
