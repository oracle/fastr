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

void R_qsort_I  (double *v, int *II, int i, int j) {
	unimplemented("R_qsort_I");
}

void R_qsort_int_I(int *iv, int *II, int i, int j) {
	unimplemented("R_qsort_int_I");
}

void R_CheckUserInterrupt() {
// TODO (we don't even do this in the Java code)
}

void R_CheckStack(void) {
	unimplemented("R_CheckStack");
}

void R_CheckStack2(size_t x) {
	unimplemented("R_CheckStack2");
}

int R_finite(double x) {
	JNIEnv *env = getEnv();
	return (*env)->CallStaticBooleanMethod(env, RRuntimeClass, isFiniteMethodID, x);
}

int R_IsNaN(double x) {
	JNIEnv *env = getEnv();
	return (*env)->CallStaticBooleanMethod(env, RRuntimeClass, isNAorNaNMethodID, x);
}

void REprintf(const char *x, ...) {
	unimplemented("REprintf");
}

R_len_t R_BadLongVector(SEXP x, const char *y, int z) {
	unimplemented("R_BadLongVector");
}

int R_IsNA(double x) {
	unimplemented("R_IsNA");
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

const char *R_ExpandFileName(const char *x) {
	unimplemented("R_ExpandFileName");
}

Rboolean R_ToplevelExec(void (*fun)(void *), void *data) {
	unimplemented("R_ToplevelExec");
}

SEXP R_ExecWithCleanup(SEXP (*fun)(void *), void *data,
		       void (*cleanfun)(void *), void *cleandata) {
	unimplemented("R_ExecWithCleanup");
}

#include <R_ext/Connections.h>

SEXP   R_new_custom_connection(const char *description, const char *mode, const char *class_name, Rconnection *ptr) {
	unimplemented("R_new_custom_connection");
}

size_t R_ReadConnection(Rconnection con, void *buf, size_t n) {
	unimplemented("R_ReadConnection");
}

size_t R_WriteConnection(Rconnection con, void *buf, size_t n) {
	unimplemented("R_WriteConnection");
}

SEXP R_tryEval(SEXP x, SEXP y, int *z) {
	unimplemented("R_tryEval");
}

SEXP R_tryEvalSilent(SEXP x, SEXP y, int *z) {
	unimplemented("R_tryEvalSilent");
}

