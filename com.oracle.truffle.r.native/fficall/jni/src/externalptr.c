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

static jclass RExternalPtrClass;
static jmethodID createExternalPtrMethodID;
static jmethodID externalPtrGetAddrMethodID;
static jmethodID externalPtrGetTagMethodID;
static jmethodID externalPtrGetProtMethodID;
static jmethodID externalPtrSetAddrMethodID;
static jmethodID externalPtrSetTagMethodID;
static jmethodID externalPtrSetProtMethodID;

void init_externalptr(JNIEnv *env) {
	RExternalPtrClass = checkFindClass(env, "com/oracle/truffle/r/runtime/data/RExternalPtr");
	createExternalPtrMethodID = checkGetMethodID(env, RDataFactoryClass, "createExternalPtr", "(JLjava/lang/Object;Ljava/lang/Object;)Lcom/oracle/truffle/r/runtime/data/RExternalPtr;", 1);
	externalPtrGetAddrMethodID = checkGetMethodID(env, RExternalPtrClass, "getAddr", "()J", 0);
	externalPtrGetTagMethodID = checkGetMethodID(env, RExternalPtrClass, "getTag", "()Ljava/lang/Object;", 0);
	externalPtrGetProtMethodID = checkGetMethodID(env, RExternalPtrClass, "getProt", "()Ljava/lang/Object;", 0);
	externalPtrSetAddrMethodID = checkGetMethodID(env, RExternalPtrClass, "setAddr", "(J)V", 0);
	externalPtrSetTagMethodID = checkGetMethodID(env, RExternalPtrClass, "setTag", "(Ljava/lang/Object;)V", 0);
	externalPtrSetProtMethodID = checkGetMethodID(env, RExternalPtrClass, "setProt", "(Ljava/lang/Object;)V", 0);

}

SEXP R_MakeExternalPtr(void *p, SEXP tag, SEXP prot) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticObjectMethod(thisenv, RDataFactoryClass, createExternalPtrMethodID, (jlong) p, tag, prot);
}

void *R_ExternalPtrAddr(SEXP s) {
	JNIEnv *thisenv = getEnv();
	return (void *) (*thisenv)->CallLongMethod(thisenv, s, externalPtrGetAddrMethodID);
}

SEXP R_ExternalPtrTag(SEXP s) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallObjectMethod(thisenv, s, externalPtrGetTagMethodID);
}

SEXP R_ExternalPtrProt(SEXP s) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallObjectMethod(thisenv, s, externalPtrGetProtMethodID);
}

void R_SetExternalPtrAddr(SEXP s, void *p) {
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallLongMethod(thisenv, s, externalPtrSetAddrMethodID, (jlong) p);
}

void R_SetExternalPtrTag(SEXP s, SEXP tag) {
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallObjectMethod(thisenv, s, externalPtrSetTagMethodID, tag);
}

void R_SetExternalPtrProt(SEXP s, SEXP p) {
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallObjectMethod(thisenv, s, externalPtrSetProtMethodID, p);
}

void R_ClearExternalPtr(SEXP s) {
	R_SetExternalPtrAddr(s, NULL);
}

