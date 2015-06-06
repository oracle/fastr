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
#include <string.h>

/*
 * All calls pass through one of the call(N) methods, which carry the JNIEnv value,
 * which needs to be saved for reuse in the many R functions such as Rf_allocVector.
 * FastR is not currently multi-threaded so the value can safely be stored in a static.
 */
jclass CallRFFIHelperClass;
jclass RDataFactoryClass;

static jclass RInternalErrorClass;
static jmethodID unimplementedMethodID;
jmethodID createSymbolMethodID;
static jmethodID validateMethodID;

JNIEnv *curenv = NULL;

//#define DEBUG_CACHE 1
#define CACHED_GLOBALREFS_TABLE_SIZE 100
static SEXP cachedGlobalRefs[CACHED_GLOBALREFS_TABLE_SIZE];
static SEXP checkCachedGlobalRef(JNIEnv *env, SEXP obj);

void init_utils(JNIEnv *env) {
	curenv = env;
	RDataFactoryClass = checkFindClass(env, "com/oracle/truffle/r/runtime/data/RDataFactory");
	CallRFFIHelperClass = checkFindClass(env, "com/oracle/truffle/r/runtime/ffi/jnr/CallRFFIHelper");
	RInternalErrorClass = checkFindClass(env, "com/oracle/truffle/r/runtime/RInternalError");
	unimplementedMethodID = checkGetMethodID(env, RInternalErrorClass, "unimplemented", "(Ljava/lang/String;)Ljava/lang/RuntimeException;", 1);
	createSymbolMethodID = checkGetMethodID(env, RDataFactoryClass, "createSymbol", "(Ljava/lang/String;)Lcom/oracle/truffle/r/runtime/data/RSymbol;", 1);
    validateMethodID = checkGetMethodID(env, CallRFFIHelperClass, "validate", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
    for (int i = 0; i < CACHED_GLOBALREFS_TABLE_SIZE; i++) {
    	cachedGlobalRefs[i] = NULL;
    }
}

SEXP mkGlobalRef(JNIEnv *env, SEXP obj) {
	SEXP result = checkCachedGlobalRef(env, obj);
	return result;
}

SEXP mkNamedGlobalRef(JNIEnv *env, int index, SEXP obj) {
	SEXP result = (*env)->NewGlobalRef(env, obj);
	if (cachedGlobalRefs[index] != NULL) {
		fatalError("duplicate named global ref index\n");
	}
	cachedGlobalRefs[index] = result;
#if DEBUG_CACHE
	printf("gref: %d=%p\n", index, result);
#endif
	return result;
}

static SEXP checkCachedGlobalRef(JNIEnv *env, SEXP obj) {
    for (int i = 0; i < CACHED_GLOBALREFS_TABLE_SIZE; i++) {
    	SEXP ref = cachedGlobalRefs[i];
    	if (ref == NULL) {
    		break;
    	}
    	if ((*env)->IsSameObject(env, ref, obj)) {
#if DEBUG_CACHE
    		printf("gref: cache hit: %d\n", i);
#endif
    		return ref;
    	}
    }
    SEXP result = (*env)->NewGlobalRef(env, obj);
#if DEBUG_CACHE
	printf("gref: new=%p\n", result);
#endif
	return result;
}

void validate(SEXP x) {
	(*curenv)->CallStaticObjectMethod(curenv, CallRFFIHelperClass, validateMethodID, x);
}

JNIEnv *getEnv() {
//	printf("getEnv()=%p\n", curenv);
	return curenv;
}

void setEnv(JNIEnv *env) {
//	printf("setEnv(%p)\n", env);
	curenv = env;
}

void unimplemented(char *msg) {
	JNIEnv *thisenv = getEnv();
	(*thisenv)->FatalError(thisenv, msg);
}

void fatalError(char *msg) {
	JNIEnv *thisenv = getEnv();
	(*thisenv)->FatalError(thisenv, msg);
}

// Class/method search
jclass checkFindClass(JNIEnv *env, const char *name) {
	jclass klass = (*env)->FindClass(env, name);
	if (klass == NULL) {
		char buf[1024];
		strcpy(buf, "failed to find class ");
		strcat(buf, name);
		(*env)->FatalError(env, buf);
	}
	return mkGlobalRef(env, klass);
}

jmethodID checkGetMethodID(JNIEnv *env, jclass klass, const char *name, const char *sig, int isStatic) {
	jmethodID methodID = isStatic ? (*env)->GetStaticMethodID(env, klass, name, sig) : (*env)->GetMethodID(env, klass, name, sig);
	if (methodID == NULL) {
		char buf[1024];
		strcpy(buf, "failed to find ");
		strcat(buf, isStatic ? "static" : "instance");
		strcat(buf, " method ");
		strcat(buf, name);
		strcat(buf, "(");
		strcat(buf, sig);
		strcat(buf, ")");
		(*env)->FatalError(env, buf);
	}
	return methodID;
}
