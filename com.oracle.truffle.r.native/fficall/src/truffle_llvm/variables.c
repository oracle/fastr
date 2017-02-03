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
#include <variables.h>

// Arith.h
double R_NaN;		/* IEEE NaN */
double R_PosInf;	/* IEEE Inf */
double R_NegInf;	/* IEEE -Inf */
double R_NaReal;	/* NA_REAL: IEEE */
int R_NaInt;	/* NA_INTEGER:= INT_MIN currently */

// R_GlobalEnv et al are not a variables in FASTR as they are RContext specific
SEXP FASTR_R_GlobalEnv() {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "R_GlobalEnv");
}

SEXP FASTR_R_BaseEnv() {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "R_BaseEnv");
}

SEXP FASTR_R_BaseNamespace() {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "R_BaseNamespace");
}

SEXP FASTR_R_NamespaceRegistry() {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "R_NamespaceRegistry");
}

Rboolean FASTR_R_Interactive() {
	IMPORT_CALLHELPER();
	return (Rboolean) truffle_invoke_i(obj, "R_Interactive");
}

char *FASTR_R_Home() {
	IMPORT_CALLHELPER();
	return (char *) truffle_invoke(obj, "R_Home");
}

// Callbacks because cannot store TruffleObjects in memory (currently)

SEXP FASTR_R_NilValue() {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "R_NilValue");

}

SEXP FASTR_R_UnboundValue() {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "R_UnboundValue");
}

SEXP FASTR_R_EmptyEnv() {
	IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_EmptyEnv");
}

SEXP FASTR_R_MissingArg() {
    IMPORT_CALLHELPER();
    return Truffle_Invoke(Obj, "R_MissingArg");
}

SEXP FASTR_R_BaseSymbol() {
    IMPORT_CALLHELPER();
    Return Truffle_Invoke(Obj, "R_BaseSymbol");
}

SEXP FASTR_R_BraceSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_BraceSymbol");
}

SEXP FASTR_R_Bracket2Symbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_Bracket2Symbol");
}

SEXP FASTR_R_BracketSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_BracketSymbol");
}

SEXP FASTR_R_ClassSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_ClassSymbol");
}

SEXP FASTR_R_DeviceSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_DeviceSymbol");
}

SEXP FASTR_R_DimNamesSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_DimNamesSymbol");
}

SEXP FASTR_R_DimSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_DimSymbol");
}

SEXP FASTR_R_DollarSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_DollarSymbol");
}

SEXP FASTR_R_DotsSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_DotsSymbol");
}

SEXP FASTR_R_DropSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_DropSymbol");
}

SEXP FASTR_R_LastvalueSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_LastvalueSymbol");
}

SEXP FASTR_R_LevelsSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_LevelsSymbol");
}

SEXP FASTR_R_ModeSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_ModeSymbol");
}

SEXP FASTR_R_NaRmSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_NaRmSymbol");
}

SEXP FASTR_R_NameSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_NameSymbol");
}

SEXP FASTR_R_NamesSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_NamesSymbol");
}

SEXP FASTR_R_NamespaceEnvSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_NamespaceEnvSymbol");
}

SEXP FASTR_R_PackageSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_PackageSymbol");
}

SEXP FASTR_R_QuoteSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_QuoteSymbol");
}

SEXP FASTR_R_RowNamesSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_RowNamesSymbol");
}

SEXP FASTR_R_SeedsSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_SeedsSymbol");
}

SEXP FASTR_R_SourceSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_SourceSymbol");
}

SEXP FASTR_R_TspSymbol() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_TspSymbol");
}

SEXP FASTR_R_dot_defined() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_dot_defined");
}

SEXP FASTR_R_dot_Method() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_dot_Method");
}

SEXP FASTR_R_dot_target() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_dot_target");
}

SEXP FASTR_R_NaString() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_NaString");
}

SEXP FASTR_R_BlankString() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_BlankString");
}

SEXP FASTR_R_BlankScalarString() {
    IMPORT_CALLHELPER();
    return truffle_invoke(obj, "R_BlankScalarString");
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


