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
#include <setjmp.h>

JNIEXPORT void JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_initialize(JNIEnv *env, jclass c,
		jobjectArray initialValues) {
	init_utils(env); // must be first
	init_variables(env, initialValues);
	init_register(env);
	init_rf_functions(env);
	init_externalptr(env);
	init_typecoerce(env);
	init_attrib(env);
	init_misc(env);
	init_rmath(env);
	init_rng(env);
	init_optim(env);
	init_vectoraccess(env);
	init_listaccess(env);
}

static jmp_buf error_jmpbuf;

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
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		call0func call0 = (call0func) address;
		result = (*call0)();
	}
	callExit(env);
	return result;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call1(JNIEnv *env, jclass c, jlong address, jobject arg1) {
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		call1func call1 = (call1func) address;
		result = (*call1)(checkRef(env, arg1));
	}
	callExit(env);
	return result;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call2(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2) {
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		call2func call2 = (call2func) address;
		result = (*call2)(checkRef(env, arg1), checkRef(env, arg2));
	}
	callExit(env);
	return result;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call3(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3) {
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		call3func call3 = (call3func) address;
		result = (*call3)(checkRef(env, arg1), checkRef(env, checkRef(env, arg2)), checkRef(env, arg3));
	}
	callExit(env);
	return result;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call4(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4) {
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		call4func call4 = (call4func) address;
		result = (*call4)(checkRef(env, arg1), checkRef(env, arg2), checkRef(env, arg3), checkRef(env, arg4));
	}
	callExit(env);
	return result;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call5(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4, jobject arg5) {
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		call5func call5 = (call5func) address;
		result = (*call5)(checkRef(env, arg1), checkRef(env, arg2), checkRef(env, arg3), checkRef(env, arg4), checkRef(env, arg5));
	}
	callExit(env);
	return result;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call6(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4, jobject arg5, jobject arg6) {
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		call6func call6 = (call6func) address;
		result = (*call6)(checkRef(env, arg1), checkRef(env, arg2), checkRef(env, arg3), checkRef(env, arg4), checkRef(env, arg5), checkRef(env, arg6));
	}
	callExit(env);
	return result;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call7(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4, jobject arg5, jobject arg6, jobject arg7) {
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		call7func call7 = (call7func) address;
		result = (*call7)(checkRef(env, arg1), checkRef(env, arg2), checkRef(env, arg3), checkRef(env, arg4), checkRef(env, arg5), checkRef(env, arg6), checkRef(env, arg7));
	}
	callExit(env);
	return result;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call8(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4, jobject arg5, jobject arg6, jobject arg7, jobject arg8) {
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		call8func call8 = (call8func) address;
		result = (*call8)(checkRef(env, arg1), checkRef(env, arg2), checkRef(env, arg3), checkRef(env, arg4), checkRef(env, arg5), checkRef(env, arg6), checkRef(env, arg7), checkRef(env, arg8));
	}
	callExit(env);
	return result;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call9(JNIEnv *env, jclass c, jlong address, jobject arg1, jobject arg2,
		jobject arg3, jobject arg4, jobject arg5, jobject arg6, jobject arg7, jobject arg8, jobject arg9) {
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		call9func call9 = (call9func) address;
		result = (*call9)(checkRef(env, arg1), checkRef(env, arg2), checkRef(env, arg3), checkRef(env, arg4), checkRef(env, arg5), checkRef(env, arg6), checkRef(env, arg7), checkRef(env, arg8), checkRef(env, arg9));
	}
	callExit(env);
	return result;
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_call(JNIEnv *env, jclass c, jlong address, jobjectArray args) {
	jobject result = NULL;
	callEnter(env, &error_jmpbuf);
	jsize len = (*env)->GetArrayLength(env, args);
	switch (len) {
	case 10: {
		// Sadly no GetObjectArrayRegion call, but there has to be a better way!
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
		if (!setjmp(error_jmpbuf)) {
			call10func call10 = (call10func) address;
			result = (*call10)(checkRef(env, arg1), checkRef(env, arg2), checkRef(env, arg3), checkRef(env, arg4), checkRef(env, arg5), checkRef(env, arg6), checkRef(env, arg7), checkRef(env, arg8), checkRef(env, arg9), arg10);
		}
		callExit(env);
		return result;
	}

	default:
		(*env)->FatalError(env, "call(JNI): unimplemented number of arguments");
		return NULL;
	}
}

typedef void (*callVoid1func)(SEXP arg1);

JNIEXPORT void JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_callVoid1(JNIEnv *env, jclass c, jlong address, jobject arg1) {
	callEnter(env, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		callVoid1func call1 = (callVoid1func) address;
		(*call1)(arg1);
	}
	callExit(env);
}



