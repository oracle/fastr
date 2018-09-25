# Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 3 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 3 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 3 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.

as.matrix.polyglot.value <- function(x, ...) { 
    v <- .fastr.interop.asVector(x, recursive=TRUE, dropDimensions=FALSE)
    return(as.matrix(v), ...)
}

as.array.polyglot.value <- function(x, ...) { 
    v <- .fastr.interop.asVector(x, recursive=TRUE, dropDimensions=FALSE)
    return(as.array(v), ...)
}

as.data.frame.polyglot.value <- function(x, row.names = NULL, optional = FALSE, ..., nm = paste(deparse(substitute(x), 
    width.cutoff = 500L), collapse = " ")) {
    v <- .fastr.interop.asVector(x, recursive=TRUE, dropDimensions=FALSE)
    if (!optional && !is.null(v) && is.atomic(v)) {
        v <- list(v)        
        names(v) <- nm
    }    
    as.data.frame(v, row.names=row.names, optional=optional, ...)
}