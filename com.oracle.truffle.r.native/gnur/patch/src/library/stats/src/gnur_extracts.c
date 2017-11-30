/*
 *  R : A Computer Language for Statistical Data Analysis
 *  Copyright (C) 2001-2012   The R Core Team.
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
 */

#include <R.h>
#include <Rinternals.h>

#include "modreg.h"
#include "nls.h"
#include "port.h"
#include "stats.h"
#include <statsR.h>
#include "ts.h"
#include <R_ext/Rdynload.h>
#include <R_ext/Visibility.h>

// extracts from some GnuR stats C files

// from d1mach.c

#include <Rmath.h>

attribute_hidden double Rf_d1mach(int i)
{
    switch(i) {
    case 1: return DBL_MIN;
    case 2: return DBL_MAX;

    case 3: /* = FLT_RADIX  ^ - DBL_MANT_DIG
	      for IEEE:  = 2^-53 = 1.110223e-16 = .5*DBL_EPSILON */
	return 0.5*DBL_EPSILON;

    case 4: /* = FLT_RADIX  ^ (1- DBL_MANT_DIG) =
	      for IEEE:  = 2^-52 = DBL_EPSILON */
	return DBL_EPSILON;

    case 5: return M_LOG10_2;

    default: return 0.0;
    }
}

#ifdef __cplusplus
extern "C"
#endif

double F77_NAME(d1mach)(int *i)
{
    return Rf_d1mach(*i);
}

// from print.c

/* xxxpr are mostly for S compatibility (as mentioned in V&R).
   The actual interfaces are now in xxxpr.f
 */

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
//    if(*ndata > 0) printRealVector(data, *ndata, 1);
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
//    if(*ndata > 0) printIntegerVector(data, *ndata, 1);
    return(0);
}

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
//	printRealVector(ddata, nd, 1);
	free(ddata);
    }
    return(0);
}
