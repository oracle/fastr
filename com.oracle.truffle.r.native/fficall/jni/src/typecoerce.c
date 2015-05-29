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

static jmethodID Rf_asIntegerMethodID;
static jmethodID Rf_asRealMethodID;
static jmethodID Rf_asCharMethodID;

void init_typecoerce(JNIEnv *env) {
	Rf_asIntegerMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_asInteger", "(Ljava/lang/Object;)I", 1);
	Rf_asRealMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_asReal", "(Ljava/lang/Object;)D", 1);
	Rf_asCharMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_asChar", "(Ljava/lang/Object;)Ljava/lang/String;", 1);
}

SEXP Rf_asChar(SEXP x){
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_asCharMethodID, x);
}

SEXP Rf_coerceVector(SEXP x, SEXPTYPE t){
	unimplemented("Rf_coerceVector");
}

SEXP Rf_PairToVectorList(SEXP x){
	unimplemented("Rf_PairToVectorList");
}

SEXP Rf_VectorToPairList(SEXP x){
	unimplemented("Rf_coerceVector");
}

SEXP Rf_asCharacterFactor(SEXP x){
	unimplemented("Rf_VectorToPairList");
}

int Rf_asLogical(SEXP x){
	unimplemented("Rf_asLogical");
}

int Rf_asInteger(SEXP x) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticIntMethod(thisenv, CallRFFIHelperClass, Rf_asIntegerMethodID, x);
}

double Rf_asReal(SEXP x) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticDoubleMethod(thisenv, CallRFFIHelperClass, Rf_asRealMethodID, x);
}

Rcomplex Rf_asComplex(SEXP x){
	unimplemented("Rf_asLogical");
}
