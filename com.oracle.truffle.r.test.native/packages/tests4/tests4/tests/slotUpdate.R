stopifnot(require(methods))
stopifnot(require(tests4))

x<-getClass("ClassUnionRepresentation"); x@virtual<-TRUE; x@virtual
x<-getClass("ClassUnionRepresentation"); slot(x, "virtual", check=TRUE)<-TRUE; x@virtual
x<-initialize@valueClass; initialize@valueClass<-"foo"; initialize@valueClass<-x

x<-function() 42; attr(x, "foo")<-7; try(y@foo<-42)
x<-function() 42; attr(x, "foo")<-7; try(slot(y, "foo")<-42)
x<-function() 42; attr(x, "foo")<-7; y<-asS4(x); try(y@foo<-42)
x<-NULL; try(`@<-`(x, foo, "bar"))
x<-NULL; try(x@foo<-"bar")
