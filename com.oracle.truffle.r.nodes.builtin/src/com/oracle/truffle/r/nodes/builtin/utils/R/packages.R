#  File src/library/utils/R/packages.R
#  Part of the R package, http://www.R-project.org
#
#  Copyright (C) 1995-2014 The R Core Team
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

# An override that works around the problems with numeric version generic ops
# We have to eval this re-definition in the utils namespace environmment as it is a private function
# stored in a local map

eval(expression(
available_packages_filters_db$R_version <-
		function(db)
{
	## Ignore packages which don't fit our version of R.
	depends <- db[, "Depends"]
	depends[is.na(depends)] <- ""
	## Collect the (versioned) R depends entries.
	x <- lapply(strsplit(sub("^[[:space:]]*", "", depends),
					"[[:space:]]*,[[:space:]]*"),
			function(s) s[grepl("^R[[:space:]]*\\(", s)])
	lens <- sapply(x, length)
	pos <- which(lens > 0L)
	if(!length(pos)) return(db)
	lens <- lens[pos]
	## Unlist.
	x <- unlist(x)
	pat <- "^R[[:space:]]*\\(([[<>=!]+)[[:space:]]+(.*)\\)[[:space:]]*"
	## Extract ops.
	ops <- sub(pat, "\\1", x)
	## Split target versions accordings to ops.
	v_t <- split(sub(pat, "\\2", x), ops)
	## Current R version.
#	v_c <- getRversion()
	v_c <- as.character(getRversion())
	## Compare current to target grouped by op.
	res <- logical(length(x))
	for(op in names(v_t))
		res[ops == op] <- do.call(op, list(v_c, v_t[[op]]))
#	  switch(op,
#		'>' = res[ops = op] <- v_c > v_t[[op]],
#		'>=' = res[ops = op] <- v_c >= v_t[[op]],
#		stop("unexpected op in"))
	## And assemble test results according to the rows of db.
	ind <- rep.int(TRUE, NROW(db))
	ind[pos] <- sapply(split(res, rep.int(seq_along(lens), lens)), all)
	db[ind, , drop = FALSE]
}), asNamespace("utils"))
