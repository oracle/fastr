/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
#include <variable_defs.h>

jmethodID getGlobalEnvMethodID;
jmethodID getBaseEnvMethodID;
jmethodID getBaseNamespaceMethodID;
jmethodID getNamespaceRegistryMethodID;
jmethodID isInteractiveMethodID;
jmethodID getGlobalContextMethodID;

// R_GlobalEnv et al are not a variables in FASTR as they are RContext specific
SEXP FASTR_GlobalEnv() {
	JNIEnv *env = getEnv();
	return (*env)->CallStaticObjectMethod(env, CallRFFIHelperClass, getGlobalEnvMethodID);
}

SEXP FASTR_BaseEnv() {
	JNIEnv *env = getEnv();
	return (*env)->CallStaticObjectMethod(env, CallRFFIHelperClass, getBaseEnvMethodID);
}

SEXP FASTR_BaseNamespace() {
	JNIEnv *env = getEnv();
	return (*env)->CallStaticObjectMethod(env, CallRFFIHelperClass, getBaseNamespaceMethodID);
}

SEXP FASTR_NamespaceRegistry() {
	JNIEnv *env = getEnv();
	return (*env)->CallStaticObjectMethod(env, CallRFFIHelperClass, getNamespaceRegistryMethodID);
}

CTXT FASTR_GlobalContext() {
	JNIEnv *env = getEnv();
	return (*env)->CallStaticObjectMethod(env, CallRFFIHelperClass, getGlobalContextMethodID);
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
	getGlobalContextMethodID = checkGetMethodID(env, CallRFFIHelperClass, "getGlobalContext", "()Ljava/lang/Object;", 1);

	int length = (*env)->GetArrayLength(env, initialValues);
	int index;
	for (index = 0; index < length; index++) {
		jobject variable = (*env)->GetObjectArrayElement(env, initialValues, index);
		jstring nameString = (*env)->CallObjectMethod(env, variable, nameMethodID);
		const char *nameChars = (*env)->GetStringUTFChars(env, nameString, NULL);
		jobject value = (*env)->CallObjectMethod(env, variable, getValueMethodID);
		if (value != NULL) {
			if (strcmp(nameChars, "R_Home") == 0) {
				R_Home = (*env)->GetStringUTFChars(env, value, NULL);
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
			} else {
				SEXP ref = createGlobalRef(env, value, 1);
				if (strcmp(nameChars, "R_EmptyEnv") == 0) {
					R_EmptyEnv = ref;
				} else if (strcmp(nameChars, "R_NilValue") == 0) {
					R_NilValue = ref;
				} else if (strcmp(nameChars, "R_UnboundValue") == 0) {
					R_UnboundValue = ref;
				} else if (strcmp(nameChars, "R_MissingArg") == 0) {
					R_MissingArg = ref;
				} else if (strcmp(nameChars, "R_Bracket2Symbol") == 0) {
					R_Bracket2Symbol = ref;
				} else if (strcmp(nameChars, "R_BracketSymbol") == 0) {
					R_BracketSymbol = ref;
				} else if (strcmp(nameChars, "R_BraceSymbol") == 0) {
					R_BraceSymbol = ref;
				} else if (strcmp(nameChars, "R_ClassSymbol") == 0) {
					R_ClassSymbol = ref;
				} else if (strcmp(nameChars, "R_DeviceSymbol") == 0) {
					R_DeviceSymbol = ref;
				} else if (strcmp(nameChars, "R_DevicesSymbol") == 0) {
					R_DevicesSymbol = ref;
				} else if (strcmp(nameChars, "R_DimNamesSymbol") == 0) {
					R_DimNamesSymbol = ref;
				} else if (strcmp(nameChars, "R_DimSymbol") == 0) {
					R_DimSymbol = ref;
				} else if (strcmp(nameChars, "R_DollarSymbol") == 0) {
					R_DollarSymbol = ref;
				} else if (strcmp(nameChars, "R_DotsSymbol") == 0) {
					R_DotsSymbol = ref;
				} else if (strcmp(nameChars, "R_DropSymbol") == 0) {
					R_DropSymbol = ref;
				} else if (strcmp(nameChars, "R_LastvalueSymbol") == 0) {
					R_LastvalueSymbol = ref;
				} else if (strcmp(nameChars, "R_LevelsSymbol") == 0) {
					R_LevelsSymbol = ref;
				} else if (strcmp(nameChars, "R_ModeSymbol") == 0) {
					R_ModeSymbol = ref;
				} else if (strcmp(nameChars, "R_NameSymbol") == 0) {
					R_NameSymbol = ref;
				} else if (strcmp(nameChars, "R_NamesSymbol") == 0) {
					R_NamesSymbol = ref;
				} else if (strcmp(nameChars, "R_NaRmSymbol") == 0) {
					R_NaRmSymbol = ref;
				} else if (strcmp(nameChars, "R_PackageSymbol") == 0) {
					R_PackageSymbol = ref;
				} else if (strcmp(nameChars, "R_QuoteSymbol") == 0) {
					R_QuoteSymbol = ref;
				} else if (strcmp(nameChars, "R_RowNamesSymbol") == 0) {
					R_RowNamesSymbol = ref;
				} else if (strcmp(nameChars, "R_SeedsSymbol") == 0) {
					R_SeedsSymbol = ref;
				} else if (strcmp(nameChars, "R_SourceSymbol") == 0) {
					R_SourceSymbol = ref;
				} else if (strcmp(nameChars, "R_TspSymbol") == 0) {
					R_TspSymbol = ref;
				} else if (strcmp(nameChars, "R_dot_defined") == 0) {
					R_dot_defined = ref;
				} else if (strcmp(nameChars, "R_dot_Method") == 0) {
					R_dot_Method = ref;
				} else if (strcmp(nameChars, "R_dot_target") == 0) {
					R_dot_target = ref;
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
				} else if (strcmp(nameChars, "R_TrueValue") == 0) {
				    R_TrueValue = ref;
				} else if (strcmp(nameChars, "R_FalseValue") == 0) {
				    R_FalseValue = ref;
				} else if (strcmp(nameChars, "R_LogicalNAValue") == 0) {
				    R_LogicalNAValue = ref;
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

void setTempDir(JNIEnv *env, jstring tempDir) {
	R_TempDir = (*env)->GetStringUTFChars(env, tempDir, NULL);
}

