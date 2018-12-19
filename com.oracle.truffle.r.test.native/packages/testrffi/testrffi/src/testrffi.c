/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

// A very simple test of the R FFI interface

#define USE_RINTERNALS
#include <R.h>
#include <Rdefines.h>
#include <Rinterface.h>
#include <Rinternals.h>
#include <Rmath.h>
#include <R_ext/Connections.h>
#include <R_ext/Parse.h>
#include <string.h>
#include <stdint.h>
#include "testrffi.h"

#define K_RMULTINOM 4

void dotCModifiedArguments(int* len, int* idata, double* rdata, int* ldata, char** cdata) {
    for (int i = 0; i < len[0]; i++) {
	idata[i] ++;
    }
    for (int i = 0; i < len[0]; i++) {
	rdata[i] *= 0.2;
    }
    for (int i = 0; i < len[0]; i++) {
    ldata[i] = ldata[i] == 0 ? 1 : 0;
    }
    for (int i = 0; i < len[0]; i++) {
        for (int j = 0; cdata[i][j] != 0; j++) {
            char c = cdata[i][j];
            cdata[i][j] = (c >= '0' && c <= '9') ? c - '0' + 'a' : 'r';
        }
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
	return ScalarInteger((intptr_t) R_ExternalPtrAddr(eptr));
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
	int error = 0;
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

SEXP shareIntElement(SEXP x, SEXP xIndex, SEXP y, SEXP yIndex) {
	int *xi = INTEGER(xIndex);
	int *yi = INTEGER(yIndex);
	int *xPtr = INTEGER(x);
	int *yPtr = INTEGER(y);
	xPtr[xi[0] - 1] = yPtr[yi[0] - 1];
	return x;
}

SEXP shareDoubleElement(SEXP x, SEXP xIndex, SEXP y, SEXP yIndex) {
	int *xi = INTEGER(xIndex);
	int *yi = INTEGER(yIndex);
	double *xPtr = REAL(x);
	double *yPtr = REAL(y);
	xPtr[xi[0] - 1] = yPtr[yi[0] - 1];
	return x;
}

SEXP shareStringElement(SEXP x, SEXP xIndex, SEXP y, SEXP yIndex) {
	int *xi = INTEGER(xIndex);
	int *yi = INTEGER(yIndex);
	SEXP *xPtr = STRING_PTR(x);
	SEXP *yPtr = STRING_PTR(y);
	xPtr[xi[0] - 1] = yPtr[yi[0] - 1];
	return x;
}

SEXP shareListElement(SEXP x, SEXP xIndex, SEXP y, SEXP yIndex) {
	int *xi = INTEGER(xIndex);
	int *yi = INTEGER(yIndex);
	SEXP *xPtr = ((SEXP *) DATAPTR(x));
	SEXP *yPtr = ((SEXP *) DATAPTR(y));
	xPtr[xi[0] - 1] = yPtr[yi[0] - 1];
	return x;
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

SEXP preserve_object(SEXP val) {
	SEXP v;
	v = allocVector(INTSXP, 1);
    int *iv = INTEGER(v);
    if(LENGTH(val) > 0) {
    	int *ival = INTEGER(val);
    	iv[0] = ival[0];
    } else {
    	iv[0] = 1234;
    }
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

SEXP test_inlined_length(SEXP x) {
    return ScalarInteger(length(x));
}

SEXP test_coerceVector(SEXP x, SEXP mode) {
    int intMode = INTEGER_VALUE(mode);
    return Rf_coerceVector(x, intMode);
}

SEXP test_ATTRIB(SEXP x) {
    return ATTRIB(x);
}

SEXP test_getAttrib(SEXP source, SEXP name) {
    return Rf_getAttrib(source, name);
}

SEXP test_stringNA(void) {
    SEXP x = allocVector(STRSXP, 1);
    SET_STRING_ELT(x, 0, NA_STRING);
    return x;
}

SEXP test_setStringElt(SEXP vec, SEXP elt) {
    SET_STRING_ELT(vec, 0, STRING_ELT(elt, 0));
    return vec;
}

SEXP test_isNAString(SEXP vec) {
    if (STRING_ELT(vec, 0) == NA_STRING) {
        return ScalarLogical(1);
    } else {
        return ScalarLogical(0);
    }
}

SEXP test_getBytes(SEXP vec) {
    const char* bytes = R_CHAR(STRING_ELT(vec, 0));
    SEXP result;
    PROTECT(result = allocVector(RAWSXP, Rf_length(STRING_ELT(vec, 0))));
    unsigned char* resData = RAW(result);
    int i = 0;
    while (*bytes != '\0') {
        resData[i++] = (unsigned char) *bytes;
        bytes++;
    }
    UNPROTECT(1);
    return result;
}

// This function is expected to be called only with environment that has single
// promise value in the '...' variable and this is asserted inside this function.
// The return value is list with the promises' expression and environment.
SEXP test_captureDotsWithSingleElement(SEXP env) {
    SEXP dots = findVarInFrame3(env, R_DotsSymbol, TRUE);
    int n_dots = length(dots);
    if (n_dots != 1) {
        printf("Error: test_captureDotsWithSingleElement expectes single promise in ...\n");
        return R_NilValue;
    }
    SEXP promise = CAR(dots);
    if (TYPEOF(promise) != PROMSXP) {
        printf("Error: test_captureDotsWithSingleElement expectes a promise in ...\n");
        return R_NilValue;
    }
    SEXP info = PROTECT(allocVector(VECSXP, 2));
    SET_VECTOR_ELT(info, 0, R_PromiseExpr(promise));
    SET_VECTOR_ELT(info, 1, PRENV(promise));
    UNPROTECT(1);
    return info;
}

SEXP test_evalAndNativeArrays(SEXP vec, SEXP expr, SEXP env) {
    SEXP symbolValue;
    int *idata;
    double *ddata;
    unsigned char *bdata;
    // note: we want to evaluate PROTECT(symbolValue = Rf_eval(expr, env)); after we take the pointer to data...
    switch (TYPEOF(vec)) {
        case INTSXP:
            idata = INTEGER(vec);
            PROTECT(symbolValue = Rf_eval(expr, env));
            idata[0] = 42;
            idata[1] = Rf_asInteger(symbolValue);
            break;
        case REALSXP:
            ddata = REAL(vec);
            PROTECT(symbolValue = Rf_eval(expr, env));
            ddata[0] = 42;
            ddata[1] = Rf_asReal(symbolValue);
            break;
        case RAWSXP:
            bdata = RAW(vec);
            PROTECT(symbolValue = Rf_eval(expr, env));
            bdata[0] = 42;
            bdata[1] = Rf_asInteger(symbolValue);  // there is no asRaw, we expect to get symbol with integer value
            break;
        case LGLSXP:
            idata = LOGICAL(vec);
            PROTECT(symbolValue = Rf_eval(expr, env));
            idata[0] = 1;
            idata[1] = Rf_asLogical(symbolValue);
            break;
        default:
            printf("Error: unexpected type");
    }

    // max of the vector could now be 42/TRUE or symbolValue
    SEXP maxSymbol, call, maxVec;
    int uprotectCount = 1;
    if (TYPEOF(vec) != RAWSXP) {
        // note: max does not support raws
        PROTECT(maxSymbol = install("max"));
        PROTECT(call = lang2(maxSymbol, vec));
        PROTECT(maxVec = eval(call, R_GlobalEnv));
        uprotectCount = 4;
    }

    switch (TYPEOF(vec)) {
        case INTSXP:
            idata[length(vec) - 1] = Rf_asInteger(maxVec);
            break;
        case REALSXP:
            ddata[length(vec) - 1] = Rf_asReal(maxVec);
            break;
        case RAWSXP:
            bdata[length(vec) - 1] = 42;
            break;
        case LGLSXP:
            idata[length(vec) - 1] = Rf_asLogical(maxVec);
            break;
        default:
            printf("Error: unexpected type");
    }

    UNPROTECT(uprotectCount);
    return vec;
}

SEXP test_writeConnection(SEXP connVec) {
	Rconnection connection = R_GetConnection(connVec);
	char* greeting = "Hello from R_WriteConnection";
	R_WriteConnection(connection, greeting, strlen(greeting));
    return R_NilValue;
}

SEXP test_readConnection(SEXP connVec) {
    Rconnection connection = R_GetConnection(connVec);
    unsigned char buffer[255];
    int size = R_ReadConnection(connection, buffer, 255);
    SEXP result;
    PROTECT(result = allocVector(RAWSXP, size));
    unsigned char* resultData = RAW(result);
    for (int i = 0; i < size; ++i) {
        resultData[i] = buffer[i];
    }
    UNPROTECT(1);
    return result;
}

static Rconnection customConn;

static void printNow(const char* message) {
    puts(message);
    fflush(stdout);
}

static void testrfficonn_destroy(Rconnection conn) {
    if (conn != customConn) {
        printNow("ERROR: destroy function did not receive expected argument\n");
    } else {
        printNow("Custom connection destroyed\n");
    }
}

static Rboolean testrfficonn_open(Rconnection conn) {
    if (conn != customConn) {
        printNow("ERROR: open function did not receive expected argument\n");
        return 0;
    } else {
        printNow("Custom connection opened\n");
        return 1;
    }
}

static void testrfficonn_close(Rconnection conn) {
    if (conn != customConn) {
        printNow("ERROR: close function did not receive expected argument\n");
    } else {
        printNow("Custom connection closed\n");
    }
}

static size_t testrfficonn_write(const void * message, size_t size, size_t nitems, Rconnection conn) {
    if (conn != customConn) {
        printNow("ERROR: write function did not receive expected argument\n");
        return 0;
    } else {
        printf("Custom connection printing: %.*s\n", (int) (size * nitems), (char*) message);
        fflush(stdout);
        return size * nitems;
    }
}

static size_t testrfficonn_read(void *buffer, size_t size, size_t niterms, Rconnection conn) {
    if (conn != customConn) {
        printNow("ERROR: read function did not receive expected argument\n");
        return 0;
    } else if (size * niterms > 0) {
        ((char *)buffer)[0] = 'Q';
        return 1;
    }
    return 0;
}

SEXP test_createNativeConnection() {
    SEXP newConnSEXP = R_new_custom_connection("Connection for testing purposes", "w", "testrfficonn", &customConn);
    customConn->isopen = 0;
    customConn->canwrite = 1;
    customConn->destroy = &testrfficonn_destroy;
    customConn->open = &testrfficonn_open;
    customConn->close = &testrfficonn_close;
    customConn->write = &testrfficonn_write;
    // customConn->read = &testrfficonn_read; TODO: read test
    return newConnSEXP;
}

SEXP test_ParseVector(SEXP src) {
    ParseStatus status;
    SEXP parseResult, result;
    PROTECT(parseResult = R_ParseVector(src, 1, &status, R_NilValue));
    PROTECT(result = allocVector(VECSXP, 2));
    SET_VECTOR_ELT(result, 0, ScalarInteger(status));
    SET_VECTOR_ELT(result, 1, parseResult);
    UNPROTECT(2);
    return result;
}

SEXP test_RfEvalWithPromiseInPairList() {
    SEXP fun = Rf_findVarInFrame(R_FindNamespace(ScalarString(mkChar("stats"))), Rf_install("runif"));
    if (TYPEOF(fun) != PROMSXP) {
        printf("ERROR: Rf_findVarInFrame evaluated the promise!");
    }
    SEXP e, ptr;
    PROTECT(e = Rf_allocVector(LANGSXP, 2));
    SETCAR(e, fun); ptr = CDR(e);
    SETCAR(ptr, ScalarInteger(5));
    SEXP result = Rf_eval(e, R_GlobalEnv);
    UNPROTECT(1);
    return result;
}

//typedef double Rf_cospi(double a);
//typedef double Rf_sinpi(double a);
//typedef double Rf_tanpi(double a);
SEXP test_RfRandomFunctions() {
    SEXP v, vNames;
    int vLen = 128;
    PROTECT(v = allocVector(VECSXP, vLen));
    PROTECT(vNames = allocVector(STRSXP, vLen));
    int n = 0;
    SET_STRING_ELT(vNames, n, mkChar("Rf_dunif")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dunif(1, 0, 1, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qunif")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qunif(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_punif")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_punif(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_runif")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_runif(0, 1)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dchisq")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dchisq(0, 1, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_pchisq")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_pchisq(0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qchisq")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qchisq(0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_rchisq")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_rchisq(1)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dnchisq")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dnchisq(1, 0, 1, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_pnchisq")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_pnchisq(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qnchisq")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qnchisq(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_rnchisq")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_rnchisq(0, 1)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dnorm4")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dnorm4(1, 0, 1, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_pnorm5")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_pnorm5(1, 0, 1, TRUE, FALSE)));

    double cum = 1.0;
    double ccum = 0.0;
    Rf_pnorm_both(2, &cum, &ccum, TRUE, FALSE);
    SET_STRING_ELT(vNames, n, mkChar("Rf_pnorm_both-arg-cum")); SET_VECTOR_ELT(v, n++, ScalarReal(cum)); // Index 15
    SET_STRING_ELT(vNames, n, mkChar("Rf_pnorm_both-arg-ccum")); SET_VECTOR_ELT(v, n++, ScalarReal(ccum));

    SET_STRING_ELT(vNames, n, mkChar("Rf_qnorm5")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qnorm5(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_rnorm")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_rnorm(1, 0)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dlnorm")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dlnorm(1, 0, 1, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_plnorm")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_plnorm(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qlnorm")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qlnorm(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_rlnorm")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_rlnorm(1, 0)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dgamma")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dgamma(1, 0, 1, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_pgamma")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_pgamma(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qgamma")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qgamma(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_rgamma")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_rgamma(1, 0)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_log1pmx")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_log1pmx(42)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_log1pexp")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_log1pexp(6)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_lgamma1p")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_lgamma1p(42)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_logspace_add")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_logspace_add(42, 6)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_logspace_sub")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_logspace_sub(42, 6)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dbeta")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dbeta(1, 0, 1, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_pbeta")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_pbeta(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qbeta")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qbeta(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_rbeta")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_rbeta(1, 0)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_df")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_df(1, 0, 1, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_pf")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_pf(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qf")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qf(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_rf")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_rf(1, 0)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dt")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dt(1, 0, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_pt")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_pt(1, 0, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qt")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qt(1, 0, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_rt")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_rt(1)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dbinom")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dbinom(1, 0, 1, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_pbinom")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_pbinom(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qbinom")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qbinom(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_rbinom")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_rbinom(1, 0)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dcauchy")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dcauchy(1, 0, 1, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_pcauchy")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_pcauchy(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qcauchy")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qcauchy(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_rcauchy")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_rcauchy(1, 0)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dexp")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dexp(1, 0, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_pexp")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_pexp(1, 0, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qexp")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qexp(1, 0, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_rexp")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_rexp(1)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dgeom")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dgeom(1, 0, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_pgeom")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_pgeom(1, 0, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qgeom")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qgeom(1, 0, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_rgeom")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_rgeom(1)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dhyper")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dhyper(1, 0, 1, 0, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_phyper")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_phyper(1, 0, 1, 0, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qhyper")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qhyper(1, 0, 1, 0, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_rhyper")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_rhyper(1, 0, 1)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dnbinom")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dnbinom(1, 0, 1, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_pnbinom")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_pnbinom(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qnbinom")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qnbinom(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_rnbinom")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_rnbinom(1, 0)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dnbinom_mu")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dnbinom_mu(1, 0, 1, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_pnbinom_mu")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_pnbinom_mu(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qnbinom_mu")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qnbinom_mu(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_rnbinom_mu")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_rnbinom_mu(1, 0)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dpois")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dpois(1, 0, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_ppois")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_ppois(1, 0, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qpois")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qpois(1, 0, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_rpois")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_rpois(1)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dweibull")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dweibull(1, 0, 1, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_pweibull")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_pweibull(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qweibull")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qweibull(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_rweibull")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_rweibull(1, 0)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dlogis")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dlogis(1, 0, 1, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_plogis")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_plogis(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qlogis")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qlogis(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_rlogis")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_rlogis(1, 0)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dnbeta")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dnbeta(1, 0, 1, 0, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_pnbeta")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_pnbeta(1, 0, 1, 0, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qnbeta")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qnbeta(1, 0, 1, 0, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dnf")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dnf(1, 0, 1, 0, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_pnf")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_pnf(1, 0, 1, 0, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qnf")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qnf(1, 0, 1, 0, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dnt")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dnt(1, 0, 1, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_pnt")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_pnt(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qnt")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qnt(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_ptukey")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_ptukey(1, 0, 1, 0, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qtukey")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qtukey(1, 0, 1, 0, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dwilcox")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dwilcox(1, 0, 1, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_pwilcox")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_pwilcox(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qwilcox")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qwilcox(1, 0, 1, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_rwilcox")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_rwilcox(1, 0)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dsignrank")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_dsignrank(1, 0, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_psignrank")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_psignrank(1, 0, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_qsignrank")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_qsignrank(1, 0, TRUE, FALSE)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_rsignrank")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_rsignrank(1)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_gammafn")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_gammafn(1)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_lgammafn")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_lgammafn(1)));

    int signRet = 0;
    SET_STRING_ELT(vNames, n, mkChar("Rf_lgammafn_sign")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_lgammafn_sign(1, &signRet)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_lgammafn_sign-arg-signRet")); SET_VECTOR_ELT(v, n++, ScalarReal(signRet));

    double ans = 0.0;
    int nz = 0;
    int ierr = 0;
    Rf_dpsifn(/*x*/1, /*n>=0*/2, /*kode>=1&&<=2*/1, /*m==1*/ 1, &ans, &nz, &ierr);
    SET_STRING_ELT(vNames, n, mkChar("Rf_dpsinfn-arg-ans")); SET_VECTOR_ELT(v, n++, ScalarReal(ans));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dpsinfn-arg-nz")); SET_VECTOR_ELT(v, n++, ScalarReal(nz));
    SET_STRING_ELT(vNames, n, mkChar("Rf_dpsinfn-arg-ierr")); SET_VECTOR_ELT(v, n++, ScalarReal(ierr));

    SET_STRING_ELT(vNames, n, mkChar("Rf_psigamma")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_psigamma(1, 0)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_digamma")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_digamma(1)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_trigamma")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_trigamma(1)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_tetragamma")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_tetragamma(1)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_pentagamma")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_pentagamma(1)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_beta")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_beta(1, 1)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_lbeta")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_lbeta(1, 1)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_choose")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_choose(1, 1)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_lchoose")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_lchoose(1, 1)));

    double nb[3] = { 0.0, 0.0, 0.0 }; // work-array of size floor(second-param) + 1
    SET_STRING_ELT(vNames, n, mkChar("Rf_bessel_i")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_bessel_i(42.0, 2.0, 1.0)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_bessel_i_ex")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_bessel_i_ex(1.0, 2.0, 1.0, nb)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_bessel_j")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_bessel_j(42.0, 2.0)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_bessel_j_ex")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_bessel_j_ex(1.0, 2.0, nb)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_bessel_k")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_bessel_k(42.0, 2.0, 1.0)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_bessel_k_ex")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_bessel_k_ex(1.0, 2.0, 1.0, nb)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_bessel_y")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_bessel_y(42.0, 2.0)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_bessel_y_ex")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_bessel_y_ex(1.0, 2.0, nb)));

//    SET_STRING_ELT(vNames, n, mkChar("Rf_cospi")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_cospi(1)));
//    SET_STRING_ELT(vNames, n, mkChar("Rf_sinpi")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_sinpi(1)));
//    SET_STRING_ELT(vNames, n, mkChar("Rf_tanpi")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_tanpi(1)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_sign")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_sign(1)));
    SET_STRING_ELT(vNames, n, mkChar("Rf_fprec")); SET_VECTOR_ELT(v, n++, ScalarReal(Rf_fprec(1,2)));
    setAttrib(v, R_NamesSymbol, vNames);
//    printf("n=%d, vLen=%d\n", n, vLen);
    UNPROTECT(2);
    return v;
}

SEXP test_RfRMultinom() {
    SEXP v;
    PROTECT(v = allocVector(REALSXP, K_RMULTINOM));
    double prob[K_RMULTINOM] = {0.1, 0.3, 0.5, 0.1};
    int rN[K_RMULTINOM] = {0, 0, 0, 0};
    Rf_rmultinom(10, prob, K_RMULTINOM, rN);
    for (int i = 0; i < K_RMULTINOM; i++) {
        REAL(v)[i] = rN[i];
    }
    UNPROTECT(1);
    return v;
}

SEXP test_RfFunctions() {
    SEXP v;
    PROTECT(v = allocVector(REALSXP, 4));
    int n = 0;
    REAL(v)[n++] = Rf_ftrunc(2.6);
    REAL(v)[n++] = Rf_ftrunc(5.3);
    REAL(v)[n++] = Rf_ftrunc(-2.6);
    REAL(v)[n++] = Rf_ftrunc(-5.3);
    UNPROTECT(1);
    return v;
}

SEXP test_DATAPTR(SEXP strings, SEXP testSingleChar) {
    if (asLogical(testSingleChar)) {
        void* data = DATAPTR(STRING_ELT(strings, 0));
        printf("DATAPTR(STRING_ELT(strings, 0)) == '%s'\n", (char *)data);
    } else {
        // pointer to CHARSXP array
        void* data = DATAPTR(strings);
        for (int i = 0; i < LENGTH(strings); ++i) {
            printf("DATAPTR(strings)[%d] == '%s'\n", i, CHAR(((SEXP*)data)[i]));
        }
    }
    fflush(stdout);
    return R_NilValue;
}


SEXP test_duplicate(SEXP val, SEXP deep) {
    if (INTEGER_VALUE(deep)) {
        return Rf_duplicate(val);
    } else {
        return Rf_shallow_duplicate(val);
    }
}

SEXP test_R_nchar(SEXP x) {
	int res = R_nchar(STRING_ELT(x, 0), Chars, FALSE, FALSE, "OutDec");
	SEXP resVec;
	PROTECT(resVec = allocVector(INTSXP, 1));
    INTEGER(resVec)[0] = res;
    UNPROTECT(1);
    return resVec;
}

SEXP test_forceAndCall(SEXP e, SEXP n, SEXP rho) {
    SEXP val = R_forceAndCall(e, Rf_asInteger(n), rho);
    return val;
}

SEXP test_constant_types() {
    SEXP res = PROTECT(allocVector(INTSXP, 43));
    int* data = INTEGER(res);
    int i = 0;
    data[i++] = TYPEOF(R_GlobalEnv);
    data[i++] = TYPEOF(R_BaseEnv);
    data[i++] = TYPEOF(R_BaseNamespace);
    data[i++] = TYPEOF(R_NamespaceRegistry);
    data[i++] = TYPEOF(R_NilValue);
    data[i++] = -1; // TYPEOF(R_UnboundValue); TODO: this is 'mkSymMarker(R_NilValue)' in GNU-R
    data[i++] = TYPEOF(R_MissingArg);
    data[i++] = TYPEOF(R_EmptyEnv);
    data[i++] = TYPEOF(R_Bracket2Symbol);
    data[i++] = TYPEOF(R_BracketSymbol);
    data[i++] = TYPEOF(R_BraceSymbol);
    data[i++] = TYPEOF(R_DoubleColonSymbol);
    data[i++] = TYPEOF(R_ClassSymbol);
    data[i++] = TYPEOF(R_DeviceSymbol);
    data[i++] = TYPEOF(R_DimNamesSymbol);
    data[i++] = TYPEOF(R_DimSymbol);
    data[i++] = TYPEOF(R_DollarSymbol);
    data[i++] = TYPEOF(R_DotsSymbol);
    data[i++] = TYPEOF(R_DropSymbol);
    data[i++] = TYPEOF(R_LastvalueSymbol);
    data[i++] = TYPEOF(R_LevelsSymbol);
    data[i++] = TYPEOF(R_ModeSymbol);
    data[i++] = TYPEOF(R_NameSymbol);
    data[i++] = TYPEOF(R_NamesSymbol);
    data[i++] = TYPEOF(R_NaRmSymbol);
    data[i++] = TYPEOF(R_PackageSymbol);
    data[i++] = TYPEOF(R_QuoteSymbol);
    data[i++] = TYPEOF(R_RowNamesSymbol);
    data[i++] = TYPEOF(R_SeedsSymbol);
    data[i++] = TYPEOF(R_SourceSymbol);
    data[i++] = TYPEOF(R_TspSymbol);
    data[i++] = TYPEOF(R_dot_defined);
    data[i++] = TYPEOF(R_dot_Method);
    data[i++] = TYPEOF(R_dot_target);
    data[i++] = TYPEOF(R_dot_packageName);
    data[i++] = TYPEOF(R_dot_Generic);
    data[i++] = TYPEOF(R_BlankString);
    data[i++] = TYPEOF(R_BlankScalarString);
    data[i++] = TYPEOF(R_BaseSymbol);
    data[i++] = TYPEOF(R_NamespaceEnvSymbol);
    data[i++] = TYPEOF(R_SortListSymbol);
    data[i++] = TYPEOF(R_SpecSymbol);
    data[i++] = TYPEOF(R_TripleColonSymbol);
    data[i++] = TYPEOF(R_PreviousSymbol);
    UNPROTECT(1);
    return res;
}

SEXP test_Rf_setVar(SEXP symbol, SEXP value, SEXP env) {
    Rf_setVar(symbol, value, env);
    return R_NilValue;
}
