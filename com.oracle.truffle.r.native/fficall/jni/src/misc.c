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

jmethodID iS4ObjectMethodID;
jmethodID isFiniteMethodID;
jmethodID isNAorNaNMethodID;

void init_misc(JNIEnv *env) {
	iS4ObjectMethodID = checkGetMethodID(env, CallRFFIHelperClass, "isS4Object", "(Ljava/lang/Object;)I", 1);
	isFiniteMethodID = checkGetMethodID(env, RRuntimeClass, "isFinite", "(D)Z", 1);
	isNAorNaNMethodID = checkGetMethodID(env, RRuntimeClass, "isNAorNaN", "(D)Z", 1);
}

char *dgettext(const char *domainname, const char *msgid) {
	unimplemented("dgettext");
}

const char *R_CHAR(SEXP string) {
	TRACE("%s(%p)", string);
	// This is nasty:
	// 1. the resulting character array has to be copied and zero-terminated.
	// 2. It causes an (inevitable?) memory leak
	JNIEnv *thisenv = getEnv();
#if VALIDATE_REFS
	validateRef(thisenv, string, "R_CHAR");
#endif
	jsize len = (*thisenv)->GetStringUTFLength(thisenv, string);
	const char *stringChars = (*thisenv)->GetStringUTFChars(thisenv, string, NULL);
	char *copyChars = malloc(len + 1);
	memcpy(copyChars, stringChars, len);
	copyChars[len] = 0;
	return copyChars;
}

void R_isort(int *x, int n) {
	unimplemented("R_isort");
}

void R_rsort(double *x, int n) {
	unimplemented("R_rsort");
}

void R_qsort_int_I(int *iv, int *II, int i, int j) {
	unimplemented("R_qsort_int_I");
}

void rsort_with_index(double *a, int *b, int c) {
	unimplemented("rsort_with_index");
}

void revsort(double *a, int *b, int c) {
	unimplemented("revsort");
}

void R_CheckUserInterrupt() {
// TODO (we don't even do this in the Java code)
}

int R_finite(double x) {
	JNIEnv *env = getEnv();
	return (*env)->CallStaticBooleanMethod(env, RRuntimeClass, isFiniteMethodID, x);
}

int R_IsNaN(double x) {
	JNIEnv *env = getEnv();
	return (*env)->CallStaticBooleanMethod(env, RRuntimeClass, isNAorNaNMethodID, x);
}

int IS_S4_OBJECT(SEXP x) {
	JNIEnv *env = getEnv();
	return 	(*env)->CallStaticIntMethod(env, CallRFFIHelperClass, iS4ObjectMethodID, x);
}

void SET_S4_OBJECT(SEXP x) {
	unimplemented("SET_S4_OBJECT");
}
void UNSET_S4_OBJECT(SEXP x) {
	unimplemented("UNSET_S4_OBJECT");
}
