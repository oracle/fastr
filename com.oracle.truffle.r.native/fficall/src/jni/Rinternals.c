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
#include <rffiutils.h>
#include <string.h>

// Most everything in RInternals.h

static jmethodID Rf_ScalarIntegerMethodID;
static jmethodID Rf_ScalarDoubleMethodID;
static jmethodID Rf_ScalarStringMethodID;
static jmethodID Rf_ScalarLogicalMethodID;
static jmethodID Rf_allocateVectorMethodID;
static jmethodID Rf_allocateArrayMethodID;
static jmethodID Rf_allocateMatrixMethodID;
static jmethodID Rf_duplicateMethodID;
static jmethodID Rf_anyDuplicatedMethodID;
static jmethodID Rf_consMethodID;
static jmethodID Rf_evalMethodID;
static jmethodID Rf_findfunMethodID;
static jmethodID Rf_defineVarMethodID;
static jmethodID Rf_findVarMethodID;
static jmethodID Rf_findVarInFrameMethodID;
static jmethodID Rf_getAttribMethodID;
static jmethodID Rf_setAttribMethodID;
static jmethodID Rf_isStringMethodID;
static jmethodID Rf_isNullMethodID;
static jmethodID Rf_warningcallMethodID;
static jmethodID Rf_warningMethodID;
static jmethodID Rf_errorMethodID;
static jmethodID Rf_NewHashedEnvMethodID;
static jmethodID Rf_rPsortMethodID;
static jmethodID Rf_iPsortMethodID;
static jmethodID RprintfMethodID;
static jmethodID R_FindNamespaceMethodID;
static jmethodID Rf_GetOption1MethodID;
static jmethodID Rf_gsetVarMethodID;
static jmethodID Rf_inheritsMethodID;
static jmethodID Rf_lengthgetsMethodID;
static jmethodID CADR_MethodID;
static jmethodID TAG_MethodID;
static jmethodID PRINTNAME_MethodID;
static jmethodID CAR_MethodID;
static jmethodID CDR_MethodID;
static jmethodID SET_TAG_MethodID;
static jmethodID SETCAR_MethodID;
static jmethodID SETCDR_MethodID;
static jmethodID SET_STRING_ELT_MethodID;
static jmethodID SET_VECTOR_ELT_MethodID;
static jmethodID RAW_MethodID;
static jmethodID INTEGER_MethodID;
static jmethodID REAL_MethodID;
static jmethodID LOGICAL_MethodID;
static jmethodID STRING_ELT_MethodID;
static jmethodID VECTOR_ELT_MethodID;
static jmethodID LENGTH_MethodID;
static jmethodID Rf_asIntegerMethodID;
//static jmethodID Rf_asRealMethodID;
static jmethodID Rf_asCharMethodID;
static jmethodID Rf_mkCharLenCEMethodID;
static jmethodID Rf_asLogicalMethodID;
static jmethodID Rf_PairToVectorListMethodID;
static jmethodID gnuRCodeForObjectMethodID;
static jmethodID NAMED_MethodID;
static jmethodID SET_TYPEOF_FASTR_MethodID;
static jmethodID TYPEOF_MethodID;
static jmethodID OBJECT_MethodID;
static jmethodID DUPLICATE_ATTRIB_MethodID;
static jmethodID isS4ObjectMethodID;
static jmethodID logObject_MethodID;

static jclass RExternalPtrClass;
static jmethodID createExternalPtrMethodID;
static jmethodID externalPtrGetAddrMethodID;
static jmethodID externalPtrGetTagMethodID;
static jmethodID externalPtrGetProtMethodID;
static jmethodID externalPtrSetAddrMethodID;
static jmethodID externalPtrSetTagMethodID;
static jmethodID externalPtrSetProtMethodID;

static jmethodID R_computeIdenticalMethodID;
static jmethodID Rf_copyListMatrixMethodID;
static jmethodID Rf_copyMatrixMethodID;

static jclass CharSXPWrapperClass;
static jfieldID CharXSPWrapperContentsFieldID;

void init_internals(JNIEnv *env) {
	Rf_ScalarIntegerMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_ScalarInteger", "(I)Lcom/oracle/truffle/r/runtime/data/RIntVector;", 1);
	Rf_ScalarDoubleMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_ScalarDouble", "(D)Lcom/oracle/truffle/r/runtime/data/RDoubleVector;", 1);
	Rf_ScalarStringMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_ScalarString", "(Ljava/lang/Object;)Lcom/oracle/truffle/r/runtime/data/RStringVector;", 1);
	Rf_ScalarLogicalMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_ScalarLogical", "(I)Lcom/oracle/truffle/r/runtime/data/RLogicalVector;", 1);
	Rf_consMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_cons", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_evalMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_eval", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_findfunMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_findfun", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_defineVarMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_defineVar", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", 1);
	Rf_findVarMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_findVar", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_findVarInFrameMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_findVarInFrame", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_getAttribMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_getAttrib", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_setAttribMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_setAttrib", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", 1);
	Rf_isStringMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_isString", "(Ljava/lang/Object;)I", 1);
	Rf_isNullMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_isNull", "(Ljava/lang/Object;)I", 1);
	Rf_warningMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_warning", "(Ljava/lang/String;)V", 1);
	Rf_warningcallMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_warningcall", "(Ljava/lang/Object;Ljava/lang/String;)V", 1);
	Rf_errorMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_error", "(Ljava/lang/String;)V", 1);
	Rf_allocateVectorMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_allocateVector", "(II)Ljava/lang/Object;", 1);
	Rf_allocateMatrixMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_allocateMatrix", "(III)Ljava/lang/Object;", 1);
	Rf_allocateArrayMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_allocateArray", "(ILjava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_duplicateMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_duplicate", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_anyDuplicatedMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_anyDuplicated", "(Ljava/lang/Object;I)I", 1);
	Rf_NewHashedEnvMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_createNewEnv", "(Lcom/oracle/truffle/r/runtime/env/REnvironment;Ljava/lang/String;ZI)Lcom/oracle/truffle/r/runtime/env/REnvironment;", 1);
	RprintfMethodID = checkGetMethodID(env, CallRFFIHelperClass, "printf", "(Ljava/lang/String;)V", 1);
	R_FindNamespaceMethodID = checkGetMethodID(env, CallRFFIHelperClass, "R_FindNamespace", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_GetOption1MethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_GetOption1", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_gsetVarMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_gsetVar", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", 1);
	Rf_inheritsMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_inherits", "(Ljava/lang/Object;Ljava/lang/String;)I", 1);
	Rf_lengthgetsMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_lengthgets", "(Ljava/lang/Object;I)Ljava/lang/Object;", 1);
//	Rf_rPsortMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_rPsort", "(Lcom/oracle/truffle/r/runtime/data/RDoubleVector;II)", 1);
//	Rf_iPsortMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_iPsort", "(Lcom/oracle/truffle/r/runtime/data/RIntVector;II)", 1);
	CADR_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "CADR", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
	TAG_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "TAG", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
	PRINTNAME_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "PRINTNAME", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
	CAR_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "CAR", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
	CDR_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "CDR", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
	SET_TAG_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "SET_TAG", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1);
	SETCAR_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "SETCAR", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1);
	SETCDR_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "SETCDR", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1);
	SET_STRING_ELT_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "SET_STRING_ELT", "(Ljava/lang/Object;ILjava/lang/Object;)V", 1);
	SET_VECTOR_ELT_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "SET_VECTOR_ELT", "(Ljava/lang/Object;ILjava/lang/Object;)V", 1);
	RAW_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "RAW", "(Ljava/lang/Object;)[B", 1);
	REAL_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "REAL", "(Ljava/lang/Object;)[D", 1);
	LOGICAL_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "LOGICAL", "(Ljava/lang/Object;)[I", 1);
	INTEGER_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "INTEGER", "(Ljava/lang/Object;)[I", 1);
	STRING_ELT_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "STRING_ELT", "(Ljava/lang/Object;I)Ljava/lang/Object;", 1);
	VECTOR_ELT_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "VECTOR_ELT", "(Ljava/lang/Object;I)Ljava/lang/Object;", 1);
	LENGTH_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "LENGTH", "(Ljava/lang/Object;)I", 1);
	Rf_asIntegerMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_asInteger", "(Ljava/lang/Object;)I", 1);
//	Rf_asRealMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_asReal", "(Ljava/lang/Object;)D", 1);
	Rf_asCharMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_asChar", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_mkCharLenCEMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_mkCharLenCE", "([BI)Ljava/lang/Object;", 1);
	Rf_asLogicalMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_asLogical", "(Ljava/lang/Object;)I", 1);
	Rf_PairToVectorListMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_PairToVectorList", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
	NAMED_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "NAMED", "(Ljava/lang/Object;)I", 1);
	SET_TYPEOF_FASTR_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "SET_TYPEOF_FASTR", "(Ljava/lang/Object;I)Ljava/lang/Object;", 1);
	TYPEOF_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "TYPEOF", "(Ljava/lang/Object;)I", 1);
	OBJECT_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "OBJECT", "(Ljava/lang/Object;)I", 1);
	DUPLICATE_ATTRIB_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "DUPLICATE_ATTRIB", "(Ljava/lang/Object;Ljava/lang/Object;)V", 1);
	isS4ObjectMethodID = checkGetMethodID(env, CallRFFIHelperClass, "isS4Object", "(Ljava/lang/Object;)I", 1);
	logObject_MethodID = checkGetMethodID(env, CallRFFIHelperClass, "logObject", "(Ljava/lang/Object;)V", 1);

	RExternalPtrClass = checkFindClass(env, "com/oracle/truffle/r/runtime/data/RExternalPtr");
	createExternalPtrMethodID = checkGetMethodID(env, RDataFactoryClass, "createExternalPtr", "(JLjava/lang/Object;Ljava/lang/Object;)Lcom/oracle/truffle/r/runtime/data/RExternalPtr;", 1);
	externalPtrGetAddrMethodID = checkGetMethodID(env, RExternalPtrClass, "getAddr", "()J", 0);
	externalPtrGetTagMethodID = checkGetMethodID(env, RExternalPtrClass, "getTag", "()Ljava/lang/Object;", 0);
	externalPtrGetProtMethodID = checkGetMethodID(env, RExternalPtrClass, "getProt", "()Ljava/lang/Object;", 0);
	externalPtrSetAddrMethodID = checkGetMethodID(env, RExternalPtrClass, "setAddr", "(J)V", 0);
	externalPtrSetTagMethodID = checkGetMethodID(env, RExternalPtrClass, "setTag", "(Ljava/lang/Object;)V", 0);
	externalPtrSetProtMethodID = checkGetMethodID(env, RExternalPtrClass, "setProt", "(Ljava/lang/Object;)V", 0);

	CharSXPWrapperClass = checkFindClass(env, "com/oracle/truffle/r/runtime/ffi/jnr/CallRFFIHelper$CharSXPWrapper");
	CharXSPWrapperContentsFieldID = checkGetFieldID(env, CharSXPWrapperClass, "contents", "Ljava/lang/String;", 0);

    R_computeIdenticalMethodID = checkGetMethodID(env, CallRFFIHelperClass, "R_computeIdentical", "(Ljava/lang/Object;Ljava/lang/Object;I)I", 1);
    Rf_copyListMatrixMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_copyListMatrix", "(Ljava/lang/Object;Ljava/lang/Object;I)V", 1);
    Rf_copyMatrixMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_copyMatrix", "(Ljava/lang/Object;Ljava/lang/Object;I)V", 1);
}

static jstring stringFromCharSXP(JNIEnv *thisenv, SEXP charsxp) {
#if VALIDATE_REFS
	validateRef(thisenv, charsxp, "stringFromCharSXP");
	if (!(*thisenv)->IsInstanceOf(thisenv, charsxp, CharSXPWrapperClass)) {

	    (*thisenv)->CallStaticVoidMethod(thisenv, CallRFFIHelperClass, logObject_MethodID, charsxp);
	    fatalError("only CharSXPWrapper expected in stringFromCharSXP");
	}
#endif
	return (*thisenv)->GetObjectField(thisenv, charsxp, CharXSPWrapperContentsFieldID);
}

SEXP Rf_ScalarInteger(int value) {
	TRACE("%s(%d)\n", value);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_ScalarIntegerMethodID, value);
    return checkRef(thisenv, result);
}

SEXP Rf_ScalarReal(double value) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_ScalarDoubleMethodID, value);
    return checkRef(thisenv, result);
}

SEXP Rf_ScalarString(SEXP value) {
	TRACE(TARG1, value);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_ScalarStringMethodID, value);
    return checkRef(thisenv, result);
}

SEXP Rf_ScalarLogical(int value) {
	TRACE(TARG1, value);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_ScalarLogicalMethodID, value);
    return checkRef(thisenv, result);
}

SEXP Rf_allocVector3(SEXPTYPE t, R_xlen_t len, R_allocator_t* allocator) {
    if (allocator != NULL) {
	unimplemented("RF_allocVector with custom allocator");
	return NULL;
    }
    TRACE(TARG2d, t, len);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_allocateVectorMethodID, t, len);
    return checkRef(thisenv, result);
}

SEXP Rf_allocArray(SEXPTYPE t, SEXP dims) {
	TRACE(TARG2d, t, dims);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_allocateArrayMethodID, t, dims);
	return checkRef(thisenv, result);
}

SEXP Rf_alloc3DArray(SEXPTYPE t, int x, int y, int z) {
	return unimplemented("Rf_alloc3DArray");
}

SEXP Rf_allocMatrix(SEXPTYPE mode, int nrow, int ncol) {
	TRACE(TARG2d, mode, nrow, ncol);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_allocateMatrixMethodID, mode, nrow, ncol);
	return checkRef(thisenv, result);
}

SEXP Rf_allocList(int x) {
	unimplemented("Rf_allocList)");
	return NULL;
}

SEXP Rf_allocSExp(SEXPTYPE t) {
	return unimplemented("Rf_allocSExp");
}

SEXP Rf_cons(SEXP car, SEXP cdr) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_consMethodID, car, cdr);
    return checkRef(thisenv, result);
}

void Rf_defineVar(SEXP symbol, SEXP value, SEXP rho) {
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_defineVarMethodID, symbol, value, rho);
}

void Rf_setVar(SEXP x, SEXP y, SEXP z) {
    unimplemented("Rf_setVar");
}

SEXP Rf_dimgets(SEXP x, SEXP y) {
	return unimplemented("Rf_dimgets");
}

SEXP Rf_dimnamesgets(SEXP x, SEXP y) {
	return unimplemented("Rf_dimnamesgets");
}

SEXP Rf_eval(SEXP expr, SEXP env) {
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_evalMethodID, expr, env);
    return checkRef(thisenv, result);
}

SEXP Rf_findFun(SEXP symbol, SEXP rho) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_findfunMethodID, symbol, rho);
	return checkRef(thisenv, result);
}

SEXP Rf_findVar(SEXP symbol, SEXP rho) {
	JNIEnv *thisenv = getEnv();
	SEXP result =(*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_findVarMethodID, symbol, rho);
    return checkRef(thisenv, result);
}

SEXP Rf_findVarInFrame(SEXP symbol, SEXP rho) {
	JNIEnv *thisenv = getEnv();
	SEXP result =(*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_findVarInFrameMethodID, symbol, rho);
    return checkRef(thisenv, result);
}

SEXP Rf_getAttrib(SEXP vec, SEXP name) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_getAttribMethodID, vec, name);
	return checkRef(thisenv, result);
}

SEXP Rf_setAttrib(SEXP vec, SEXP name, SEXP val) {
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_setAttribMethodID, vec, name, val);
	return val;
}

SEXP Rf_duplicate(SEXP x) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_duplicateMethodID, x);
	return checkRef(thisenv, result);
}

R_xlen_t Rf_any_duplicated(SEXP x, Rboolean from_last) {
    if (!isVector(x)) error(_("'duplicated' applies only to vectors"));
	JNIEnv *thisenv = getEnv();
    return (*thisenv)->CallStaticIntMethod(thisenv, CallRFFIHelperClass, Rf_anyDuplicatedMethodID, x, from_last);
}

SEXP Rf_duplicated(SEXP x, Rboolean y) {
	unimplemented("Rf_duplicated");
	return NULL;
}

SEXP Rf_applyClosure(SEXP x, SEXP y, SEXP z, SEXP a, SEXP b) {
	return unimplemented("Rf_applyClosure");
}

void Rf_copyMostAttrib(SEXP x, SEXP y) {
	unimplemented("Rf_copyMostAttrib");
}

void Rf_copyVector(SEXP x, SEXP y) {
	unimplemented("Rf_copyVector");
}

Rboolean Rf_inherits(SEXP x, const char * klass) {
    JNIEnv *thisenv = getEnv();
    jstring klazz = (*thisenv)->NewStringUTF(thisenv, klass);
    return (*thisenv)->CallStaticIntMethod(thisenv, CallRFFIHelperClass, Rf_inheritsMethodID, x, klazz);
}

Rboolean Rf_isReal(SEXP x) {
    return TYPEOF(x) == REALSXP;
}

Rboolean Rf_isSymbol(SEXP x) {
    return TYPEOF(x) == SYMSXP;
}

Rboolean Rf_isComplex(SEXP x) {
    return TYPEOF(x) == CPLXSXP;
}

Rboolean Rf_isEnvironment(SEXP x) {
    return TYPEOF(x) == ENVSXP;
}

Rboolean Rf_isExpression(SEXP x) {
    return TYPEOF(x) == EXPRSXP;
}

Rboolean Rf_isLogical(SEXP x) {
    return TYPEOF(x) == LGLSXP;
}

Rboolean Rf_isObject(SEXP s) {
	unimplemented("Rf_isObject");
	return FALSE;
}

void Rf_PrintValue(SEXP x) {
	unimplemented("Rf_PrintValue");
}

SEXP Rf_install(const char *name) {
	JNIEnv *thisenv = getEnv();
	jstring string = (*thisenv)->NewStringUTF(thisenv, name);
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, RDataFactoryClass, createSymbolMethodID, string);
	return checkRef(thisenv, result);
}

SEXP Rf_installChar(SEXP charsxp) {
	TRACE("%s(%p)\n", charsxp);
	JNIEnv *thisenv = getEnv();
	jstring string = stringFromCharSXP(thisenv, charsxp);
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, RDataFactoryClass, createSymbolMethodID, string);
	return checkRef(thisenv, result);
}

Rboolean Rf_isNull(SEXP s) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticIntMethod(thisenv, CallRFFIHelperClass, Rf_isNullMethodID, s);
}

Rboolean Rf_isString(SEXP s) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticIntMethod(thisenv, CallRFFIHelperClass, Rf_isStringMethodID, s);
}

Rboolean R_cycle_detected(SEXP s, SEXP child) {
	unimplemented("R_cycle_detected");
	return 0;
}

cetype_t Rf_getCharCE(SEXP x) {
    // unimplemented("Rf_getCharCE");
    // TODO: real implementation
    return CE_NATIVE;
}

SEXP Rf_mkChar(const char *x) {
	return Rf_mkCharLenCE(x, strlen(x), CE_NATIVE);
}

SEXP Rf_mkCharCE(const char *x, cetype_t y) {
	return Rf_mkCharLenCE(x, strlen(x), y);
}

SEXP Rf_mkCharLen(const char *x, int y) {
	return Rf_mkCharLenCE(x, y, CE_NATIVE);
}

SEXP Rf_mkCharLenCE(const char *x, int len, cetype_t enc) {
	JNIEnv *thisenv = getEnv();
	jbyteArray bytes = (*thisenv)->NewByteArray(thisenv, len);
	(*thisenv)->SetByteArrayRegion(thisenv, bytes, 0, len, x);
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_mkCharLenCEMethodID, bytes, (int) enc);
	return checkRef(thisenv, result);
}

const char *Rf_reEnc(const char *x, cetype_t ce_in, cetype_t ce_out, int subst) {
	// TODO proper implementation
	return x;
}

SEXP Rf_mkString(const char *s) {
	return ScalarString(Rf_mkChar(s));
}

int Rf_ncols(SEXP x) {
	unimplemented("Rf_ncols");
	return 0;
}

int Rf_nrows(SEXP x) {
	unimplemented("Rf_nrows");
	return 0;
}


SEXP Rf_protect(SEXP x) {
	return x;
}

void Rf_unprotect(int x) {
	// TODO perhaps we can use this
}

void R_ProtectWithIndex(SEXP x, PROTECT_INDEX *y) {

}

void R_Reprotect(SEXP x, PROTECT_INDEX y) {

}


void Rf_unprotect_ptr(SEXP x) {
	// TODO perhaps we can use this
}

#define BUFSIZE 8192

static int Rvsnprintf(char *buf, size_t size, const char  *format, va_list ap)
{
    int val;
    val = vsnprintf(buf, size, format, ap);
    buf[size-1] = '\0';
    return val;
}


void Rf_error(const char *format, ...) {
	// This is a bit tricky. The usual error handling model in Java is "throw RError.error(...)" but
	// RError.error does quite a lot of stuff including potentially searching for R condition handlers
	// and, if it finds any, does not return, but throws a different exception than RError.
	// We definitely need to exit the FFI call and we certainly cannot return to our caller.
	// So we call CallRFFIHelper.Rf_error to throw the RError exception. When the pending
	// exception (whatever it is) is observed by JNI, the call to Rf_error will return where we do a
	// non-local transfer of control back to the entry point (which will cleanup).
	char buf[8192];
	va_list(ap);
	va_start(ap,format);
	Rvsnprintf(buf, BUFSIZE - 1, format, ap);
	va_end(ap);
	JNIEnv *thisenv = getEnv();
	jstring string = (*thisenv)->NewStringUTF(thisenv, buf);
	// This will set a pending exception
	(*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_errorMethodID, string);
	// just transfer back which will cleanup and exit the entire JNI call
	longjmp(*getErrorJmpBuf(), 1);

}

void Rf_errorcall(SEXP x, const char *format, ...) {
	unimplemented("Rf_errorcall");
}

void Rf_warningcall(SEXP x, const char *format, ...) {
	char buf[8192];
	va_list(ap);
	va_start(ap,format);
	Rvsnprintf(buf, BUFSIZE - 1, format, ap);
	va_end(ap);
	JNIEnv *thisenv = getEnv();
	jstring string = (*thisenv)->NewStringUTF(thisenv, buf);
	(*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_warningcallMethodID, x, string);
}

void Rf_warning(const char *format, ...) {
	char buf[8192];
	va_list(ap);
	va_start(ap,format);
	Rvsnprintf(buf, BUFSIZE - 1, format, ap);
	va_end(ap);
	JNIEnv *thisenv = getEnv();
	jstring string = (*thisenv)->NewStringUTF(thisenv, buf);
	(*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_warningMethodID, string);
}

void Rprintf(const char *format, ...) {
	char buf[8192];
	va_list(ap);
	va_start(ap,format);
	Rvsnprintf(buf, BUFSIZE - 1, format, ap);
	va_end(ap);
	JNIEnv *thisenv = getEnv();
	jstring string = (*thisenv)->NewStringUTF(thisenv, buf);
	(*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, RprintfMethodID, string);
}

/*
  REprintf is used by the error handler do not add
  anything unless you're sure it won't
  cause problems
*/
void REprintf(const char *format, ...)
{
	// TODO: determine correct target for this message
	char buf[8192];
	va_list(ap);
	va_start(ap,format);
	Rvsnprintf(buf, BUFSIZE - 1, format, ap);
	va_end(ap);
	JNIEnv *thisenv = getEnv();
	jstring string = (*thisenv)->NewStringUTF(thisenv, buf);
	(*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, RprintfMethodID, string);
}

void Rvprintf(const char *format, va_list args) {
	unimplemented("Rvprintf");
}
void REvprintf(const char *format, va_list args) {
	unimplemented("REvprintf");
}

void R_FlushConsole(void) {
	// ignored
}

void R_ProcessEvents(void) {
	unimplemented("R_ProcessEvents");
}

// Tools package support, not in public API

SEXP R_NewHashedEnv(SEXP parent, SEXP size) {
	JNIEnv *thisenv = getEnv();
	int sizeAsInt = Rf_asInteger(size);
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, RDataFactoryClass, Rf_NewHashedEnvMethodID, parent, NULL, JNI_TRUE, sizeAsInt);
	return checkRef(thisenv, result);
}

SEXP Rf_classgets(SEXP x, SEXP y) {
	unimplemented("Rf_classgets");
	return NULL;
}

const char *Rf_translateChar(SEXP x) {
//	unimplemented("Rf_translateChar");
	// TODO: proper implementation
	const char *result = CHAR(x);
//	printf("translateChar: '%s'\n", result);
	return result;
}

const char *Rf_translateChar0(SEXP x) {
	unimplemented("Rf_translateChar0");
	return NULL;
}

const char *Rf_translateCharUTF8(SEXP x) {
	unimplemented("Rf_translateCharUTF8");
	return NULL;
}

SEXP R_FindNamespace(SEXP info) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, R_FindNamespaceMethodID, info);
	return checkRef(thisenv, result);
}

SEXP Rf_lengthgets(SEXP x, R_len_t y) {
	TRACE("%s(%p)\n", x);
	JNIEnv *thisenv = getEnv();
	invalidateCopiedObject(thisenv, x);
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_lengthgetsMethodID, x, y);
	return checkRef(thisenv, result);
}

SEXP Rf_xlengthgets(SEXP x, R_xlen_t y) {
	return unimplemented("Rf_xlengthgets");

}

SEXP Rf_namesgets(SEXP x, SEXP y) {
	return unimplemented("Rf_namesgets");
}

SEXP GetOption1(SEXP tag)
{
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_GetOption1MethodID, tag);
	return checkRef(thisenv, result);
}

SEXP GetOption(SEXP tag, SEXP rho)
{
    return GetOption1(tag);
}

int GetOptionCutoff(void)
{
    int w;
    w = asInteger(GetOption1(install("deparse.cutoff")));
    if (w == NA_INTEGER || w <= 0) {
	warning(_("invalid 'deparse.cutoff', used 60"));
	w = 60;
    }
    return w;
}

#define R_MIN_WIDTH_OPT		10
#define R_MAX_WIDTH_OPT		10000
#define R_MIN_DIGITS_OPT	0
#define R_MAX_DIGITS_OPT	22

int GetOptionWidth(void)
{
    int w;
    w = asInteger(GetOption1(install("width")));
    if (w < R_MIN_WIDTH_OPT || w > R_MAX_WIDTH_OPT) {
	warning(_("invalid printing width, used 80"));
	return 80;
    }
    return w;
}

int GetOptionDigits(void)
{
    int d;
    d = asInteger(GetOption1(install("digits")));
    if (d < R_MIN_DIGITS_OPT || d > R_MAX_DIGITS_OPT) {
	warning(_("invalid printing digits, used 7"));
	return 7;
    }
    return d;
}

Rboolean Rf_GetOptionDeviceAsk(void)
{
    int ask;
    ask = asLogical(GetOption1(install("device.ask.default")));
    if(ask == NA_LOGICAL) {
	warning(_("invalid value for \"device.ask.default\", using FALSE"));
	return FALSE;
    }
    return ask != 0;
}

void Rf_gsetVar(SEXP symbol, SEXP value, SEXP rho)
{
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_gsetVarMethodID, symbol, value, rho);
}

SEXP TAG(SEXP e) {
    TRACE(TARG1, e);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, TAG_MethodID, e);
    return checkRef(thisenv, result);
}

SEXP PRINTNAME(SEXP e) {
    TRACE(TARG1, e);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, PRINTNAME_MethodID, e);
    return checkRef(thisenv, result);
}

SEXP CAR(SEXP e) {
    TRACE(TARG1, e);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, CAR_MethodID, e);
    return checkRef(thisenv, result);
}

SEXP CDR(SEXP e) {
    TRACE(TARG1, e);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, CDR_MethodID, e);
    return checkRef(thisenv, result);
}

SEXP CAAR(SEXP e) {
    unimplemented("CAAR");
    return NULL;
}

SEXP CDAR(SEXP e) {
    unimplemented("CDAR");
    return NULL;
}

SEXP CADR(SEXP e) {
    TRACE(TARG1, e);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, CADR_MethodID, e);
    return checkRef(thisenv, result);
}

SEXP CDDR(SEXP e) {
    unimplemented("CDDR");
    return NULL;
}

SEXP CDDDR(SEXP e) {
    unimplemented("CDDDR");
    return NULL;
}

SEXP CADDR(SEXP e) {
    unimplemented("CADDR");
    return NULL;
}

SEXP CADDDR(SEXP e) {
    unimplemented("CADDDR");
    return NULL;
}

SEXP CAD4R(SEXP e) {
    unimplemented("CAD4R");
    return NULL;
}

int MISSING(SEXP x){
    unimplemented("MISSING");
    return 0;
}

void SET_MISSING(SEXP x, int v) {
    unimplemented("SET_MISSING");
}

void SET_TAG(SEXP x, SEXP y) {
    TRACE(TARG2, x, y);
    JNIEnv *thisenv = getEnv();
    (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, SET_TAG_MethodID, x, y);
}

SEXP SETCAR(SEXP x, SEXP y) {
    TRACE(TARG2, x, y);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, SETCAR_MethodID, x, y);
    return checkRef(thisenv, result);
}

SEXP SETCDR(SEXP x, SEXP y) {
    TRACE(TARG2, x, y);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, SETCDR_MethodID, x, y);
    return checkRef(thisenv, result);
}

SEXP SETCADR(SEXP x, SEXP y) {
    unimplemented("SETCADR");
    return NULL;
}

SEXP SETCADDR(SEXP x, SEXP y) {
    unimplemented("SETCADDR");
    return NULL;
}

SEXP SETCADDDR(SEXP x, SEXP y) {
    unimplemented("SETCADDDR");
    return NULL;
}

SEXP SETCAD4R(SEXP e, SEXP y) {
    unimplemented("SETCAD4R");
    return NULL;
}

SEXP FORMALS(SEXP x) {
    return unimplemented("FORMALS");
}

SEXP BODY(SEXP x) {
	return unimplemented("BODY");
}

SEXP CLOENV(SEXP x) {
	return unimplemented("CLOENV");
}

int RDEBUG(SEXP x) {
    unimplemented("RDEBUG");
    return 0;
}

int RSTEP(SEXP x) {
	unimplemented("RSTEP");
    return 0;
}

int RTRACE(SEXP x) {
	unimplemented("RTRACE");
    return 0;
}

void SET_RDEBUG(SEXP x, int v) {
    unimplemented("SET_RDEBUG");
}

void SET_RSTEP(SEXP x, int v) {
    unimplemented("SET_RSTEP");
}

void SET_RTRACE(SEXP x, int v) {
    unimplemented("SET_RTRACE");
}

void SET_FORMALS(SEXP x, SEXP v) {
    unimplemented("SET_FORMALS");
}

void SET_BODY(SEXP x, SEXP v) {
    unimplemented("SET_BODY");
}

void SET_CLOENV(SEXP x, SEXP v) {
    unimplemented("SET_CLOENV");
}

SEXP SYMVALUE(SEXP x) {
	return unimplemented("SYMVALUE");
}

SEXP INTERNAL(SEXP x) {
	return unimplemented("INTERNAL");
}

int DDVAL(SEXP x) {
	unimplemented("DDVAL");
    return 0;
}

void SET_DDVAL(SEXP x, int v) {
    unimplemented("SET_DDVAL");
}

void SET_SYMVALUE(SEXP x, SEXP v) {
    unimplemented("SET_SYMVALUE");
}

void SET_INTERNAL(SEXP x, SEXP v) {
    unimplemented("SET_INTERNAL");
}


SEXP FRAME(SEXP x) {
	return unimplemented("FRAME");
}

SEXP ENCLOS(SEXP x) {
	return unimplemented("ENCLOS");
}

SEXP HASHTAB(SEXP x) {
	return unimplemented("HASHTAB");
}

int ENVFLAGS(SEXP x) {
	unimplemented("ENVFLAGS");
    return 0;
}

void SET_ENVFLAGS(SEXP x, int v) {
	unimplemented("SET_ENVFLAGS");
}

void SET_FRAME(SEXP x, SEXP v) {
    unimplemented("SET_FRAME");
}

void SET_ENCLOS(SEXP x, SEXP v) {
	unimplemented("SET_ENCLOS");
}

void SET_HASHTAB(SEXP x, SEXP v) {
	unimplemented("SET_HASHTAB");
}


SEXP PRCODE(SEXP x) {
	return unimplemented("PRCODE");
}

SEXP PRENV(SEXP x) {
	unimplemented("PRSEEN");
    return 0;
}

SEXP PRVALUE(SEXP x) {
	return unimplemented("PRVALUE");
}

int PRSEEN(SEXP x) {
	return (int) unimplemented("PRSEEN");
}

void SET_PRSEEN(SEXP x, int v) {
    unimplemented("SET_PRSEEN");
}

void SET_PRENV(SEXP x, SEXP v) {
    unimplemented("SET_PRENV");
}

void SET_PRVALUE(SEXP x, SEXP v) {
    unimplemented("SET_PRVALUE");
}

void SET_PRCODE(SEXP x, SEXP v) {
    unimplemented("SET_PRCODE");
}

int LENGTH(SEXP x) {
    TRACE(TARG1, x);
    JNIEnv *thisenv = getEnv();
    return (*thisenv)->CallStaticIntMethod(thisenv, CallRFFIHelperClass, LENGTH_MethodID, x);
}

int TRUELENGTH(SEXP x){
    unimplemented("unimplemented");
    return 0;
}


void SETLENGTH(SEXP x, int v){
    unimplemented("SETLENGTH");
}


void SET_TRUELENGTH(SEXP x, int v){
    unimplemented("SET_TRUELENGTH");
}


R_xlen_t XLENGTH(SEXP x){
    // xlength seems to be used for long vectors (no such thing in FastR at the moment)
    return LENGTH(x);
}


R_xlen_t XTRUELENGTH(SEXP x){
	unimplemented("XTRUELENGTH");
	return 0;
}


int IS_LONG_VEC(SEXP x){
	unimplemented("IS_LONG_VEC");
	return 0;
}


int LEVELS(SEXP x){
	unimplemented("LEVELS");
	return 0;
}


int SETLEVELS(SEXP x, int v){
	unimplemented("SETLEVELS");
	return 0;
}

int *LOGICAL(SEXP x){
	TRACE(TARG1, x);
	JNIEnv *thisenv = getEnv();
	jint *data = (jint *) findCopiedObject(thisenv, x);
	if (data == NULL) {
	    jintArray intArray = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, LOGICAL_MethodID, x);
	    int len = (*thisenv)->GetArrayLength(thisenv, intArray);
	    data = (*thisenv)->GetIntArrayElements(thisenv, intArray, NULL);
	    addCopiedObject(thisenv, x, LGLSXP, intArray, data);
	}
	return data;
}

int *INTEGER(SEXP x){
	TRACE(TARG1, x);
	JNIEnv *thisenv = getEnv();
	jint *data = (jint *) findCopiedObject(thisenv, x);
	if (data == NULL) {
	    jintArray intArray = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, INTEGER_MethodID, x);
	    int len = (*thisenv)->GetArrayLength(thisenv, intArray);
	    data = (*thisenv)->GetIntArrayElements(thisenv, intArray, NULL);
	    addCopiedObject(thisenv, x, INTSXP, intArray, data);
	}
	return data;
}


Rbyte *RAW(SEXP x){
	JNIEnv *thisenv = getEnv();
	jbyte *data = (jbyte *) findCopiedObject(thisenv, x);
	if (data == NULL) {
	    jbyteArray byteArray = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, RAW_MethodID, x);
	    int len = (*thisenv)->GetArrayLength(thisenv, byteArray);
	    data = (*thisenv)->GetByteArrayElements(thisenv, byteArray, NULL);
        addCopiedObject(thisenv, x, RAWSXP, byteArray, data);
    }
	return (Rbyte*) data;
}


double *REAL(SEXP x){
    JNIEnv *thisenv = getEnv();
    jdouble *data = (jdouble *) findCopiedObject(thisenv, x);
    if (data == NULL) {
	jdoubleArray doubleArray = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, REAL_MethodID, x);
	int len = (*thisenv)->GetArrayLength(thisenv, doubleArray);
	data = (*thisenv)->GetDoubleArrayElements(thisenv, doubleArray, NULL);
	addCopiedObject(thisenv, x, REALSXP, doubleArray, data);
    }
    return data;
}


Rcomplex *COMPLEX(SEXP x){
	unimplemented("COMPLEX");
	return NULL;
}


SEXP STRING_ELT(SEXP x, R_xlen_t i){
	TRACE(TARG2d, x, i);
	JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, STRING_ELT_MethodID, x, i);
    return checkRef(thisenv, result);
}


SEXP VECTOR_ELT(SEXP x, R_xlen_t i){
	TRACE(TARG2d, x, i);
	JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, VECTOR_ELT_MethodID, x, i);
    return checkRef(thisenv, result);
}

void SET_STRING_ELT(SEXP x, R_xlen_t i, SEXP v){
	TRACE("%s(%p, %d, %p)\n", x, i, v);
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallStaticVoidMethod(thisenv, CallRFFIHelperClass, SET_STRING_ELT_MethodID, x, i, v);
}


SEXP SET_VECTOR_ELT(SEXP x, R_xlen_t i, SEXP v){
	TRACE("%s(%p, %d, %p)\n", x, i, v);
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallStaticVoidMethod(thisenv, CallRFFIHelperClass, SET_VECTOR_ELT_MethodID, x, i, v);
	return v;
}


SEXP *STRING_PTR(SEXP x){
	unimplemented("STRING_PTR");
	return NULL;
}


SEXP *VECTOR_PTR(SEXP x){
	unimplemented("VECTOR_PTR");
	return NULL;
}

SEXP Rf_asChar(SEXP x){
	TRACE(TARG1, x);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_asCharMethodID, x);
	return checkRef(thisenv, result);
}

SEXP Rf_PairToVectorList(SEXP x){
	TRACE(TARG1, x);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_PairToVectorListMethodID, x);
	return checkRef(thisenv, result);
}

SEXP Rf_VectorToPairList(SEXP x){
	unimplemented("Rf_VectorToPairList");
	return NULL;
}

SEXP Rf_asCharacterFactor(SEXP x){
	unimplemented("Rf_VectorToPairList");
	return NULL;
}

int Rf_asLogical(SEXP x){
	TRACE(TARG1, x);
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticIntMethod(thisenv, CallRFFIHelperClass, Rf_asLogicalMethodID, x);
}

int Rf_asInteger(SEXP x) {
	TRACE(TARG1, x);
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticIntMethod(thisenv, CallRFFIHelperClass, Rf_asIntegerMethodID, x);
}

//double Rf_asReal(SEXP x) {
//	TRACE(TARG1, x);
//	JNIEnv *thisenv = getEnv();
//	return (*thisenv)->CallStaticDoubleMethod(thisenv, CallRFFIHelperClass, Rf_asRealMethodID, x);
//}

Rcomplex Rf_asComplex(SEXP x){
	unimplemented("Rf_asLogical");
	Rcomplex c; return c;
}

int TYPEOF(SEXP x) {
	TRACE(TARG1, x);
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticIntMethod(thisenv, CallRFFIHelperClass, TYPEOF_MethodID, x);
}

SEXP ATTRIB(SEXP x){
    unimplemented("ATTRIB");
    return NULL;
}

int OBJECT(SEXP x){
	JNIEnv *env = getEnv();
	return 	(*env)->CallStaticIntMethod(env, CallRFFIHelperClass, OBJECT_MethodID, x);
}

int MARK(SEXP x){
    unimplemented("MARK");
    return 0;
}

int NAMED(SEXP x){
    JNIEnv *thisenv = getEnv();
    return (*thisenv)->CallStaticIntMethod(thisenv, CallRFFIHelperClass, NAMED_MethodID, x);
}

int REFCNT(SEXP x){
    unimplemented("REFCNT");
    return 0;
}

void SET_OBJECT(SEXP x, int v){
    unimplemented("SET_OBJECT");
}

void SET_TYPEOF(SEXP x, int v){
    unimplemented("SET_TYPEOF");
}

SEXP SET_TYPEOF_FASTR(SEXP x, int v){
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, SET_TYPEOF_FASTR_MethodID, x, v);
    return checkRef(thisenv, result);
}

void SET_NAMED(SEXP x, int v){
    unimplemented("SET_NAMED");
}

void SET_ATTRIB(SEXP x, SEXP v){
    unimplemented("SET_ATTRIB");
}

void DUPLICATE_ATTRIB(SEXP to, SEXP from){
    JNIEnv *thisenv = getEnv();
    (*thisenv)->CallStaticVoidMethod(thisenv, CallRFFIHelperClass, DUPLICATE_ATTRIB_MethodID, to, from);
}

char *dgettext(const char *domainname, const char *msgid) {
	printf("dgettext: '%s'\n", msgid);
	return (char*) msgid;
}

char *dngettext(const char *domainname, const char *msgid, const char * msgid_plural, unsigned long int n) {
    printf("dngettext: singular - '%s' ; plural - '%s'\n", msgid, msgid_plural);
    return (char*) (n == 1 ? msgid : msgid_plural);
}

const char *R_CHAR(SEXP charsxp) {
	TRACE("%s(%p)", charsxp);
	JNIEnv *thisenv = getEnv();
	// This is nasty:
	// 1. the resulting character array has to be copied and zero-terminated.
	// 2. It causes an (inevitable?) memory leak
	jstring string = stringFromCharSXP(thisenv, charsxp);
	jsize len = (*thisenv)->GetStringUTFLength(thisenv, string);
	const char *stringChars = (*thisenv)->GetStringUTFChars(thisenv, string, NULL);
	char *copyChars = malloc(len + 1);
	memcpy(copyChars, stringChars, len);
	copyChars[len] = 0;
	TRACE(" %s(%s)\n", copyChars);
	return copyChars;
}

void *(R_DATAPTR)(SEXP x) {
    unimplemented("R_DATAPTR");
	return NULL;
}

void R_qsort_I  (double *v, int *II, int i, int j) {
	unimplemented("R_qsort_I");
}

void R_qsort_int_I(int *iv, int *II, int i, int j) {
	unimplemented("R_qsort_int_I");
}

R_len_t R_BadLongVector(SEXP x, const char *y, int z) {
	return (R_len_t) unimplemented("R_BadLongVector");
}

int IS_S4_OBJECT(SEXP x) {
	JNIEnv *env = getEnv();
	return 	(*env)->CallStaticIntMethod(env, CallRFFIHelperClass, isS4ObjectMethodID, x);
}

void SET_S4_OBJECT(SEXP x) {
	unimplemented("SET_S4_OBJECT");
}
void UNSET_S4_OBJECT(SEXP x) {
	unimplemented("UNSET_S4_OBJECT");
}

Rboolean R_ToplevelExec(void (*fun)(void *), void *data) {
	return (Rboolean) unimplemented("R_ToplevelExec");
}

SEXP R_ExecWithCleanup(SEXP (*fun)(void *), void *data,
		       void (*cleanfun)(void *), void *cleandata) {
	return unimplemented("R_ExecWithCleanup");
}

SEXP R_tryEval(SEXP x, SEXP y, int *z) {
	return unimplemented("R_tryEval");
}

SEXP R_tryEvalSilent(SEXP x, SEXP y, int *z) {
	return unimplemented("R_tryEvalSilent");
}

double R_atof(const char *str) {
	unimplemented("R_atof");
	return 0;
}

double R_strtod(const char *c, char **end) {
	unimplemented("R_strtod");
	return 0;
}

SEXP R_PromiseExpr(SEXP x) {
	return unimplemented("R_PromiseExpr");
}

SEXP R_ClosureExpr(SEXP x) {
	return unimplemented("R_ClosureExpr");
}

SEXP R_forceAndCall(SEXP e, int n, SEXP rho) {
	return unimplemented("R_forceAndCall");
}

SEXP R_MakeExternalPtr(void *p, SEXP tag, SEXP prot) {
	JNIEnv *thisenv = getEnv();
	SEXP result =  (*thisenv)->CallStaticObjectMethod(thisenv, RDataFactoryClass, createExternalPtrMethodID, (jlong) p, tag, prot);
    return checkRef(thisenv, result);
}

void *R_ExternalPtrAddr(SEXP s) {
	JNIEnv *thisenv = getEnv();
	return (void *) (*thisenv)->CallLongMethod(thisenv, s, externalPtrGetAddrMethodID);
}

SEXP R_ExternalPtrTag(SEXP s) {
	JNIEnv *thisenv = getEnv();
	SEXP result =  (*thisenv)->CallObjectMethod(thisenv, s, externalPtrGetTagMethodID);
    return checkRef(thisenv, result);
}

SEXP R_ExternalPtrProt(SEXP s) {
	JNIEnv *thisenv = getEnv();
	SEXP result =  (*thisenv)->CallObjectMethod(thisenv, s, externalPtrGetProtMethodID);
    return checkRef(thisenv, result);
}

void R_SetExternalPtrAddr(SEXP s, void *p) {
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallLongMethod(thisenv, s, externalPtrSetAddrMethodID, (jlong) p);
}

void R_SetExternalPtrTag(SEXP s, SEXP tag) {
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallObjectMethod(thisenv, s, externalPtrSetTagMethodID, tag);
}

void R_SetExternalPtrProt(SEXP s, SEXP p) {
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallObjectMethod(thisenv, s, externalPtrSetProtMethodID, p);
}

void R_ClearExternalPtr(SEXP s) {
	R_SetExternalPtrAddr(s, NULL);
}

void R_RegisterFinalizer(SEXP s, SEXP fun) {
	// TODO implement, but not fail for now
}
void R_RegisterCFinalizer(SEXP s, R_CFinalizer_t fun) {
	// TODO implement, but not fail for now
}

void R_RegisterFinalizerEx(SEXP s, SEXP fun, Rboolean onexit) {
	// TODO implement, but not fail for now

}

void R_RegisterCFinalizerEx(SEXP s, R_CFinalizer_t fun, Rboolean onexit) {
	// TODO implement, but not fail for now
}

void R_RunPendingFinalizers(void) {
	// TODO implement, but not fail for now
}

SEXP R_do_slot(SEXP obj, SEXP name) {
	return unimplemented("R_do_slot");
}

SEXP R_do_slot_assign(SEXP obj, SEXP name, SEXP value) {
	return unimplemented("R_do_slot_assign");
}

int R_has_slot(SEXP obj, SEXP name) {
	return (int) unimplemented("R_has_slot");
}

SEXP R_do_MAKE_CLASS(const char *what) {
	return unimplemented("R_do_MAKE_CLASS");
}

SEXP R_getClassDef (const char *what) {
	return unimplemented("R_getClassDef");
}

SEXP R_do_new_object(SEXP class_def) {
	return unimplemented("R_do_new_object");
}

int R_check_class_and_super(SEXP x, const char **valid, SEXP rho) {
	return (int) unimplemented("R_check_class_and_super");
}

int R_check_class_etc (SEXP x, const char **valid) {
	return (int) unimplemented("R_check_class_etc");
}

void R_PreserveObject(SEXP x) {
	// Not applicable
}

void R_ReleaseObject(SEXP x) {
	// Not applicable
}

Rboolean R_compute_identical(SEXP x, SEXP y, int flags) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallStaticIntMethod(thisenv, CallRFFIHelperClass, R_computeIdenticalMethodID, x, y, flags);
}

void Rf_copyListMatrix(SEXP s, SEXP t, Rboolean byrow) {
	JNIEnv *thisenv = getEnv();
    (*thisenv)->CallStaticIntMethod(thisenv, CallRFFIHelperClass, Rf_copyListMatrixMethodID, s, t, byrow);  
}

void Rf_copyMatrix(SEXP s, SEXP t, Rboolean byrow) {
	JNIEnv *thisenv = getEnv();
    (*thisenv)->CallStaticIntMethod(thisenv, CallRFFIHelperClass, Rf_copyMatrixMethodID, s, t, byrow);  
}
