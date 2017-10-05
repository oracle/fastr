/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
extern void dotCModifiedArguments(int* len, int* idata, double* rdata, int* ldata, char** cdata);

extern SEXP addInt(SEXP a, SEXP b);

extern SEXP addDouble(SEXP a, SEXP b);

extern SEXP populateIntVector(SEXP n);

extern SEXP populateLogicalVector(SEXP n);

extern SEXP createExternalPtr(SEXP addr, SEXP tag, SEXP prot);

extern SEXP getExternalPtrAddr(SEXP eptr);

extern SEXP invoke_TYPEOF(SEXP x);

extern SEXP invoke_error(SEXP msg);

extern SEXP dot_external_access_args(SEXP args);

extern SEXP invoke_isString(SEXP s);

extern SEXP invoke12(SEXP a1, SEXP a2, SEXP a3, SEXP a4, SEXP a5, SEXP a6, SEXP a7, SEXP a8, SEXP a9, SEXP a10, SEXP a11, SEXP a12);

extern SEXP interactive(void);

extern SEXP tryEval(SEXP expr, SEXP env);

extern SEXP rHomeDir();

extern SEXP nestedCall1(SEXP upcall, SEXP env);

extern SEXP nestedCall2(SEXP v);

extern SEXP r_home(void);

extern SEXP mkStringFromChar(void);

extern SEXP mkStringFromBytes(void);

extern SEXP null(void);

extern SEXP iterate_iarray(SEXP x);

extern SEXP iterate_iptr(SEXP x);

extern SEXP preserve_object(SEXP val);

extern SEXP release_object(SEXP x);

extern SEXP findvar(SEXP x, SEXP env);

extern SEXP test_asReal(SEXP x);

extern SEXP test_asChar(SEXP x);

extern SEXP test_asInteger(SEXP x);

extern SEXP test_asLogical(SEXP x);

extern SEXP test_CAR(SEXP x);

extern SEXP test_CDR(SEXP x);

extern SEXP test_LENGTH(SEXP x);

extern SEXP test_inlined_length(SEXP x);

extern SEXP test_coerceVector(SEXP x, SEXP mode);

extern SEXP test_ATTRIB(SEXP);

extern SEXP test_stringNA(void);

extern SEXP test_captureDotsWithSingleElement(SEXP env);

extern SEXP test_evalAndNativeArrays(SEXP vec, SEXP expr, SEXP env);

extern SEXP test_writeConnection(SEXP conn);

extern SEXP test_readConnection(SEXP conn);

extern SEXP test_createNativeConnection(void);

extern SEXP test_ParseVector(SEXP src);
