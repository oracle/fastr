/*
 *  R : A Computer Language for Statistical Data Analysis
 *  Copyright (C) 2002-2012	The R Core Team.
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
 */
/*
 *  This file replaces the previously used ROUTINES file and is used to
 *  explicitly register native routines that are located in the R
 *  executable (e.g. R.bin, Rgui.exe) but which are intended to be
 *  accessible to S code via .C(), .Fortran(), .Call(), .External().
 *  The approach we use here is the regular registration mechanism that
 *  packages can use to explicitly list the symbols to be exported.
 *  For .C() and .Call() routines, we give the number of arguments
 *  expected.
 *  For .C() routines, we also specify the types of the arguments.
 *  For .Fortran() and .External() routines, we specify only the name
 *  and symbol.

 *  To add an entry, first determine by which interface the routine will
 *  be accessed:
 *   .C, .Call, .External or .Fortran
 *  Then add an entry to
 *    cMethods, callMethods, externalMethods, or fortranMethods
 *  respectively
 *
 *  DTL 14-Dec-2002
 */

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif

//#include <Defn.h>
#include <R_ext/Rdynload.h>
#include <R_ext/Applic.h>
#include <R_ext/Linpack.h>


/*  These get the declarations of some routines refernced here but
    not explicitly declared.    This is necessary when we link with
    a C++ compiler because the linkage changes as the declarations
    are (currently) within extern "C" blocks.
*/
#include <R_ext/Callbacks.h>
//#include <Rdynpriv.h>

extern void dqrdc2_(double *x, int *ldx, int *n, int *p, double *tol, int *rank, double *qraux, int* pivot, double *work);
extern void dqrcf_(double *x, int *n, int *k, double *qraux, double *y, int *ny, double *b, int* info);
extern void dqrls_(double *x, int *n, int *p, double *y, int *ny, double *tol, double *b, double *rsd, double *qty, int *k, int *jpvt, double *qraux, double *work);
extern void dqrqty_(double *x, int *n, int *k, double *qraux, double *y, int *ny, double *qty);
extern void dqrqy_(double *x, int *n, int *k, double *qraux, double *y, int *ny, double *qy);
extern void dqrrsd_(double *x, int *n, int *k, double *qraux, double *y, int *ny, double *rsd);
extern void dqrxb_(double *x, int *n, int *k, double *qraux, double *y, int *ny, double *xb);


#define FDEF(name, n)  {#name, (DL_FUNC) &(name##_), n, NULL}
static R_FortranMethodDef fortranMethods[] = {
    /* LINPACK */
    FDEF(dqrcf, 8), // qr and auxiliaries
    FDEF(dqrdc2, 9),
    FDEF(dqrqty, 7),
    FDEF(dqrqy, 7),
    FDEF(dqrrsd, 7),
    FDEF(dqrxb, 7),
    FDEF(dtrco, 6), // .kappa_tri

    {NULL, NULL, 0}
};

void R_init_base(DllInfo *dll)
{
//    R_registerRoutines(dll, NULL, callMethods, fortranMethods, NULL);
	R_registerRoutines(dll, NULL, NULL, fortranMethods, NULL);
    R_useDynamicSymbols(dll, FALSE);
}
