actParsToList <-
function (pars, env) {
    l <- list()
    for (i in seq_along(pars)) {
        strrep <- as.character(pars[[i]])
        if (is.symbol(pars[[i]])) {
            value <- eval(pars[[i]], envir = env)
            l[[strrep]] <- value
        }
        else {
            warning(paste0("Skipping '", strrep, "' because only symbols are allowed"))
        }
    }
    l
}
