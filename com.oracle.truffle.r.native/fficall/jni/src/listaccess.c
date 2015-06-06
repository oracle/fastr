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

static jmethodID CADR_MethodID;
static jmethodID CAR_MethodID;
static jmethodID CDR_MethodID;
static jmethodID SETCAR_MethodID;
static jmethodID SETCDR_MethodID;

void init_listaccess(JNIEnv *env) {
	CADR_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "CADR", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
	CAR_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "CAR", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
	CDR_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "CDR", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
	SETCAR_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "SETCAR", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1);
	SETCDR_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "SETCDR", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1);
}

SEXP TAG(SEXP e) {
    unimplemented("TAG");
}

SEXP CAR(SEXP e) {
	JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, CAR_MethodID, e);
    return mkGlobalRef(thisenv, result);
}

SEXP CDR(SEXP e) {
	JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, CDR_MethodID, e);
    return mkGlobalRef(thisenv, result);
}

SEXP CAAR(SEXP e) {
    unimplemented("CAAR");
}

SEXP CDAR(SEXP e) {
    unimplemented("CDAR");
}

SEXP CADR(SEXP e) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, CADR_MethodID, e);
    return mkGlobalRef(thisenv, result);
}

SEXP CDDR(SEXP e) {
    unimplemented("CDDR");
}

SEXP CADDR(SEXP e) {
    unimplemented("CADDR");
}

SEXP CADDDR(SEXP e) {
    unimplemented("CADDDR");
}

SEXP CAD4R(SEXP e) {
    unimplemented("CAD4R");
}

int MISSING(SEXP x){
    unimplemented("MISSING");
}

void SET_MISSING(SEXP x, int v) {
    unimplemented("SET_MISSING");
}

void SET_TAG(SEXP x, SEXP y) {
    unimplemented("SET_TAG");
}

SEXP SETCAR(SEXP x, SEXP y) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, SETCAR_MethodID, x, y);
    return mkGlobalRef(thisenv, result);
}

SEXP SETCDR(SEXP x, SEXP y) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, SETCDR_MethodID, x, y);
    return mkGlobalRef(thisenv, result);
}

SEXP SETCADR(SEXP x, SEXP y) {
    unimplemented("SETCADR");
}

SEXP SETCADDR(SEXP x, SEXP y) {
    unimplemented("SETCADDR");
}

SEXP SETCADDDR(SEXP x, SEXP y) {
    unimplemented("SETCADDDR");
}

SEXP SETCAD4R(SEXP e, SEXP y) {
    unimplemented("SETCAD4R");
}

