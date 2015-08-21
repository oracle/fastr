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

#include <string.h>
#include <jni.h>
#include <Rinternals.h>
#include "rffiutils.h"


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
SEXP R_DimNamesSymbol;   /* "dimnames" */
SEXP R_DimSymbol;     /* "dim" */
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
SEXP	R_NaString;	    /* NA_STRING as a CHARSXP */
SEXP	R_BlankString;	    /* "" as a CHARSXP */

// Symbols not part of public API but used in FastR tools implementation
SEXP R_SrcrefSymbol;
SEXP R_SrcfileSymbol;

// Arith.h
double R_NaN;		/* IEEE NaN */
double R_PosInf;	/* IEEE Inf */
double R_NegInf;	/* IEEE -Inf */
double R_NaReal;	/* NA_REAL: IEEE */
int	 R_NaInt;	/* NA_INTEGER:= INT_MIN currently */

jmethodID getGlobalEnvMethodID;
jmethodID getBaseEnvMethodID;
jmethodID getBaseNamespaceMethodID;
jmethodID getNamespaceRegistryMethodID;
jmethodID isInteractiveMethodID;

// R_GlobalEnv et al are not a variables in FASTR as they are RContext specific
SEXP FASTR_GlobalEnv() {
	JNIEnv *env = getEnv();
	(*env)->CallStaticObjectMethod(env, CallRFFIHelperClass, getGlobalEnvMethodID);
}

SEXP FASTR_BaseEnv() {
	JNIEnv *env = getEnv();
	(*env)->CallStaticObjectMethod(env, CallRFFIHelperClass, getBaseEnvMethodID);
}

SEXP FASTR_BaseNamespace() {
	JNIEnv *env = getEnv();
	(*env)->CallStaticObjectMethod(env, CallRFFIHelperClass, getBaseNamespaceMethodID);
}

SEXP FASTR_NamespaceRegistry() {
	JNIEnv *env = getEnv();
	(*env)->CallStaticObjectMethod(env, CallRFFIHelperClass, getNamespaceRegistryMethodID);
}

Rboolean FASTR_IsInteractive() {
	JNIEnv *env = getEnv();
	int r = (*env)->CallStaticObjectMethod(env, CallRFFIHelperClass, isInteractiveMethodID);
	return r;
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

	getGlobalEnvMethodID = checkGetMethodID(env, CallRFFIHelperClass, "getGlobalEnv", "()Ljava/lang/Object;", 1);
	getBaseEnvMethodID = checkGetMethodID(env, CallRFFIHelperClass, "getBaseEnv", "()Ljava/lang/Object;", 1);
	getBaseNamespaceMethodID = checkGetMethodID(env, CallRFFIHelperClass, "getBaseNamespace", "()Ljava/lang/Object;", 1);
	getNamespaceRegistryMethodID = checkGetMethodID(env, CallRFFIHelperClass, "getNamespaceRegistry", "()Ljava/lang/Object;", 1);
	isInteractiveMethodID = checkGetMethodID(env, CallRFFIHelperClass, "isInteractive", "()I", 1);

	int length = (*env)->GetArrayLength(env, initialValues);
	int index;
	for (index = 0; index < length; index++) {
		jobject variable = (*env)->GetObjectArrayElement(env, initialValues, index);
		jstring nameString = (*env)->CallObjectMethod(env, variable, nameMethodID);
		const char *nameChars = (*env)->GetStringUTFChars(env, nameString, NULL);
		jobject value = (*env)->CallObjectMethod(env, variable, getValueMethodID);
		if (value != NULL) {
			if (strcmp(nameChars, "R_NaN") == 0) {
				R_NaN = (*env)->CallDoubleMethod(env, value, doubleValueMethodID);
			} else if (strcmp(nameChars, "R_PosInf") == 0) {
				R_PosInf = (*env)->CallDoubleMethod(env, value, doubleValueMethodID);
			} else if (strcmp(nameChars, "R_NegInf") == 0) {
				R_NegInf = (*env)->CallDoubleMethod(env, value, doubleValueMethodID);
			} else if (strcmp(nameChars, "R_NaReal") == 0) {
				R_NaReal = (*env)->CallDoubleMethod(env, value, doubleValueMethodID);
			} else if (strcmp(nameChars, "R_NaInt") == 0) {
				R_NaInt = (*env)->CallIntMethod(env, value, doubleValueMethodID);
			} else {
				SEXP ref = mkNamedGlobalRef(env, index, value);
				if (strcmp(nameChars, "R_EmptyEnv") == 0) {
					R_EmptyEnv = ref;
				} else if (strcmp(nameChars, "R_NilValue") == 0) {
					R_NilValue = ref;
				} else if (strcmp(nameChars, "R_UnboundValue") == 0) {
					R_UnboundValue = ref;
				} else if (strcmp(nameChars, "R_MissingArg") == 0) {
					R_MissingArg = ref;
				} else if (strcmp(nameChars, "R_ClassSymbol") == 0) {
					R_ClassSymbol = ref;
				} else if (strcmp(nameChars, "R_SrcfileSymbol") == 0) {
					R_SrcfileSymbol = ref;
				} else if (strcmp(nameChars, "R_SrcrefSymbol") == 0) {
					R_SrcrefSymbol = ref;
				} else if (strcmp(nameChars, "R_DimSymbol") == 0) {
					R_DimSymbol = ref;
				} else if (strcmp(nameChars, "R_DimNamesSymbol") == 0) {
					R_DimNamesSymbol = ref;
				} else if (strcmp(nameChars, "R_NaString") == 0) {
					R_NaString = ref;
				} else if (strcmp(nameChars, "R_BlankString") == 0) {
					R_BlankString = ref;
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

