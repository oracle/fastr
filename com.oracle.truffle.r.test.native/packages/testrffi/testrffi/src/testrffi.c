/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

// A very simple test of the R FFI interface

#include <R.h>
#include <Rdefines.h>
#include <Rinternals.h>

SEXP addInt(SEXP a, SEXP b) {
	int aInt = INTEGER_VALUE(a);
	int bInt = INTEGER_VALUE(b);
	return ScalarInteger(aInt + bInt);
}

SEXP addDouble(SEXP a, SEXP b) {
	double aDouble = NUMERIC_VALUE(a);
	double bDouble = NUMERIC_VALUE(b);
	return ScalarReal(aDouble + bDouble);
}

SEXP populateIntVector(SEXP n) {
    SEXP v;
    int intN = INTEGER_VALUE(n);
    PROTECT(v = allocVector(INTSXP, intN));
    int i;
    for (i = 0; i < intN; i++) {
    	INTEGER(v)[i] = i;
    }
    UNPROTECT(1);
    return v;
}

SEXP createExternalPtr(SEXP addr, SEXP tag, SEXP prot) {
	return R_MakeExternalPtr((void *) (long) INTEGER_VALUE(addr), tag, prot);
}

SEXP getExternalPtrAddr(SEXP eptr) {
	return ScalarInteger((int) R_ExternalPtrAddr(eptr));
}

SEXP invoke_TYPEOF(SEXP x) {
	return ScalarInteger(TYPEOF(x));
}
