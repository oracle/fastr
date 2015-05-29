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
#include <Rdynload.h>

// Registering routines from loaded shared libraries

static jclass DLLClass;
static jclass DotSymbolClass;

static jmethodID registerRoutinesID;
static jmethodID registerCCallableID;
static jmethodID useDynamicSymbolsID;
static jmethodID forceSymbolsID;
static jmethodID setDotSymbolValuesID;

void init_register(JNIEnv *env) {
    DLLClass = checkFindClass(env, "com/oracle/truffle/r/runtime/ffi/DLL");
    DotSymbolClass = checkFindClass(env, "com/oracle/truffle/r/runtime/ffi/DLL$DotSymbol");

    registerRoutinesID = checkGetMethodID(env, DLLClass, "registerRoutines", "(Lcom/oracle/truffle/r/runtime/ffi/DLL$DLLInfo;IIJ)V", 1);
    registerCCallableID = checkGetMethodID(env, DLLClass, "registerCCallable", "(Ljava/lang/String;Ljava/lang/String;J)V", 1);
    useDynamicSymbolsID = checkGetMethodID(env, DLLClass, "useDynamicSymbols", "(Lcom/oracle/truffle/r/runtime/ffi/DLL$DLLInfo;I)I", 1);
    forceSymbolsID = checkGetMethodID(env, DLLClass, "forceSymbols", "(Lcom/oracle/truffle/r/runtime/ffi/DLL$DLLInfo;I)I", 1);
    setDotSymbolValuesID = checkGetMethodID(env, DLLClass, "setDotSymbolValues", "(Ljava/lang/String;JI)Lcom/oracle/truffle/r/runtime/ffi/DLL$DotSymbol;", 1);
}

// Must match ordinal value for DLL.NativeSymbolType
#define C_NATIVE_TYPE 0
#define CALL_NATIVE_TYPE 1
#define FORTRAN_NATIVE_TYPE 2
#define EXTERNAL_NATIVE_TYPE 3

int
R_registerRoutines(DllInfo *info, const R_CMethodDef * const croutines,
		   const R_CallMethodDef * const callRoutines,
		   const R_FortranMethodDef * const fortranRoutines,
		   const R_ExternalMethodDef * const externalRoutines) {
	// In theory we could create all the data here and pass it up, but in practice there were inexplicable
	// Hotspot SEGV crashes creating Java arrays and Java objects in this function
	JNIEnv *thisenv = getEnv();
	int num;
	if (croutines) {
		for(num = 0; croutines[num].name != NULL; num++) {;}
		(*thisenv)->CallStaticVoidMethod(thisenv, DLLClass, registerRoutinesID, info, C_NATIVE_TYPE, num, croutines);
	}
	if (callRoutines) {
		for(num = 0; callRoutines[num].name != NULL; num++) {;}
		(*thisenv)->CallStaticVoidMethod(thisenv, DLLClass, registerRoutinesID, info, CALL_NATIVE_TYPE, num, callRoutines);
	}
	if (fortranRoutines) {
		for(num = 0; fortranRoutines[num].name != NULL; num++) {;}
		(*thisenv)->CallStaticVoidMethod(thisenv, DLLClass, registerRoutinesID, info, FORTRAN_NATIVE_TYPE, num, fortranRoutines);
	}
	if (externalRoutines) {
		for(num = 0; externalRoutines[num].name != NULL; num++) {;}
		(*thisenv)->CallStaticVoidMethod(thisenv, DLLClass, registerRoutinesID, info, EXTERNAL_NATIVE_TYPE, num, externalRoutines);
	}
    return 1;
}

void R_RegisterCCallable(const char *package, const char *name, DL_FUNC fptr) {
	JNIEnv *thisenv = getEnv();
//	printf("pkgname %s, name %s\n", package, name);
	jstring packageString = (*thisenv)->NewStringUTF(thisenv, package);
	jstring nameString = (*thisenv)->NewStringUTF(thisenv, name);
	(*thisenv)->CallStaticVoidMethod(thisenv, DLLClass, registerCCallableID, packageString, nameString, fptr);
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_DLL_setSymbol(JNIEnv *env, jclass c, jint nstOrd, jlong routinesAddr, jint index) {
	const char *name;
	long fun;
	int numArgs;

	switch (nstOrd) {
	case C_NATIVE_TYPE: {
		R_CMethodDef *croutines = (R_CMethodDef *) routinesAddr;
		name = croutines[index].name;
		fun = (long) croutines[index].fun;
		numArgs = croutines[index].numArgs;
		break;
	}
	case CALL_NATIVE_TYPE: {
		R_CallMethodDef *callRoutines = (R_CallMethodDef *) routinesAddr;
		name = callRoutines[index].name;
		fun = (long) callRoutines[index].fun;
		numArgs = callRoutines[index].numArgs;
		break;
	}
	case FORTRAN_NATIVE_TYPE: {
		R_FortranMethodDef * fortranRoutines = (R_FortranMethodDef *) routinesAddr;
		name = fortranRoutines[index].name;
		fun = (long) fortranRoutines[index].fun;
		numArgs = fortranRoutines[index].numArgs;
		break;
	}
	case EXTERNAL_NATIVE_TYPE: {
		R_ExternalMethodDef * externalRoutines = (R_ExternalMethodDef *) routinesAddr;
		name = externalRoutines[index].name;
		fun = (long) externalRoutines[index].fun;
		numArgs = externalRoutines[index].numArgs;
		break;
	}
	default: (*env)->FatalError(env, "NativeSymbolType out of range");
	}
//	printf("name %s, fun %0lx, numArgs %d\n", name, fun, numArgs);
	jstring nameString = (*env)->NewStringUTF(env, name);
	return (*env)->CallStaticObjectMethod(env, DLLClass, setDotSymbolValuesID, nameString, fun, numArgs);

}

Rboolean R_useDynamicSymbols(DllInfo *dllInfo, Rboolean value) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticIntMethod(thisenv, DLLClass, useDynamicSymbolsID, dllInfo, value);
}

Rboolean R_forceSymbols(DllInfo *dllInfo, Rboolean value) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticIntMethod(thisenv, DLLClass, forceSymbolsID, dllInfo, value);

}
