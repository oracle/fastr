/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
#include <rffiutils.h>
#include <Rdynload.h>

// Registering routines from loaded shared libraries

static jclass DLLClass;
static jclass JNI_PkgInitClass;
static jclass DotSymbolClass;

static jmethodID registerRoutinesID;
static jmethodID registerCCallableID;
static jmethodID useDynamicSymbolsID;
static jmethodID forceSymbolsID;
static jmethodID setDotSymbolValuesID;

void init_dynload(JNIEnv *env) {
    DLLClass = checkFindClass(env, "com/oracle/truffle/r/runtime/ffi/DLL");
    JNI_PkgInitClass = checkFindClass(env, "com/oracle/truffle/r/runtime/ffi/jnr/JNI_PkgInit");
    DotSymbolClass = checkFindClass(env, "com/oracle/truffle/r/runtime/ffi/DLL$DotSymbol");

    registerRoutinesID = checkGetMethodID(env, JNI_PkgInitClass, "registerRoutines", "(Lcom/oracle/truffle/r/runtime/ffi/DLL$DLLInfo;IIJ)V", 1);
    registerCCallableID = checkGetMethodID(env, JNI_PkgInitClass, "registerCCallable", "(Ljava/lang/String;Ljava/lang/String;J)V", 1);
    useDynamicSymbolsID = checkGetMethodID(env, JNI_PkgInitClass, "useDynamicSymbols", "(Lcom/oracle/truffle/r/runtime/ffi/DLL$DLLInfo;I)I", 1);
    forceSymbolsID = checkGetMethodID(env, JNI_PkgInitClass, "forceSymbols", "(Lcom/oracle/truffle/r/runtime/ffi/DLL$DLLInfo;I)I", 1);
    setDotSymbolValuesID = checkGetMethodID(env, JNI_PkgInitClass, "setDotSymbolValues", "(Ljava/lang/String;JI)Lcom/oracle/truffle/r/runtime/ffi/DLL$DotSymbol;", 1);
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
		(*thisenv)->CallStaticVoidMethod(thisenv, JNI_PkgInitClass, registerRoutinesID, info, C_NATIVE_TYPE, num, croutines);
	}
	if (callRoutines) {
		for(num = 0; callRoutines[num].name != NULL; num++) {;}
		(*thisenv)->CallStaticVoidMethod(thisenv, JNI_PkgInitClass, registerRoutinesID, info, CALL_NATIVE_TYPE, num, callRoutines);
	}
	if (fortranRoutines) {
		for(num = 0; fortranRoutines[num].name != NULL; num++) {;}
		(*thisenv)->CallStaticVoidMethod(thisenv, JNI_PkgInitClass, registerRoutinesID, info, FORTRAN_NATIVE_TYPE, num, fortranRoutines);
	}
	if (externalRoutines) {
		for(num = 0; externalRoutines[num].name != NULL; num++) {;}
		(*thisenv)->CallStaticVoidMethod(thisenv, JNI_PkgInitClass, registerRoutinesID, info, EXTERNAL_NATIVE_TYPE, num, externalRoutines);
	}
    return 1;
}

void R_RegisterCCallable(const char *package, const char *name, DL_FUNC fptr) {
	JNIEnv *thisenv = getEnv();
//	printf("pkgname %s, name %s\n", package, name);
	jstring packageString = (*thisenv)->NewStringUTF(thisenv, package);
	jstring nameString = (*thisenv)->NewStringUTF(thisenv, name);
	(*thisenv)->CallStaticVoidMethod(thisenv, JNI_PkgInitClass, registerCCallableID, packageString, nameString, fptr);
}

JNIEXPORT jobject JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_JNI_1PkgInit_setSymbol(JNIEnv *env, jclass c, jint nstOrd, jlong routinesAddr, jint index) {
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
	return (*env)->CallStaticObjectMethod(env, JNI_PkgInitClass, setDotSymbolValuesID, nameString, fun, numArgs);

}

Rboolean R_useDynamicSymbols(DllInfo *dllInfo, Rboolean value) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticIntMethod(thisenv, JNI_PkgInitClass, useDynamicSymbolsID, dllInfo, value);
}

Rboolean R_forceSymbols(DllInfo *dllInfo, Rboolean value) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticIntMethod(thisenv, JNI_PkgInitClass, forceSymbolsID, dllInfo, value);
}

DL_FUNC R_GetCCallable(const char *package, const char *name) {
	unimplemented("R_GetCCallable");
	return NULL;
}

DL_FUNC R_FindSymbol(char const *name, char const *pkg,
		     R_RegisteredNativeSymbol *symbol) {
    unimplemented("R_FindSymbol");
    return NULL;
}

DllInfo *R_getEmbeddingDllInfo(void) {
	return (DllInfo*) unimplemented("R_getEmbeddingDllInfo");
}
