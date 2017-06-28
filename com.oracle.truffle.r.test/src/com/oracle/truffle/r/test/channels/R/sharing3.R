# test remote update in base space - values should remain distinct

if (any(R.version$engine == "FastR")) {
    ch1 <- .fastr.channel.create(1L)
	# use an obscure name so it doesn't clash with other tests
    code <- "ch2 <- .fastr.channel.get(1L); assign('tmp59857', 7, env=baseenv()); .fastr.channel.send(ch2, get('tmp59857', env=baseenv(), inherits=F))"
    assign('tmp59857', 42, env=baseenv())
    cx <- .fastr.context.spawn(code)
    y <- .fastr.channel.receive(ch1)
    .fastr.context.join(cx)
    .fastr.channel.close(ch1)
    print(c(get('tmp59857', env=baseenv(), inherits=F), y))
} else {
    print(c(42L, 7L))
}
