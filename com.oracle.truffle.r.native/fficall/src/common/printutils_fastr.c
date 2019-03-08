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

extern void *unimplemented(char *msg);

// Rstrlen is used in code from gnur-patch
// It is not clear, whether we really need it
int Rstrlen(SEXP s, int quote) {
    unimplemented("Rstrlen");
    return 0;
}

// IndexWidth used in code from gnur-patch
// It is not clear, whether we really need it
int attribute_hidden IndexWidth(R_xlen_t n) {
    return (int) (log10(n + 0.5) + 1);
}

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

