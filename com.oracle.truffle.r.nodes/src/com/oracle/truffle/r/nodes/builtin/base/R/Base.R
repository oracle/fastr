#
# This material is distributed under the GNU General Public License
# Version 2. You may review the terms of this license at
# http://www.gnu.org/licenses/gpl-2.0.html
#
# Copyright (c) 1995-2012, The R Core Team
# Copyright (c) 2003, The R Foundation
# Copyright (c) 2013, 2014, Oracle and/or its affiliates
#
# All rights reserved.
#
# builtins for the Base package

`%in%` <- function(x, table) match(x, table, nomatch=0L) > 0L

`xor` <- function(x, y) (x | y) & !(x & y)

as.list <- function(x, ...) if (typeof(x) == "list") x else as.vector(x, "list")

vector <- function(mode = "logical", length = 0L) .Internal(vector(mode, length))
