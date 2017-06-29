# Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.

eval(expression({
setBreakpoint <- function (srcfile, line, nameonly = TRUE, envir = parent.frame(), 
    lastenv, verbose = TRUE, tracer, print = FALSE, clear = FALSE, 
    ...) 
{
    res <- .fastr.setBreakpoint(srcfile, line, clear)
    if(is.null(res))
    	res <- structure(list(), class="findLineNumResult")
    if (verbose) 
        print(res, steps = !clear)
}

}), asNamespace("utils"))

eval(expression({
help <- function (topic, package = NULL, lib.loc = NULL, verbose = getOption("verbose"), try.all.packages = getOption("help.try.all.packages"), help_type = getOption("help_type")) {
    types <- c("text", "html", "pdf")
    if (!missing(package))
        if (is.name(y <- substitute(package)))
            package <- as.character(y)
    if (missing(topic)) {
        if (!is.null(package)) {
            help_type <- if (!length(help_type))
                "text"
                else match.arg(tolower(help_type), types)
            if (interactive() && help_type == "html") {
                port <- tools::startDynamicHelp(NA)
                if (port <= 0L)
                  return(library(help = package, lib.loc = lib.loc, character.only = TRUE))
                browser <- if (.Platform$GUI == "AQUA") {
                  get("aqua.browser", envir = as.environment("tools:RGUI"))
                }
                else getOption("browser")
                browseURL(paste0("http://127.0.0.1:", port, "/library/", package, "/html/00Index.html"), browser)
                return(invisible())
            }
            else return(library(help = package, lib.loc = lib.loc, character.only = TRUE))
        }
        if (!is.null(lib.loc))
            return(library(lib.loc = lib.loc))
        topic <- "help"
        package <- "utils"
        lib.loc <- .Library
    }
    ischar <- tryCatch(is.character(topic) && length(topic) == 1L, error = identity)
    if (inherits(ischar, "error"))
        ischar <- FALSE
    if (!ischar) {
        reserved <- c("TRUE", "FALSE", "NULL", "Inf", "NaN", "NA", "NA_integer_", "NA_real_", "NA_complex_", "NA_character_")
        stopic <- deparse(substitute(topic))
        if (!is.name(substitute(topic)) && !stopic %in% reserved)
            stop("'topic' should be a name, length-one character vector or reserved word")
        topic <- stopic
    }
    # Fastr >>>>
    fastrHelpRd <- .fastr.interop.getHelpRd(topic)
    if (!is.null(fastrHelpRd)) {
        fastrHelpRd <- tools::parse_Rd(textConnection(fastrHelpRd))
        cat("==== R Help on ‘", topic, "’ ====\n", sep = "")
        return(tools::Rd2txt(fastrHelpRd))
    }
    # Fastr <<<<
    help_type <- if (!length(help_type))
        "text"
        else match.arg(tolower(help_type), types)
    paths <- index.search(topic, find.package(if (is.null(package))
        loadedNamespaces()
        else package, lib.loc, verbose = verbose))
    tried_all_packages <- FALSE
    if (!length(paths) && is.logical(try.all.packages) && !is.na(try.all.packages) && try.all.packages && is.null(package) && is.null(lib.loc)) {
        for (lib in .libPaths()) {
            packages <- .packages(TRUE, lib)
            packages <- packages[is.na(match(packages, .packages()))]
            paths <- c(paths, index.search(topic, file.path(lib, packages)))
        }
        paths <- paths[nzchar(paths)]
        tried_all_packages <- TRUE
    }
    paths <- unique(paths)
    attributes(paths) <- list(call = match.call(), topic = topic, tried_all_packages = tried_all_packages, type = help_type)
    class(paths) <- "help_files_with_topic"
    paths
}
}), asNamespace("utils"))