stopifnot(require(methods))
stopifnot(require(tests4))

try(standardGeneric(42))
try(standardGeneric(character()))
try(standardGeneric(""))
try(standardGeneric("foo", 42))
x<-42; class(x)<-character(); try(standardGeneric("foo", x))
