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

static jmethodID Rf_scalarIntegerMethodID;
static jmethodID Rf_scalarDoubleMethodID;
static jmethodID createIntArrayMethodID;
static jmethodID createDoubleArrayMethodID;
static jmethodID createStringArrayMethodID;
static jmethodID Rf_duplicateMethodID;
static jmethodID createSymbolMethodID;

void init_rf_functions(JNIEnv *env) {
	Rf_scalarIntegerMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_ScalarInteger", "(I)Lcom/oracle/truffle/r/runtime/data/RIntVector;", 1);
	Rf_scalarDoubleMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_ScalarDouble", "(D)Lcom/oracle/truffle/r/runtime/data/RDoubleVector;", 1);
	createIntArrayMethodID = checkGetMethodID(env, RDataFactoryClass, "createIntVector", "(I)Lcom/oracle/truffle/r/runtime/data/RIntVector;", 1);
	createDoubleArrayMethodID = checkGetMethodID(env, RDataFactoryClass, "createDoubleVector", "(I)Lcom/oracle/truffle/r/runtime/data/RDoubleVector;", 1);
	createStringArrayMethodID = checkGetMethodID(env, RDataFactoryClass, "createStringVector", "(I)Lcom/oracle/truffle/r/runtime/data/RStringVector;", 1);
	Rf_duplicateMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_duplicate", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
	createSymbolMethodID = checkGetMethodID(env, RDataFactoryClass, "createSymbol", "(Ljava/lang/String;)Lcom/oracle/truffle/r/runtime/data/RSymbol;", 1);
}

SEXP Rf_ScalarInteger(int value) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_scalarIntegerMethodID, value);
}

SEXP Rf_ScalarReal(double value) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_scalarDoubleMethodID, value);
}

SEXP Rf_allocVector(SEXPTYPE t, R_xlen_t len) {
	JNIEnv *thisenv = getEnv();
	switch (t) {
	case INTSXP: {
		return (*thisenv)->CallStaticObjectMethod(thisenv, RDataFactoryClass, createIntArrayMethodID, len);
	}
	case REALSXP: {
		return (*thisenv)->CallStaticObjectMethod(thisenv, RDataFactoryClass, createDoubleArrayMethodID, len);
	}
	case STRSXP: {
		return (*thisenv)->CallStaticObjectMethod(thisenv, RDataFactoryClass, createStringArrayMethodID, len);
	}
	default:
		unimplemented("vector type not handled");
		return NULL;
	}
}

SEXP Rf_duplicate(SEXP x) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_duplicateMethodID, x);
}

void Rf_error(const char *msg, ...) {
	unimplemented("Rf_error");
}

SEXP Rf_install(const char *name) {
	JNIEnv *thisenv = getEnv();
	jstring string = (*thisenv)->NewStringUTF(thisenv, name);
	return (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, createSymbolMethodID, string);
}

SEXP Rf_mkChar(const char *x) {
	JNIEnv *thisenv = getEnv();
	// TODO encoding, assume UTF for now
	return (*thisenv)->NewStringUTF(thisenv, x);
}

SEXP Rf_protect(SEXP x) {
	// TODO perhaps we can use this
}

void Rf_unprotect(int x) {
	// TODO perhaps we can use this
}
