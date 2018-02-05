/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include <Rinterface.h>
#include <rffiutils.h>
#include <Rinternals_common.h>
#include <truffle.h>
#include "../common/rffi_upcalls.h"

// Most everything in RInternals.h

#define INTERNAL_UPCALLS_TABLE_SIZE UPCALLS_TABLE_SIZE + 2
#define bytesToNativeCharArray_x UPCALLS_TABLE_SIZE
#define charSXPToNativeCharArray_x UPCALLS_TABLE_SIZE + 1

typedef char* (*call_bytesToNativeCharArray)(SEXP e);
typedef char* (*call_charSXPToNativeCharArray)(SEXP e);
typedef char* (*call_R_Home)();

__thread void **callbacks = NULL;

void Rinternals_addCallback(void** theCallbacks, int index, void *callback) {
	if (callbacks == NULL) {
		callbacks = truffle_managed_malloc(INTERNAL_UPCALLS_TABLE_SIZE * sizeof(void*));
	}
//	printf("setting callback %d\n", index);
	callbacks[index] = callback;
}

void*** Rinternals_getCallbacksAddress() {
        return &callbacks;
}

typedef SEXP (*call_Test)(const char *name);

SEXP Rinternals_invoke(int index) {
	void *callback = callbacks[index];
	return ((call_Test) callback)("aaa");
}

static char *ensure_truffle_chararray_n(const char *x, int n) {
	if (truffle_is_truffle_object(x)) {
		return x;
	} else {
		return ((call_bytesToNativeCharArray) callbacks[bytesToNativeCharArray_x])(truffle_read_n_bytes(x, n));
	}
}

char *ensure_truffle_chararray(const char *x) {
	if (truffle_is_truffle_object(x)) {
		return (char *)x;
	} else {
		return ((call_bytesToNativeCharArray) callbacks[bytesToNativeCharArray_x])(truffle_read_n_bytes(x, strlen(x)));
  }
}

void *ensure_string(const char *x) {
	if (truffle_is_truffle_object(x)) {
		// The input argument may also be a NativeCharArray object. The wrapped String is
		// extracted by sending the EXECUTE message.
		return ((void*(*)())x)();
	} else {
		return x == NULL ? NULL : truffle_read_string(x);
	}
}

void *ensure_function(void *fptr) {
	return truffle_address_to_function(fptr);
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
