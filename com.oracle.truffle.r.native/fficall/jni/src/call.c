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
#include <jni.h>

#include <Rinternals.h>

/*
 * All calls pass through one of the call(N) methods, which carry the JNIEnv value,
 * which needs to be saved for reuse in the many R functions such as Rf_allocVector.
 * FastR is not currently multi-threaded so the value can safely be stored in a static.
 */

static JNIEnv *curenv = NULL;

JNIEnv *getEnv() {
	return curenv;
}

static jclass RDataFactoryClass;
static jclass CallRFFIHelperClass;
static jmethodID scalarIntegerMethodID;
static jmethodID createIntArrayMethodID;
static jmethodID getIntDataAtZeroID;

static jclass checkFindClass(JNIEnv *env, const char *name);
static jmethodID checkGetMethodID(JNIEnv *env, jclass klass, const char *name, const char *sig, int isStatic);

JNIEXPORT void JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_initialize(JNIEnv *env, jclass c) {
	RDataFactoryClass = checkFindClass(env, "com/oracle/truffle/r/runtime/data/RDataFactory");
	CallRFFIHelperClass = checkFindClass(env, "com/oracle/truffle/r/runtime/ffi/jnr/CallRFFIHelper");

	scalarIntegerMethodID = checkGetMethodID(env, CallRFFIHelperClass, "ScalarInteger", "(I)Lcom/oracle/truffle/r/runtime/data/RIntVector;", 1);
	createIntArrayMethodID = checkGetMethodID(env, RDataFactoryClass, "createIntVector", "(I)Lcom/oracle/truffle/r/runtime/data/RIntVector;", 1);
	getIntDataAtZeroID = checkGetMethodID(env, CallRFFIHelperClass, "getIntDataAtZero", "(Ljava/lang/Object;)I", 1);

}

SEXP Rf_ScalarInteger(int value) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, scalarIntegerMethodID, value);
}

SEXP Rf_allocVector(SEXPTYPE t, R_xlen_t len) {
	JNIEnv *thisenv = getEnv();
	switch (t) {
	case INTSXP: {
		return (*thisenv)->CallStaticObjectMethod(thisenv, RDataFactoryClass, createIntArrayMethodID, len);
	default:
		(*thisenv)->FatalError(thisenv, "vector type not handled");
		return NULL;
	}
	}
}

int Rf_asInteger(SEXP x) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticIntMethod(thisenv, CallRFFIHelperClass, getIntDataAtZeroID, x);
}

// Class/method search
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



// Boilerplate methods for the actual calls

typedef SEXP (*call0func)();
typedef SEXP (*call1func)(SEXP arg1);
typedef SEXP (*call2func)(SEXP arg1, SEXP arg2);
typedef SEXP (*call3func)(SEXP arg1, SEXP arg2, SEXP arg3);
typedef SEXP (*call4func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4);
typedef SEXP (*call5func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5);
typedef SEXP (*call6func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6);
typedef SEXP (*call7func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7);
typedef SEXP (*call8func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8);
typedef SEXP (*call9func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8, SEXP arg9);
typedef SEXP (*call10func)(SEXP arg1, SEXP arg2, SEXP arg3, SEXP arg4, SEXP arg5, SEXP arg6, SEXP arg7, SEXP arg8, SEXP arg9, SEXP arg10);

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

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call3(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3) {
	curenv = env;
	call3func call3 = (call3func) address;
	return (*call3)(arg1, arg2, arg3);
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call4(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4) {
	curenv = env;
	call4func call4 = (call4func) address;
	return (*call4)(arg1, arg2, arg3, arg4);
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call5(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4, jobject arg5) {
	curenv = env;
	call5func call5 = (call5func) address;
	return (*call5)(arg1, arg2, arg3, arg4, arg5);
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call6(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4, jobject arg5, jobject arg6) {
	curenv = env;
	call6func call6 = (call6func) address;
	return (*call6)(arg1, arg2, arg3, arg4, arg5, arg6);
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call7(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4, jobject arg5, jobject arg6, jobject arg7) {
	curenv = env;
	call7func call7 = (call7func) address;
	return (*call7)(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call8(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4, jobject arg5, jobject arg6, jobject arg7, jobject arg8) {
	curenv = env;
	call8func call8 = (call8func) address;
	return (*call8)(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call9(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4, jobject arg5, jobject arg6, jobject arg7, jobject arg8, jobject arg9) {
	curenv = env;
	call9func call9 = (call9func) address;
	return (*call9)(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call(JNIEnv *env, jclass c, jlong address, jobjectArray args) {
	curenv = env;
	jsize len = (*env)->GetArrayLength(env, args);
	switch (len) {
	case 10: {
		jobject arg1 = (*env)->GetObjectArrayElement(env, args, 0);
		jobject arg2 = (*env)->GetObjectArrayElement(env, args, 1);
		jobject arg3 = (*env)->GetObjectArrayElement(env, args, 2);
		jobject arg4 = (*env)->GetObjectArrayElement(env, args, 3);
		jobject arg5 = (*env)->GetObjectArrayElement(env, args, 4);
		jobject arg6 = (*env)->GetObjectArrayElement(env, args, 5);
		jobject arg7 = (*env)->GetObjectArrayElement(env, args, 6);
		jobject arg8 = (*env)->GetObjectArrayElement(env, args, 7);
		jobject arg9 = (*env)->GetObjectArrayElement(env, args, 8);
		jobject arg10 = (*env)->GetObjectArrayElement(env, args, 9);
		call10func call10 = (call10func) address;
		return (*call10)(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
	}

	default:
		(*env)->FatalError(env, "call(JNI): unimplemented number of arguments");
		return NULL;
	}
}


