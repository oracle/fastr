/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include <Rinterface.h>
#include <Rinternals.h>
#include <Rinterface.h>
#include "testrffi.h"

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

SEXP invoke_error(SEXP msg) {
	error(R_CHAR(STRING_ELT(msg, 0)));
}

// returns a
SEXP dot_external_access_args(SEXP args) {
	args = CDR(args);
	int index = 0;
	SEXP list;
	PROTECT(list = allocVector(VECSXP, length(args)));
	for (; args != R_NilValue; args = CDR(args)) {
		SEXP tag = TAG(args);
		SEXP value = CAR(args);
		SEXP listElement;
		PROTECT(listElement = allocVector(VECSXP, 2));
		SET_VECTOR_ELT(listElement, 0, tag);
		SEXP firstValue = R_NilValue;
		if (length(value) == 0) {
			firstValue = PROTECT(R_NilValue);
		} else {
			switch (TYPEOF(value)) {
			case LGLSXP:
			case INTSXP:{
				PROTECT(firstValue = allocVector(INTSXP, 1));
				INTEGER(firstValue)[0] = INTEGER(value)[0];
				break;
			}
			case REALSXP: {
				PROTECT(firstValue = allocVector(REALSXP, 1));
				REAL(firstValue)[0] = REAL(value)[0];
				break;
			}
			case STRSXP:
				PROTECT(firstValue = ScalarString(STRING_ELT(value, 0)));
				break;
			case RAWSXP: {
				PROTECT(firstValue = allocVector(RAWSXP, 1));
				RAW(firstValue)[0] = RAW(value)[0];
				break;
			}
			default:
				firstValue = PROTECT(R_NilValue);
			}
		}

		SET_VECTOR_ELT(listElement, 1, firstValue);
		SET_VECTOR_ELT(list, index, listElement);
		UNPROTECT(1); // firstValue
		UNPROTECT(1); // listElement
		index++;
	}
	UNPROTECT(1); // list
	return list;
}

SEXP invoke_isString(SEXP s) {
  return ScalarLogical(isString(s));
}

SEXP invoke12(SEXP a1, SEXP a2, SEXP a3, SEXP a4, SEXP a5, SEXP a6, SEXP a7, SEXP a8, SEXP a9, SEXP a10, SEXP a11, SEXP a12) {
	return a12;
}

SEXP interactive(void) {
	return ScalarLogical(R_Interactive);
}

SEXP tryEval(SEXP expr, SEXP env) {
	int error;
	SEXP r = R_tryEval(expr, env, &error);
	SEXP v;
	PROTECT(v = allocVector(VECSXP, 2));
	if (error) {
		r = R_NilValue;
	}
	SET_VECTOR_ELT(v, 0, r);
	SET_VECTOR_ELT(v, 1, ScalarLogical(error));
	UNPROTECT(1);
	return v;
}

SEXP rHomeDir() {
	char *dir = R_HomeDir();
	return ScalarString(mkChar(dir));
}

SEXP nestedCall1(SEXP upcall, SEXP env) {
	SEXP vec;
	PROTECT(vec = allocVector(INTSXP, 10));
	int *vecstar = INTEGER(vec);
	for (int i = 0; i < 10; i++) {
		vecstar[i] = i + 1;
	}
	SEXP upcallResult = tryEval(upcall, env);
	int *vecstar2 = INTEGER(vec);
	int ok = vecstar == vecstar2;
	if (ok) {
		for (int i = 0; i < 10; i++) {
			if (vecstar[i] != i + 1) {
				ok = 0;
				break;
			}
		}
	}
	SEXP result;
	PROTECT(result = allocVector(VECSXP, 2));
	SET_VECTOR_ELT(result, 0, upcallResult);
	SET_VECTOR_ELT(result, 1, ScalarLogical(ok));
	UNPROTECT(2);
	return result;
}

SEXP nestedCall2(SEXP v) {
	SEXP sumVec;
	PROTECT(sumVec = allocVector(INTSXP, 1));
	int len = Rf_length(v);
	int sum = 0;
	for (int i = 0; i < len; i++) {
		sum += INTEGER(v)[i];
	}
	INTEGER(sumVec)[0] = sum;
	UNPROTECT(1);
	return sumVec;
}

SEXP r_home(void) {
	return mkString(R_Home);
}

SEXP char_length(SEXP x) {
	const char *cx = R_CHAR(STRING_ELT(x, 0));
	int count  = 0;
	while (*cx++ != 0) {
		count++;
	}
	return ScalarInteger(count);
}

SEXP mkStringFromChar(void) {
	return mkString("hello");
}

SEXP mkStringFromBytes(void) {
	char *helloworld = "hello world";
	return ScalarString(mkCharLen(helloworld, 5));
}

SEXP null(void) {
	return R_NilValue;
}

SEXP iterate_iarray(SEXP x) {
	int *cx = INTEGER(x);
	int len = LENGTH(x);
    SEXP v;
    PROTECT(v = allocVector(INTSXP, len));
    int *iv = INTEGER(v);
    int i;
    for (i = 0; i < len; i++) {
    	iv[i] = cx[i];
    }
    UNPROTECT(1);
    return v;
}

SEXP iterate_iptr(SEXP x) {
	int *cx = INTEGER(x);
	int len = LENGTH(x);
    SEXP v;
    PROTECT(v = allocVector(INTSXP, len));
    int *iv = INTEGER(v);
    int i;
    for (i = 0; i < len; i++) {
    	*iv++ = *cx++;
    }
    UNPROTECT(1);
    return v;
}

SEXP preserve_object(void) {
	SEXP v;
	v = allocVector(INTSXP, 1);
	R_PreserveObject(v);
	return v;
}

SEXP release_object(SEXP x) {
	R_ReleaseObject(x);
    return R_NilValue;
}

SEXP findvar(SEXP x, SEXP env) {
	SEXP v = Rf_findVar(x, env);
	if (v == R_UnboundValue) {
		Rf_error("'%s' not found", R_CHAR(PRINTNAME(x)));
	} else {
		return v;
	}
}

SEXP test_asReal(SEXP x) {
	return Rf_ScalarReal(Rf_asReal(x));
}

SEXP test_asInteger(SEXP x) {
	return Rf_ScalarInteger(Rf_asInteger(x));
}

SEXP test_asLogical(SEXP x) {
	return Rf_ScalarLogical(Rf_asLogical(x));
}

SEXP test_asChar(SEXP x) {
	return Rf_ScalarString(Rf_asChar(x));
}

SEXP test_CAR(SEXP x) {
	return CAR(x);
}

SEXP test_CDR(SEXP x) {
	return CDR(x);
}

SEXP test_LENGTH(SEXP x) {
	return ScalarInteger(LENGTH(x));
}
