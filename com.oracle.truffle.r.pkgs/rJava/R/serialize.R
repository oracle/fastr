##
 # This material is distributed under the GNU General Public License
 # Version 2. You may review the terms of this license at
 # http://www.gnu.org/licenses/gpl-2.0.html
 #
 # Copyright (c) 2006 Simon Urbanek <simon.urbanek@r-project.org>
 # Copyright (c) 2018, Oracle and/or its affiliates
 #
 # All rights reserved.
##

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
