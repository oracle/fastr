setClassUnion("DDAVindex", members =  c("numeric", "logical", "character"))
setClass("DDAVFoo", representation(a = "numeric", b = "numeric"))

subsetFoo <- function(x, i, j, drop) {
  cat(paste0("subsetFoo[",i,",",j,",drop=",drop,"]\n"))
  c(x@a[[i]], x@b[[j]])
}

subsetFooMissingDrop <- function(x, i, j, drop) {
  cat(paste0("subsetFooMissingDrop[",i,",",j,",drop=missing]\n"))
  c(x@a[[i]], x@b[[j]])  
}
setMethod("[", signature(x = "DDAVFoo", i = "DDAVindex", j = "DDAVindex", drop = "logical"), subsetFoo)
setMethod("[", signature(x = "DDAVFoo", i = "DDAVindex", j = "DDAVindex", drop = "missing"), subsetFooMissingDrop)

obj <- new("DDAVFoo", a=c(1,2,3),b=c(4,5,6))
obj[2,3]

obj[drop=T,j=3,i=2]

obj[drop=T,j=3,i=2,a="unnecessary"]