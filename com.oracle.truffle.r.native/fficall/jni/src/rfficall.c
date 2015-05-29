/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

#include "rffiutils.h"
#include <string.h>

SEXP R_NilValue;

JNIEXPORT void JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_initialize(JNIEnv *env, jclass c, jobject RNullInstance) {
	init_utils(env);

	R_NilValue = RNullInstance;

	init_register(env);
	init_rf_functions(env);
	init_externalptr(env);
	init_typecoerce(env);
	init_attrib(env);
	init_misc(env);
	init_vectoraccess(env);
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
	setEnv(env);
	call0func call0 = (call0func) address;
	return (*call0)();
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call1(JNIEnv *env, jclass c, jlong address, jobject arg1) {
	setEnv(env);
	call1func call1 = (call1func) address;
	return (*call1)(arg1);
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call2(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2) {
	setEnv(env);
	call2func call2 = (call2func) address;
	return (*call2)(arg1, arg2);
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call3(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3) {
	setEnv(env);
	call3func call3 = (call3func) address;
	return (*call3)(arg1, arg2, arg3);
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call4(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4) {
	setEnv(env);
	call4func call4 = (call4func) address;
	return (*call4)(arg1, arg2, arg3, arg4);
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call5(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4, jobject arg5) {
	setEnv(env);
	call5func call5 = (call5func) address;
	return (*call5)(arg1, arg2, arg3, arg4, arg5);
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call6(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4, jobject arg5, jobject arg6) {
	setEnv(env);
	call6func call6 = (call6func) address;
	return (*call6)(arg1, arg2, arg3, arg4, arg5, arg6);
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call7(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4, jobject arg5, jobject arg6, jobject arg7) {
	setEnv(env);
	call7func call7 = (call7func) address;
	return (*call7)(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call8(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4, jobject arg5, jobject arg6, jobject arg7, jobject arg8) {
	setEnv(env);
	call8func call8 = (call8func) address;
	return (*call8)(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call9(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4, jobject arg5, jobject arg6, jobject arg7, jobject arg8, jobject arg9) {
	setEnv(env);
	call9func call9 = (call9func) address;
	return (*call9)(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call(JNIEnv *env, jclass c, jlong address, jobjectArray args) {
	setEnv(env);
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

typedef void (*callVoid1func)(SEXP arg1);

JNIEXPORT void JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_callVoid1(JNIEnv *env, jclass c, jlong address, jobject arg1) {
	setEnv(env);
	callVoid1func call1 = (callVoid1func) address;
	(*call1)(arg1);
}



