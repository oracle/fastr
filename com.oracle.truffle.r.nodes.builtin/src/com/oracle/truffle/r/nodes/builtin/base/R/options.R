#  File src/library/base/R/options.R
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

# temporarily substituted to get argument names
#options <- function(...)
#    .Internal(options(...))

# missing does not work correctly on parameters with default values
# getOption <- function(x, default = NULL)
getOption <- function(x, default)
    {
    ## To avoid always performing the %in%,
    ## we use the original code if default is not specified.
    if(missing(default)) return(options(x)[[1L]])

    if(x %in% names(options())) options(x)[[1L]] else default
}
