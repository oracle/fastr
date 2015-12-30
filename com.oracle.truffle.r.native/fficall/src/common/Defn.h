/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

// This is a cut-down version of src/include/Defn.h that is a safe replacement for use with FastR

#ifndef DEFN_H_
#define DEFN_H_

#include <jni.h>
#include <stdlib.h>
#include <Rinternals.h>

// various definitions required to compile GNU-R code:

#define attribute_hidden
#define HAVE_NEARBYINT

#define F77_SYMBOL(x)	x
#define F77_QSYMBOL(x) #x

#define Rexp10(x) pow(10.0, x)

// no NLS:
#ifndef _
#define _(String) (String)
#endif
#define N_(String) String
#define ngettext(String, StringP, N) (N > 1 ? StringP: String)

void sortVector(SEXP, Rboolean);
int Scollate(SEXP a, SEXP b);
void Rf_checkArityCall(SEXP, SEXP, SEXP);

/* ../main/devices.c, used in memory.c, gnuwin32/extra.c */
#define R_MaxDevices 64

extern SEXP R_DeviceSymbol;
extern SEXP R_DevicesSymbol;
extern Rboolean R_Interactive;
extern Rboolean R_Visible;
int	R_ReadConsole(const char *, unsigned char *, int, int);

//#define HAVE_MBSTATE_T 1 // actually from config.h

extern Rboolean utf8locale;
extern Rboolean mbcslocale;
extern Rboolean latin1locale;

extern int R_dec_min_exponent;
extern unsigned int max_contour_segments;

typedef SEXP (*CCODE)(SEXP, SEXP, SEXP, SEXP);

CCODE (PRIMFUN)(SEXP x);


#endif /* DEFN_H_ */
