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
static jmethodID TAG_MethodID;
static jmethodID PRINTNAME_MethodID;
static jmethodID CAR_MethodID;
static jmethodID CDR_MethodID;
static jmethodID SETCAR_MethodID;
static jmethodID SETCDR_MethodID;

void init_listaccess(JNIEnv *env) {
    CADR_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "CADR", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
    TAG_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "TAG", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
    PRINTNAME_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "PRINTNAME", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
    CAR_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "CAR", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
    CDR_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "CDR", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
    SETCAR_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "SETCAR", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1);
    SETCDR_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "SETCDR", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1);
}

SEXP TAG(SEXP e) {
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, TAG_MethodID, e);
    return checkRef(thisenv, result);
}

SEXP PRINTNAME(SEXP e) {
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, PRINTNAME_MethodID, e);
    return checkRef(thisenv, result);
}

SEXP CAR(SEXP e) {
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, CAR_MethodID, e);
    return checkRef(thisenv, result);
}

SEXP CDR(SEXP e) {
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, CDR_MethodID, e);
    return checkRef(thisenv, result);
}

SEXP CAAR(SEXP e) {
    unimplemented("CAAR");
    return NULL;
}

SEXP CDAR(SEXP e) {
    unimplemented("CDAR");
    return NULL;
}

SEXP CADR(SEXP e) {
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, CADR_MethodID, e);
    return checkRef(thisenv, result);
}

SEXP CDDR(SEXP e) {
    unimplemented("CDDR");
    return NULL;
}

SEXP CADDR(SEXP e) {
    unimplemented("CADDR");
    return NULL;
}

SEXP CADDDR(SEXP e) {
    unimplemented("CADDDR");
    return NULL;
}

SEXP CAD4R(SEXP e) {
    unimplemented("CAD4R");
    return NULL;
}

int MISSING(SEXP x){
    unimplemented("MISSING");
    return 0;
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
    return checkRef(thisenv, result);
}

SEXP SETCDR(SEXP x, SEXP y) {
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, SETCDR_MethodID, x, y);
    return checkRef(thisenv, result);
}

SEXP SETCADR(SEXP x, SEXP y) {
    unimplemented("SETCADR");
    return NULL;
}

SEXP SETCADDR(SEXP x, SEXP y) {
    unimplemented("SETCADDR");
    return NULL;
}

SEXP SETCADDDR(SEXP x, SEXP y) {
    unimplemented("SETCADDDR");
    return NULL;
}

SEXP SETCAD4R(SEXP e, SEXP y) {
    unimplemented("SETCAD4R");
    return NULL;
}

SEXP FORMALS(SEXP x) {
    return unimplemented("FORMALS");
}

SEXP BODY(SEXP x) {
	return unimplemented("BODY");
}

SEXP CLOENV(SEXP x) {
	return unimplemented("CLOENV");
}

int RDEBUG(SEXP x) {
    return (int) unimplemented("RDEBUG");
}

int RSTEP(SEXP x) {
	return (int) unimplemented("RSTEP");
}

int RTRACE(SEXP x) {
	return (int) unimplemented("RTRACE");
}

void SET_RDEBUG(SEXP x, int v) {
    unimplemented("SET_RDEBUG");
}

void SET_RSTEP(SEXP x, int v) {
    unimplemented("SET_RSTEP");
}

void SET_RTRACE(SEXP x, int v) {
    unimplemented("SET_RTRACE");
}

void SET_FORMALS(SEXP x, SEXP v) {
    unimplemented("SET_FORMALS");
}

void SET_BODY(SEXP x, SEXP v) {
    unimplemented("SET_BODY");
}

void SET_CLOENV(SEXP x, SEXP v) {
    unimplemented("SET_CLOENV");
}

SEXP SYMVALUE(SEXP x) {
	return unimplemented("SYMVALUE");
}

SEXP INTERNAL(SEXP x) {
	return unimplemented("INTERNAL");
}

int DDVAL(SEXP x) {
	return (int) unimplemented("DDVAL");
}

void SET_DDVAL(SEXP x, int v) {
    unimplemented("SET_DDVAL");
}

void SET_SYMVALUE(SEXP x, SEXP v) {
    unimplemented("SET_SYMVALUE");
}

void SET_INTERNAL(SEXP x, SEXP v) {
    unimplemented("SET_INTERNAL");
}


SEXP FRAME(SEXP x) {
	return unimplemented("FRAME");
}

SEXP ENCLOS(SEXP x) {
	return unimplemented("ENCLOS");
}

SEXP HASHTAB(SEXP x) {
	return unimplemented("HASHTAB");
}

int ENVFLAGS(SEXP x) {
	return (int) unimplemented("ENVFLAGS");
}

void SET_ENVFLAGS(SEXP x, int v) {
	unimplemented("SET_ENVFLAGS");
}

void SET_FRAME(SEXP x, SEXP v) {
    unimplemented("SET_FRAME");
}

void SET_ENCLOS(SEXP x, SEXP v) {
	unimplemented("SET_ENCLOS");
}

void SET_HASHTAB(SEXP x, SEXP v) {
	unimplemented("SET_HASHTAB");
}


SEXP PRCODE(SEXP x) {
	return unimplemented("PRCODE");
}

SEXP PRENV(SEXP x) {
	return unimplemented("PRENV");
}

SEXP PRVALUE(SEXP x) {
	return unimplemented("PRVALUE");
}

int PRSEEN(SEXP x) {
	return (int) unimplemented("PRSEEN");
}

void SET_PRSEEN(SEXP x, int v) {
    unimplemented("SET_PRSEEN");
}

void SET_PRENV(SEXP x, SEXP v) {
    unimplemented("SET_PRENV");
}

void SET_PRVALUE(SEXP x, SEXP v) {
    unimplemented("SET_PRVALUE");
}

void SET_PRCODE(SEXP x, SEXP v) {
    unimplemented("SET_PRCODE");
}


