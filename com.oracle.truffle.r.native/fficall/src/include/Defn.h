/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

// This is a cut-down version of src/include/Defn.h that is a safe replacement for use with FastR

#ifndef DEFN_H_
#define DEFN_H_

#define HAVE_ERRNO_H 1

#include <stdlib.h>
#include <alloca.h> // Required for non gcc compilers
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

#define checkArity(a,b) Rf_checkArityCall(a,b,call)

void sortVector(SEXP, Rboolean);
int Scollate(SEXP a, SEXP b);
void Rf_checkArityCall(SEXP, SEXP, SEXP);

/* ../main/devices.c, used in memory.c, gnuwin32/extra.c */
#define R_MaxDevices 64

extern SEXP R_DevicesSymbol;
extern SEXP R_DeviceSymbol;
extern Rboolean FASTR_R_Interactive();
#define R_Interactive FASTR_R_Interactive()
extern Rboolean R_Visible;
int	R_ReadConsole(const char *, unsigned char *, int, int);
extern const char *R_Home;
extern const char *R_TempDir;

//#define HAVE_MBSTATE_T 1 // actually from config.h

extern Rboolean utf8locale;
extern Rboolean mbcslocale;
extern Rboolean latin1locale;

#define INI_as(v)
extern char* OutDec	INI_as(".");
extern Rboolean known_to_be_latin1 INI_as(FALSE);
extern Rboolean known_to_be_utf8 INI_as(FALSE);

extern int R_dec_min_exponent;
extern unsigned int max_contour_segments;

typedef SEXP (*CCODE)(SEXP, SEXP, SEXP, SEXP);

CCODE (PRIMFUN)(SEXP x);

/* main/sort.c */
void orderVector1(int *indx, int n, SEXP key, Rboolean nalast,
		  Rboolean decreasing, SEXP rho);

#define Unix
#ifdef Unix
# define OSTYPE      "unix"
# define FILESEP     "/"
#endif /* Unix */

#ifdef Win32
# define OSTYPE      "windows"
# define FILESEP     "/"
#endif /* Win32 */

#include <wchar.h>

typedef unsigned short ucs2_t;

#define streql(s, t)	(!strcmp((s), (t)))

#endif /* DEFN_H_ */
