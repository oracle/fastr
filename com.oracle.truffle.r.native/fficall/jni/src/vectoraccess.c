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
jmethodID SET_INTEGER_ELT_MethodID;
jmethodID SET_VECTOR_ELT_MethodID;
jmethodID RAW_MethodID;
jmethodID INTEGER_MethodID;
jmethodID STRING_ELT_MethodID;
jmethodID VECTOR_ELT_MethodID;
jmethodID LENGTH_MethodID;

void init_vectoraccess(JNIEnv *env) {
	SET_STRING_ELT_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "SET_STRING_ELT", "(Ljava/lang/Object;ILjava/lang/Object;)V", 1);
	SET_INTEGER_ELT_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "SET_INTEGER_ELT", "(Ljava/lang/Object;II)V", 1);
	SET_VECTOR_ELT_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "SET_VECTOR_ELT", "(Ljava/lang/Object;ILjava/lang/Object;)V", 1);
	RAW_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "RAW", "(Ljava/lang/Object;)[B", 1);
	INTEGER_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "INTEGER", "(Ljava/lang/Object;)[I", 1);
	STRING_ELT_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "STRING_ELT", "(Ljava/lang/Object;I)Ljava/lang/String;", 1);
	VECTOR_ELT_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "VECTOR_ELT", "(Ljava/lang/Object;I)Ljava/lang/Object;", 1);
	LENGTH_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "LENGTH", "(Ljava/lang/Object;)I", 1);
}


int LENGTH(SEXP x) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, LENGTH_MethodID, x);
}

R_len_t  Rf_length(SEXP x) {
	return LENGTH(x);
}


int TRUELENGTH(SEXP x){
	unimplemented("unimplemented");
}


void SETLENGTH(SEXP x, int v){
	unimplemented("SETLENGTH");
}


void SET_TRUELENGTH(SEXP x, int v){
	unimplemented("SET_TRUELENGTH");
}


R_xlen_t XLENGTH(SEXP x){
	unimplemented("XLENGTH");
}


R_xlen_t XTRUELENGTH(SEXP x){
	unimplemented("XTRUELENGTH");
}


int IS_LONG_VEC(SEXP x){
	unimplemented("IS_LONG_VEC");
}


int LEVELS(SEXP x){
	unimplemented("LEVELS");
}


int SETLEVELS(SEXP x, int v){
	unimplemented("SETLEVELS");
}



int *LOGICAL(SEXP x){
	unimplemented("LOGICAL");
}


int *INTEGER(SEXP x){
	// TODO This does not support write access, e.g. INTEGER(x)[i]
	JNIEnv *thisenv = getEnv();
	jintArray intarray = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, INTEGER_MethodID, x);
	int len = (*thisenv)->GetArrayLength(thisenv, intarray);
	jint *data = (*thisenv)->GetIntArrayElements(thisenv, intarray, NULL);
	void *result = malloc(len * 4);
	memcpy(result, data, len * 4);
	(*thisenv)->ReleaseIntArrayElements(thisenv, intarray, data, JNI_ABORT);
	return (int *) result;
}


Rbyte *RAW(SEXP x){
	// TODO This does not support write access, e.g. RAW(x)[i]
	JNIEnv *thisenv = getEnv();
	jbyteArray bytearray = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, RAW_MethodID, x);
	int len = (*thisenv)->GetArrayLength(thisenv, bytearray);
	jbyte *data = (*thisenv)->GetByteArrayElements(thisenv, bytearray, NULL);
	void *result = malloc(len);
	memcpy(result, data, len);
	(*thisenv)->ReleaseByteArrayElements(thisenv, bytearray, data, JNI_ABORT);
	return (Rbyte *) result;
}


double *REAL(SEXP x){
	unimplemented("REAL");
}


Rcomplex *COMPLEX(SEXP x){
	unimplemented("COMPLEX");
}


SEXP STRING_ELT(SEXP x, R_xlen_t i){
	JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, STRING_ELT_MethodID, x, i);
    return mkGlobalRef(thisenv, result);
}


SEXP VECTOR_ELT(SEXP x, R_xlen_t i){
	JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, VECTOR_ELT_MethodID, x, i);
    return mkGlobalRef(thisenv, result);
}

void SET_INTEGER_ELT(SEXP x, R_xlen_t i, int v) {
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallStaticVoidMethod(thisenv, CallRFFIHelperClass, SET_INTEGER_ELT_MethodID, x, i, v);

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
}


SEXP *VECTOR_PTR(SEXP x){
	unimplemented("VECTOR_PTR");
}
