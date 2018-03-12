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
