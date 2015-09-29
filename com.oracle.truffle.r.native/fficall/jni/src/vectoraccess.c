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
#include <stdlib.h>
#include <string.h>

jmethodID SET_STRING_ELT_MethodID;
jmethodID SET_VECTOR_ELT_MethodID;
jmethodID RAW_MethodID;
jmethodID INTEGER_MethodID;
jmethodID REAL_MethodID;
jmethodID LOGICAL_MethodID;
jmethodID STRING_ELT_MethodID;
jmethodID VECTOR_ELT_MethodID;
jmethodID LENGTH_MethodID;

void init_vectoraccess(JNIEnv *env) {
	SET_STRING_ELT_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "SET_STRING_ELT", "(Ljava/lang/Object;ILjava/lang/Object;)V", 1);
	SET_VECTOR_ELT_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "SET_VECTOR_ELT", "(Ljava/lang/Object;ILjava/lang/Object;)V", 1);
	RAW_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "RAW", "(Ljava/lang/Object;)[B", 1);
	REAL_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "REAL", "(Ljava/lang/Object;)[D", 1);
	LOGICAL_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "LOGICAL", "(Ljava/lang/Object;)[I", 1);
	INTEGER_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "INTEGER", "(Ljava/lang/Object;)[I", 1);
	STRING_ELT_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "STRING_ELT", "(Ljava/lang/Object;I)Ljava/lang/String;", 1);
	VECTOR_ELT_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "VECTOR_ELT", "(Ljava/lang/Object;I)Ljava/lang/Object;", 1);
	LENGTH_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "LENGTH", "(Ljava/lang/Object;)I", 1);
}


int LENGTH(SEXP x) {
    TRACE(TARG1, x);
    JNIEnv *thisenv = getEnv();
    return (*thisenv)->CallStaticIntMethod(thisenv, CallRFFIHelperClass, LENGTH_MethodID, x);
}

R_len_t  Rf_length(SEXP x) {
    return LENGTH(x);
}


R_xlen_t  Rf_xlength(SEXP x) {
    // xlength seems to be used for long vectors (no such thing in FastR at the moment)
    return LENGTH(x);
}

int TRUELENGTH(SEXP x){
    unimplemented("unimplemented");
    return 0;
}


void SETLENGTH(SEXP x, int v){
    unimplemented("SETLENGTH");
}


void SET_TRUELENGTH(SEXP x, int v){
    unimplemented("SET_TRUELENGTH");
}


R_xlen_t XLENGTH(SEXP x){
    // xlength seems to be used for long vectors (no such thing in FastR at the moment)
    return LENGTH(x);
}


R_xlen_t XTRUELENGTH(SEXP x){
	unimplemented("XTRUELENGTH");
	return 0;
}


int IS_LONG_VEC(SEXP x){
	unimplemented("IS_LONG_VEC");
	return 0;
}


int LEVELS(SEXP x){
	unimplemented("LEVELS");
	return 0;
}


int SETLEVELS(SEXP x, int v){
	unimplemented("SETLEVELS");
	return 0;
}

int *LOGICAL(SEXP x){
	TRACE(TARG1, x);
	JNIEnv *thisenv = getEnv();
	jint *data = (jint *) findCopiedObject(thisenv, x);
	if (data == NULL) {
	    jintArray intArray = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, LOGICAL_MethodID, x);
	    int len = (*thisenv)->GetArrayLength(thisenv, intArray);
	    data = (*thisenv)->GetIntArrayElements(thisenv, intArray, NULL);
	    addCopiedObject(thisenv, x, LGLSXP, intArray, data);
	}
	return data;
}

int *INTEGER(SEXP x){
	TRACE(TARG1, x);
	JNIEnv *thisenv = getEnv();
	jint *data = (jint *) findCopiedObject(thisenv, x);
	if (data == NULL) {
	    jintArray intArray = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, INTEGER_MethodID, x);
	    int len = (*thisenv)->GetArrayLength(thisenv, intArray);
	    data = (*thisenv)->GetIntArrayElements(thisenv, intArray, NULL);
	    addCopiedObject(thisenv, x, INTSXP, intArray, data);
	}
	return data;
}


Rbyte *RAW(SEXP x){
	JNIEnv *thisenv = getEnv();
	jbyte *data = (jbyte *) findCopiedObject(thisenv, x);
	if (data == NULL) {
	    jbyteArray byteArray = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, RAW_MethodID, x);
	    int len = (*thisenv)->GetArrayLength(thisenv, byteArray);
	    data = (*thisenv)->GetByteArrayElements(thisenv, byteArray, NULL);
        addCopiedObject(thisenv, x, RAWSXP, byteArray, data);
    }
	return data;
}


double *REAL(SEXP x){
    JNIEnv *thisenv = getEnv();
    jdouble *data = (jdouble *) findCopiedObject(thisenv, x);
    if (data == NULL) {
	jdoubleArray doubleArray = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, REAL_MethodID, x);
	int len = (*thisenv)->GetArrayLength(thisenv, doubleArray);
	data = (*thisenv)->GetDoubleArrayElements(thisenv, doubleArray, NULL);
	addCopiedObject(thisenv, x, REALSXP, doubleArray, data);
    }
    return data;
}


Rcomplex *COMPLEX(SEXP x){
	unimplemented("COMPLEX");
	return NULL;
}


SEXP STRING_ELT(SEXP x, R_xlen_t i){
	TRACE(TARG2d, x, i);
	JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, STRING_ELT_MethodID, x, i);
    return checkRef(thisenv, result);
}


SEXP VECTOR_ELT(SEXP x, R_xlen_t i){
	JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, VECTOR_ELT_MethodID, x, i);
    return checkRef(thisenv, result);
}

void SET_STRING_ELT(SEXP x, R_xlen_t i, SEXP v){
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallStaticVoidMethod(thisenv, CallRFFIHelperClass, SET_STRING_ELT_MethodID, x, i, v);
}


SEXP SET_VECTOR_ELT(SEXP x, R_xlen_t i, SEXP v){
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallStaticVoidMethod(thisenv, CallRFFIHelperClass, SET_VECTOR_ELT_MethodID, x, i, v);
	return v;
}


SEXP *STRING_PTR(SEXP x){
	unimplemented("STRING_PTR");
	return NULL;
}


SEXP *VECTOR_PTR(SEXP x){
	unimplemented("VECTOR_PTR");
	return NULL;
}

