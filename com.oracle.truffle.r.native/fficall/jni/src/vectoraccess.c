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
jmethodID RAW_MethodID;
jmethodID LENGTH_MethodID;

void init_vectoraccess(JNIEnv *env) {
	SET_STRING_ELT_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "SET_STRING_ELT", "(Ljava/lang/Object;ILjava/lang/Object;)V", 1);
	RAW_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "RAW", "(Ljava/lang/Object;)[B", 1);
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
	unimplemented("INTEGER");
}


Rbyte *RAW(SEXP x){
	JNIEnv *thisenv = getEnv();
	jbyteArray bytearray = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, RAW_MethodID, x);
	int len = (*thisenv)->GetArrayLength(thisenv, bytearray);
	jbyte *data = (*thisenv)->GetByteArrayElements(thisenv, bytearray, NULL);
	void *result = malloc(len);
	memcpy(result, data, len);
	return (Rbyte *) result;
}


double *REAL(SEXP x){
	unimplemented("REAL");
}


Rcomplex *COMPLEX(SEXP x){
	unimplemented("COMPLEX");
}


SEXP STRING_ELT(SEXP x, R_xlen_t i){
	unimplemented("STRING_ELT");
}


SEXP VECTOR_ELT(SEXP x, R_xlen_t i){
	unimplemented("VECTOR_ELT");
}


void SET_STRING_ELT(SEXP x, R_xlen_t i, SEXP v){
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallStaticVoidMethod(thisenv, CallRFFIHelperClass, SET_STRING_ELT_MethodID, x, i, v);
}


SEXP SET_VECTOR_ELT(SEXP x, R_xlen_t i, SEXP v){
	unimplemented("SET_VECTOR_ELT");
}


SEXP *STRING_PTR(SEXP x){
	unimplemented("STRING_PTR");
}


SEXP *VECTOR_PTR(SEXP x){
	unimplemented("VECTOR_PTR");
}
