## Java serialization/unserialization

.jserialize <- function(o) {
  if (!is(o, "jobjRef"))
    stop("can serialize Java objects only")
  .jcall("RJavaClassLoader","[B","toByte",.jcast(o, "java.lang.Object"))
}

.junserialize <- function(data) {
  if (!is.raw(data))
    stop("can de-serialize raw vectors only")
  o <- .jcall("RJavaClassLoader","Ljava/lang/Object;","toObjectPL",.jarray(data, dispatch = FALSE))
  if (!is.jnull(o)) {
    cl<-try(.jclass(o), silent=TRUE)
    if (all(class(cl) == "character"))
      o@jclass <- gsub("\\.","/",cl)
  }
  o
}

.jcache <- function(o, update=TRUE) {
  if (!is(o, "jobjRef"))
    stop("o must be a Java object")
  if (!is.null(update) && (!is.logical(update) || length(update) != 1))
    stop("update must be TRUE, FALSE of NULL")
  what <- update
  if (isTRUE(what)) what <- .jserialize(o)
  invisible(.Call(javaObjectCache, o@jobj, what))
}
