/*
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates
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

// This is a cut-down version of src/include/Defn.h that is a safe replacement for use with FastR

#ifndef DEFN_H_
#define DEFN_H_

#define HAVE_ERRNO_H 1

#include <stdlib.h>
#include <alloca.h> // Required for non gcc compilers
#include <Rinternals.h>

#ifdef R_USE_SIGNALS
#ifdef Win32
# include <psignal.h>
#else
# include <signal.h>
# include <setjmp.h>
#endif
#endif

// various definitions required to compile GNU-R code:

#define attribute_hidden
#define HAVE_NEARBYINT

#define F77_SYMBOL(x)	x
#define F77_QSYMBOL(x) #x

#define Rexp10(x) pow(10.0, x)

# define onintr			Rf_onintr
# define onintrNoResume		Rf_onintrNoResume

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
void onintr(void);
void onintrNoResume(void);

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

extern Rboolean R_interrupts_suspended;
extern int R_interrupts_pending;

//#define HAVE_MBSTATE_T 1 // actually from config.h

extern Rboolean utf8locale;
extern Rboolean mbcslocale;
extern Rboolean latin1locale;

#define mbs_init(x) memset(x, 0, sizeof(mbstate_t))

int Rstrlen(SEXP s, int quote);

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

/* From localecharset.c */
extern const char *locale2charset(const char *);

#define Unix
#ifdef Unix
# define OSTYPE      "unix"
# define FILESEP     "/"
#endif /* Unix */

#ifdef Win32
# define OSTYPE      "windows"
# define FILESEP     "/"
#endif /* Win32 */

#ifdef R_USE_SIGNALS
#ifdef HAVE_POSIX_SETJMP
# define SIGJMP_BUF sigjmp_buf
# define SIGSETJMP(x,s) sigsetjmp(x,s)
# define SIGLONGJMP(x,i) siglongjmp(x,i)
# define JMP_BUF sigjmp_buf
# define SETJMP(x) sigsetjmp(x,0)
# define LONGJMP(x,i) siglongjmp(x,i)
#else
# define SIGJMP_BUF jmp_buf
# define SIGSETJMP(x,s) setjmp(x)
# define SIGLONGJMP(x,i) longjmp(x,i)
# define JMP_BUF jmp_buf
# define SETJMP(x) setjmp(x)
# define LONGJMP(x,i) longjmp(x,i)
#endif
#endif

#include <wchar.h>

typedef unsigned short ucs2_t;

#define streql(s, t)	(!strcmp((s), (t)))

#endif /* DEFN_H_ */
