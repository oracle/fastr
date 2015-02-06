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

#include <string.h>
#include <jni.h>

#include <Rinternals.h>
#include <Rdynload.h>

/*
 * All calls pass through one of the call(N) methods, which carry the JNIEnv value,
 * which needs to be saved for reuse in the many R functions such as Rf_allocVector.
 * FastR is not currently multi-threaded so the value can safely be stored in a static.
 */

static JNIEnv *curenv = NULL;

JNIEnv *getEnv() {
//	printf("getEnv()=%p\n", curenv);
	return curenv;
}

void setEnv(JNIEnv *env) {
//	printf("setEnv(%p)\n", env);
	curenv = env;
}

static jclass RDataFactoryClass;
static jclass CallRFFIHelperClass;
static jclass DLLClass;
static jclass DotSymbolClass;

static jmethodID scalarIntegerMethodID;
static jmethodID scalarDoubleMethodID;
static jmethodID createIntArrayMethodID;
static jmethodID createDoubleArrayMethodID;
static jmethodID getIntDataAtZeroID;
static jmethodID getDoubleDataAtZeroID;
static jmethodID registerRoutinesID;
static jmethodID registerCCallableID;
static jmethodID useDynamicSymbolsID;
static jmethodID forceSymbolsID;
static jmethodID setDotSymbolValuesID;

static jclass checkFindClass(JNIEnv *env, const char *name);
static jmethodID checkGetMethodID(JNIEnv *env, jclass klass, const char *name, const char *sig, int isStatic);

JNIEXPORT void JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_CallRFFIWithJNI_initialize(JNIEnv *env, jclass c) {
	RDataFactoryClass = checkFindClass(env, "com/oracle/truffle/r/runtime/data/RDataFactory");
	CallRFFIHelperClass = checkFindClass(env, "com/oracle/truffle/r/runtime/ffi/jnr/CallRFFIHelper");
	DLLClass = checkFindClass(env, "com/oracle/truffle/r/runtime/ffi/DLL");
	DotSymbolClass = checkFindClass(env, "com/oracle/truffle/r/runtime/ffi/DLL$DotSymbol");

	scalarIntegerMethodID = checkGetMethodID(env, CallRFFIHelperClass, "ScalarInteger", "(I)Lcom/oracle/truffle/r/runtime/data/RIntVector;", 1);
	scalarDoubleMethodID = checkGetMethodID(env, CallRFFIHelperClass, "ScalarDouble", "(D)Lcom/oracle/truffle/r/runtime/data/RDoubleVector;", 1);
	createIntArrayMethodID = checkGetMethodID(env, RDataFactoryClass, "createIntVector", "(I)Lcom/oracle/truffle/r/runtime/data/RIntVector;", 1);
	createDoubleArrayMethodID = checkGetMethodID(env, RDataFactoryClass, "createDoubleVector", "(I)Lcom/oracle/truffle/r/runtime/data/RDoubleVector;", 1);
	getIntDataAtZeroID = checkGetMethodID(env, CallRFFIHelperClass, "getIntDataAtZero", "(Ljava/lang/Object;)I", 1);
	getDoubleDataAtZeroID = checkGetMethodID(env, CallRFFIHelperClass, "getDoubleDataAtZero", "(Ljava/lang/Object;)D", 1);
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

SEXP Rf_ScalarInteger(int value) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, scalarIntegerMethodID, value);
}

SEXP Rf_ScalarReal(double value) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, scalarDoubleMethodID, value);
}

SEXP Rf_allocVector(SEXPTYPE t, R_xlen_t len) {
	JNIEnv *thisenv = getEnv();
	switch (t) {
	case INTSXP: {
		return (*thisenv)->CallStaticObjectMethod(thisenv, RDataFactoryClass, createIntArrayMethodID, len);
	}
	case REALSXP: {
		return (*thisenv)->CallStaticObjectMethod(thisenv, RDataFactoryClass, createDoubleArrayMethodID, len);
	}
	default:
		(*thisenv)->FatalError(thisenv, "vector type not handled");
		return NULL;
	}
}

int Rf_asInteger(SEXP x) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticIntMethod(thisenv, CallRFFIHelperClass, getIntDataAtZeroID, x);
}

double Rf_asReal(SEXP x) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticDoubleMethod(thisenv, CallRFFIHelperClass, getDoubleDataAtZeroID, x);
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



