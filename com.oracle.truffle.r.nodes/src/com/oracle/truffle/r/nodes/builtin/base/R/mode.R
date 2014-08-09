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

mode <- function(x) {
    if(is.expression(x)) return("expression")
    if(is.call(x))
		return("call")
		##TODO Fix deparse
#        return(switch(deparse(x[[1L]])[1L],
#                        "(" = "(",
#                        ## otherwise
#                        "call"))
    if(is.name(x)) "name" else
    switch(tx <- typeof(x),
# TODO: fix implementation of switch
#            double =, integer = "numeric", # 'real=' dropped, 2000/Jan/14
#            closure =, builtin =, special = "function",
            double ="numeric", integer = "numeric", # 'real=' dropped, 2000/Jan/14
            closure ="function", builtin ="function", special = "function",
            ## otherwise
            tx)
}

storage.mode <- function(x)
    switch(tx <- typeof(x),
        #TODO:switch should be able to handle missing args.
        closure = "function" , builtin = "function", special = "function",
        ## otherwise
        tx)
