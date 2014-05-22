#  File src/library/base/R/character.R
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

substr <- function(x, start, stop)
{
    if(!is.character(x)) x <- as.character(x)
    .Internal(substr(x, as.integer(start), as.integer(stop)))
}

`substr<-` <- function(x, start, stop, value)
    .Internal(`substr<-`(x, as.integer(start), as.integer(stop), value))
