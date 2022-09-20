/*
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates
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

#include <Defn.h>
#include <Print.h>
#include <R_ext/RS.h>


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
int PrintDefaults(void)
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
    return 0;
}

void F77_NAME(xerbla)(const char *srname, int *info)
{
   /* srname is not null-terminated.  It should be 6 characters. */
    char buf[7];
    strncpy(buf, srname, 6);
    buf[6] = '\0';
    printf("BLAS/LAPACK routine '%6s' gave error code %d", buf, -(*info));
}

// following functions are here to resolve externals used in functions from gnur-patch

attribute_hidden
int F77_NAME(realp0) (const char *label, int *nchar, float *data, int *ndata)
{
    int k, nc = *nchar, nd = *ndata;
    double *ddata;

    if(nc < 0) nc = (int) strlen(label);
    if(nc > 255) {
        warning(_("invalid character length in 'realpr'"));
        nc = 0;
    }
    else if(nc > 0) {
        for (k = 0; k < nc; k++)
            Rprintf("%c", label[k]);
        Rprintf("\n");
    }
    if(nd > 0) {
        ddata = (double *) malloc(nd*sizeof(double));
        if(!ddata) error(_("memory allocation error in 'realpr'"));
        for (k = 0; k < nd; k++) ddata[k] = (double) data[k];
        error("Unimplemented: printRealVector in print_fastr.c");
        // printRealVector(ddata, nd, 1);
        free(ddata);
    }
    return(0);
}

attribute_hidden
int F77_NAME(intpr0) (const char *label, int *nchar, int *data, int *ndata)
{
    int k, nc = *nchar;

    if(nc < 0) nc = (int) strlen(label);
    if(nc > 255) {
        warning(_("invalid character length in 'intpr'"));
        nc = 0;
    } else if(nc > 0) {
        for (k = 0; k < nc; k++)
            Rprintf("%c", label[k]);
        Rprintf("\n");
    }
    if(*ndata > 0) {
        error("Unimplemented: printIntegerVector in print_fastr.c");
        // printIntegerVector(data, *ndata, 1);
    }
    return(0);
}

attribute_hidden
int F77_NAME(dblep0) (const char *label, int *nchar, double *data, int *ndata)
{
    int k, nc = *nchar;

    if(nc < 0) nc = (int) strlen(label);
    if(nc > 255) {
        warning(_("invalid character length in 'dblepr'"));
        nc = 0;
    } else if(nc > 0) {
        for (k = 0; k < nc; k++)
            Rprintf("%c", label[k]);
        Rprintf("\n");
    }
    if(*ndata > 0) {
        error("Unimplemented: printRealVector in print_fastr.c");
        // printRealVector(data, *ndata, 1);
    }
    return(0);
}

// FastR: the rest of the file is omitted
