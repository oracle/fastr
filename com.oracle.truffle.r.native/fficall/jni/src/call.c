/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

#include <string.h>
#include <Rinternals.h>

/*
 * All calls pass through one of the call(N) methods, which carry the JNIEnv value.
 * This needs to be saved for reuse in the many R functions such as Rf_allocVector.
 * FastR is not currently multi-threaded so the value can be stored in a static.
 */

typedef SEXP (*call0func)();
typedef SEXP (*call1func)(SEXP arg1);
typedef SEXP (*call2func)(SEXP arg1, SEXP arg2);

static JNIEnv *curenv = NULL;

JNIEnv *getEnv() {
	return curenv;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call(JNIEnv *env, jclass c, jlong address, jobjectArray args) {
	curenv = env;
	jsize len = (*env)->GetArrayLength(env, args);
	switch (len) {
	case 0: {
		call0func call0 = (call0func) address;
		return (*call0)();
	}

	case 1: {
		jobject arg1 = (*env)->GetObjectArrayElement(env, args, 0);
		call1func call1 = (call1func) address;
		return (*call1)(arg1);
	}

	case 2: {
		jobject arg1 = (*env)->GetObjectArrayElement(env, args, 0);
		jobject arg2 = (*env)->GetObjectArrayElement(env, args, 1);
		call2func call2 = (call2func) address;
		return (*call2)(arg1, arg2);
	}

	default:
		(*env)->FatalError(env, "call(JNI): unexpected number of arguments");
		return NULL;
	}

}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call0(JNIEnv *env, jclass c, jlong address) {
	curenv = env;
	call0func call0 = (call0func) address;
	return (*call0)();
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call1(JNIEnv *env, jclass c, jlong address, jobject arg1) {
	curenv = env;
	call1func call1 = (call1func) address;
	return (*call1)(arg1);
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call2(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2) {
	curenv = env;
	call2func call2 = (call2func) address;
	return (*call2)(arg1, arg2);
}

static jclass checkFindClass(JNIEnv *env, const char *name) {
	jclass klass = (*env)->FindClass(env, name);
	if (klass == NULL) {
		char buf[1024];
		strcpy(buf, "failed to find class ");
		strcat(buf, name);
		(*env)->FatalError(env, buf);
	}
	return klass;
}

static jmethodID checkGetMethodID(JNIEnv *env, jclass klass, const char *name, const char *sig, int isStatic) {
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

SEXP Rf_allocVector(SEXPTYPE t, R_xlen_t len) {
	JNIEnv *thisenv = getEnv();
	jclass dfClass = checkFindClass(thisenv, "com/oracle/truffle/r/runtime/data/RDataFactory");
	switch (t) {
	case INTSXP: {
		jmethodID createIntArrayMethodID = checkGetMethodID(thisenv, dfClass, "createIntVector", "(I)Lcom/oracle/truffle/r/runtime/data/RIntVector;", 1);
		return (*thisenv)->CallStaticObjectMethod(thisenv, dfClass, createIntArrayMethodID, len);
	default:
		(*thisenv)->FatalError(thisenv, "vector type not handled");
		return NULL;
	}
	}
}

SEXP SET_VECTOR_ELT(SEXP x, R_xlen_t i, SEXP v) {
	// This assumes int
	JNIEnv *thisenv = getEnv();
	jclass callClass = checkFindClass(thisenv, "com/oracle/truffle/r/runtime/ffi/jnr/CallRFFIWithJNI");
	jmethodID updateDataAtMethodID = checkGetMethodID(thisenv, callClass, "updateIntDataAt", "(Lcom/oracle/truffle/r/runtime/data/RIntVector;II)V", 1);
	(*thisenv)->CallVoidMethod(thisenv, callClass, updateDataAtMethodID, x, 0, (int) v);
	return v;
}

int Rf_asInteger(SEXP x) {
	JNIEnv *thisenv = getEnv();
	jclass callClass = checkFindClass(thisenv, "com/oracle/truffle/r/runtime/ffi/jnr/CallRFFIWithJNI");
	jmethodID getIntDataAtZeroID = checkGetMethodID(thisenv, callClass, "getIntDataAtZero", "(Ljava/lang/Object;)I", 1);
	return (*thisenv)->CallStaticIntMethod(thisenv, callClass, getIntDataAtZeroID, x);
}

