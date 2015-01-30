/*
 *  R : A Computer Language for Statistical Data Analysis
 *  Copyright (C) 2012-2013  The R Core Team
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
 */

#ifdef ENABLE_NLS
//#include <libintl.h>
#define _(String) dgettext ("utils", String)
#else
#define _(String) (String)
#endif

SEXP objectSize(SEXP s) { return NULL; }
SEXP unzip(SEXP args) { return NULL; }
SEXP Rprof(SEXP args) { return NULL; }
SEXP Rprofmem(SEXP args) { return NULL; }

SEXP countfields(SEXP args) { return NULL; }
SEXP flushconsole(void) { return NULL; }
SEXP menu(SEXP args) { return NULL; }
SEXP readtablehead(SEXP args) { return NULL; }
SEXP typeconvert(SEXP call, SEXP op, SEXP args, SEXP env) { return NULL; }
SEXP writetable(SEXP call, SEXP op, SEXP args, SEXP env) { return NULL; }

SEXP crc64(SEXP in) { return NULL; }
SEXP nsl(SEXP hostname) { return NULL; }
SEXP download(SEXP args) { return NULL; }

SEXP sockconnect(SEXP sport, SEXP shost) { return NULL; }
SEXP sockread(SEXP sport, SEXP smaxlen) { return NULL; }
SEXP sockclose(SEXP sport) { return NULL; }
SEXP sockopen(SEXP sport) { return NULL; }
SEXP socklisten(SEXP sport) { return NULL; }
SEXP sockwrite(SEXP sport, SEXP sstring) { return NULL; }

SEXP addhistory(SEXP call, SEXP op, SEXP args, SEXP rho) { return NULL; }
SEXP loadhistory(SEXP call, SEXP op, SEXP args, SEXP rho) { return NULL; }
SEXP savehistory(SEXP call, SEXP op, SEXP args, SEXP rho) { return NULL; }
SEXP dataentry(SEXP call, SEXP op, SEXP args, SEXP rho) { return NULL; }
SEXP dataviewer(SEXP call, SEXP op, SEXP args, SEXP rho) { return NULL; }
SEXP edit(SEXP call, SEXP op, SEXP args, SEXP rho) { return NULL; }
SEXP fileedit(SEXP call, SEXP op, SEXP args, SEXP rho) { return NULL; }
SEXP selectlist(SEXP call, SEXP op, SEXP args, SEXP rho) { return NULL; }

SEXP processevents(void) { return NULL; }

SEXP octsize(SEXP s) { return NULL; }

