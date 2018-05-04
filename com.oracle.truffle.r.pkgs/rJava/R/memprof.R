.jmemprof <- function(file = "-") {
  if (is.null(file)) file <- ""
  invisible(.Call(RJava_set_memprof, as.character(file)))
}
