/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

#include "rffiutils.h"
#include "Defn.h"

extern SEXP R_DevicesSymbol;
extern Rboolean R_Interactive;
extern Rboolean R_Visible;

#define attribute_hidden
#define R_MaxDevices 64

#define HAVE_MBSTATE_T 1

extern Rboolean utf8locale;
extern Rboolean mbcslocale;
extern Rboolean latin1locale;

#include <wchar.h>


/* The type of the do_xxxx functions. */
/* These are the built-in R functions. */
typedef SEXP (*CCODE)(SEXP, SEXP, SEXP, SEXP);

CCODE (PRIMFUN)(SEXP x);

#define streql(s, t)	(!strcmp((s), (t)))


/* main/util.c */
void UNIMPLEMENTED_TYPE(const char *s, SEXP x);
void UNIMPLEMENTED_TYPEt(const char *s, SEXPTYPE t);
Rboolean Rf_strIsASCII(const char *str);
int utf8clen(char c);

typedef unsigned short ucs2_t;
size_t mbcsToUcs2(const char *in, ucs2_t *out, int nout, int enc);
/* size_t mbcsMblen(char *in);
size_t ucs2ToMbcs(ucs2_t *in, char *out);
size_t ucs2Mblen(ucs2_t *in); */
size_t utf8toucs(wchar_t *wc, const char *s);
size_t utf8towcs(wchar_t *wc, const char *s, size_t n);
size_t ucstomb(char *s, const unsigned int wc);
size_t ucstoutf8(char *s, const unsigned int wc);
size_t mbtoucs(unsigned int *wc, const char *s, size_t n);
size_t wcstoutf8(char *s, const wchar_t *wc, size_t n);

SEXP Rf_installTrChar(SEXP);

const wchar_t *wtransChar(SEXP x); /* from sysutils.c */

#define mbs_init(x) memset(x, 0, sizeof(mbstate_t))
size_t Mbrtowc(wchar_t *wc, const char *s, size_t n, mbstate_t *ps);
Rboolean mbcsValid(const char *str);
Rboolean utf8Valid(const char *str);
char *Rf_strchr(const char *s, int c);
char *Rf_strrchr(const char *s, int c);

#include "../../../gnur/R-3.1.3/src/main/engine.c"
