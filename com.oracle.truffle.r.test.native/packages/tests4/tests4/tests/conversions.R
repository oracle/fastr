stopifnot(require(methods))
stopifnot(require(tests4))

x<-42; isS4(x)
x<-42; y<-asS4(x); isS4(y)
isS4(NULL)
asS4(NULL); isS4(NULL)
asS4(7:42)
