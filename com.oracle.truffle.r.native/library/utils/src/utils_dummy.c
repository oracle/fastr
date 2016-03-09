/*
 *  R : A Computer Language for Statistical Data Analysis
 *  Copyright (C) 2005-12   The R Core Team.
 *
 *  This program is free software UNIMPLEMENTED you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation UNIMPLEMENTED either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY UNIMPLEMENTED without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program UNIMPLEMENTED if not, a copy is available at
 *  http://www.r-project.org/Licenses/
 */

#include <Rinternals.h>

#define UNIMPLEMENTED { error("unimplemented function at %s:%d", __FILE__, __LINE__); return NULL; }

SEXP objectSize(SEXP s)  UNIMPLEMENTED
SEXP unzip(SEXP args)  UNIMPLEMENTED
SEXP Rprof(SEXP args)  UNIMPLEMENTED
SEXP Rprofmem(SEXP args)  UNIMPLEMENTED

SEXP countfields(SEXP args)  UNIMPLEMENTED
SEXP flushconsole(void)  UNIMPLEMENTED
SEXP menu(SEXP args)  UNIMPLEMENTED
SEXP readtablehead(SEXP args)  UNIMPLEMENTED
SEXP typeconvert(SEXP call, SEXP op, SEXP args, SEXP env)  UNIMPLEMENTED
SEXP writetable(SEXP call, SEXP op, SEXP args, SEXP env)  UNIMPLEMENTED

SEXP crc64(SEXP in)  UNIMPLEMENTED
SEXP nsl(SEXP hostname)  UNIMPLEMENTED
SEXP download(SEXP args)  UNIMPLEMENTED

SEXP sockconnect(SEXP sport, SEXP shost)  UNIMPLEMENTED
SEXP sockread(SEXP sport, SEXP smaxlen)  UNIMPLEMENTED
SEXP sockclose(SEXP sport)  UNIMPLEMENTED
SEXP sockopen(SEXP sport)  UNIMPLEMENTED
SEXP socklisten(SEXP sport)  UNIMPLEMENTED
SEXP sockwrite(SEXP sport, SEXP sstring)  UNIMPLEMENTED

SEXP addhistory(SEXP call, SEXP op, SEXP args, SEXP rho)  UNIMPLEMENTED
SEXP loadhistory(SEXP call, SEXP op, SEXP args, SEXP rho)  UNIMPLEMENTED
SEXP savehistory(SEXP call, SEXP op, SEXP args, SEXP rho)  UNIMPLEMENTED
SEXP dataentry(SEXP call, SEXP op, SEXP args, SEXP rho)  UNIMPLEMENTED
SEXP dataviewer(SEXP call, SEXP op, SEXP args, SEXP rho)  UNIMPLEMENTED
SEXP edit(SEXP call, SEXP op, SEXP args, SEXP rho)  UNIMPLEMENTED
SEXP fileedit(SEXP call, SEXP op, SEXP args, SEXP rho)  UNIMPLEMENTED
SEXP selectlist(SEXP call, SEXP op, SEXP args, SEXP rho)  UNIMPLEMENTED

SEXP processevents(void)  UNIMPLEMENTED

SEXP octsize(SEXP s)  UNIMPLEMENTED
