/*
 *  R : A Computer Language for Statistical Data Analysis
 *  Copyright (C) 2005-12   The R Core Team.
 *
 *  This program is free software{ unimplemented_utils(); unimplemented_tools(); return NULL; } you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation{ unimplemented_utils(); unimplemented_tools(); return NULL; } either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY{ unimplemented_utils(); unimplemented_tools(); return NULL; } without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program{ unimplemented_utils(); unimplemented_tools(); return NULL; } if not, a copy is available at
 *  http://www.r-project.org/Licenses/
 */

#include <Rinternals.h>

#define UNIMPLEMENTED { error("unimplemented function at %s:%d", __FILE__, __LINE__); return NULL; }

SEXP delim_match(SEXP x, SEXP delims) UNIMPLEMENTED
SEXP dirchmod(SEXP dr) UNIMPLEMENTED
SEXP Rmd5(SEXP files) UNIMPLEMENTED
SEXP check_nonASCII(SEXP text, SEXP ignore_quotes) UNIMPLEMENTED
SEXP check_nonASCII2(SEXP text) UNIMPLEMENTED
SEXP doTabExpand(SEXP strings, SEXP starts) UNIMPLEMENTED
SEXP ps_kill(SEXP pid, SEXP signal) UNIMPLEMENTED
SEXP ps_sigs(SEXP pid) UNIMPLEMENTED
SEXP ps_priority(SEXP pid, SEXP value) UNIMPLEMENTED
SEXP codeFilesAppend(SEXP f1, SEXP f2) UNIMPLEMENTED
SEXP getfmts(SEXP format) UNIMPLEMENTED
SEXP startHTTPD(SEXP sIP, SEXP sPort) UNIMPLEMENTED
SEXP stopHTTPD(void) UNIMPLEMENTED

SEXP C_parseLatex(SEXP call, SEXP op, SEXP args, SEXP env) UNIMPLEMENTED
//SEXP C_parseRd(SEXP call, SEXP op, SEXP args, SEXP env);
SEXP C_parseRd(SEXP con, SEXP source, SEXP verbose, SEXP fragment, SEXP basename, SEXP warningcalls);
SEXP C_deparseRd(SEXP e, SEXP state) UNIMPLEMENTED
