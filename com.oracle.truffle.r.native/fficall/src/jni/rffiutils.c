/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
#include <rffiutils.h>
#include <string.h>
#include <stdlib.h>

/*
 * All calls pass through one of the call(N) methods in rfficall.c, which carry the JNIEnv value,
 * that needs to be saved for reuse in the many R functions such as Rf_allocVector.
 * Currently only single threaded access is permitted (via a semaphore in CallRFFIWithJNI)
 * so we are safe to use static variables. TODO Figure out where to store such state
 * (portably) for MT use. JNI provides no help. N.B. The MT restriction also precludes
 * recursive calls.
 */
jclass CallRFFIHelperClass;
jclass RDataFactoryClass;
jclass RRuntimeClass;
jclass CharSXPWrapperClass;

static jclass RInternalErrorClass;
static jmethodID unimplementedMethodID;
jmethodID createSymbolMethodID;
static jmethodID validateMethodID;

JNIEnv *curenv = NULL;
jmp_buf *callErrorJmpBuf;

#define DEBUG_CACHE 0
#define TRACE_COPIES 0
#define CACHED_GLOBALREFS_TABLE_SIZE 100
static SEXP cachedGlobalRefs[CACHED_GLOBALREFS_TABLE_SIZE];
static SEXP checkCachedGlobalRef(JNIEnv *env, SEXP obj);

typedef struct CopiedVectors_struct {
	SEXPTYPE type;
	SEXP obj;
	void *jArray;
	void *data;
} CopiedVector;

#define COPIED_VECTORS_INITIAL_SIZE 100
// A table of vectors that have been accessed and whose contents, e.g. the actual data
// as a primitive array have been copied and handed out to the native code.
static CopiedVector *copiedVectors;
// hwm of copiedVectors
static int copiedVectorsIndex;
static int copiedVectorsLength;


void init_utils(JNIEnv *env) {
	curenv = env;
	RDataFactoryClass = checkFindClass(env, "com/oracle/truffle/r/runtime/data/RDataFactory");
	CallRFFIHelperClass = checkFindClass(env, "com/oracle/truffle/r/runtime/ffi/jnr/CallRFFIHelper");
	RRuntimeClass = checkFindClass(env, "com/oracle/truffle/r/runtime/RRuntime");
	RInternalErrorClass = checkFindClass(env, "com/oracle/truffle/r/runtime/RInternalError");
	unimplementedMethodID = checkGetMethodID(env, RInternalErrorClass, "unimplemented", "(Ljava/lang/String;)Ljava/lang/RuntimeException;", 1);
	createSymbolMethodID = checkGetMethodID(env, RDataFactoryClass, "createSymbolInterned", "(Ljava/lang/String;)Lcom/oracle/truffle/r/runtime/data/RSymbol;", 1);
    validateMethodID = checkGetMethodID(env, CallRFFIHelperClass, "validate", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
    for (int i = 0; i < CACHED_GLOBALREFS_TABLE_SIZE; i++) {
    	cachedGlobalRefs[i] = NULL;
    }
	copiedVectors = malloc(sizeof(CopiedVector) * COPIED_VECTORS_INITIAL_SIZE);
	copiedVectorsLength = COPIED_VECTORS_INITIAL_SIZE;
	copiedVectorsIndex = 0;
}

void callEnter(JNIEnv *env, jmp_buf *jmpbuf) {
	setEnv(env);
	callErrorJmpBuf = jmpbuf;
//	printf("callEnter\n");
}

jmp_buf *getErrorJmpBuf() {
	return callErrorJmpBuf;
}

void releaseCopiedVector(JNIEnv *env, CopiedVector cv) {
    if (cv.obj != NULL) {
	switch (cv.type) {
	    case INTSXP: case LGLSXP: {
		    jintArray intArray = (jintArray) cv.jArray;
		    (*env)->ReleaseIntArrayElements(env, intArray, (jint *)cv.data, 0);
		    break;
	    }

	    case REALSXP: {
		    jdoubleArray doubleArray = (jdoubleArray) cv.jArray;
		    (*env)->ReleaseDoubleArrayElements(env, doubleArray, (jdouble *)cv.data, 0);
		    break;

	    }

	    case RAWSXP: {
		    jbyteArray byteArray = (jbyteArray) cv.jArray;
		    (*env)->ReleaseByteArrayElements(env, byteArray, (jbyte *)cv.data, 0);
		    break;

	    }
	    default:
		fatalError("copiedVector type");
	}
    }
}

void callExit(JNIEnv *env) {
//	printf("callExit\n");
	int i;
	for (i = 0; i < copiedVectorsIndex; i++) {
		releaseCopiedVector(env, copiedVectors[i]);
	}
	copiedVectorsIndex = 0;
}

void invalidateCopiedObject(JNIEnv *env, SEXP oldObj) {
	int i;
	for (i = 0; i < copiedVectorsIndex; i++) {
		CopiedVector cv = copiedVectors[i];
		if ((*env)->IsSameObject(env, cv.obj, oldObj)) {
#if TRACE_COPIES
			printf("invalidateCopiedObject(%p): found\n", x);
#endif
			releaseCopiedVector(env, cv);
			copiedVectors[i].obj = NULL;
		}
	}
#if TRACE_COPIES
	printf("invalidateCopiedObject(%p): not found\n", x);
#endif
}

void *findCopiedObject(JNIEnv *env, SEXP x) {
	int i;
	for (i = 0; i < copiedVectorsIndex; i++) {
		CopiedVector cv = copiedVectors[i];
		if ((*env)->IsSameObject(env, cv.obj, x)) {
			void *data = cv.data;
#if TRACE_COPIES
			printf("findCopiedObject(%p): found %p\n", x, data);
#endif
			return data;
		}
	}
#if TRACE_COPIES
	printf("findCopiedObject(%p): not found\n", x);
#endif
	return NULL;
}

void addCopiedObject(JNIEnv *env, SEXP x, SEXPTYPE type, void *jArray, void *data) {
#if TRACE_COPIES
	printf("addCopiedObject(%p, %p)\n", x, data);
#endif
	if (copiedVectorsIndex >= copiedVectorsLength) {
		int newLength = 2 * copiedVectorsLength;
		CopiedVector *newCopiedVectors = malloc(sizeof(CopiedVector) * newLength);
		if (newCopiedVectors == NULL) {
			fatalError("malloc failure");
		}
		memcpy(newCopiedVectors, copiedVectors, copiedVectorsLength * sizeof(CopiedVector));
		free(copiedVectors);
		copiedVectors = newCopiedVectors;
		copiedVectorsLength = newLength;
	}
	copiedVectors[copiedVectorsIndex].obj = x;
	copiedVectors[copiedVectorsIndex].data = data;
	copiedVectors[copiedVectorsIndex].type = type;
	copiedVectors[copiedVectorsIndex].jArray = jArray;
	copiedVectorsIndex++;
#if TRACE_COPIES
	printf("copiedVectorsIndex: %d\n", copiedVectorsIndex);
#endif
}

SEXP checkRef(JNIEnv *env, SEXP obj) {
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
#if USE_GLOBAL
    SEXP result = (*env)->NewGlobalRef(env, obj);
#else
    SEXP result = obj;
#endif
	return result;
}

void validateRef(JNIEnv *env, SEXP x, const char *msg) {
	jobjectRefType t = (*env)->GetObjectRefType(env, x);
	if (t == JNIInvalidRefType) {
		char buf[1000];
		sprintf(buf, "%s %p", msg,x);
		fatalError(buf);
	}
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

void *unimplemented(char *msg) {
	JNIEnv *thisenv = getEnv();
	char buf[1024];
	strcpy(buf, "unimplemented ");
	strcat(buf, msg);
	(*thisenv)->FatalError(thisenv, buf);
	// to keep compiler happy
	return NULL;
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
	return (*env)->NewGlobalRef(env, klass);
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

jfieldID checkGetFieldID(JNIEnv *env, jclass klass, const char *name, const char *sig, int isStatic) {
	jfieldID fieldID = isStatic ? (*env)->GetStaticFieldID(env, klass, name, sig) : (*env)->GetFieldID(env, klass, name, sig);
	if (fieldID == NULL) {
		char buf[1024];
		strcpy(buf, "failed to find ");
		strcat(buf, isStatic ? "static" : "instance");
		strcat(buf, " field ");
		strcat(buf, name);
		strcat(buf, "(");
		strcat(buf, sig);
		strcat(buf, ")");
		(*env)->FatalError(env, buf);
	}
	return fieldID;
}
