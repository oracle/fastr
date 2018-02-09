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