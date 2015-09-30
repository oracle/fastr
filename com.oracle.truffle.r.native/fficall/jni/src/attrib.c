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

static jclass SEXPTYPEClass;
static jmethodID gnuRCodeForObjectMethodID;
static jmethodID NAMED_MethodID;

void init_attrib(JNIEnv *env) {
	SEXPTYPEClass = checkFindClass(env, "com/oracle/truffle/r/runtime/gnur/SEXPTYPE");
	gnuRCodeForObjectMethodID = checkGetMethodID(env, SEXPTYPEClass, "gnuRCodeForObject", "(Ljava/lang/Object;)I", 1);
	NAMED_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "NAMED", "(Ljava/lang/Object;)I", 1);
}

int TYPEOF(SEXP x) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticIntMethod(thisenv, SEXPTYPEClass, gnuRCodeForObjectMethodID, x);
}

SEXP ATTRIB(SEXP x){
    unimplemented("ATTRIB");
    return NULL;
}

int OBJECT(SEXP x){
    unimplemented("OBJECT");
    return 0;
}

int MARK(SEXP x){
    unimplemented("MARK");
    return 0;
}

int NAMED(SEXP x){
    JNIEnv *thisenv = getEnv();
    return (*thisenv)->CallStaticIntMethod(thisenv, CallRFFIHelperClass, NAMED_MethodID, x);
}

int REFCNT(SEXP x){
    unimplemented("REFCNT");
    return 0;
}

void SET_OBJECT(SEXP x, int v){
    unimplemented("SET_OBJECT");
}

void SET_TYPEOF(SEXP x, int v){
    unimplemented("SET_TYPEOF");
}

void SET_NAMED(SEXP x, int v){
    unimplemented("SET_NAMED");
}

void SET_ATTRIB(SEXP x, SEXP v){
    unimplemented("SET_ATTRIB");
}

void DUPLICATE_ATTRIB(SEXP to, SEXP from){
    unimplemented("DUPLICATE_ATTRIB");
}

