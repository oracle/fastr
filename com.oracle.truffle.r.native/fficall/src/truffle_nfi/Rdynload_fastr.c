/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
#include <rffiutils.h>
#include <Rdynload.h>
#include <stdio.h>

static void (*call_registerRoutines)(DllInfo *dllInfo, int nstOrd, int num, long routines);
static int (*call_useDynamicSymbols)(DllInfo *dllInfo, Rboolean value);
static TruffleObject (*call_setDotSymbolValues)(DllInfo *dllInfo, char *name, long fun, int numArgs);
static int (*call_forceSymbols)(DllInfo *dllInfo, Rboolean value);

#define registerRoutines_x 0
#define useDynamicSymbols_x 1
#define setDotSymbolValues_x 2
#define forceSymbols_x 3

void Rdynload_init(TruffleEnv* env, int index, void* closure) {
	(*env)->newClosureRef(env, closure);
	switch (index) {
	case registerRoutines_x: call_registerRoutines = closure; break;
	case useDynamicSymbols_x: call_useDynamicSymbols = closure; break;
	case setDotSymbolValues_x: call_setDotSymbolValues = closure; break;
	case forceSymbols_x: call_forceSymbols = closure; break;
	}
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
	int num;
	if (croutines) {
		for(num = 0; croutines[num].name != NULL; num++) {;}
		//printf("R_registerRoutines %p,%d,%d,%p\n", info, C_NATIVE_TYPE, num, croutines);
		call_registerRoutines(info, C_NATIVE_TYPE, num, (long) croutines);
	}
	if (callRoutines) {
		for(num = 0; callRoutines[num].name != NULL; num++) {;}
		//printf("R_registerRoutines %p,%d,%d,%p\n", info, CALL_NATIVE_TYPE, num, callRoutines);
		call_registerRoutines(info, CALL_NATIVE_TYPE, num, (long) callRoutines);
	}
	if (fortranRoutines) {
		for(num = 0; fortranRoutines[num].name != NULL; num++) {;}
		//printf("R_registerRoutines %p,%p,%d,%d,%p\n", call_registerRoutines, info, FORTRAN_NATIVE_TYPE, num, fortranRoutines);
		call_registerRoutines(info, FORTRAN_NATIVE_TYPE, num, (long) fortranRoutines);
	}
	if (externalRoutines) {
		for(num = 0; externalRoutines[num].name != NULL; num++) {;}
		//printf("R_registerRoutines %p,%d,%d,%p\n", info, EXTERNAL_NATIVE_TYPE, num, externalRoutines);
		call_registerRoutines(info, EXTERNAL_NATIVE_TYPE, num, (long) externalRoutines);
	}
    return 1;
}

Rboolean R_useDynamicSymbols(DllInfo *dllInfo, Rboolean value) {
	return call_useDynamicSymbols(dllInfo, value);
}

Rboolean R_forceSymbols(DllInfo *dllInfo, Rboolean value) {
	return call_forceSymbols(dllInfo, value);
}



TruffleObject Rdynload_setSymbol(DllInfo *info, int nstOrd, long routinesAddr, int index) {
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
	}
	//printf("call_setDotSymbolValues %p, %s, %p, %d\n", info, name, fun, numArgs);
	TruffleObject result = call_setDotSymbolValues(info, (char *)name, fun, numArgs);

	return result;
}

extern SEXP unimplemented(char *fun);

void R_RegisterCCallable(const char *package, const char *name, DL_FUNC fptr) {
	// we ignore this for now
}

DL_FUNC R_GetCCallable(const char *package, const char *name) {
	return unimplemented("R_GetCCallable");
}

DL_FUNC R_FindSymbol(char const *name, char const *pkg,
		     R_RegisteredNativeSymbol *symbol) {
    return unimplemented("R_FindSymbol");
}
