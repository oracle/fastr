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

# Runs as.vector with all possible combinations of arguments and prints out:
# if there was error/warning, result type, and if it retained names and/or custom attributes
values <- list(list(1,2, 3),
    as.pairlist(c(1,2,3)),
    c(1L, 2L, 4L),
    c(1, 2, 4),
    as.raw(c(1, 2, 4)),
    c('1', '2', '4'),
    c(T, F, T),
    c(1+1i, 2+1i, 4+2i),
    parse(text='x; y; z')
    # parse(text='-y')[[1]],
    # function() 42
)

modes <- c(
    "integer",
    "numeric",
    "double",
    "raw",
    "logical",
    "complex",
    "character",
    "list",
    "pairlist",
    # "expression",
    "symbol",
    "name",
    # "closure",
    # "function",
    "any"
)

padLeft <- function(x, size) {
    paste0(x, paste0(rep(" ", size - nchar(x)), collapse=""))
}

for (i in seq_along(values)) {
    for (m in seq_along(modes)) {
        x <- values[[i]]
        if (length(x) > 2) {
            names(x) <- c('a', 'b', 'c')
        }
        attr(x, 'mya') <- 42
        wasWarn <- F
        wasError <- F
        res <- NULL
        tryCatch(res <<- as.vector(x, mode=modes[[m]]),
            warning = function(e) wasWarn <<- T,
            error = function(e) wasError <<- T)
        cat(padLeft(typeof(x), 10), "->", padLeft(modes[[m]], 10),
            "result: ", padLeft(typeof(res), 10),
            if (wasError) "E " else if (wasWarn) "W " else "  ",
            "names:", if (length(names(res)) > 0) "yes  " else "no   ",
            "attrs:", if (is.null(attr(res, 'mya'))) "no" else "yes", "\n")
    }
}
