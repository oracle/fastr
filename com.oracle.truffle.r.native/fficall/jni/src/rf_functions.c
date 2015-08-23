/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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
#include "rffiutils.h"
#include <string.h>

// Most of the functions with a Rf_ prefix
// TODO Lots missing yet

static jmethodID Rf_ScalarIntegerMethodID;
static jmethodID Rf_ScalarDoubleMethodID;
static jmethodID Rf_ScalarStringMethodID;
static jmethodID Rf_ScalarLogicalMethodID;
static jmethodID Rf_allocateVectorMethodID;
static jmethodID Rf_allocateArrayMethodID;
static jmethodID Rf_allocateMatrixMethodID;
static jmethodID Rf_duplicateMethodID;
static jmethodID Rf_consMethodID;
static jmethodID Rf_defineVarMethodID;
static jmethodID Rf_findVarMethodID;
static jmethodID Rf_getAttribMethodID;
static jmethodID Rf_setAttribMethodID;
static jmethodID Rf_isStringMethodID;
static jmethodID Rf_isNullMethodID;
static jmethodID Rf_warningMethodID;
static jmethodID Rf_errorMethodID;
static jmethodID Rf_NewHashedEnvMethodID;
static jmethodID Rf_rPsortMethodID;
static jmethodID Rf_iPsortMethodID;

void init_rf_functions(JNIEnv *env) {
	Rf_ScalarIntegerMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_ScalarInteger", "(I)Lcom/oracle/truffle/r/runtime/data/RIntVector;", 1);
	Rf_ScalarDoubleMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_ScalarDouble", "(D)Lcom/oracle/truffle/r/runtime/data/RDoubleVector;", 1);
	Rf_ScalarStringMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_ScalarString", "(Ljava/lang/String;)Lcom/oracle/truffle/r/runtime/data/RStringVector;", 1);
	Rf_ScalarLogicalMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_ScalarLogical", "(I)Lcom/oracle/truffle/r/runtime/data/RLogicalVector;", 1);
	Rf_consMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_cons", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_defineVarMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_defineVar", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", 1);
	Rf_findVarMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_findVar", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_getAttribMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_getAttrib", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_setAttribMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_setAttrib", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", 1);
	Rf_isStringMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_isString", "(Ljava/lang/Object;)I", 1);
	Rf_isNullMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_isNull", "(Ljava/lang/Object;)I", 1);
	Rf_warningMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_warning", "(Ljava/lang/String;)V", 1);
	Rf_errorMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_error", "(Ljava/lang/String;)V", 1);
	Rf_allocateVectorMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_allocateVector", "(II)Ljava/lang/Object;", 1);
	Rf_allocateMatrixMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_allocateMatrix", "(III)Ljava/lang/Object;", 1);
	Rf_allocateArrayMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_allocateArray", "(ILjava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_duplicateMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_duplicate", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_NewHashedEnvMethodID = checkGetMethodID(env, RDataFactoryClass, "createNewEnv", "(Lcom/oracle/truffle/r/runtime/env/REnvironment;Ljava/lang/String;ZI)Lcom/oracle/truffle/r/runtime/env/REnvironment;", 1);
//	Rf_rPsortMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_rPsort", "(Lcom/oracle/truffle/r/runtime/data/RDoubleVector;II)", 1);
//	Rf_iPsortMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_iPsort", "(Lcom/oracle/truffle/r/runtime/data/RIntVector;II)", 1);
}

SEXP Rf_ScalarInteger(int value) {
	TRACE("%s(%d)\n", value);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_ScalarIntegerMethodID, value);
    return checkRef(thisenv, result);
}

SEXP Rf_ScalarReal(double value) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_ScalarDoubleMethodID, value);
    return checkRef(thisenv, result);
}

SEXP Rf_ScalarString(SEXP value) {
	TRACE(TARG1, value);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_ScalarStringMethodID, value);
    return checkRef(thisenv, result);
}

SEXP Rf_ScalarLogical(int value) {
	TRACE(TARG1, value);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_ScalarLogicalMethodID, value);
    return checkRef(thisenv, result);
}

SEXP Rf_allocVector(SEXPTYPE t, R_xlen_t len) {
	TRACE(TARG2d, t, len);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_allocateVectorMethodID, t, len);
	return checkRef(thisenv, result);
}

SEXP Rf_allocArray(SEXPTYPE t, SEXP dims) {
	TRACE(TARG2d, t, len);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_allocateArrayMethodID, t, dims);
	return checkRef(thisenv, result);
}

SEXP Rf_allocMatrix(SEXPTYPE mode, int nrow, int ncol) {
	TRACE(TARG2d, t, len);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_allocateMatrixMethodID, mode, nrow, ncol);
	return checkRef(thisenv, result);
}

SEXP Rf_cons(SEXP car, SEXP cdr) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_consMethodID, car, cdr);
    return checkRef(thisenv, result);
}

void Rf_defineVar(SEXP symbol, SEXP value, SEXP rho) {
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_defineVarMethodID, symbol, value, rho);
}

SEXP Rf_eval(SEXP expr, SEXP env) {
	unimplemented("Rf_eval)");
}

SEXP Rf_findVar(SEXP symbol, SEXP rho) {
	JNIEnv *thisenv = getEnv();
	SEXP result =(*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_findVarMethodID, symbol, rho);
    return checkRef(thisenv, result);
}

SEXP Rf_getAttrib(SEXP vec, SEXP name) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_getAttribMethodID, vec, name);
	return checkRef(thisenv, result);
}

SEXP Rf_setAttrib(SEXP vec, SEXP name, SEXP val) {
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_setAttribMethodID, vec, name, val);
	return val;
}

SEXP Rf_duplicate(SEXP x) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_duplicateMethodID, x);
	return checkRef(thisenv, result);
}

Rboolean Rf_inherits(SEXP x, const char * klass) {
	unimplemented("Rf_inherits)");
}

Rboolean Rf_isFunction(SEXP x) {
	unimplemented("Rf_isFunction)");
}

Rboolean Rf_isArray(SEXP x) {
    unimplemented("Rf_isArray");
}

Rboolean Rf_isFactor(SEXP x) {
    unimplemented("Rf_isFactor");
}

Rboolean Rf_isFrame(SEXP x) {
    unimplemented("Rf_isFrame");
}

Rboolean Rf_isInteger(SEXP x) {
    unimplemented("Rf_isInteger");
}

Rboolean Rf_isLanguage(SEXP x) {
    unimplemented("Rf_isLanguage");
}

Rboolean Rf_isList(SEXP x) {
    unimplemented("Rf_isList");
}

Rboolean Rf_isMatrix(SEXP x) {
    unimplemented("Rf_isMatrix");
}

Rboolean Rf_isNewList(SEXP x) {
    unimplemented("Rf_isNewList");
}

Rboolean Rf_isNumber(SEXP x) {
    unimplemented("Rf_isNumber");
}

Rboolean Rf_isNumeric(SEXP x) {
    unimplemented("Rf_isNumeric");
}

Rboolean Rf_isPairList(SEXP x) {
    unimplemented("Rf_isPairList");
}

Rboolean Rf_isPrimitive(SEXP x) {
    unimplemented("Rf_isPrimitive");
}

Rboolean Rf_isTs(SEXP x) {
    unimplemented("Rf_isTs");
}

Rboolean Rf_isUserBinop(SEXP x) {
    unimplemented("Rf_isUserBinop");
}

Rboolean Rf_isValidString(SEXP x) {
    unimplemented("Rf_isValidString");
}

Rboolean Rf_isValidStringF(SEXP x) {
    unimplemented("Rf_isValidStringF");
}

Rboolean Rf_isVector(SEXP x) {
    unimplemented("Rf_isVector");
}

Rboolean Rf_isVectorAtomic(SEXP x) {
    unimplemented("Rf_isVectorAtomic");
}

Rboolean Rf_isVectorList(SEXP x) {
    unimplemented("Rf_isVectorList");
}

Rboolean Rf_isVectorizable(SEXP x) {
    unimplemented("Rf_isVectorizable");
}

SEXP Rf_install(const char *name) {
	JNIEnv *thisenv = getEnv();
	jstring string = (*thisenv)->NewStringUTF(thisenv, name);
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, createSymbolMethodID, string);
	return checkRef(thisenv, result);
}

Rboolean Rf_isNull(SEXP s) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticIntMethod(thisenv, CallRFFIHelperClass, Rf_isNullMethodID, s);
}

Rboolean Rf_isString(SEXP s) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticIntMethod(thisenv, CallRFFIHelperClass, Rf_isStringMethodID, s);

}

SEXP Rf_lcons(SEXP x, SEXP y) {
	unimplemented("Rf_lcons");
}


SEXP Rf_mkChar(const char *x) {
	JNIEnv *thisenv = getEnv();
	// TODO encoding, assume UTF for now
	SEXP result = (*thisenv)->NewStringUTF(thisenv, x);
	return checkRef(thisenv, result);
}

SEXP Rf_mkCharLenCE(const char *x, int len, cetype_t enc) {
	JNIEnv *thisenv = getEnv();
	char buf[len + 1];
	memcpy(buf, x, len);
	buf[len] = 0;
	// TODO encoding, assume UTF for now, zero terminated
	SEXP result = (*thisenv)->NewStringUTF(thisenv, buf);
	return checkRef(thisenv, result);
}

SEXP Rf_mkString(const char *s) {
	JNIEnv *thisenv = getEnv();
	jstring string = (*thisenv)->NewStringUTF(thisenv, s);
	return ScalarString(string);
}

SEXP Rf_protect(SEXP x) {
	return x;
}

void Rf_unprotect(int x) {
	// TODO perhaps we can use this
}

void Rf_unprotect_ptr(SEXP x) {
	// TODO perhaps we can use this
}

void Rf_error(const char *msg, ...) {
	// TODO if msg is a format string how do we get the args given the number is determined by the format?
	// This is a bit tricky. The usual error handling model in Java is "throw RError.error(...)" but
	// RError.error does quite a lot of stuff including potentially searching for R condition handlers
	// and, if it finds any, does not return, but throws a different exception than RError.
	// We definitely need to exit the FFI call and we certainly cannot return to our caller.
	// So we call CallRFFIHelper.Rf_error to throw the RError exception. When the pending
	// exception (whatever it is) is observed by JNI, the call to Rf_error will return where we do a
	// non-local transfer of control back to the entry point (which will cleanup).
	JNIEnv *thisenv = getEnv();
	jstring string = (*thisenv)->NewStringUTF(thisenv, msg);
	// This will set a pending exception
	(*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_errorMethodID, string);
	// just transfer back which will cleanup and exit the entire JNI call
	longjmp(*getErrorJmpBuf(), 1);

}

void Rf_warningcall(SEXP x, const char *msg, ...) {
	unimplemented("Rf_warningcall");
}

void Rf_warning(const char *msg, ...) {
	// TODO if msg is a format string how do we get the args given the number is determined by the format?
	JNIEnv *thisenv = getEnv();
	jstring string = (*thisenv)->NewStringUTF(thisenv, msg);
	(*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_warningMethodID, string);
}

void Rprintf(const char *msg, ...) {
	va_list argptr;
	va_start(argptr, msg);
	vprintf(msg, argptr);
}

// Tools package support, not in public API

SEXP R_NewHashedEnv(SEXP parent, SEXP size) {
	JNIEnv *thisenv = getEnv();
	int sizeAsInt = Rf_asInteger(size);
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, RDataFactoryClass, Rf_NewHashedEnvMethodID, parent, NULL, JNI_TRUE, sizeAsInt);
	return checkRef(thisenv, result);
}

void Rf_iPsort(int *x, int n, int k)
{
	JNIEnv *thisenv = getEnv();
	unimplemented("Rf_iPsort");
}

void Rf_rPsort(double *x, int n, int k) {
	JNIEnv *thisenv = getEnv();
	unimplemented("Rf_rPsort");
}
