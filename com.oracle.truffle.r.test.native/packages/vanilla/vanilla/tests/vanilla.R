stopifnot(require(vanilla))

vanilla()
functionTest(c(1,2,3,4,5,6),8:10)
r<-42; vanilla::foo(r)<-7; r
