/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#define NO_FASTR_REDEFINE
#include <Rinterface.h>
#include <rffiutils.h>
#include <Rinternals_common.h>
#include <polyglot.h>
#include "../common/rffi_upcalls.h"

// Most everything in RInternals.h

#define INTERNAL_UPCALLS_TABLE_SIZE UPCALLS_TABLE_SIZE + 2
#define bytesToNativeCharArray_x UPCALLS_TABLE_SIZE
#define charSXPToNativeCharArray_x UPCALLS_TABLE_SIZE + 1

typedef char* (*call_bytesToNativeCharArray)(SEXP e);
typedef char* (*call_charSXPToNativeCharArray)(SEXP e);
typedef char* (*call_R_Home)();

void **callbacks = NULL;

void Rinternals_setCallbacksAddress(void** theCallbacks) {
	callbacks = theCallbacks;
}

typedef SEXP (*call_Test)(const char *name);

SEXP Rinternals_invoke(int index) {
	void *callback = callbacks[index];
	return ((call_Test) callback)("aaa");
}

static char *ensure_truffle_chararray_n(const char *x, long n) {
	if (polyglot_is_value(x)) {
		return x;
	} else {
		return ((call_bytesToNativeCharArray) callbacks[bytesToNativeCharArray_x])(polyglot_from_string_n(x, n, "ascii"));
	}
}

char *ensure_truffle_chararray(const char *x) {
	if (polyglot_is_value(x)) {
		return (char *)x;
	} else {
		return ((call_bytesToNativeCharArray) callbacks[bytesToNativeCharArray_x])(polyglot_from_string(x, "ascii"));
  }
}

void *ensure_string(const char *x) {
    return x == NULL ? NULL : polyglot_from_string(x, "ascii");
}

void *ensure_function(void *fptr) {
	return fptr;
}

char *FASTR_R_Home() {
	return ((call_R_Home) callbacks[R_Home_x])();
}


#include <string.h>
#include "../truffle_common/Rinternals_truffle_common.h"

int *INTEGER(SEXP x){
	return (int*) ((call_INTEGER) callbacks[INTEGER_x])(x);
}

double *REAL(SEXP x){
	return (double*) ((call_REAL) callbacks[REAL_x])(x);
}

/* Unwind-protect mechanism to support C++ stack unwinding. */

// NB: It cannot be properly implemented until Sulong supports setjmp and longjmp.

SEXP R_MakeUnwindCont() {
    return R_NilValue;
}

void NORET R_ContinueUnwind(SEXP cont) {
}

SEXP R_UnwindProtect(SEXP (*fun)(void *data), void *data,
		     void (*cleanfun)(void *data, Rboolean jump),
		     void *cleandata, SEXP cont) {
    SEXP result;

	result = fun(data);
    cleanfun(cleandata, FALSE);

    return result;
}

int call_base_dispatchHandlers() {
    return dispatchHandlers();
}
