/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
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

