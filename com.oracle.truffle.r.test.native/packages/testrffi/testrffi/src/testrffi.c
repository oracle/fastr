/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include <R_ext/Connections.h>
#include <R_ext/Parse.h>
#include <string.h>
#include "testrffi.h"

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

SEXP test_stringNA(void) {
    SEXP x = allocVector(STRSXP, 1);
    SET_STRING_ELT(x, 0, NA_STRING);
    return x;
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
