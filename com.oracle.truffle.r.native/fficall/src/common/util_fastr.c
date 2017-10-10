/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

#include <Rinternals.h>
#include <stdlib.h>
#include <float.h>
#include <R_ext/RS.h>

#define _(Source) (Source)

// selected functions from util.c:

const static struct {
    const char * const str;
    const int type;
}
TypeTable[] = {
    { "NULL",		NILSXP	   },  /* real types */
    { "symbol",		SYMSXP	   },
    { "pairlist",	LISTSXP	   },
    { "closure",	CLOSXP	   },
    { "environment",	ENVSXP	   },
    { "promise",	PROMSXP	   },
    { "language",	LANGSXP	   },
    { "special",	SPECIALSXP },
    { "builtin",	BUILTINSXP },
    { "char",		CHARSXP	   },
    { "logical",	LGLSXP	   },
    { "integer",	INTSXP	   },
    { "double",		REALSXP	   }, /*-  "real", for R <= 0.61.x */
    { "complex",	CPLXSXP	   },
    { "character",	STRSXP	   },
    { "...",		DOTSXP	   },
    { "any",		ANYSXP	   },
    { "expression",	EXPRSXP	   },
    { "list",		VECSXP	   },
    { "externalptr",	EXTPTRSXP  },
    { "bytecode",	BCODESXP   },
    { "weakref",	WEAKREFSXP },
    { "raw",		RAWSXP },
    { "S4",		S4SXP },
    /* aliases : */
    { "numeric",	REALSXP	   },
    { "name",		SYMSXP	   },

    { (char *)NULL,	-1	   }
};

const char *Rf_type2char(SEXPTYPE t) {
    int i;

    for (i = 0; TypeTable[i].str; i++) {
	if (TypeTable[i].type == t)
	    return TypeTable[i].str;
    }
    warning(_("type %d is unimplemented in '%s'"), t, "type2char");
    static char buf[50];
    snprintf(buf, 50, "unknown type #%d", t);
    return buf;
}

SEXP Rf_type2str(SEXPTYPE t) {
    // implementation copied (almost) verbatim from util.c
    int i;

    for (i = 0; TypeTable[i].str; i++) {
	if (TypeTable[i].type == t)
	    return mkChar(TypeTable[i].str);
    }
    warning(_("type %d is unimplemented in '%s'"), t, "type2str");
    char buf[50];
    snprintf(buf, 50, "unknown type #%d", t);
    return Rf_mkChar(buf);
}

void init_util() {

}

void F77_NAME(rexitc)(char *msg, int *nchar)
{
    int nc = *nchar;
    char buf[256];
    if(nc > 255) {
	warning(_("error message truncated to 255 chars"));
	nc = 255;
    }
    strncpy(buf, msg, (size_t) nc);
    buf[nc] = '\0';
    error("%s", buf);
}

void F77_NAME(rwarnc)(char *msg, int *nchar)
{
    int nc = *nchar;
    char buf[256];
    if(nc > 255) {
	warning(_("warning message truncated to 255 chars"));
	nc = 255;
    }
    strncpy(buf, msg, (size_t) nc);
    buf[nc] = '\0';
    warning("%s", buf);
}

void F77_NAME(rchkusr)(void)
{
    R_CheckUserInterrupt();
}

size_t
Rf_utf8towcs(wchar_t *wc, const char *s, size_t n)
{
    ssize_t m, res = 0;
    const char *t;
    wchar_t *p;
    wchar_t local;

    if(wc)
	for(p = wc, t = s; ; p++, t += m) {
	    m  = (ssize_t) utf8toucs(p, t);
	    if (m < 0) error(_("invalid input '%s' in 'utf8towcs'"), s);
	    if (m == 0) break;
	    res ++;
	    if (res >= n) break;
	}
    else
	for(t = s; ; res++, t += m) {
	    m  = (ssize_t) utf8toucs(&local, t);
	    if (m < 0) error(_("invalid input '%s' in 'utf8towcs'"), s);
	    if (m == 0) break;
	}
    return (size_t) res;
}

#include <errno.h>

/* Previous versions of R (< 2.3.0) assumed wchar_t was in Unicode
   (and it commonly is).  These functions do not. */
# ifdef WORDS_BIGENDIAN
static const char UCS2ENC[] = "UCS-2BE";
# else
static const char UCS2ENC[] = "UCS-2LE";
# endif

typedef unsigned short ucs2_t;

/*
 * out=NULL returns the number of the MBCS chars
 */
/* Note: this does not terminate out, as all current uses are to look
 * at 'out' a wchar at a time, and sometimes just one char.
 */
size_t mbcsToUcs2(const char *in, ucs2_t *out, int nout, int enc)
{
    void   *cd = NULL ;
    const char *i_buf;
    char *o_buf;
    size_t  i_len, o_len, status, wc_len;
    /* out length */
    wc_len = (enc == CE_UTF8)? utf8towcs(NULL, in, 0) : mbstowcs(NULL, in, 0);
    if (out == NULL || (int)wc_len < 0) return wc_len;

    if ((void*)-1 == (cd = Riconv_open(UCS2ENC, (enc == CE_UTF8) ? "UTF-8": "")))
	return (size_t) -1;

    i_buf = (char *)in;
    i_len = strlen(in); /* not including terminator */
    o_buf = (char *)out;
    o_len = ((size_t) nout) * sizeof(ucs2_t);
    status = Riconv(cd, &i_buf, (size_t *)&i_len, &o_buf, (size_t *)&o_len);
    int serrno = errno;
    Riconv_close(cd);
    if (status == (size_t)-1) {
	switch(serrno){
	case EINVAL:
	    return (size_t) -2;
	case EILSEQ:
	    return (size_t) -1;
	case E2BIG:
	    break;
	default:
	    errno = EILSEQ;
	    return (size_t) -1;
	}
    }
    return wc_len; /* status would be better? */
}

Rboolean strIsASCII(const char *str)
{
    const char *p;
    for(p = str; *p; p++)
	if((unsigned int)*p > 0x7F) return FALSE;
    return TRUE;
}



SEXP nthcdr(SEXP s, int n)
{
    if (isList(s) || isLanguage(s) || isFrame(s) || TYPEOF(s) == DOTSXP ) {
	while( n-- > 0 ) {
	    if (s == R_NilValue)
		error(_("'nthcdr' list shorter than %d"), n);
	    s = CDR(s);
	}
	return s;
    }
    else error(_("'nthcdr' needs a list to CDR down"));
    return R_NilValue;/* for -Wall */
}

#define LDOUBLE double

double R_strtod5(const char *str, char **endptr, char dec,
                 Rboolean NA, int exact)
{
    LDOUBLE ans = 0.0, p10 = 10.0, fac = 1.0;
    int n, expn = 0, sign = 1, ndigits = 0, exph = -1;
    const char *p = str;

    /* optional whitespace */
    while (isspace(*p)) p++;

    if (NA && strncmp(p, "NA", 2) == 0) {
        ans = NA_REAL;
        p += 2;
        goto done;
    }

   /* optional sign */
    switch (*p) {
    case '-': sign = -1;
    case '+': p++;
    default: ;
    }

    if (strncasecmp(p, "NaN", 3) == 0) {
        ans = R_NaN;
        p += 3;
        goto done;
    /* C99 specifies this: must come first to avoid 'inf' match */
    } else if (strncasecmp(p, "infinity", 8) == 0) {
        ans = R_PosInf;
        p += 8;
        goto done;
    } else if (strncasecmp(p, "Inf", 3) == 0) {
        ans = R_PosInf;
        p += 3;
        goto done;
    }

    if(strlen(p) > 2 && p[0] == '0' && (p[1] == 'x' || p[1] == 'X')) {
        /* This will overflow to Inf if appropriate */
        for(p += 2; p; p++) {
            if('0' <= *p && *p <= '9') ans = 16*ans + (*p -'0');
            else if('a' <= *p && *p <= 'f') ans = 16*ans + (*p -'a' + 10);
            else if('A' <= *p && *p <= 'F') ans = 16*ans + (*p -'A' + 10);
            else if(*p == dec) {exph = 0; continue;}
            else break;
            if (exph >= 0) exph += 4;
        }
#define strtod_EXACT_CLAUSE                                             \
        if(exact && ans > 0x1.fffffffffffffp52) {                       \
            if(exact == NA_LOGICAL)                                     \
                warning(_(                                              \
                "accuracy loss in conversion from \"%s\" to numeric"),  \
                        str);                                           \
            else {                                                      \
                ans = NA_REAL;                                          \
                p = str; /* back out */                                 \
                goto done;                                              \
            }                                                           \
        }
        strtod_EXACT_CLAUSE;
        if (*p == 'p' || *p == 'P') {
            int expsign = 1;
            double p2 = 2.0;
            switch(*++p) {
            case '-': expsign = -1;
            case '+': p++;
            default: ;
            }
            /* The test for n is in response to PR#16358; it's not right if the exponent is
               very large, but the overflow or underflow below will handle it. */
#define MAX_EXPONENT_PREFIX 9999
            for (n = 0; *p >= '0' && *p <= '9'; p++) n = (n < MAX_EXPONENT_PREFIX) ? n * 10 + (*p - '0') : n;
            if (ans != 0.0) { /* PR#15976:  allow big exponents on 0 */
                expn += expsign * n;
                if(exph > 0) {
                    if (expn - exph < -122) {   /* PR#17199:  fac may overflow below if expn - exph is too small.
                                                   2^-122 is a bit bigger than 1E-37, so should be fine on all systems */
                        for (n = exph, fac = 1.0; n; n >>= 1, p2 *= p2)
                            if (n & 1) fac *= p2;
                        ans /= fac;
                        p2 = 2.0;
                    } else
                        expn -= exph;
                }
                if (expn < 0) {
                    for (n = -expn, fac = 1.0; n; n >>= 1, p2 *= p2)
                        if (n & 1) fac *= p2;
                    ans /= fac;
                } else {
                    for (n = expn, fac = 1.0; n; n >>= 1, p2 *= p2)
                        if (n & 1) fac *= p2;
                    ans *= fac;
                }
            }
        }
        goto done;
    }

    for ( ; *p >= '0' && *p <= '9'; p++, ndigits++) ans = 10*ans + (*p - '0');
    if (*p == dec)
        for (p++; *p >= '0' && *p <= '9'; p++, ndigits++, expn--)
            ans = 10*ans + (*p - '0');
    if (ndigits == 0) {
        ans = NA_REAL;
        p = str; /* back out */
        goto done;
    }
    strtod_EXACT_CLAUSE;

    if (*p == 'e' || *p == 'E') {
        int expsign = 1;
        switch(*++p) {
        case '-': expsign = -1;
        case '+': p++;
        default: ;
        }
        for (n = 0; *p >= '0' && *p <= '9'; p++) n = (n < MAX_EXPONENT_PREFIX) ? n * 10 + (*p - '0') : n;
        expn += expsign * n;
    }

    /* avoid unnecessary underflow for large negative exponents */
    if (expn + ndigits < -300) {
        for (n = 0; n < ndigits; n++) ans /= 10.0;
        expn += ndigits;
    }
    if (expn < -307) { /* use underflow, not overflow */
        for (n = -expn, fac = 1.0; n; n >>= 1, p10 *= p10)
            if (n & 1) fac /= p10;
        ans *= fac;
    } else if (expn < 0) { /* positive powers are exact */
        for (n = -expn, fac = 1.0; n; n >>= 1, p10 *= p10)
            if (n & 1) fac *= p10;
        ans /= fac;
    } else if (ans != 0.0) { /* PR#15976:  allow big exponents on 0, e.g. 0E4933 */
        for (n = expn, fac = 1.0; n; n >>= 1, p10 *= p10)
            if (n & 1) fac *= p10;
        ans *= fac;
    }

    /* explicit overflow to infinity */
    if (ans > DBL_MAX) {
        if (endptr) *endptr = (char *) p;
        return (sign > 0) ? R_PosInf : R_NegInf;
    }

done:
    if (endptr) *endptr = (char *) p;
    return sign * (double) ans;
}


double R_strtod4(const char *str, char **endptr, char dec, Rboolean NA)
{
    return R_strtod5(str, endptr, dec, NA, FALSE);
}

double R_strtod(const char *str, char **endptr)
{
    return R_strtod5(str, endptr, '.', FALSE, FALSE);
}

double R_atof(const char *str)
{
    return R_strtod5(str, NULL, '.', FALSE, FALSE);
}
