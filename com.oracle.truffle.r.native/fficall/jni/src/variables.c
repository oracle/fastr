/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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

// Rinternals defines lots of extern variables that we set here (eventually)
// Everything here must be a JNI Global Reference, and must be canonical because
// C code compares then with "==" (a JNI no-no really).

#include <jni.h>
#include <Rinternals.h>
#include "rffiutils.h"

static int R_NilValue_Index = 0;
static int R_UnboundValue_Index = 1;
static int R_MissingArg_Index = 2;
static int R_GlobalEnv_Index = 3;
static int R_EmptyEnv_Index = 4;
static int R_BaseEnv_Index = 5;
static int R_BaseNamespace_Index = 6;
static int R_NamespaceRegistry_Index = 7;
static int R_Srcref_Index = 8;
static int R_Bracket2Symbol_Index = 8;
static int R_BracketSymbol_Index = 10;
static int R_BraceSymbol_Index = 11;
static int R_ClassSymbol_Index = 12;
static int R_DeviceSymbol_Index = 12;
static int R_DimNamesSymbol_Index = 14;
static int R_DimSymbol_Index = 15;
static int R_DollarSymbol_Index = 16;
static int R_DotsSymbol_Index = 17;
static int R_DropSymbol_Index = 18;
static int R_LastvalueSymbol_Index = 19;
static int R_LevelsSymbol_Index = 20;
static int R_ModeSymbol_Index = 21;
static int R_NameSymbol_Index = 22;
static int R_NamesSymbol_Index = 23;
static int R_NaRmSymbol_Index = 24;
static int R_PackageSymbol_Index = 25;
static int R_QuoteSymbol_Index = 26;
static int R_RowNamesSymbol_Index = 27;
static int R_SeedsSymbol_Index = 28;
static int R_SourceSymbol_Index = 29;
static int R_TspSymbol_Index = 30;
static int R_dot_defined_Index = 31;
static int R_dot_Method_Index = 32;
static int R_dot_target_Index = 33;
static int R_SrcrefSymbol_Index = 34;
static int R_SrcfileSymbol_Index = 35;

/* Evaluation Environment */
//SEXP R_GlobalEnv;
SEXP R_EmptyEnv;
//SEXP R_BaseEnv;
//SEXP R_BaseNamespace;
//SEXP R_NamespaceRegistry;

//SEXP R_Srcref;

/* Special Values */
SEXP R_NilValue;
SEXP R_UnboundValue;
SEXP R_MissingArg;

/* Symbol Table Shortcuts */
//SEXP R_Bracket2Symbol;   /* "[[" */
//SEXP R_BracketSymbol;    /* "[" */
//SEXP R_BraceSymbol;      /* "{" */
SEXP R_ClassSymbol;     /* "class" */
//SEXP R_DeviceSymbol;     /* ".Device" */
//SEXP R_DimNamesSymbol;   /* "dimnames" */
//SEXP R_DimSymbol;     /* "dim" */
//SEXP R_DollarSymbol;     /* "$" */
//SEXP R_DotsSymbol;     /* "..." */
//SEXP R_DropSymbol;     /* "drop" */
//SEXP R_LastvalueSymbol;  /* ".Last.value" */
//SEXP R_LevelsSymbol;     /* "levels" */
//SEXP R_ModeSymbol;     /* "mode" */
//SEXP R_NameSymbol;     /* "name" */
//SEXP R_NamesSymbol;     /* "names" */
//SEXP R_NaRmSymbol;     /* "na.rm" */
//SEXP R_PackageSymbol;    /* "package" */
//SEXP R_QuoteSymbol;     /* "quote" */
//SEXP R_RowNamesSymbol;   /* "row.names" */
//SEXP R_SeedsSymbol;     /* ".Random.seed" */
//SEXP R_SourceSymbol;     /* "source" */
//SEXP R_TspSymbol;     /* "tsp" */

//SEXP  R_dot_defined;      /* ".defined" */
//SEXP  R_dot_Method;       /* ".Method" */
//SEXP  R_dot_target;       /* ".target" */

// Symbols not part of public API but used in FastR tools implementation
SEXP R_SrcrefSymbol;
SEXP R_SrcfileSymbol;

void init_variables(JNIEnv *env, jobjectArray initialValues) {
	R_EmptyEnv = mkNamedGlobalRef(env, R_EmptyEnv_Index, (*env)->GetObjectArrayElement(env, initialValues, 0));
    R_NilValue = mkNamedGlobalRef(env, R_NilValue_Index, (*env)->GetObjectArrayElement(env, initialValues, 1));
    R_UnboundValue = mkNamedGlobalRef(env, R_UnboundValue_Index, (*env)->GetObjectArrayElement(env, initialValues, 2));
    R_MissingArg = mkNamedGlobalRef(env, R_MissingArg_Index, (*env)->GetObjectArrayElement(env, initialValues, 3));
    R_ClassSymbol = mkNamedGlobalRef(env, R_ClassSymbol_Index, (*env)->GetObjectArrayElement(env, initialValues, 4));
	jstring name = (*env)->NewStringUTF(env, "srcfile");
	R_SrcfileSymbol = mkNamedGlobalRef(env, R_SrcfileSymbol_Index, (*env)->CallStaticObjectMethod(env, CallRFFIHelperClass, createSymbolMethodID, name));
	name = (*env)->NewStringUTF(env, "srcref");
	R_SrcrefSymbol = mkNamedGlobalRef(env, R_SrcrefSymbol_Index, (*env)->CallStaticObjectMethod(env, CallRFFIHelperClass, createSymbolMethodID, name));
}

