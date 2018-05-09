/*
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */

// Copied from unix/X11.c

#include <Rinternals.h>

SEXP do_bmVersion(void)
{
    SEXP ans = PROTECT(allocVector(STRSXP, 3)),
	nms = PROTECT(allocVector(STRSXP, 3));
    setAttrib(ans, R_NamesSymbol, nms);
    SET_STRING_ELT(nms, 0, mkChar("libpng"));
    SET_STRING_ELT(nms, 1, mkChar("jpeg"));
    SET_STRING_ELT(nms, 2, mkChar("libtiff"));
    UNPROTECT(2);
    return ans;
}

