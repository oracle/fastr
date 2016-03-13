/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

// the following code is copied from "printutils.c"

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <Defn.h>
#include <Print.h>

#ifndef min
#define min(a, b) (((a)<(b))?(a):(b))
#endif
#define NB 1000

const char *EncodeReal(double x, int w, int d, int e, char cdec)
{
    char dec[2];
    dec[0] = cdec; dec[1] = '\0';
    return EncodeReal0(x, w, d, e, dec);
}

const char *EncodeReal0(double x, int w, int d, int e, const char *dec)
{
    static char buff[NB], buff2[2*NB];
    char fmt[20], *out = buff;

    /* IEEE allows signed zeros (yuck!) */
    if (x == 0.0) x = 0.0;
    if (!R_FINITE(x)) {
	if(ISNA(x)) snprintf(buff, NB, "%*s", min(w, (NB-1)), CHAR(R_print.na_string));
	else if(ISNAN(x)) snprintf(buff, NB, "%*s", min(w, (NB-1)), "NaN");
	else if(x > 0) snprintf(buff, NB, "%*s", min(w, (NB-1)), "Inf");
	else snprintf(buff, NB, "%*s", min(w, (NB-1)), "-Inf");
    }
    else if (e) {
	if(d) {
	    sprintf(fmt,"%%#%d.%de", min(w, (NB-1)), d);
	    snprintf(buff, NB, fmt, x);
	}
	else {
	    sprintf(fmt,"%%%d.%de", min(w, (NB-1)), d);
	    snprintf(buff, NB, fmt, x);
	}
    }
    else { /* e = 0 */
	sprintf(fmt,"%%%d.%df", min(w, (NB-1)), d);
	snprintf(buff, NB, fmt, x);
    }
    buff[NB-1] = '\0';

    if(strcmp(dec, ".")) {
	char *p, *q;
	for(p = buff, q = buff2; *p; p++) {
	    if(*p == '.') for(const char *r = dec; *r; r++) *q++ = *r;
	    else *q++ = *p;
	}
	*q = '\0';
	out = buff2;
    }

    return out;
}

