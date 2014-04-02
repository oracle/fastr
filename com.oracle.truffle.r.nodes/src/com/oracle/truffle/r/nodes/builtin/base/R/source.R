#  File src/library/base/R/source.R
#  Part of the R package, http://www.R-project.org
#
#  Copyright (C) 1995-2012 The R Core Team
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation; either version 2 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  A copy of the GNU General Public License is available at
#  http://www.r-project.org/Licenses/

sys.source <-
function(file, envir = baseenv(), chdir = FALSE,
    keep.source = getOption("keep.source.pkgs"))
{
    if(!(is.character(file) && file.exists(file)))
        stop(gettextf("'%s' is not an existing file", file))
    keep.source <- as.logical(keep.source)
    oop <- options(keep.source = keep.source,
       topLevelEnvironment = as.environment(envir))
    on.exit(options(oop))
    if (keep.source) {
      lines <- readLines(file, warn = FALSE)
      srcfile <- srcfilecopy(file, lines, file.info(file)[1,"mtime"], isFile = TRUE)
      exprs <- parse(text = lines, srcfile = srcfile, keep.source = TRUE)
    } else
      exprs <- parse(n = -1, file = file, srcfile = NULL, keep.source = FALSE)
    if (length(exprs) == 0L)
        return(invisible())
    if (chdir && (path <- dirname(file)) != ".") {
        owd <- getwd()
        if(is.null(owd))
            stop("cannot 'chdir' as current directory is unknown")
        on.exit(setwd(owd), add = TRUE)
        setwd(path)
    }
    for (i in exprs) eval(i, envir)
    invisible()
}
