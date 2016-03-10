# test remote list transfer for read - should use the same vector elements despite non-sharable content
# also in the list
 
if (length(grep("FastR", R.Version()$version.string)) == 1) {
    ch <- fastr.channel.create(1L)
    cx <- fastr.context.create("SHARED_NOTHING")
    code <- "ch <- fastr.channel.get(1L); x<-fastr.channel.receive(ch); y<-x[[1]][1]; z<-c(fastr.identity(x[[1]]), fastr.identity(x[[2]])) ; fastr.channel.send(ch, z)"
    fastr.context.spawn(cx, code)
    y<-list(c(7, 42), function(x) 1)
    z<-fastr.identity(y[[1]])
    w<-fastr.identity(y[[2]])
    fastr.channel.send(ch, y)
    x<-fastr.channel.receive(ch)
    fastr.context.join(cx)
    fastr.channel.close(ch)
    print(c(x[1] == z, x[2] == w))
} else {
    print(c(TRUE, FALSE))
}
