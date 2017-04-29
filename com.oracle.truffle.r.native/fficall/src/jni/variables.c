/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

#include <string.h>
#include <jni.h>
#include <Rinterface.h>
#include <rffiutils.h>

jmethodID R_GlobalEnvMethodID;
jmethodID R_BaseEnvMethodID;
jmethodID R_BaseNamespaceMethodID;
jmethodID R_NamespaceRegistryMethodID;
jmethodID R_InteractiveMethodID;
jmethodID R_GlobalContextMethodID;

// The variables have been rewritten as functions in Rinternals.h
// Apart from those that are RContext specific in FastR, the rest are
// stored here as JNI globals refs.

static SEXP R_EmptyEnv_static;
static SEXP R_Srcref_static;
static SEXP R_NilValue_static;
static SEXP R_UnboundValue_static;
static SEXP R_MissingArg_static;
static SEXP R_BaseSymbol_static;
static SEXP R_Bracket2Symbol_static;   /* "[[" */
static SEXP R_BracketSymbol_static;    /* "[" */
static SEXP R_BraceSymbol_static;      /* "{" */
static SEXP R_ClassSymbol_static;     /* "class" */
static SEXP R_DeviceSymbol_static;     /* ".Device" */
static SEXP R_DevicesSymbol_static;     /* ".Devices" */
static SEXP R_DimNamesSymbol_static;   /* "dimnames" */
static SEXP R_DimSymbol_static;     /* "dim" */
static SEXP R_DollarSymbol_static;     /* "$" */
static SEXP R_DotsSymbol_static;     /* "..." */
static SEXP R_DropSymbol_static;     /* "drop" */
static SEXP R_LastvalueSymbol_static;  /* ".Last.value" */
static SEXP R_LevelsSymbol_static;     /* "levels" */
static SEXP R_ModeSymbol_static;     /* "mode" */
static SEXP R_NameSymbol_static;     /* "name" */
static SEXP R_NamesSymbol_static;     /* "names" */
static SEXP R_NaRmSymbol_static;     /* "na.rm" */
static SEXP R_PackageSymbol_static;    /* "package" */
static SEXP R_QuoteSymbol_static;     /* "quote" */
static SEXP R_RowNamesSymbol_static;   /* "row.names" */
static SEXP R_SeedsSymbol_static;     /* ".Random.seed" */
static SEXP R_SourceSymbol_static;     /* "source" */
static SEXP R_TspSymbol_static;     /* "tsp" */
static SEXP R_dot_defined_static;      /* ".defined" */
static SEXP R_dot_Method_static;       /* ".Method" */
static SEXP R_dot_target_static;       /* ".target" */
static SEXP R_NaString_static;	    /* NA_STRING as a CHARSXP */
static SEXP R_BlankString_static;	    /* "" as a CHARSXP */
static SEXP R_BlankScalarString_static;	    /* "" as a STRSXP */
static SEXP	R_NamespaceEnvSymbol_static;   // ".__NAMESPACE__."

// Symbols not part of public API but used in FastR tools implementation
static SEXP R_SrcrefSymbol_static;
static SEXP R_SrcfileSymbol_static;
static SEXP R_RestartToken_static;

static const char *R_Home_static;
static const char *R_TempDir_static;

// Arith.h
double R_NaN;		/* IEEE NaN */
double R_PosInf;	/* IEEE Inf */
double R_NegInf;	/* IEEE -Inf */
double R_NaReal;	/* NA_REAL: IEEE */
int R_NaInt;	/* NA_INTEGER:= INT_MIN currently */

// various ignored flags and variables nevertheless needed to resolve symbols
Rboolean R_Visible;
Rboolean R_interrupts_suspended;
int R_interrupts_pending;
Rboolean mbcslocale;
Rboolean useaqua;
char* OutDec = ".";
Rboolean utf8locale = FALSE;
Rboolean mbcslocale = FALSE;
Rboolean latin1locale = FALSE;
int R_dec_min_exponent = -308;
int max_contour_segments = 25000;

// from sys-std.c
#include <R_ext/eventloop.h>

static InputHandler BasicInputHandler = {2, -1, NULL};
InputHandler *R_InputHandlers = &BasicInputHandler;

// R_GlobalEnv et al are not a variables in FASTR as they are RContext specific
SEXP FASTR_R_GlobalEnv() {
	JNIEnv *env = getEnv();
	return (*env)->CallObjectMethod(env, UpCallsRFFIObject, R_GlobalEnvMethodID);
}

SEXP FASTR_R_BaseEnv() {
	JNIEnv *env = getEnv();
	return (*env)->CallObjectMethod(env, UpCallsRFFIObject, R_BaseEnvMethodID);
}

SEXP FASTR_R_BaseNamespace() {
	JNIEnv *env = getEnv();
	return (*env)->CallObjectMethod(env, UpCallsRFFIObject, R_BaseNamespaceMethodID);
}

SEXP FASTR_R_NamespaceRegistry() {
	JNIEnv *env = getEnv();
	return (*env)->CallObjectMethod(env, UpCallsRFFIObject, R_NamespaceRegistryMethodID);
}

CTXT FASTR_GlobalContext() {
	JNIEnv *env = getEnv();
	CTXT res = (*env)->CallObjectMethod(env, UpCallsRFFIObject, R_GlobalContextMethodID);
    return addGlobalRef(env, res, 0);
}

char *FASTR_R_Home() {
	return (char *) R_Home_static;
}

char *FASTR_R_TempDir() {
	return (char *) R_TempDir_static;
}

Rboolean FASTR_R_Interactive() {
	JNIEnv *env = getEnv();
	int res = (int) (*env)->CallObjectMethod(env, UpCallsRFFIObject, R_InteractiveMethodID);
	return (Rboolean) res;
}

SEXP FASTR_R_EmptyEnv() {
    return R_EmptyEnv_static;
}

SEXP FASTR_R_Srcref() {
    return R_Srcref_static;
}

SEXP FASTR_R_NilValue() {
    return R_NilValue_static;
}

SEXP FASTR_R_UnboundValue() {
    return R_UnboundValue_static;
}

SEXP FASTR_R_MissingArg() {
    return R_MissingArg_static;
}

SEXP FASTR_R_BaseSymbol() {
    return R_BaseSymbol_static;
}


SEXP FASTR_R_BraceSymbol() {
    return R_BraceSymbol_static;
}

SEXP FASTR_R_Bracket2Symbol() {
    return R_Bracket2Symbol_static;
}

SEXP FASTR_R_BracketSymbol() {
    return R_BracketSymbol_static;
}

SEXP FASTR_R_ClassSymbol() {
    return R_ClassSymbol_static;
}

SEXP FASTR_R_DimNamesSymbol() {
    return R_DimNamesSymbol_static;
}

SEXP FASTR_R_DimSymbol() {
    return R_DimSymbol_static;
}


SEXP FASTR_R_DollarSymbol() {
    return R_DollarSymbol_static;
}

SEXP FASTR_R_DotsSymbol() {
    return R_DotsSymbol_static;
}


SEXP FASTR_R_DropSymbol() {
    return R_DropSymbol_static;
}

SEXP FASTR_R_LastvalueSymbol() {
    return R_LastvalueSymbol_static;
}


SEXP FASTR_R_LevelsSymbol() {
    return R_LevelsSymbol_static;
}

SEXP FASTR_R_ModeSymbol() {
    return R_ModeSymbol_static;
}

SEXP FASTR_R_NaRmSymbol() {
    return R_NaRmSymbol_static;
}


SEXP FASTR_R_NameSymbol() {
    return R_NameSymbol_static;
}

SEXP FASTR_R_NamesSymbol() {
    return R_NamesSymbol_static;
}


SEXP FASTR_R_NamespaceEnvSymbol() {
    return R_NamespaceEnvSymbol_static;
}

SEXP FASTR_R_PackageSymbol() {
    return R_PackageSymbol_static;
}

SEXP FASTR_R_QuoteSymbol() {
    return R_QuoteSymbol_static;
}

SEXP FASTR_R_RowNamesSymbol() {
    return R_RowNamesSymbol_static;
}

SEXP FASTR_R_SeedsSymbol() {
    return R_SeedsSymbol_static;
}

SEXP FASTR_R_SourceSymbol() {
    return R_SourceSymbol_static;
}

SEXP FASTR_R_TspSymbol() {
    return R_TspSymbol_static;
}

SEXP FASTR_R_dot_defined() {
    return R_dot_defined_static;
}

SEXP FASTR_R_dot_Method() {
    return R_dot_Method_static;
}

SEXP FASTR_R_dot_target() {
    return R_dot_target_static;
}

SEXP FASTR_R_NaString() {
    return R_NaString_static;
}


SEXP FASTR_R_BlankString() {
    return R_BlankString_static;
}

SEXP FASTR_R_BlankScalarString() {
    return R_BlankScalarString_static;
}

SEXP FASTR_R_DevicesSymbol() {
    return R_DevicesSymbol_static;
}

SEXP FASTR_R_DeviceSymbol() {
    return R_DeviceSymbol_static;
}

SEXP FASTR_R_SrcrefSymbol() {
    return R_SrcrefSymbol_static;
}

SEXP FASTR_R_SrcfileSymbol() {
    return R_SrcfileSymbol_static;
}

void init_variables(JNIEnv *env, jobjectArray initialValues) {
	// initialValues is an array of enums
	jclass enumClass = (*env)->GetObjectClass(env, (*env)->GetObjectArrayElement(env, initialValues, 0));
	jmethodID nameMethodID = checkGetMethodID(env, enumClass, "name", "()Ljava/lang/String;", 0);
	jmethodID ordinalMethodID = checkGetMethodID(env, enumClass, "ordinal", "()I", 0);
	jmethodID getValueMethodID = checkGetMethodID(env, enumClass, "getValue", "()Ljava/lang/Object;", 0);

	jclass doubleClass = checkFindClass(env, "java/lang/Double");
	jclass intClass = checkFindClass(env, "java/lang/Integer");
	jmethodID doubleValueMethodID = checkGetMethodID(env, doubleClass, "doubleValue", "()D", 0);
	jmethodID intValueMethodID = checkGetMethodID(env, intClass, "intValue", "()I", 0);

	R_GlobalEnvMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_GlobalEnv", "()Ljava/lang/Object;", 0);
	R_BaseEnvMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_BaseEnv", "()Ljava/lang/Object;", 0);
	R_BaseNamespaceMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_BaseNamespace", "()Ljava/lang/Object;", 0);
	R_NamespaceRegistryMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_NamespaceRegistry", "()Ljava/lang/Object;", 0);
	R_InteractiveMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_Interactive", "()I", 0);
	R_GlobalContextMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_GlobalContext", "()Ljava/lang/Object;", 0);

	int length = (*env)->GetArrayLength(env, initialValues);
	int index;
	for (index = 0; index < length; index++) {
		jobject variable = (*env)->GetObjectArrayElement(env, initialValues, index);
		jstring nameString = (*env)->CallObjectMethod(env, variable, nameMethodID);
		const char *nameChars = (*env)->GetStringUTFChars(env, nameString, NULL);
		jobject value = (*env)->CallObjectMethod(env, variable, getValueMethodID);
		if (value != NULL) {
			if (strcmp(nameChars, "R_Home") == 0) {
				R_Home_static = (*env)->GetStringUTFChars(env, value, NULL);
			} else if (strcmp(nameChars, "R_NaN") == 0) {
				R_NaN = (*env)->CallDoubleMethod(env, value, doubleValueMethodID);
			} else if (strcmp(nameChars, "R_PosInf") == 0) {
				R_PosInf = (*env)->CallDoubleMethod(env, value, doubleValueMethodID);
			} else if (strcmp(nameChars, "R_NegInf") == 0) {
				R_NegInf = (*env)->CallDoubleMethod(env, value, doubleValueMethodID);
			} else if (strcmp(nameChars, "R_NaReal") == 0) {
				R_NaReal = (*env)->CallDoubleMethod(env, value, doubleValueMethodID);
			} else if (strcmp(nameChars, "R_NaInt") == 0) {
				R_NaInt = (*env)->CallIntMethod(env, value, intValueMethodID);
			} else if (strcmp(nameChars, "R_TempDir") == 0) {
				R_TempDir_static = (*env)->GetStringUTFChars(env, value, NULL);
			} else {
				SEXP ref = createGlobalRef(env, value, 1);
				if (strcmp(nameChars, "R_EmptyEnv") == 0) {
					R_EmptyEnv_static = ref;
				} else if (strcmp(nameChars, "R_NilValue") == 0) {
					R_NilValue_static = ref;
				} else if (strcmp(nameChars, "R_UnboundValue") == 0) {
					R_UnboundValue_static = ref;
				} else if (strcmp(nameChars, "R_MissingArg") == 0) {
					R_MissingArg_static = ref;
				} else if (strcmp(nameChars, "R_BaseSymbol") == 0) {
					R_BaseSymbol_static = ref;
				} else if (strcmp(nameChars, "R_Bracket2Symbol") == 0) {
					R_Bracket2Symbol_static = ref;
				} else if (strcmp(nameChars, "R_BracketSymbol") == 0) {
					R_BracketSymbol_static = ref;
				} else if (strcmp(nameChars, "R_BraceSymbol") == 0) {
					R_BraceSymbol_static = ref;
				} else if (strcmp(nameChars, "R_ClassSymbol") == 0) {
					R_ClassSymbol_static = ref;
				} else if (strcmp(nameChars, "R_DeviceSymbol") == 0) {
					R_DeviceSymbol_static = ref;
				} else if (strcmp(nameChars, "R_DevicesSymbol") == 0) {
					R_DevicesSymbol_static = ref;
				} else if (strcmp(nameChars, "R_DimNamesSymbol") == 0) {
					R_DimNamesSymbol_static = ref;
				} else if (strcmp(nameChars, "R_DimSymbol") == 0) {
					R_DimSymbol_static = ref;
				} else if (strcmp(nameChars, "R_DollarSymbol") == 0) {
					R_DollarSymbol_static = ref;
				} else if (strcmp(nameChars, "R_DotsSymbol") == 0) {
					R_DotsSymbol_static = ref;
				} else if (strcmp(nameChars, "R_DropSymbol") == 0) {
					R_DropSymbol_static = ref;
				} else if (strcmp(nameChars, "R_LastvalueSymbol") == 0) {
					R_LastvalueSymbol_static = ref;
				} else if (strcmp(nameChars, "R_LevelsSymbol") == 0) {
					R_LevelsSymbol_static = ref;
				} else if (strcmp(nameChars, "R_ModeSymbol") == 0) {
					R_ModeSymbol_static = ref;
				} else if (strcmp(nameChars, "R_NameSymbol") == 0) {
					R_NameSymbol_static = ref;
				} else if (strcmp(nameChars, "R_NamesSymbol") == 0) {
					R_NamesSymbol_static = ref;
				} else if (strcmp(nameChars, "R_NaRmSymbol") == 0) {
					R_NaRmSymbol_static = ref;
				} else if (strcmp(nameChars, "R_PackageSymbol") == 0) {
					R_PackageSymbol_static = ref;
				} else if (strcmp(nameChars, "R_QuoteSymbol") == 0) {
					R_QuoteSymbol_static = ref;
				} else if (strcmp(nameChars, "R_RowNamesSymbol") == 0) {
					R_RowNamesSymbol_static = ref;
				} else if (strcmp(nameChars, "R_SeedsSymbol") == 0) {
					R_SeedsSymbol_static = ref;
				} else if (strcmp(nameChars, "R_SourceSymbol") == 0) {
					R_SourceSymbol_static = ref;
				} else if (strcmp(nameChars, "R_TspSymbol") == 0) {
					R_TspSymbol_static = ref;
				} else if (strcmp(nameChars, "R_dot_defined") == 0) {
					R_dot_defined_static = ref;
				} else if (strcmp(nameChars, "R_dot_Method") == 0) {
					R_dot_Method_static = ref;
				} else if (strcmp(nameChars, "R_dot_target") == 0) {
					R_dot_target_static = ref;
				} else if (strcmp(nameChars, "R_SrcfileSymbol") == 0) {
					R_SrcfileSymbol_static = ref;
				} else if (strcmp(nameChars, "R_SrcrefSymbol") == 0) {
					R_SrcrefSymbol_static = ref;
				} else if (strcmp(nameChars, "R_DimSymbol") == 0) {
					R_DimSymbol_static = ref;
				} else if (strcmp(nameChars, "R_DimNamesSymbol") == 0) {
					R_DimNamesSymbol_static = ref;
				} else if (strcmp(nameChars, "R_NaString") == 0) {
					R_NaString_static = ref;
				} else if (strcmp(nameChars, "R_BlankString") == 0) {
					R_BlankString_static = ref;
				} else if (strcmp(nameChars, "R_BlankScalarString") == 0) {
					R_BlankScalarString_static = ref;
				} else if (strcmp(nameChars, "R_NamespaceEnvSymbol") == 0) {
					R_NamespaceEnvSymbol_static = ref;
				} else {
					char msg[128];
					strcpy(msg, "non-null R variable not assigned: ");
					strcat(msg, nameChars);
					fatalError(msg);
				}
			}
		}
	}
}

