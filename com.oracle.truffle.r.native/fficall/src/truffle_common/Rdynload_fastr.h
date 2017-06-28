/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
#include <Rdynload.h>

// The implementation must implement:
// void *ensure_function(void *fun)

typedef void (*call_registerRoutines)(DllInfo *dllInfo, int nstOrd, int num, long routines);
typedef int (*call_useDynamicSymbols)(DllInfo *dllInfo, Rboolean value);
typedef void * (*call_setDotSymbolValues)(DllInfo *dllInfo, char *name, void *fun, int numArgs);
typedef int (*call_forceSymbols)(DllInfo *dllInfo, Rboolean value);
typedef int (*call_registerCCallable)(const char *pkgname, const char *name, void *fun);
typedef void* (*call_getCCallable)(const char *pkgname, const char *name);

#define registerRoutines_x 0
#define setDotSymbolValues_x 1
#define useDynamicSymbols_x 2
#define forceSymbols_x 3
#define registerCCallable_x 4
#define getCCallable_x 5
#define CALLBACK_TABLE_SIZE 6

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
	int num;
	if (croutines) {
		for(num = 0; croutines[num].name != NULL; num++) {;}
		//printf("R_registerRoutines %p,%d,%d,%p\n", info, C_NATIVE_TYPE, num, croutines);
		((call_registerRoutines) dynload_callbacks[registerRoutines_x])(info, C_NATIVE_TYPE, num, (long) croutines);
	}
	if (callRoutines) {
		for(num = 0; callRoutines[num].name != NULL; num++) {;}
		//printf("R_registerRoutines %p,%d,%d,%p\n", info, CALL_NATIVE_TYPE, num, callRoutines);
		((call_registerRoutines) dynload_callbacks[registerRoutines_x])(info, CALL_NATIVE_TYPE, num, (long) callRoutines);
	}
	if (fortranRoutines) {
		for(num = 0; fortranRoutines[num].name != NULL; num++) {;}
		//printf("R_registerRoutines %p,%p,%d,%d,%p\n", info, FORTRAN_NATIVE_TYPE, num, fortranRoutines);
		((call_registerRoutines) dynload_callbacks[registerRoutines_x])(info, FORTRAN_NATIVE_TYPE, num, (long) fortranRoutines);
	}
	if (externalRoutines) {
		for(num = 0; externalRoutines[num].name != NULL; num++) {;}
		//printf("R_registerRoutines %p,%d,%d,%p\n", info, EXTERNAL_NATIVE_TYPE, num, externalRoutines);
		((call_registerRoutines) dynload_callbacks[registerRoutines_x])(info, EXTERNAL_NATIVE_TYPE, num, (long) externalRoutines);
	}
    return 1;
}

Rboolean R_useDynamicSymbols(DllInfo *dllInfo, Rboolean value) {
	//printf("R_useDynamicSymbols %p %d %p\n", dllInfo, value);
	return ((call_useDynamicSymbols) dynload_callbacks[useDynamicSymbols_x])(dllInfo, value);
}

Rboolean R_forceSymbols(DllInfo *dllInfo, Rboolean value) {
	//printf("R_forceSymbols %p %d\n", dllInfo, value);
	return ((call_forceSymbols) dynload_callbacks[forceSymbols_x])(dllInfo, value);
}


void *Rdynload_setSymbol(DllInfo *info, int nstOrd, long routinesAddr, int index) {
	const char *name;
	void * fun;
	int numArgs;
	switch (nstOrd) {
	case C_NATIVE_TYPE: {
		R_CMethodDef *croutines = (R_CMethodDef *) routinesAddr;
		name = croutines[index].name;
		fun = croutines[index].fun;
		numArgs = croutines[index].numArgs;
		break;
	}
	case CALL_NATIVE_TYPE: {
		R_CallMethodDef *callRoutines = (R_CallMethodDef *) routinesAddr;
		name = callRoutines[index].name;
		fun = callRoutines[index].fun;
		numArgs = callRoutines[index].numArgs;
		break;
	}
	case FORTRAN_NATIVE_TYPE: {
		R_FortranMethodDef * fortranRoutines = (R_FortranMethodDef *) routinesAddr;
		name = fortranRoutines[index].name;
		fun = fortranRoutines[index].fun;
		numArgs = fortranRoutines[index].numArgs;
		break;
	}
	case EXTERNAL_NATIVE_TYPE: {
		R_ExternalMethodDef * externalRoutines = (R_ExternalMethodDef *) routinesAddr;
		name = externalRoutines[index].name;
		fun = externalRoutines[index].fun;
		numArgs = externalRoutines[index].numArgs;
		break;
	}
	}
	//printf("call_setDotSymbolValues %p, %s, %p, %d\n", info, name, fun, numArgs);
	void *result = ((call_setDotSymbolValues) dynload_callbacks[setDotSymbolValues_x])(info, ensure_string(name), ensure_fun(fun), numArgs);
	return result;
}

void R_RegisterCCallable(const char *package, const char *name, DL_FUNC fptr) {
	//printf("R_RegisterCCallable %s %s %p\n", package, name, fptr);
	((call_registerCCallable) dynload_callbacks[registerCCallable_x])(ensure_string(package), ensure_string(name), ensure_fun((void *)fptr));
}

DL_FUNC R_GetCCallable(const char *package, const char *name) {
	//printf("R_GetCCallable %s %s %p\n", package, name);
	return ((call_getCCallable) dynload_callbacks[getCCallable_x])(ensure_string(package), ensure_string(name));
}

DL_FUNC R_FindSymbol(char const *name, char const *pkg,
		     R_RegisteredNativeSymbol *symbol) {
    return unimplemented("R_FindSymbol");
}
