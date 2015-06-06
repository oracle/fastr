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

// Most of the functions with a Rf_ prefix
// TODO Lots missing yet

static jmethodID Rf_ScalarIntegerMethodID;
static jmethodID Rf_ScalarDoubleMethodID;
static jmethodID Rf_ScalarStringMethodID;
static jmethodID createIntArrayMethodID;
static jmethodID createDoubleArrayMethodID;
static jmethodID createStringArrayMethodID;
static jmethodID createListMethodID;
static jmethodID Rf_duplicateMethodID;
static jmethodID Rf_consMethodID;
static jmethodID Rf_defineVarMethodID;
static jmethodID Rf_findVarMethodID;
static jmethodID Rf_getAttribMethodID;
static jmethodID Rf_setAttribMethodID;
static jmethodID Rf_isStringMethodID;
static jmethodID Rf_isNullMethodID;
static jmethodID Rf_NewHashedEnvMethodID;

void init_rf_functions(JNIEnv *env) {
	Rf_ScalarIntegerMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_ScalarInteger", "(I)Lcom/oracle/truffle/r/runtime/data/RIntVector;", 1);
	Rf_ScalarDoubleMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_ScalarDouble", "(D)Lcom/oracle/truffle/r/runtime/data/RDoubleVector;", 1);
	Rf_ScalarStringMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_ScalarString", "(Ljava/lang/String;)Lcom/oracle/truffle/r/runtime/data/RStringVector;", 1);
	Rf_consMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_cons", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_defineVarMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_defineVar", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", 1);
	Rf_findVarMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_findVar", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_getAttribMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_getAttrib", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_setAttribMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_setAttrib", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", 1);
	Rf_isStringMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_isString", "(Ljava/lang/Object;)I", 1);
	Rf_isNullMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_isNull", "(Ljava/lang/Object;)I", 1);
	createIntArrayMethodID = checkGetMethodID(env, RDataFactoryClass, "createIntVector", "(I)Lcom/oracle/truffle/r/runtime/data/RIntVector;", 1);
	createDoubleArrayMethodID = checkGetMethodID(env, RDataFactoryClass, "createDoubleVector", "(I)Lcom/oracle/truffle/r/runtime/data/RDoubleVector;", 1);
	createStringArrayMethodID = checkGetMethodID(env, RDataFactoryClass, "createStringVector", "(I)Lcom/oracle/truffle/r/runtime/data/RStringVector;", 1);
	createListMethodID = checkGetMethodID(env, RDataFactoryClass, "createList", "(I)Lcom/oracle/truffle/r/runtime/data/RList;", 1);
	Rf_duplicateMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_duplicate", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_NewHashedEnvMethodID = checkGetMethodID(env, RDataFactoryClass, "createNewEnv", "(Lcom/oracle/truffle/r/runtime/env/REnvironment;Ljava/lang/String;ZI)Lcom/oracle/truffle/r/runtime/env/REnvironment;", 1);
}

SEXP Rf_ScalarInteger(int value) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_ScalarIntegerMethodID, value);
    return mkGlobalRef(thisenv, result);
}

SEXP Rf_ScalarReal(double value) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_ScalarDoubleMethodID, value);
    return mkGlobalRef(thisenv, result);
}

SEXP Rf_ScalarString(SEXP value) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_ScalarStringMethodID, value);
    return mkGlobalRef(thisenv, result);
}

SEXP Rf_allocVector(SEXPTYPE t, R_xlen_t len) {
	JNIEnv *thisenv = getEnv();
	SEXP result;
	switch (t) {
	case INTSXP: {
		result = (*thisenv)->CallStaticObjectMethod(thisenv, RDataFactoryClass, createIntArrayMethodID, len);
		break;
	}
	case REALSXP: {
		result = (*thisenv)->CallStaticObjectMethod(thisenv, RDataFactoryClass, createDoubleArrayMethodID, len);
		break;
	}
	case STRSXP: {
		result = (*thisenv)->CallStaticObjectMethod(thisenv, RDataFactoryClass, createStringArrayMethodID, len);
		break;
	}
	case VECSXP: {
		result = (*thisenv)->CallStaticObjectMethod(thisenv, RDataFactoryClass, createListMethodID, len);
		break;
	}
	default:
		printf("t=%d\n", t);
		unimplemented("vector type not handled");
		return NULL;
	}
	return mkGlobalRef(thisenv, result);
}

SEXP Rf_cons(SEXP car, SEXP cdr) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_consMethodID, car, cdr);
    return mkGlobalRef(thisenv, result);
}

void Rf_defineVar(SEXP symbol, SEXP value, SEXP rho) {
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_defineVarMethodID, symbol, value, rho);
}

SEXP Rf_findVar(SEXP symbol, SEXP rho) {
	JNIEnv *thisenv = getEnv();
	SEXP result =(*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_findVarMethodID, symbol, rho);
    return mkGlobalRef(thisenv, result);
}

SEXP Rf_getAttrib(SEXP vec, SEXP name) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_getAttribMethodID, vec, name);
	return mkGlobalRef(thisenv, result);
}

SEXP Rf_setAttrib(SEXP vec, SEXP name, SEXP val) {
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_setAttribMethodID, vec, name, val);
	return val;
}

SEXP Rf_duplicate(SEXP x) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_duplicateMethodID, x);
	return mkGlobalRef(thisenv, result);
}

SEXP Rf_install(const char *name) {
	JNIEnv *thisenv = getEnv();
	jstring string = (*thisenv)->NewStringUTF(thisenv, name);
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, createSymbolMethodID, string);
	return mkGlobalRef(thisenv, result);
}

Rboolean Rf_isNull(SEXP s) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_isNullMethodID, s);
}

Rboolean Rf_isString(SEXP s) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_isStringMethodID, s);

}

SEXP Rf_mkChar(const char *x) {
	JNIEnv *thisenv = getEnv();
	// TODO encoding, assume UTF for now
	SEXP result = (*thisenv)->NewStringUTF(thisenv, x);
	return mkGlobalRef(thisenv, result);
}

SEXP Rf_mkCharLenCE(const char *x, int len, cetype_t enc) {
	JNIEnv *thisenv = getEnv();
	char buf[len + 1];
	memcpy(buf, x, len);
	buf[len] = 0;
	// TODO encoding, assume UTF for now, zero terminated
	SEXP result = (*thisenv)->NewStringUTF(thisenv, buf);
	return mkGlobalRef(thisenv, result);
}

SEXP Rf_mkString(const char *s) {
	JNIEnv *thisenv = getEnv();
	jstring string = (*thisenv)->NewStringUTF(thisenv, s);
	return ScalarString(string);
}

SEXP Rf_protect(SEXP x) {
	// TODO perhaps we can use this
}

void Rf_unprotect(int x) {
	// TODO perhaps we can use this
}

void Rf_unprotect_ptr(SEXP x) {
	// TODO perhaps we can use this
}

void Rf_error(const char *msg, ...) {
	unimplemented("Rf_error");
}

void Rf_warningcall(SEXP x, const char *msg, ...) {
	unimplemented("Rf_warningcall");
}

void Rf_warning(const char *msg, ...) {
	unimplemented("Rf_warning");
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
	return mkGlobalRef(thisenv, result);
}
