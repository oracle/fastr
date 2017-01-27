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
#include <variable_defs.h>

// R_GlobalEnv et al are not a variables in FASTR as they are RContext specific
SEXP FASTR_GlobalEnv() {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "R_GlobalEnv");
}

SEXP FASTR_BaseEnv() {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "R_BaseEnv");
}

SEXP FASTR_BaseNamespace() {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "R_BaseNamespace");
}

SEXP FASTR_NamespaceRegistry() {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "R_NamespaceRegistry");
}

Rboolean FASTR_IsInteractive() {
	IMPORT_CALLHELPER();
	return (Rboolean) truffle_invoke_i(obj, "isInteractive");
}

char *FASTR_R_Home() {
	IMPORT_CALLHELPER();
	return (char *) truffle_invoke(obj, "R_Home");
}

SEXP FASTR_R_NilValue() {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "R_NilValue");

}

SEXP FASTR_R_UnboundValue() {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "R_UnboundValue");
}

void Call_initvar_obj(int index, void* value) {
	/*
	switch (index) {
    case R_Home_x: R_Home = (char *) value; break;
    case R_TempDir_x: R_TempDir = value; break;
    case R_NilValue_x: R_NilValue = value; break;
    case R_UnboundValue_x: R_UnboundValue = value; break;
    case R_MissingArg_x: R_MissingArg = value; break;
    case R_Srcref_x: R_Srcref = value; break;
    case R_Bracket2Symbol_x: R_Bracket2Symbol = value; break;
    case R_BracketSymbol_x: R_BracketSymbol = value; break;
    case R_BraceSymbol_x: R_BraceSymbol = value; break;
    case R_ClassSymbol_x: R_ClassSymbol = value; break;
    case R_DeviceSymbol_x: R_DeviceSymbol = value; break;
    case R_DevicesSymbol_x: R_DevicesSymbol = value; break;
    case R_DimNamesSymbol_x: R_DimNamesSymbol = value; break;
    case R_DimSymbol_x: R_DimSymbol = value; break;
    case R_DollarSymbol_x: R_DollarSymbol = value; break;
    case R_DotsSymbol_x: R_DotsSymbol = value; break;
    case R_DropSymbol_x: R_DropSymbol = value; break;
    case R_LastvalueSymbol_x: R_LastvalueSymbol = value; break;
    case R_LevelsSymbol_x: R_LevelsSymbol = value; break;
    case R_ModeSymbol_x: R_ModeSymbol = value; break;
    case R_NameSymbol_x: R_NameSymbol = value; break;
    case R_NamesSymbol_x: R_NamesSymbol = value; break;
    case R_NaRmSymbol_x: R_NaRmSymbol = value; break;
    case R_PackageSymbol_x: R_PackageSymbol = value; break;
    case R_QuoteSymbol_x: R_QuoteSymbol = value; break;
    case R_RowNamesSymbol_x: R_RowNamesSymbol = value; break;
    case R_SeedsSymbol_x: R_SeedsSymbol = value; break;
    case R_SourceSymbol_x: R_SourceSymbol = value; break;
    case R_TspSymbol_x: R_TspSymbol = value; break;
    case R_dot_defined_x: R_dot_defined = value; break;
    case R_dot_Method_x: R_dot_Method = value; break;
    case R_dot_target_x: R_dot_target = value; break;
    case R_SrcrefSymbol_x: R_SrcrefSymbol = value; break;
    case R_SrcfileSymbol_x: R_SrcfileSymbol = value; break;
    case R_NaString_x: R_NaString = value; break;
    case R_NaInt_x: R_NaInt = (int) value; break;
    case R_BlankString_x: R_BlankString = value; break;
    case R_TrueValue_x: R_TrueValue = value; break;
    case R_FalseValue_x: R_FalseValue = value; break;
    case R_LogicalNAValue_x: R_LogicalNAValue = value; break;
	}
	*/
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


