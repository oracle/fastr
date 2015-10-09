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
#include <Print.h>

Rboolean known_to_be_latin1 = FALSE;
Rboolean known_to_be_utf8 = FALSE;



int R_cairoCdynload(int local, int now)
{
	unimplemented("R_cairoCdynload");
    return 0;
}

SEXP do_X11(SEXP call, SEXP op, SEXP args, SEXP rho)
{
	unimplemented("do_X11");
    return R_NilValue;
}

SEXP do_saveplot(SEXP call, SEXP op, SEXP args, SEXP rho)
{
	unimplemented("do_saveplot");
    return R_NilValue;
}


SEXP do_getGraphicsEvent(SEXP call, SEXP op, SEXP args, SEXP env)
{
    unimplemented("do_getGraphicsEvent");
    return R_NilValue;
}


SEXP do_setGraphicsEventEnv(SEXP call, SEXP op, SEXP args, SEXP env)
{
    unimplemented("do_setGraphicsEventEnv");
    return R_NilValue;
}

SEXP do_getGraphicsEventEnv(SEXP call, SEXP op, SEXP args, SEXP env)
{
    unimplemented("do_getGraphicsEventEnv");
    return R_NilValue;
}

const char *locale2charset(const char *locale)
{
	unimplemented("locale2charset");
	return NULL;
}

void setup_RdotApp(void) {
	unimplemented("setup_RdotApp");
}

const char *Rf_EncodeComplex(Rcomplex x, int wr, int dr, int er, int wi, int di, int ei, char cdec)
{
	unimplemented("Rf_EncodeComplex");
	return NULL;
}

const char *Rf_EncodeInteger(int x, int w)
{
	unimplemented("Rf_EncodeInteger");
	return NULL;
}

const char *Rf_EncodeLogical(int x, int w)
{
	unimplemented("Rf_EncodeLogical");
	return NULL;
}

// from printutils.c
#ifndef min
#define min(a, b) (((a)<(b))?(a):(b))
#endif
#define NB 1000
const char *Rf_EncodeReal(double x, int w, int d, int e, char cdec)
{
    static char buff[NB];
    char *p, fmt[20];

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

    if(cdec != '.')
      for(p = buff; *p; p++) if(*p == '.') *p = cdec;

    return buff;
}


