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
#include <errno.h>

/*
 * All calls pass through one of the call(N) methods in rfficall.c, which carry the JNIEnv value,
 * that needs to be saved for reuse in the many R functions such as Rf_allocVector.
 * Currently only single threaded access is permitted (via a semaphore in CallRFFIWithJNI)
 * so we are safe to use static variables. TODO Figure out where to store such state
 * (portably) for MT use. JNI provides no help.
 */
jclass CallRFFIHelperClass;
jclass RDataFactoryClass;
jclass RRuntimeClass;
jclass CharSXPWrapperClass;

static jclass RInternalErrorClass;
static jmethodID unimplementedMethodID;
jmethodID createSymbolMethodID;
static jmethodID validateMethodID;

static JNIEnv *curenv = NULL;

// default for trace output when enabled
FILE *traceFile = NULL;

typedef struct globalRefTable_struct {
	int permanent;
	SEXP gref;         // The jobject (SEXP) global ref
} GlobalRefElem;

#define CACHED_GLOBALREFS_INITIAL_SIZE 64
static GlobalRefElem *cachedGlobalRefs;
static int cachedGlobalRefsHwm;
static int cachedGlobalRefsLength;

// Data structure for managing the required copying of
// Java arrays to return C arrays, e.g, int*.
// N.B. There are actually two levels to this as FastR
// wraps, e.g.,  int[] in an RIntVector.
typedef struct nativeArrayTable_struct {
	SEXPTYPE type;
	SEXP obj;         // The jobject (SEXP) that data is derived from (e.g, RIntVector)
	void *jArray;     // the jarray corresponding to obj
	void *data;       // the (possibly) copied (or pinned) data from JNI GetXXXArrayElements
} NativeArrayElem;

#define NATIVE_ARRAY_TABLE_INITIAL_SIZE 64
// A table of vectors that have been accessed and whose contents, e.g. the actual data
// as a primitive array have been copied and handed out to the native code.
static NativeArrayElem *nativeArrayTable;
// hwm of nativeArrayTable
static int nativeArrayTableHwm;
static int nativeArrayTableLength;
static void releaseNativeArray(JNIEnv *env, int index, int freedata);

static int isEmbedded = 0;
void setEmbedded() {
	isEmbedded = 1;
}

// native down call depth, indexes nativeArrayTableHwmStack
int callDepth = 0;

#define CALLDEPTH_STACK_SIZE 16
static int nativeArrayTableHwmStack[CALLDEPTH_STACK_SIZE];

// stack of jmp_buf ptrs for non-local control transfer on error
static jmp_buf* callErrorJmpBufTable[CALLDEPTH_STACK_SIZE];


void init_utils(JNIEnv *env) {
	curenv = env;
	if (TRACE_ENABLED && traceFile == NULL) {
		if (!isEmbedded) {
			traceFile = stdout;
		} else {
			jclass RFFIUtilsClass = checkFindClass(env, "com/oracle/truffle/r/runtime/ffi/RFFIUtils");
			jclass FileDescriptorClass = checkFindClass(env, "java/io/FileDescriptor");
			jmethodID getTraceFileDescriptorMethodID = checkGetMethodID(env, RFFIUtilsClass, "getTraceFileDescriptor", "()Ljava/io/FileDescriptor;", 1);
			// ASSUMPTION: FileDescriptor has an "fd" field
			jobject tfd = (*env)->CallStaticObjectMethod(env, RFFIUtilsClass, getTraceFileDescriptorMethodID);
			jfieldID fdField = checkGetFieldID(env, FileDescriptorClass, "fd", "I", 0);
			int fd = (*env)->GetIntField(env, tfd, fdField);
		    traceFile = fdopen(fd, "w");
		    if (traceFile == NULL) {
				fprintf(stderr, "%s, %d", "failed to fdopen trace file on JNI side\n", errno);
				exit(1);
			}
		    // no buffering
		    setvbuf(traceFile, (char*) NULL, _IONBF, 0);
		}
	}
	RDataFactoryClass = checkFindClass(env, "com/oracle/truffle/r/runtime/data/RDataFactory");
	CallRFFIHelperClass = checkFindClass(env, "com/oracle/truffle/r/runtime/ffi/jnr/CallRFFIHelper");
	RRuntimeClass = checkFindClass(env, "com/oracle/truffle/r/runtime/RRuntime");
	RInternalErrorClass = checkFindClass(env, "com/oracle/truffle/r/runtime/RInternalError");
	unimplementedMethodID = checkGetMethodID(env, RInternalErrorClass, "unimplemented", "(Ljava/lang/String;)Ljava/lang/RuntimeException;", 1);
	createSymbolMethodID = checkGetMethodID(env, RDataFactoryClass, "createSymbolInterned", "(Ljava/lang/String;)Lcom/oracle/truffle/r/runtime/data/RSymbol;", 1);
    validateMethodID = checkGetMethodID(env, CallRFFIHelperClass, "validate", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
    cachedGlobalRefs = calloc(CACHED_GLOBALREFS_INITIAL_SIZE, sizeof(GlobalRefElem));
    cachedGlobalRefsLength = CACHED_GLOBALREFS_INITIAL_SIZE;
    cachedGlobalRefsHwm = 0;
	nativeArrayTable = calloc(NATIVE_ARRAY_TABLE_INITIAL_SIZE, sizeof(NativeArrayElem));
	nativeArrayTableLength = NATIVE_ARRAY_TABLE_INITIAL_SIZE;
	nativeArrayTableHwm = 0;
}

const char *stringToChars(JNIEnv *jniEnv, jstring string) {
	// This is nasty:
	// 1. the resulting character array has to be copied and zero-terminated.
	// 2. It causes an (inevitable?) memory leak
	jsize len = (*jniEnv)->GetStringUTFLength(jniEnv, string);
	const char *stringChars = (*jniEnv)->GetStringUTFChars(jniEnv, string, NULL);
	char *copyChars = malloc(len + 1);
	memcpy(copyChars, stringChars, len);
	copyChars[len] = 0;
	return copyChars;
}

void callEnter(JNIEnv *env, jmp_buf *jmpbuf) {
	setEnv(env);
	//printf("callEnter: callDepth %d, jmpbufptr %p\n", callDepth, jmpbuf);
	callErrorJmpBufTable[callDepth] = jmpbuf;
	if (callDepth >= CALLDEPTH_STACK_SIZE) {
		fatalError("call stack overflow\n");
	}
	nativeArrayTableHwmStack[callDepth] = nativeArrayTableHwm;
	callDepth++;
}

jmp_buf *getErrorJmpBuf() {
	// printf("getErrorJmpBuf: callDepth %d, jmpbufptr %p\n", callDepth, callErrorJmpBufTable[callDepth - 1]);
	return callErrorJmpBufTable[callDepth - 1];
}

void callExit(JNIEnv *env) {
	int oldHwm = nativeArrayTableHwmStack[callDepth - 1];
	for (int i = oldHwm; i < nativeArrayTableHwm; i++) {
               releaseNativeArray(env, i, 1);
	}
	nativeArrayTableHwm = oldHwm;
	callDepth--;
}

void invalidateNativeArray(JNIEnv *env, SEXP oldObj) {
	for (int i = 0; i < nativeArrayTableHwm; i++) {
		NativeArrayElem cv = nativeArrayTable[i];
		if ((*env)->IsSameObject(env, cv.obj, oldObj)) {
#if TRACE_NATIVE_ARRAYS
			fprintf(traceFile, "invalidateNativeArray(%p): found\n", oldObj);
#endif
			releaseNativeArray(env, i, 1);
			nativeArrayTable[i].obj = NULL;
		}
	}
#if TRACE_NATIVE_ARRAYS
	fprintf(traceFile, "invalidateNativeArray(%p): not found\n", oldObj);
#endif
}

void updateNativeArrays(JNIEnv *env) {
	int oldHwm = nativeArrayTableHwmStack[callDepth - 1];
	for (int i = oldHwm; i < nativeArrayTableHwm; i++) {
        releaseNativeArray(env, i, 0);
	}
}


static void *findNativeArray(JNIEnv *env, SEXP x) {
	int i;
	for (i = 0; i < nativeArrayTableHwm; i++) {
		NativeArrayElem cv = nativeArrayTable[i];
		if (cv.obj != NULL) {
			if ((*env)->IsSameObject(env, cv.obj, x)) {
				void *data = cv.data;
#if TRACE_NATIVE_ARRAYS
				fprintf(traceFile, "findNativeArray(%p): found %p\n", x, data);
#endif
				return data;
			}
		}
	}
#if TRACE_NATIVE_ARRAYS
	fprintf(traceFile, "findNativeArray(%p): not found\n", x);
#endif
	return NULL;
}

static void addNativeArray(JNIEnv *env, SEXP x, SEXPTYPE type, void *jArray, void *data) {
#if TRACE_NATIVE_ARRAYS
	fprintf(traceFile, "addNativeArray(x=%p, t=%p, ix=%d)\n", x, data, nativeArrayTableHwm);
#endif
	// check for overflow
	if (nativeArrayTableHwm >= nativeArrayTableLength) {
		int newLength = 2 * nativeArrayTableLength;
		NativeArrayElem *newnativeArrayTable = calloc(newLength, sizeof(NativeArrayElem));
		if (newnativeArrayTable == NULL) {
			fatalError("FFI copied vectors table expansion failure");
		}
		memcpy(newnativeArrayTable, nativeArrayTable, nativeArrayTableLength * sizeof(NativeArrayElem));
		free(nativeArrayTable);
		nativeArrayTable = newnativeArrayTable;
		nativeArrayTableLength = newLength;
	}
	nativeArrayTable[nativeArrayTableHwm].obj = x;
	nativeArrayTable[nativeArrayTableHwm].data = data;
	nativeArrayTable[nativeArrayTableHwm].type = type;
	nativeArrayTable[nativeArrayTableHwm].jArray = jArray;
	nativeArrayTableHwm++;
}

void *getNativeArray(JNIEnv *thisenv, SEXP x, SEXPTYPE type) {
	void *data = findNativeArray(thisenv, x);
	jboolean isCopy;
	if (data == NULL) {
		jarray jArray;
		switch (type) {
		case INTSXP: {
			jintArray intArray = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, INTEGER_MethodID, x);
			int len = (*thisenv)->GetArrayLength(thisenv, intArray);
			data = (*thisenv)->GetIntArrayElements(thisenv, intArray, &isCopy);
			jArray = intArray;
			break;
		}

		case REALSXP: {
			jdoubleArray doubleArray = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, REAL_MethodID, x);
			int len = (*thisenv)->GetArrayLength(thisenv, doubleArray);
			data = (*thisenv)->GetDoubleArrayElements(thisenv, doubleArray, &isCopy);
			jArray = doubleArray;
			break;
		}

		case RAWSXP: {
		    jbyteArray byteArray = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, RAW_MethodID, x);
		    int len = (*thisenv)->GetArrayLength(thisenv, byteArray);
		    data = (*thisenv)->GetByteArrayElements(thisenv, byteArray, &isCopy);
	        jArray = byteArray;
	        break;
		}

		case LGLSXP: {
			// Special treatment becuase R FFI wants int* and FastR represents using byte[]
		    jbyteArray byteArray = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, LOGICAL_MethodID, x);
		    int len = (*thisenv)->GetArrayLength(thisenv, byteArray);
		    jbyte* internalData = (*thisenv)->GetByteArrayElements(thisenv, byteArray, &isCopy);
		    int* idata = malloc(len * sizeof(int));
		    for (int i = 0; i < len; i++) {
		    	char value = internalData[i];
		    	idata[i] = value == 0 ? FALSE : value == 1 ? TRUE : NA_INTEGER;
		    }
		    (*thisenv)->ReleaseByteArrayElements(thisenv, byteArray, internalData, JNI_ABORT);
		    jArray = byteArray;
		    data = idata;
		    break;
		}

		default:
			fatalError("getNativeArray: unexpected type");

		}
		addNativeArray(thisenv, x, type, jArray, data);
	}
	return data;
}

static void releaseNativeArray(JNIEnv *env, int i, int freedata) {
	NativeArrayElem cv = nativeArrayTable[i];
#if TRACE_NATIVE_ARRAYS
               fprintf(traceFile, "releaseNativeArray(x=%p, ix=%d, freedata=%d)\n", cv.obj, i, freedata);
#endif
	if (cv.obj != NULL) {
		switch (cv.type) {
		case INTSXP: {
			jintArray intArray = (jintArray) cv.jArray;
			(*env)->ReleaseIntArrayElements(env, intArray, (jint *)cv.data, freedata ? 0 : JNI_COMMIT);
			break;
		}

		case LGLSXP: {
			// for LOGICAL, we need to convert back to 1-byte elements
			jintArray byteArray = (jbyteArray) cv.jArray;
			int len = (*env)->GetArrayLength(env, byteArray);
			jbyte* internalData = (*env)->GetByteArrayElements(env, byteArray, NULL);
			int* data = (int*) cv.data;
			for (int i = 0; i < len; i++) {
				internalData[i] = data[i] == NA_INTEGER ? 255 : (jbyte) data[i];
			}
			(*env)->ReleaseByteArrayElements(env, byteArray, internalData, 0);
                       if (freedata){
                           free(data); // was malloc'ed in addNativeArray
                       }
			break;
		}

		case REALSXP: {
			jdoubleArray doubleArray = (jdoubleArray) cv.jArray;
			(*env)->ReleaseDoubleArrayElements(env, doubleArray, (jdouble *)cv.data, freedata ? 0 : JNI_COMMIT);
			break;

		}

		case RAWSXP: {
			jbyteArray byteArray = (jbyteArray) cv.jArray;
			(*env)->ReleaseByteArrayElements(env, byteArray, (jbyte *)cv.data, freedata ? 0 : JNI_COMMIT);
			break;

		}
		default:
			fatalError("releaseNativeArray type");
		}
               if (freedata) {
                   // free up the slot
		    cv.obj = NULL;
               }
	}
}

static SEXP findCachedGlobalRef(JNIEnv *env, SEXP obj) {
	for (int i = 0; i < cachedGlobalRefsHwm; i++) {
		GlobalRefElem elem = cachedGlobalRefs[i];
		if (elem.gref == NULL) {
			continue;
		}
		if ((*env)->IsSameObject(env, elem.gref, obj)) {
#if TRACE_REF_CACHE
			fprintf(traceFile, "gref: cache hit: %d\n", i);
#endif
			return elem.gref;
		}
	}
	return NULL;
}

SEXP addGlobalRef(JNIEnv *env, SEXP obj, int permanent) {
	SEXP gref;
	if (cachedGlobalRefsHwm >= cachedGlobalRefsLength) {
		int newLength = cachedGlobalRefsLength * 2;
#if TRACE_REF_CACHE
		fprintf(traceFile, "gref: extending table to %d\n", newLength);
#endif
		SEXP newCachedGlobalRefs = calloc(newLength, sizeof(GlobalRefElem));
		if (newCachedGlobalRefs == NULL) {
			fatalError("FFI global refs table expansion failure");
		}
		memcpy(newCachedGlobalRefs, cachedGlobalRefs, cachedGlobalRefsLength * sizeof(GlobalRefElem));
		free(cachedGlobalRefs);
		cachedGlobalRefs = newCachedGlobalRefs;
		cachedGlobalRefsLength = newLength;
	}
	gref = (*env)->NewGlobalRef(env, obj);
	cachedGlobalRefs[cachedGlobalRefsHwm].gref = gref;
	cachedGlobalRefs[cachedGlobalRefsHwm].permanent = permanent;
#if TRACE_REF_CACHE
			fprintf(traceFile, "gref: add: index %d, ref %p\n", cachedGlobalRefsHwm), gref;
#endif
	cachedGlobalRefsHwm++;
	return gref;
}

SEXP checkRef(JNIEnv *env, SEXP obj) {
	SEXP gref = findCachedGlobalRef(env, obj);
	TRACE(TARGpp, obj, global);
	if (gref == NULL) {
		return obj;
	} else {
	    return gref;
	}
}

SEXP createGlobalRef(JNIEnv *env, SEXP obj, int permanent) {
	SEXP gref = findCachedGlobalRef(env, obj);
	if (gref == NULL) {
		gref = addGlobalRef(env, obj, permanent);
	}
	return gref;
}

void releaseGlobalRef(JNIEnv *env, SEXP obj) {
	for (int i = 0; i < cachedGlobalRefsHwm; i++) {
		GlobalRefElem elem = cachedGlobalRefs[i];
		if (elem.gref == NULL || elem.permanent) {
			continue;
		}
		if ((*env)->IsSameObject(env, elem.gref, obj)) {
#if TRACE_REF_CACHE
			fprintf(traceFile, "gref: release: index %d, gref: %p\n", i, elem.gref);
#endif
			(*env)->DeleteGlobalRef(env, elem.gref);
			cachedGlobalRefs[i].gref = NULL;
		}
	}
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
//	fprintf(traceFile, "getEnv()=%p\n", curenv);
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
