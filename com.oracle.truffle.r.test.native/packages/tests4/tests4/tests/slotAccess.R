stopifnot(require(methods))
stopifnot(require(tests4))

`@`(getClass("ClassUnionRepresentation"), "virtual")
`@`(getClass("ClassUnionRepresentation"), "virtual")
try(`@`(getClass("ClassUnionRepresentation"), c("virtual", "foo")))
getClass("ClassUnionRepresentation")@virtual
getClass("ClassUnionRepresentation")@.S3Class
c(42)@.Data
x<-42; `@`(x, ".Data")
x<-42; `@`(x, .Data)
x<-42; slot(x, ".Data")
setClass("foo", contains="numeric"); x<-new("foo"); res<-x@.Data; removeClass("foo"); res
setClass("foo", contains="numeric"); x<-new("foo"); res<-slot(x, ".Data"); removeClass("foo"); res
try(getClass("ClassUnionRepresentation")@foo)
try(c(42)@foo)
x<-42; attr(x, "foo")<-7; try(x@foo)
x<-42; attr(x, "foo")<-7; slot(x, "foo")
x<-c(42); class(x)<-"bar"; try(x@foo)
x<-getClass("ClassUnionRepresentation"); slot(x, "virtual")
x<-getClass("ClassUnionRepresentation"); try( slot(x, virtual))
x<-function() 42; attr(x, "foo")<-7; y<-asS4(x); y@foo
x<-NULL; try(`@`(x, foo))
x<-NULL; try(x@foo)
x<-paste0(".", "Data"); y<-42; slot(y, x)
