/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "../common/rffi_upcalls.h"

void **callbacks = NULL;

static TruffleContext* truffleContext = NULL;

void Rinternals_addCallback(TruffleEnv* env, int index, void *closure) {
    if (truffleContext == NULL) {
        truffleContext = (*env)->getTruffleContext(env);
    }
	if (callbacks == NULL) {
		callbacks = malloc(UPCALLS_TABLE_SIZE * sizeof(void*));
	}
	(*env)->newClosureRef(env, closure);
	callbacks[index] = closure;
}

static int* return_int;
static double* return_double;
static char* return_byte;

char *ensure_truffle_chararray_n(const char *x, int n) {
	return (char *) x;
}

void *ensure_string(const char * x) {
	return (void *) x;
}

#include "../truffle_common/Rinternals_truffle_common.h"

long return_INTEGER_CREATE(int *value, int len) {
	int* idata = malloc(len * sizeof(int));
	memcpy(idata, value, len * sizeof(int));
	return_int = idata;
	return (long) idata;
}

long return_DOUBLE_CREATE(double *value, int len) {
	double* ddata = malloc(len * sizeof(double));
	memcpy(ddata, value, len * sizeof(double));
	return_double = ddata;
	return (long) ddata;
}

long return_BYTE_CREATE(char *value, int len, int isString) {
	if (isString) {
		len += 1;
	}
	char* bdata = malloc(len * sizeof(char));
	memcpy(bdata, value, len * sizeof(char));
	if (isString) {
		bdata[len] = 0;
	}
	return_byte = bdata;
	return (long) bdata;
}

void return_INTEGER_EXISTING(long address) {
	return_int = (int*) address;
}

void return_DOUBLE_EXISTING(long address) {
	return_double = (double*) address;
}

void return_BYTE_EXISTING(long address) {
	return_byte = (char*) address;
}

void return_FREE(void *address) {
//	free(address);
}

int *INTEGER(SEXP x) {
	((call_INTEGER) callbacks[INTEGER_x])(x);
	return return_int;
}

int *LOGICAL(SEXP x){
	((call_LOGICAL) callbacks[LOGICAL_x])(x);
	return return_int;
}

double *REAL(SEXP x){
	((call_REAL) callbacks[REAL_x])(x);
	return return_double;
}

Rbyte *RAW(SEXP x) {
	((call_RAW) callbacks[RAW_x])(x);
		return (Rbyte *) return_byte;
}

const char * R_CHAR(SEXP x) {
	((call_R_CHAR) callbacks[R_CHAR_x])(x);
	return return_byte;
}

