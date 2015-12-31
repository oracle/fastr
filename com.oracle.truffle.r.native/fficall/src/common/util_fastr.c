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

#include <Rinternals.h>
#include <stdlib.h>
#include <R_ext/RS.h>

#define _(Source) (Source)

// selected functions from util.c:

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
