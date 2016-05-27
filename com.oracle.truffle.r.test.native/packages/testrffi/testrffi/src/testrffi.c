/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

// A very simple test of the R FFI interface

#include <R.h>
#include <Rdefines.h>
#include <Rinternals.h>

void dotCModifiedArguments(int* len, int* idata, double* rdata, int* ldata) {
    for (int i = 0; i < len[0]; i++) {
	idata[i] ++;
    }
    for (int i = 0; i < len[0]; i++) {
	rdata[i] *= 0.2;
    }
    for (int i = 0; i < len[0]; i++) {
	ldata[i] = ldata[i] == 0 ? 1 : 0;
    }
}

SEXP addInt(SEXP a, SEXP b) {
	int aInt = INTEGER_VALUE(a);
	int bInt = INTEGER_VALUE(b);
	return ScalarInteger(aInt + bInt);
}

SEXP addDouble(SEXP a, SEXP b) {
	double aDouble = NUMERIC_VALUE(a);
	double bDouble = NUMERIC_VALUE(b);
	return ScalarReal(aDouble + bDouble);
}

SEXP populateIntVector(SEXP n) {
    SEXP v;
    int intN = INTEGER_VALUE(n);
    PROTECT(v = allocVector(INTSXP, intN));
    int i;
    for (i = 0; i < intN; i++) {
    	INTEGER(v)[i] = i;
    }
    UNPROTECT(1);
    return v;
}

SEXP populateLogicalVector(SEXP n) {
    SEXP v;
    int intN = INTEGER_VALUE(n);
    PROTECT(v = allocVector(LGLSXP, intN));
    int i;
    for (i = 0; i < intN; i++) {
    	LOGICAL(v)[i] = i == 0 ? TRUE : i == 1 ? NA_INTEGER : FALSE;
    }
    UNPROTECT(1);
    return v;
}

SEXP createExternalPtr(SEXP addr, SEXP tag, SEXP prot) {
	return R_MakeExternalPtr((void *) (long) INTEGER_VALUE(addr), tag, prot);
}

SEXP getExternalPtrAddr(SEXP eptr) {
	return ScalarInteger((int) R_ExternalPtrAddr(eptr));
}

SEXP invoke_TYPEOF(SEXP x) {
	return ScalarInteger(TYPEOF(x));
}

SEXP invoke_error() {
	error("invoke_error in testrffi");
}

SEXP dot_external_access_args(SEXP args) {
    args = CDR(args);
    int index = 0;
    for (; args != R_NilValue; args = CDR(args)) {
	index++;
	SEXP tag = TAG(args);
	const char *name = isNull(tag) ? "" : CHAR(PRINTNAME(tag));
	SEXP value = CAR(args);
	if (length(value) == 0) {
	    Rprintf("%d: '%s' length 0\n", index, name);
	    continue;
	}
	switch (TYPEOF(value)) {
	case LGLSXP:
	case INTSXP:
	    Rprintf("%d: '%s' %d\n", index, name, INTEGER(value)[0]);
	    break;
	case REALSXP:
	    Rprintf("%d: '%s' %f\n", index, name, REAL(value)[0]);
	    break;
	case CPLXSXP: {
	    Rcomplex complexValue = COMPLEX(value)[0];
	    Rprintf("%d: '%s' %f+%fi\n", index, name, complexValue.r,
		    complexValue.i);
	    break;
	}
	case STRSXP:
	    Rprintf("%d: '%s' %s\n", index, name, CHAR(STRING_ELT(value, 0)));
	    break;
	default:
	    Rprintf("%d: %s other\n", index, name);
	}
    }
    return R_NilValue;
}

SEXP invoke_isString(SEXP s) {
  return ScalarLogical(isString(s));
}

SEXP invoke12(SEXP a1, SEXP a2, SEXP a3, SEXP a4, SEXP a5, SEXP a6, SEXP a7, SEXP a8, SEXP a9, SEXP a10, SEXP a11, SEXP a12) {
	return a12;
}
