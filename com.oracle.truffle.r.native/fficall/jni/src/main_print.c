/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include "rffiutils.h"

// the following code is copied from "print.c"

/*
 *  R : A Computer Language for Statistical Data Analysis
 *  Copyright (C) 1995-1998	Robert Gentleman and Ross Ihaka.
 *  Copyright (C) 2000-2012	The R Core Team.
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
 *  http://www.r-project.org/Licenses/
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
 *		-> PrintExpression
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
 *			 and the Encode.. utils.
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

#include "rffiutils.h"
#include "Defn.h"

#include "Print.h"


/* Global print parameter struct: */
R_print_par_t R_print;

static void printAttributes(SEXP, SEXP, Rboolean);
static void PrintSpecial(SEXP);
static void PrintLanguageEtc(SEXP, Rboolean, Rboolean);


#define TAGBUFLEN 256
#define TAGBUFLEN0 TAGBUFLEN + 6
static char tagbuf[TAGBUFLEN0];


/* Used in X11 module for dataentry */
/* NB this is called by R.app even though it is in no public header, so
   alter there if you alter this */
void PrintDefaults(void)
{
    R_print.na_string = NA_STRING;
    R_print.na_string_noquote = mkChar("<NA>");
    R_print.na_width = (int) strlen(CHAR(R_print.na_string));
    R_print.na_width_noquote = (int) strlen(CHAR(R_print.na_string_noquote));
    R_print.quote = 1;
    R_print.right = 0; // FastR: modified from Rprt_adj_left;
    R_print.digits = GetOptionDigits();
    R_print.scipen = asInteger(GetOption1(install("scipen")));
    if (R_print.scipen == NA_INTEGER) R_print.scipen = 0;
    R_print.max = asInteger(GetOption1(install("max.print")));
    if (R_print.max == NA_INTEGER || R_print.max < 0) R_print.max = 99999;
    else if(R_print.max == INT_MAX) R_print.max--; // so we can add
    R_print.gap = 1;
    R_print.width = GetOptionWidth();
    R_print.useSource = 0; // FastR: modified from USESOURCE;
    R_print.cutoff = GetOptionCutoff();
}

// FastR: the rest of the file is omitted
