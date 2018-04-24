.joptions <- function(...) {
  l <- list(...)
  if (length(l)==0) return(list())
  if ("jni.cache" %in% names(l)) {
    v <- l[["jni.cache"]]
    if (!is.logical(v) || length(v)!=1)
      stop("jni.cache must be a logical vector of length 1")
    .C(RuseJNICache,v)
    invisible(NULL)
  }
}
