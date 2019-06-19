z/*
 *  R : A Computer Language for Statistical Data Analysis
 *  Copyright (C) 2000-2018	The R Core Team.
 *  Copyright (C) 1995-1998	Robert Gentleman and Ross Ihaka.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, a copy is available at
 *  https://www.R-project.org/Licenses/
 *
 *
 *  print.default()  ->	 do_printdefault (with call tree below)
 *
 *  auto-printing   ->  PrintValueEnv
 *                      -> PrintValueRec
 *                      -> call print() for objects
 *  Note that auto-printing does not call print.default.
 *  PrintValue, R_PV are similar to auto-printing.
 *
 *  do_printdefault
 *	-> PrintDefaults
 *	-> CustomPrintValue
 *	    -> PrintValueRec
 *		-> __ITSELF__  (recursion)
 *		-> PrintGenericVector	-> PrintValueRec  (recursion)
 *		-> printList		-> PrintValueRec  (recursion)
 *		-> printAttributes	-> PrintValueRec  (recursion)
 *		-> PrintSpecial
 *		-> PrintExpression
 *		-> PrintLanguage        -> PrintLanguageEtc
 *		-> PrintClosure         -> PrintLanguageEtc
 *		-> printVector		>>>>> ./printvector.c
 *		-> printNamedVector	>>>>> ./printvector.c
 *		-> printMatrix		>>>>> ./printarray.c
 *		-> printArray		>>>>> ./printarray.c
 *
 *  do_prmatrix
 *	-> PrintDefaults
 *	-> printMatrix			>>>>> ./printarray.c
 *
 *
 *  See ./printutils.c	 for general remarks on Printing
 *			 and the EncodeString() and all Encode*() utils,
 *
 *  Also ./printvector.c,  ./printarray.c
 *
 *  do_sink moved to connections.c as of 1.3.0
 *
 *  <FIXME> These routines are not re-entrant: they reset the
 *  global R_print.
 *  </FIXME>
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include "Defn.h"
#include <Internal.h>
#include <R_ext/RS.h>

/* Fortran-callable error routine for lapack */

void NORET F77_NAME(xerbla)(const char *srname, int *info)
{
   /* srname is not null-terminated.  It should be 6 characters. */
    char buf[7];
    strncpy(buf, srname, 6);
    buf[6] = '\0';
    error(_("BLAS/LAPACK routine '%6s' gave error code %d"), buf, -(*info));
}
