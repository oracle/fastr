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

// Registering routines from loaded shared libraries - LLVM variant

#include <R_ext/Rdynload.h>
#include <truffle.h>
#include <rffiutils.h>

// Must match ordinal value for DLL.NativeSymbolType
#define C_NATIVE_TYPE 0
#define CALL_NATIVE_TYPE 1
#define FORTRAN_NATIVE_TYPE 2
#define EXTERNAL_NATIVE_TYPE 3

#define IMPORT_PKG_INIT() void *obj = truffle_import_cached("_fastr_rffi_pkginit")

int
R_registerRoutines(DllInfo *info, const R_CMethodDef * const croutines,
		   const R_CallMethodDef * const callRoutines,
		   const R_FortranMethodDef * const fortranRoutines,
		   const R_ExternalMethodDef * const externalRoutines) {
	IMPORT_PKG_INIT();
	int num;
	if (croutines) {
		for(num = 0; croutines[num].name != NULL; num++) {;}
		truffle_invoke(obj, "registerRoutines", info, C_NATIVE_TYPE, num, (long) croutines);
	}
	if (callRoutines) {
		for(num = 0; callRoutines[num].name != NULL; num++) {;}
		truffle_invoke(obj, "registerRoutines", info, CALL_NATIVE_TYPE, num, (long) callRoutines);
	}
	if (fortranRoutines) {
		for(num = 0; fortranRoutines[num].name != NULL; num++) {;}
		truffle_invoke(obj, "registerRoutines", info, FORTRAN_NATIVE_TYPE, num, (long) fortranRoutines);
	}
	if (externalRoutines) {
		for(num = 0; externalRoutines[num].name != NULL; num++) {;}
		truffle_invoke(obj, "registerRoutines", info, EXTERNAL_NATIVE_TYPE, num, (long) externalRoutines);
	}
    return 1;
}

void *PkgInit_setSymbol(int nstOrd, long routinesAddr, int index) {
	const char *name;
	void *fun;
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
	void *nameString = truffle_read_string(name);
	void *fundesc = truffle_address_to_function(fun);
	IMPORT_PKG_INIT();
	void *result = truffle_invoke(obj, "createDotSymbol", nameString, fundesc, numArgs);
	return result;
}

void R_RegisterCCallable(const char *package, const char *name, DL_FUNC fptr) {
	void *packageString = truffle_read_string(package);
	void *nameString = truffle_read_string(name);
	IMPORT_PKG_INIT();
	truffle_invoke(obj, "registerCCallable", packageString, nameString, (long) fptr);
}

Rboolean R_useDynamicSymbols(DllInfo *dllInfo, Rboolean value) {
	IMPORT_PKG_INIT();
	return truffle_invoke_i(obj, "useDynamicSymbols", dllInfo, value);
}

Rboolean R_forceSymbols(DllInfo *dllInfo, Rboolean value) {
	IMPORT_PKG_INIT();
	return truffle_invoke_i(obj, "forceSymbols", dllInfo, value);
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

