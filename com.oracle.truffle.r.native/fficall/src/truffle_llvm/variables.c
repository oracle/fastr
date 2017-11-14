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
#include <rffiutils.h>
#include "../common/rffi_variablesindex.h"


// TODO: this file can likely simply be removed, using the NFI version instead


// Arith.h
double R_NaN;		/* IEEE NaN */
double R_PosInf;	/* IEEE Inf */
double R_NegInf;	/* IEEE -Inf */
double R_NaReal;	/* NA_REAL: IEEE */
int R_NaInt;	/* NA_INTEGER:= INT_MIN currently */

static void **variables = NULL;

SEXP FASTR_R_NilValue() {
	return (SEXP) variables[R_NilValue_x];
}

SEXP FASTR_R_UnboundValue() {
	return variables[R_UnboundValue_x];
}

SEXP FASTR_R_EmptyEnv() {
    return variables[R_EmptyEnv_x];
}

SEXP FASTR_R_MissingArg() {
    return variables[R_MissingArg_x];
}

SEXP FASTR_R_BaseSymbol() {
    return variables[R_BaseSymbol_x];
}

SEXP FASTR_R_BraceSymbol() {
    return variables[R_BraceSymbol_x];
}

SEXP FASTR_R_Bracket2Symbol() {
    return variables[R_Bracket2Symbol_x];
}

SEXP FASTR_R_BracketSymbol() {
    return variables[R_BracketSymbol_x];
}

SEXP FASTR_R_ClassSymbol() {
    return variables[R_ClassSymbol_x];
}

SEXP FASTR_R_DeviceSymbol() {
    return variables[R_DeviceSymbol_x];
}

SEXP FASTR_R_DevicesSymbol() {
    return variables[R_DevicesSymbol_x];
}

SEXP FASTR_R_DimNamesSymbol() {
    return variables[R_DimNamesSymbol_x];
}

SEXP FASTR_R_DimSymbol() {
    return variables[R_DimSymbol_x];
}

SEXP FASTR_R_DollarSymbol() {
    return variables[R_DollarSymbol_x];
}

SEXP FASTR_R_DotsSymbol() {
    return variables[R_DotsSymbol_x];
}

SEXP FASTR_R_DropSymbol() {
    return variables[R_DropSymbol_x];
}

SEXP FASTR_R_LastvalueSymbol() {
    return variables[R_LastvalueSymbol_x];
}

SEXP FASTR_R_LevelsSymbol() {
    return variables[R_LevelsSymbol_x];
}

SEXP FASTR_R_ModeSymbol() {
    return variables[R_ModeSymbol_x];
}

SEXP FASTR_R_NaRmSymbol() {
    return variables[R_NaRmSymbol_x];
}

SEXP FASTR_R_NameSymbol() {
    return variables[R_NameSymbol_x];
}

SEXP FASTR_R_NamesSymbol() {
    return variables[R_NamesSymbol_x];
}

SEXP FASTR_R_NamespaceEnvSymbol() {
    return variables[R_NamespaceEnvSymbol_x];
}

SEXP FASTR_R_PackageSymbol() {
    return variables[R_PackageSymbol_x];
}

SEXP FASTR_R_QuoteSymbol() {
    return variables[R_QuoteSymbol_x];
}

SEXP FASTR_R_RowNamesSymbol() {
    return variables[R_RowNamesSymbol_x];
}

SEXP FASTR_R_SeedsSymbol() {
    return variables[R_SeedsSymbol_x];
}

SEXP FASTR_R_SourceSymbol() {
    return variables[R_SourceSymbol_x];
}

SEXP FASTR_R_TspSymbol() {
    return variables[R_TspSymbol_x];
}

SEXP FASTR_R_dot_defined() {
    return variables[R_dot_defined_x];
}

SEXP FASTR_R_dot_Method() {
    return variables[R_dot_Method_x];
}

SEXP FASTR_R_dot_target() {
    return variables[R_dot_target_x];
}

SEXP FASTR_R_NaString() {
    return variables[R_NaString_x];
}

SEXP FASTR_R_BlankString() {
    return variables[R_BlankString_x];
}

SEXP FASTR_R_BlankScalarString() {
    return variables[R_BlankScalarString_x];
}

SEXP FASTR_R_SrcrefSymbol() {
    return variables[R_SrcrefSymbol_x];
}

SEXP FASTR_R_SrcfileSymbol() {
    return variables[R_SrcfileSymbol_x];
}
void Call_initvar_double(int index, double value) {
	switch (index) {
    case R_NaN_x: R_NaN = value; break;
	}
}

void Call_initvar_int(int index, int value) {
	switch (index) {
    case R_NaInt_x: R_NaInt = value; break;
    case R_PosInf_x: R_PosInf = value; break;
    case R_NegInf_x: R_NegInf = value; break;
    case R_NaReal_x: R_NaReal = value; break;
	}
}

void Call_initvar_obj(int index, void *value) {
	if (variables == NULL) {
		variables = truffle_managed_malloc(VARIABLES_TABLE_SIZE * sizeof(void*));
	}
	variables[index] = value;
}


