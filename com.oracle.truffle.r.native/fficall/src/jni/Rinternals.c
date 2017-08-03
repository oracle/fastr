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
#include <rffiutils.h>
#include <string.h>
#include <Rinternals_common.h>

// Most everything in RInternals.h

// N.B. When implementing a new method that returns a SEXP, you MUST
// explicitly (or implicitly) return via "checkRef(thisenv, result)"
// to ensure that a global JNI handle is created (if necessary) and returned,
// otherwise a GC might reclaim the result.

// N.B. ALL functions go via UpCallsRFFI to provide a single point of re-entry

static jmethodID Rf_ScalarIntegerMethodID;
static jmethodID Rf_ScalarDoubleMethodID;
static jmethodID Rf_ScalarStringMethodID;
static jmethodID Rf_ScalarLogicalMethodID;
static jmethodID Rf_allocVectorMethodID;
static jmethodID Rf_allocArrayMethodID;
static jmethodID Rf_allocMatrixMethodID;
static jmethodID Rf_duplicateMethodID;
static jmethodID Rf_any_duplicatedMethodID;
static jmethodID Rf_consMethodID;
static jmethodID Rf_evalMethodID;
static jmethodID Rf_findFunMethodID;
static jmethodID Rf_defineVarMethodID;
static jmethodID Rf_findVarMethodID;
static jmethodID Rf_findVarInFrameMethodID;
static jmethodID Rf_findVarInFrame3MethodID;
static jmethodID ATTRIBMethodID;
static jmethodID Rf_getAttribMethodID;
static jmethodID Rf_setAttribMethodID;
static jmethodID Rf_isStringMethodID;
static jmethodID Rf_isNullMethodID;
static jmethodID Rf_installCharMethodID;
static jmethodID Rf_installMethodID;
static jmethodID Rf_warningcallMethodID;
static jmethodID Rf_errorcallMethodID;
static jmethodID Rf_warningMethodID;
static jmethodID Rf_errorMethodID;
static jmethodID R_NewHashedEnvMethodID;
static jmethodID Rf_classgetsMethodID;
static jmethodID Rf_rPsortMethodID;
static jmethodID Rf_iPsortMethodID;
static jmethodID RprintfMethodID;
static jmethodID R_FindNamespaceMethodID;
static jmethodID R_BindingIsLockedID;
static jmethodID Rf_GetOption1MethodID;
static jmethodID Rf_gsetVarMethodID;
static jmethodID Rf_inheritsMethodID;
static jmethodID Rf_lengthgetsMethodID;
static jmethodID CADR_MethodID;
static jmethodID CADDR_MethodID;
static jmethodID TAG_MethodID;
static jmethodID PRINTNAME_MethodID;
static jmethodID CAR_MethodID;
static jmethodID CDR_MethodID;
static jmethodID CDDR_MethodID;
static jmethodID SET_TAG_MethodID;
static jmethodID SETCAR_MethodID;
static jmethodID SETCDR_MethodID;
static jmethodID SETCADR_MethodID;
static jmethodID SYMVALUE_MethodID;
static jmethodID SET_SYMVALUE_MethodID;
static jmethodID SET_STRING_ELT_MethodID;
static jmethodID SET_VECTOR_ELT_MethodID;
jmethodID RAW_MethodID;
jmethodID INTEGER_MethodID;
jmethodID REAL_MethodID;
jmethodID LOGICAL_MethodID;
jmethodID logNotCharSXPWrapperMethodID;
static jmethodID STRING_ELT_MethodID;
static jmethodID VECTOR_ELT_MethodID;
static jmethodID LENGTH_MethodID;
static jmethodID R_do_slot_MethodID;
static jmethodID R_do_slot_assign_MethodID;
static jmethodID R_MethodsNamespaceMethodID;
static jmethodID Rf_str2type_MethodID;
static jmethodID Rf_asIntegerMethodID;
static jmethodID Rf_asRealMethodID;
static jmethodID Rf_asCharMethodID;
static jmethodID Rf_coerceVectorMethodID;
static jmethodID Rf_mkCharLenCEMethodID;
static jmethodID Rf_asLogicalMethodID;
static jmethodID Rf_PairToVectorListMethodID;
static jmethodID gnuRCodeForObjectMethodID;
static jmethodID NAMED_MethodID;
static jmethodID SET_TYPEOF_FASTR_MethodID;
static jmethodID SET_NAMED_FASTR_MethodID;
static jmethodID TYPEOF_MethodID;
static jmethodID OBJECT_MethodID;
static jmethodID DUPLICATE_ATTRIB_MethodID;
static jmethodID IS_S4_OBJECTMethodID;
static jmethodID SET_S4_OBJECTMethodID;
static jmethodID UNSET_S4_OBJECTMethodID;
static jmethodID R_tryEvalMethodID;
static jmethodID RDEBUGMethodID;
static jmethodID SET_RDEBUGMethodID;
static jmethodID RSTEPMethodID;
static jmethodID SET_RSTEPMethodID;
static jmethodID ENCLOSMethodID;
static jmethodID PRVALUEMethodID;
static jmethodID R_lsInternal3MethodID;
static jmethodID R_do_MAKE_CLASS_MethodID;
static jmethodID R_do_new_object_MethodID;
static jmethodID PRSEENMethodID;
static jmethodID PRENVMethodID;
static jmethodID R_PromiseExprMethodID;
static jmethodID PRCODEMethodID;

static jmethodID R_ToplevelExecMethodID;
static jmethodID restoreHandlerStacksMethodID;

static jmethodID R_MakeExternalPtrMethodID;
static jmethodID R_ExternalPtrAddrMethodID;
static jmethodID R_ExternalPtrTagMethodID;
static jmethodID R_ExternalPtrProtectedMethodID;
static jmethodID R_SetExternalPtrAddrMethodID;
static jmethodID R_SetExternalPtrTagMethodID;
static jmethodID R_SetExternalPtrProtMethodID;

static jmethodID R_compute_identicalMethodID;
static jmethodID Rf_copyListMatrixMethodID;
static jmethodID Rf_copyMatrixMethodID;
static jmethodID Rf_nrowsMethodID;
static jmethodID Rf_ncolsMethodID;
static jmethodID Rf_namesgetsMethodID;

static jclass CharSXPWrapperClass;
jclass JNIUpCallsRFFIImplClass;

jmethodID setCompleteMethodID;

void init_internals(JNIEnv *env) {
	Rf_ScalarIntegerMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_ScalarInteger", "(I)Lcom/oracle/truffle/r/runtime/data/RIntVector;", 0);
	Rf_ScalarDoubleMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_ScalarDouble", "(D)Lcom/oracle/truffle/r/runtime/data/RDoubleVector;", 0);
	Rf_ScalarStringMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_ScalarString", "(Ljava/lang/Object;)Lcom/oracle/truffle/r/runtime/data/RStringVector;", 0);
	Rf_ScalarLogicalMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_ScalarLogical", "(I)Lcom/oracle/truffle/r/runtime/data/RLogicalVector;", 0);
	Rf_consMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_cons", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 0);
	Rf_evalMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_eval", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 0);
	Rf_findFunMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_findFun", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 0);
	Rf_defineVarMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_defineVar", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)I", 0);
	Rf_findVarMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_findVar", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 0);
	Rf_findVarInFrameMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_findVarInFrame", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 0);
	Rf_findVarInFrame3MethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_findVarInFrame3", "(Ljava/lang/Object;Ljava/lang/Object;I)Ljava/lang/Object;", 0);
	ATTRIBMethodID = checkGetMethodID(env, UpCallsRFFIClass, "ATTRIB", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	Rf_getAttribMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_getAttrib", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 0);
	Rf_setAttribMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_setAttrib", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)I", 0);
	Rf_isStringMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_isString", "(Ljava/lang/Object;)I", 0);
	Rf_isNullMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_isNull", "(Ljava/lang/Object;)I", 0);
	Rf_installMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_install", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	Rf_installCharMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_installChar", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	Rf_warningMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_warning", "(Ljava/lang/Object;)I", 0);
	Rf_warningcallMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_warningcall", "(Ljava/lang/Object;Ljava/lang/Object;)I", 0);
	Rf_errorcallMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_errorcall", "(Ljava/lang/Object;Ljava/lang/Object;)I", 0);
	Rf_errorMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_error", "(Ljava/lang/Object;)I", 0);
	Rf_allocVectorMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_allocVector", "(IJ)Ljava/lang/Object;", 0);
	Rf_allocMatrixMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_allocMatrix", "(III)Ljava/lang/Object;", 0);
	Rf_allocArrayMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_allocArray", "(ILjava/lang/Object;)Ljava/lang/Object;", 0);
	Rf_duplicateMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_duplicate", "(Ljava/lang/Object;I)Ljava/lang/Object;", 0);
	Rf_any_duplicatedMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_any_duplicated", "(Ljava/lang/Object;I)J", 0);
	R_NewHashedEnvMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_NewHashedEnv", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 0);
	Rf_classgetsMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_classgets", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 0);
	RprintfMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rprintf", "(Ljava/lang/Object;)I", 0);
	R_do_MAKE_CLASS_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_do_MAKE_CLASS", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	R_do_new_object_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_do_new_object", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	R_FindNamespaceMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_FindNamespace", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	R_BindingIsLockedID = checkGetMethodID(env, UpCallsRFFIClass, "R_BindingIsLocked", "(Ljava/lang/Object;Ljava/lang/Object;)I", 0);
	Rf_GetOption1MethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_GetOption1", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	Rf_gsetVarMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_gsetVar", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)I", 0);
	Rf_inheritsMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_inherits", "(Ljava/lang/Object;Ljava/lang/Object;)I", 0);
	Rf_lengthgetsMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_lengthgets", "(Ljava/lang/Object;I)Ljava/lang/Object;", 0);
//	Rf_rPsortMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_rPsort", "(Lcom/oracle/truffle/r/runtime/data/RDoubleVector;II)", 0);
//	Rf_iPsortMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_iPsort", "(Lcom/oracle/truffle/r/runtime/data/RIntVector;II)", 0);
	CADR_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "CADR", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	CADDR_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "CADDR", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	TAG_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "TAG", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	PRINTNAME_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "PRINTNAME", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	CAR_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "CAR", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	CDR_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "CDR", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	CDDR_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "CDDR", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	SET_TAG_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "SET_TAG", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 0);
	SETCAR_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "SETCAR", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 0);
	SETCDR_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "SETCDR", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 0);
	SETCADR_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "SETCADR", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 0);
	SYMVALUE_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "SYMVALUE", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	SET_SYMVALUE_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "SET_SYMVALUE", "(Ljava/lang/Object;Ljava/lang/Object;)I", 0);
	SET_STRING_ELT_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "SET_STRING_ELT", "(Ljava/lang/Object;JLjava/lang/Object;)I", 0);
	SET_VECTOR_ELT_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "SET_VECTOR_ELT", "(Ljava/lang/Object;JLjava/lang/Object;)I", 0);
	RAW_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "RAW", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	REAL_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "REAL", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	LOGICAL_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "LOGICAL", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	INTEGER_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "INTEGER", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	STRING_ELT_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "STRING_ELT", "(Ljava/lang/Object;J)Ljava/lang/Object;", 0);
	VECTOR_ELT_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "VECTOR_ELT", "(Ljava/lang/Object;J)Ljava/lang/Object;", 0);
	LENGTH_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "LENGTH", "(Ljava/lang/Object;)I", 0);
	R_do_slot_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_do_slot", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 0);
	R_do_slot_assign_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_do_slot_assign", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 0);
	R_MethodsNamespaceMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_MethodsNamespace", "()Ljava/lang/Object;", 0);
	Rf_str2type_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_str2type", "(Ljava/lang/Object;)I", 0);
	Rf_asIntegerMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_asInteger", "(Ljava/lang/Object;)I", 0);
	Rf_asRealMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_asReal", "(Ljava/lang/Object;)D", 0);
	Rf_asCharMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_asChar", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	Rf_mkCharLenCEMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_mkCharLenCE", "(Ljava/lang/Object;II)Ljava/lang/Object;", 0);
        Rf_coerceVectorMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_coerceVector", "(Ljava/lang/Object;I)Ljava/lang/Object;", 0);
	Rf_asLogicalMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_asLogical", "(Ljava/lang/Object;)I", 0);
	Rf_PairToVectorListMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_PairToVectorList", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	NAMED_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "NAMED", "(Ljava/lang/Object;)I", 0);
	SET_TYPEOF_FASTR_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "SET_TYPEOF_FASTR", "(Ljava/lang/Object;I)Ljava/lang/Object;", 0);
	SET_NAMED_FASTR_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "SET_NAMED_FASTR", "(Ljava/lang/Object;I)Ljava/lang/Object;", 0);
	TYPEOF_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "TYPEOF", "(Ljava/lang/Object;)I", 0);
	OBJECT_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "OBJECT", "(Ljava/lang/Object;)I", 0);
	DUPLICATE_ATTRIB_MethodID = checkGetMethodID(env, UpCallsRFFIClass, "DUPLICATE_ATTRIB", "(Ljava/lang/Object;Ljava/lang/Object;)I", 0);
	IS_S4_OBJECTMethodID = checkGetMethodID(env, UpCallsRFFIClass, "IS_S4_OBJECT", "(Ljava/lang/Object;)I", 0);
	SET_S4_OBJECTMethodID = checkGetMethodID(env, UpCallsRFFIClass, "SET_S4_OBJECT", "(Ljava/lang/Object;)I", 0);
	UNSET_S4_OBJECTMethodID = checkGetMethodID(env, UpCallsRFFIClass, "UNSET_S4_OBJECT", "(Ljava/lang/Object;)I", 0);
	R_tryEvalMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_tryEval", "(Ljava/lang/Object;Ljava/lang/Object;I)Ljava/lang/Object;", 0);
	RDEBUGMethodID = checkGetMethodID(env, UpCallsRFFIClass, "RDEBUG", "(Ljava/lang/Object;)I", 0);
	SET_RDEBUGMethodID = checkGetMethodID(env, UpCallsRFFIClass, "SET_RDEBUG", "(Ljava/lang/Object;I)I", 0);
	RSTEPMethodID = checkGetMethodID(env, UpCallsRFFIClass, "RSTEP", "(Ljava/lang/Object;)I", 0);
	SET_RSTEPMethodID = checkGetMethodID(env, UpCallsRFFIClass, "SET_RSTEP", "(Ljava/lang/Object;I)I", 0);
	ENCLOSMethodID = checkGetMethodID(env, UpCallsRFFIClass, "ENCLOS", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	PRVALUEMethodID = checkGetMethodID(env, UpCallsRFFIClass, "PRVALUE", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	R_lsInternal3MethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_lsInternal3", "(Ljava/lang/Object;II)Ljava/lang/Object;", 0);
	PRSEENMethodID = checkGetMethodID(env, UpCallsRFFIClass, "PRSEEN", "(Ljava/lang/Object;)I", 0);
	PRENVMethodID = checkGetMethodID(env, UpCallsRFFIClass, "PRENV", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	R_PromiseExprMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_PromiseExpr", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	PRCODEMethodID = checkGetMethodID(env, UpCallsRFFIClass, "PRCODE", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);

	R_ToplevelExecMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_ToplevelExec", "()Ljava/lang/Object;", 0);

	R_MakeExternalPtrMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_MakeExternalPtr", "(JLjava/lang/Object;Ljava/lang/Object;)Lcom/oracle/truffle/r/runtime/data/RExternalPtr;", 0);
	R_ExternalPtrAddrMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_ExternalPtrAddr", "(Ljava/lang/Object;)J", 0);
	R_ExternalPtrTagMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_ExternalPtrTag", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	R_ExternalPtrProtectedMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_ExternalPtrProtected", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
	R_SetExternalPtrAddrMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_SetExternalPtrAddr", "(Ljava/lang/Object;J)I", 0);
	R_SetExternalPtrTagMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_SetExternalPtrTag", "(Ljava/lang/Object;Ljava/lang/Object;)I", 0);
	R_SetExternalPtrProtMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_SetExternalPtrProtected", "(Ljava/lang/Object;Ljava/lang/Object;)I", 0);

    R_compute_identicalMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_compute_identical", "(Ljava/lang/Object;Ljava/lang/Object;I)I", 0);
    Rf_copyListMatrixMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_copyListMatrix", "(Ljava/lang/Object;Ljava/lang/Object;I)I", 0);
    Rf_copyMatrixMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_copyMatrix", "(Ljava/lang/Object;Ljava/lang/Object;I)I", 0);
    Rf_nrowsMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_nrows", "(Ljava/lang/Object;)I", 0);
    Rf_ncolsMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_ncols", "(Ljava/lang/Object;)I", 0);
    Rf_namesgetsMethodID = checkGetMethodID(env, UpCallsRFFIClass, "Rf_namesgets", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 0);

    // static JNI-specific methods
	JNIUpCallsRFFIImplClass = checkFindClass(env, "com/oracle/truffle/r/ffi/impl/jni/JNIUpCallsRFFIImpl");
	restoreHandlerStacksMethodID = checkGetMethodID(env, JNIUpCallsRFFIImplClass, "R_ToplevelExecRestoreErrorHandlerStacks", "(Ljava/lang/Object;)V", 1);
    setCompleteMethodID = checkGetMethodID(env, JNIUpCallsRFFIImplClass, "setComplete", "(Ljava/lang/Object;Z)V", 1);
	logNotCharSXPWrapperMethodID = checkGetMethodID(env, JNIUpCallsRFFIImplClass, "logNotCharSXPWrapper", "(Ljava/lang/Object;)V", 1);
}

SEXP Rf_ScalarInteger(int value) {
	TRACE(TARGp, value);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_ScalarIntegerMethodID, value);
    return checkRef(thisenv, result);
}

SEXP Rf_ScalarReal(double value) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_ScalarDoubleMethodID, value);
    return checkRef(thisenv, result);
}

SEXP Rf_ScalarString(SEXP value) {
	TRACE(TARGp, value);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_ScalarStringMethodID, value);
    return checkRef(thisenv, result);
}

SEXP Rf_ScalarLogical(int value) {
	TRACE(TARGp, value);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_ScalarLogicalMethodID, value);
    return checkRef(thisenv, result);
}

SEXP Rf_allocVector3(SEXPTYPE t, R_xlen_t len, R_allocator_t* allocator) {
    if (allocator != NULL) {
  	    unimplemented("RF_allocVector with custom allocator");
	    return NULL;
    }
    TRACE(TARGpd, t, len);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_allocVectorMethodID, t, len);
    return checkRef(thisenv, result);
}

SEXP Rf_allocArray(SEXPTYPE t, SEXP dims) {
	TRACE(TARGppd, t, dims);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_allocArrayMethodID, t, dims);
	return checkRef(thisenv, result);
}

SEXP Rf_alloc3DArray(SEXPTYPE t, int x, int y, int z) {
	return unimplemented("Rf_alloc3DArray");
}

SEXP Rf_allocMatrix(SEXPTYPE mode, int nrow, int ncol) {
	TRACE(TARGppd, mode, nrow, ncol);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_allocMatrixMethodID, mode, nrow, ncol);
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
	TRACE(TARGpp, car, cdr);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_consMethodID, car, cdr);
    return checkRef(thisenv, result);
}

void Rf_defineVar(SEXP symbol, SEXP value, SEXP rho) {
	TRACE(TARGppp, symbol, value, rho);
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_defineVarMethodID, symbol, value, rho);
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
	TRACE(TARGpp, expr, env);
    JNIEnv *thisenv = getEnv();
    updateNativeArrays(thisenv);
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_evalMethodID, expr, env);
    return checkRef(thisenv, result);
}

SEXP Rf_findFun(SEXP symbol, SEXP rho) {
	TRACE(TARGpp, symbol, rho);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_findFunMethodID, symbol, rho);
	return checkRef(thisenv, result);
}

SEXP Rf_findVar(SEXP sym, SEXP rho) {
	TRACE(TARGpp, sym, rho);
	JNIEnv *thisenv = getEnv();
	SEXP result =(*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_findVarMethodID, sym, rho);
    return checkRef(thisenv, result);
}

SEXP Rf_findVarInFrame(SEXP rho, SEXP sym) {
	TRACE(TARGpp, rho, sym);
	JNIEnv *thisenv = getEnv();
	SEXP result =(*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_findVarInFrameMethodID, rho, sym);
    return checkRef(thisenv, result);
}

SEXP Rf_findVarInFrame3(SEXP rho, SEXP sym, Rboolean b) {
	TRACE(TARGppd, rho, sym, b);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_findVarInFrame3MethodID, rho, sym, b);
    return checkRef(thisenv, result);
}

SEXP Rf_getAttrib(SEXP vec, SEXP name) {
	TRACE(TARGpp, vec, name);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_getAttribMethodID, vec, name);
	return checkRef(thisenv, result);
}

SEXP Rf_setAttrib(SEXP vec, SEXP name, SEXP val) {
	TRACE(TARGppp, vec,name, val);
	JNIEnv *thisenv = getEnv();
	updateNativeArray(thisenv, val);
	(*thisenv)->CallVoidMethod(thisenv, UpCallsRFFIObject, Rf_setAttribMethodID, vec, name, val);
	return val;
}

SEXP Rf_duplicate(SEXP x) {
	TRACE(TARGp, x);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_duplicateMethodID, x, 1);
	return checkRef(thisenv, result);
}

SEXP Rf_shallow_duplicate(SEXP x) {
	TRACE(TARGp, x);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_duplicateMethodID, x, 0);
	return checkRef(thisenv, result);
}

R_xlen_t Rf_any_duplicated(SEXP x, Rboolean from_last) {
	TRACE(TARGpd, x, from_last);
    if (!isVector(x)) error(_("'duplicated' applies only to vectors"));
	JNIEnv *thisenv = getEnv();
    return (*thisenv)->CallIntMethod(thisenv, UpCallsRFFIObject, Rf_any_duplicatedMethodID, x, from_last);
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

int Rf_countContexts(int x, int y) {
	return (int) unimplemented("Rf_countContexts");
}

Rboolean Rf_inherits(SEXP x, const char * klass) {
	TRACE(TARGps, x, klass);
    JNIEnv *thisenv = getEnv();
    jstring klazz = (*thisenv)->NewStringUTF(thisenv, klass);
    return (*thisenv)->CallIntMethod(thisenv, UpCallsRFFIObject, Rf_inheritsMethodID, x, klazz);
}


Rboolean Rf_isObject(SEXP s) {
	unimplemented("Rf_isObject");
	return FALSE;
}

void Rf_PrintValue(SEXP x) {
	unimplemented("Rf_PrintValue");
}

SEXP Rf_install(const char *name) {
	TRACE(TARGs, name);
	JNIEnv *thisenv = getEnv();
	jstring string = (*thisenv)->NewStringUTF(thisenv, name);
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_installMethodID, string);
        addGlobalRef(thisenv, result, 1);
	return checkRef(thisenv, result);
}

SEXP Rf_installChar(SEXP charsxp) {
	TRACE(TARGp, charsxp);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_installCharMethodID, charsxp);
	return checkRef(thisenv, result);
}

Rboolean Rf_isNull(SEXP s) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallIntMethod(thisenv, UpCallsRFFIObject, Rf_isNullMethodID, s);
}

Rboolean Rf_isString(SEXP s) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallIntMethod(thisenv, UpCallsRFFIObject, Rf_isStringMethodID, s);
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
	TRACE(TARGsdd, x, len, enc);
	JNIEnv *thisenv = getEnv();
	jbyteArray bytes = (*thisenv)->NewByteArray(thisenv, len);
	(*thisenv)->SetByteArrayRegion(thisenv, bytes, 0, len, (const jbyte *) x);
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_mkCharLenCEMethodID, bytes, len, (int) enc);
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
	TRACE(TARGs, x);
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallIntMethod(thisenv, UpCallsRFFIObject, Rf_ncolsMethodID, x);
}

int Rf_nrows(SEXP x) {
	TRACE(TARGs, x);
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallIntMethod(thisenv, UpCallsRFFIObject, Rf_nrowsMethodID, x);
}


SEXP Rf_protect(SEXP x) {
	TRACE(TARGp, x);
	return x;
}

void Rf_unprotect(int x) {
	TRACE(TARGp, x);
}

void R_ProtectWithIndex(SEXP x, PROTECT_INDEX *y) {
	TRACE(TARGpd, x,y);
}

void R_Reprotect(SEXP x, PROTECT_INDEX y) {
	TRACE(TARGpd, x,y);
}


void Rf_unprotect_ptr(SEXP x) {
	TRACE(TARGp, x);
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
	// So we call RFFIUpCallsObject.Rf_error to throw the RError exception. When the pending
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
	(*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_errorMethodID, string);
	// just transfer back which will cleanup and exit the entire JNI call
	longjmp(*getErrorJmpBuf(), 1);
}

void Rf_errorcall(SEXP x, const char *format, ...) {
	char buf[8192];
	va_list(ap);
	va_start(ap,format);
	Rvsnprintf(buf, BUFSIZE - 1, format, ap);
	va_end(ap);
	JNIEnv *thisenv = getEnv();
	jstring string = (*thisenv)->NewStringUTF(thisenv, buf);
	(*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_errorcallMethodID, x, string);
}

void Rf_warningcall(SEXP x, const char *format, ...) {
	char buf[8192];
	va_list(ap);
	va_start(ap,format);
	Rvsnprintf(buf, BUFSIZE - 1, format, ap);
	va_end(ap);
	JNIEnv *thisenv = getEnv();
	jstring string = (*thisenv)->NewStringUTF(thisenv, buf);
	(*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_warningcallMethodID, x, string);
}

void Rf_warning(const char *format, ...) {
	char buf[8192];
	va_list(ap);
	va_start(ap,format);
	Rvsnprintf(buf, BUFSIZE - 1, format, ap);
	va_end(ap);
	JNIEnv *thisenv = getEnv();
	jstring string = (*thisenv)->NewStringUTF(thisenv, buf);
	(*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_warningMethodID, string);
}

void Rprintf(const char *format, ...) {
	char buf[8192];
	va_list(ap);
	va_start(ap,format);
	Rvsnprintf(buf, BUFSIZE - 1, format, ap);
	va_end(ap);
	JNIEnv *thisenv = getEnv();
	jstring string = (*thisenv)->NewStringUTF(thisenv, buf);
	(*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, RprintfMethodID, string);
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
	(*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, RprintfMethodID, string);
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
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, R_NewHashedEnvMethodID, parent, size);
	return checkRef(thisenv, result);
}

SEXP Rf_classgets(SEXP vec, SEXP klass) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_classgetsMethodID, vec, klass);
	return checkRef(thisenv, result);
}

const char *Rf_translateChar(SEXP x) {
	// TODO: proper implementation
	TRACE(TARGp, x);
	const char *result = CHAR(x);
	return result;
}

const char *Rf_translateChar0(SEXP x) {
	// TODO: proper implementation
	TRACE(TARGp, x);
	const char *result = CHAR(x);
	return result;
}

const char *Rf_translateCharUTF8(SEXP x) {
	// TODO: proper implementation
	TRACE(TARGp, x);
	const char *result = CHAR(x);
	return result;
}

SEXP Rf_lengthgets(SEXP x, R_len_t y) {
	TRACE(TARGp, x);
	JNIEnv *thisenv = getEnv();
	invalidateNativeArray(thisenv, x);
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_lengthgetsMethodID, x, y);
	return checkRef(thisenv, result);
}

SEXP Rf_xlengthgets(SEXP x, R_xlen_t y) {
	return unimplemented("Rf_xlengthgets");

}

SEXP R_lsInternal(SEXP env, Rboolean all) {
	return R_lsInternal3(env, all, TRUE);
}

SEXP R_lsInternal3(SEXP env, Rboolean all, Rboolean sorted) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, R_lsInternal3MethodID, env, all, sorted);
	return checkRef(thisenv, result);
}

SEXP Rf_namesgets(SEXP x, SEXP y) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_namesgetsMethodID, x, y);
	return checkRef(thisenv, result);
}

SEXP GetOption1(SEXP tag)
{
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_GetOption1MethodID, tag);
	return checkRef(thisenv, result);
}

void Rf_gsetVar(SEXP symbol, SEXP value, SEXP rho)
{
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_gsetVarMethodID, symbol, value, rho);
}

SEXP TAG(SEXP e) {
    TRACE(TARGp, e);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, TAG_MethodID, e);
    return checkRef(thisenv, result);
}

SEXP PRINTNAME(SEXP e) {
    TRACE(TARGp, e);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, PRINTNAME_MethodID, e);
    return checkRef(thisenv, result);
}

SEXP CAR(SEXP e) {
    TRACE(TARGp, e);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, CAR_MethodID, e);
    return checkRef(thisenv, result);
}

SEXP CDR(SEXP e) {
    TRACE(TARGp, e);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, CDR_MethodID, e);
    return checkRef(thisenv, result);
}

SEXP CAAR(SEXP e) {
    return CAR(CAR(e));
}

SEXP CDAR(SEXP e) {
    return CDR(CAR(e));
}

SEXP CADR(SEXP e) {
    TRACE(TARGp, e);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, CADR_MethodID, e);
    return checkRef(thisenv, result);
}

SEXP CDDR(SEXP e) {
    TRACE(TARGp, e);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, CDDR_MethodID, e);
    return checkRef(thisenv, result);
}

SEXP CDDDR(SEXP e) {
    unimplemented("CDDDR");
    return NULL;
}

SEXP CADDR(SEXP e) {
    TRACE(TARGp, e);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, CADDR_MethodID, e);
    return checkRef(thisenv, result);
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
    TRACE(TARGpp, x, y);
    JNIEnv *thisenv = getEnv();
    (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, SET_TAG_MethodID, x, y);
}

SEXP SETCAR(SEXP x, SEXP y) {
    TRACE(TARGpp, x, y);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, SETCAR_MethodID, x, y);
    return checkRef(thisenv, result);
}

SEXP SETCDR(SEXP x, SEXP y) {
    TRACE(TARGpp, x, y);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, SETCDR_MethodID, x, y);
    return checkRef(thisenv, result);
}

SEXP SETCADR(SEXP x, SEXP y) {
    TRACE(TARGpp, x, y);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, SETCADR_MethodID, x, y);
    return checkRef(thisenv, result);
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
    JNIEnv *thisenv = getEnv();
    return (*thisenv)->CallIntMethod(thisenv, UpCallsRFFIObject, RDEBUGMethodID, x);
}

int RSTEP(SEXP x) {
    JNIEnv *thisenv = getEnv();
    return (*thisenv)->CallIntMethod(thisenv, UpCallsRFFIObject, RSTEPMethodID, x);
}

int RTRACE(SEXP x) {
	unimplemented("RTRACE");
    return 0;
}

void SET_RDEBUG(SEXP x, int v) {
    JNIEnv *thisenv = getEnv();
    (*thisenv)->CallVoidMethod(thisenv, UpCallsRFFIObject, SET_RDEBUGMethodID, x, v);
}

void SET_RSTEP(SEXP x, int v) {
    JNIEnv *thisenv = getEnv();
    (*thisenv)->CallVoidMethod(thisenv, UpCallsRFFIObject, SET_RSTEPMethodID, x, v);
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
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, SYMVALUE_MethodID, x);
    return checkRef(thisenv, result);
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
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallVoidMethod(thisenv, UpCallsRFFIObject, SET_SYMVALUE_MethodID, x, v);
}

void SET_INTERNAL(SEXP x, SEXP v) {
    unimplemented("SET_INTERNAL");
}

SEXP FRAME(SEXP x) {
	return unimplemented("FRAME");
}

SEXP ENCLOS(SEXP x) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, ENCLOSMethodID, x);
    return checkRef(thisenv, result);
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
    JNIEnv *thisenv = getEnv();
    return (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, PRCODEMethodID, x);
}

SEXP PRENV(SEXP x) {
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, PRENVMethodID, x);
    return checkRef(thisenv, result);
}

SEXP PRVALUE(SEXP x) {
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, PRVALUEMethodID, x);
    return checkRef(thisenv, result);
}

int PRSEEN(SEXP x) {
    JNIEnv *thisenv = getEnv();
    return (int) (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, PRSEENMethodID, x);
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
    TRACE(TARGp, x);
    JNIEnv *thisenv = getEnv();
    return (int) (*thisenv)->CallIntMethod(thisenv, UpCallsRFFIObject, LENGTH_MethodID, x);
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
	TRACE(TARGp, x);
	JNIEnv *thisenv = getEnv();
	jint *data = (jint *) getNativeArray(thisenv, x, LGLSXP);
	return data;
}

int *INTEGER(SEXP x){
	TRACE(TARGp, x);
	JNIEnv *thisenv = getEnv();
	jint *data = (jint *) getNativeArray(thisenv, x, INTSXP);
	return data;
}


Rbyte *RAW(SEXP x){
	JNIEnv *thisenv = getEnv();
	Rbyte *data = (Rbyte*) getNativeArray(thisenv, x, RAWSXP);
	return data;
}


double *REAL(SEXP x){
    JNIEnv *thisenv = getEnv();
    jdouble *data = (jdouble *) getNativeArray(thisenv, x, REALSXP);
    return data;
}


Rcomplex *COMPLEX(SEXP x){
	unimplemented("COMPLEX");
	return NULL;
}


SEXP STRING_ELT(SEXP x, R_xlen_t i){
	TRACE(TARGpd, x, i);
	JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, STRING_ELT_MethodID, x, i);
    return checkRef(thisenv, result);
}


SEXP VECTOR_ELT(SEXP x, R_xlen_t i){
	TRACE(TARGpd, x, i);
	JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, VECTOR_ELT_MethodID, x, i);
    return checkRef(thisenv, result);
}

void SET_STRING_ELT(SEXP x, R_xlen_t i, SEXP v){
	TRACE("%s(%p, %d, %p)\n", x, i, v);
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallVoidMethod(thisenv, UpCallsRFFIObject, SET_STRING_ELT_MethodID, x, i, v);
}


SEXP SET_VECTOR_ELT(SEXP x, R_xlen_t i, SEXP v){
	TRACE("%s(%p, %d, %p)\n", x, i, v);
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallVoidMethod(thisenv, UpCallsRFFIObject, SET_VECTOR_ELT_MethodID, x, i, v);
	return v;
}


SEXP *STRING_PTR(SEXP x){
	unimplemented("STRING_PTR");
	return NULL;
}


SEXP * NORET VECTOR_PTR(SEXP x){
	unimplemented("VECTOR_PTR");
}

SEXP Rf_asChar(SEXP x){
	TRACE(TARGp, x);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_asCharMethodID, x);
	return checkRef(thisenv, result);
}

SEXP Rf_coerceVector(SEXP x, SEXPTYPE mode){
	TRACE(TARGp, x);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_coerceVectorMethodID, x, mode);
	return checkRef(thisenv, result);
}

SEXP Rf_PairToVectorList(SEXP x){
	TRACE(TARGp, x);
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_PairToVectorListMethodID, x);
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
	TRACE(TARGp, x);
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallIntMethod(thisenv, UpCallsRFFIObject, Rf_asLogicalMethodID, x);
}

int Rf_asInteger(SEXP x) {
	TRACE(TARGp, x);
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallIntMethod(thisenv, UpCallsRFFIObject, Rf_asIntegerMethodID, x);
}

double Rf_asReal(SEXP x) {
	TRACE(TARGp, x);
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallDoubleMethod(thisenv, UpCallsRFFIObject, Rf_asRealMethodID, x);
}

Rcomplex Rf_asComplex(SEXP x){
	unimplemented("Rf_asComplex");
	Rcomplex c; return c;
}

int TYPEOF(SEXP x) {
	TRACE(TARGp, x);
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallIntMethod(thisenv, UpCallsRFFIObject, TYPEOF_MethodID, x);
}

SEXP ATTRIB(SEXP x) {
    TRACE(TARGp, x);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, ATTRIBMethodID, x);
    return checkRef(thisenv, result);
}

int OBJECT(SEXP x){
	JNIEnv *env = getEnv();
	return 	(*env)->CallIntMethod(env, UpCallsRFFIObject, OBJECT_MethodID, x);
}

int MARK(SEXP x){
    unimplemented("MARK");
    return 0;
}

int NAMED(SEXP x){
    JNIEnv *thisenv = getEnv();
    return (*thisenv)->CallIntMethod(thisenv, UpCallsRFFIObject, NAMED_MethodID, x);
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
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, SET_TYPEOF_FASTR_MethodID, x, v);
    return checkRef(thisenv, result);
}

void SET_NAMED(SEXP x, int v){
    JNIEnv *thisenv = getEnv();
    (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, SET_NAMED_FASTR_MethodID, x, v);
}

void SET_ATTRIB(SEXP x, SEXP v){
    unimplemented("SET_ATTRIB");
}

void DUPLICATE_ATTRIB(SEXP to, SEXP from){
    JNIEnv *thisenv = getEnv();
    (*thisenv)->CallVoidMethod(thisenv, UpCallsRFFIObject, DUPLICATE_ATTRIB_MethodID, to, from);
}

const char *R_CHAR(SEXP charsxp) {
	TRACE("%s(%p)", charsxp);
	JNIEnv *thisenv = getEnv();
	const char *copyChars = (const char *) getNativeArray(thisenv, charsxp, CHARSXP);
	TRACE(" %s(%s)\n", copyChars);
	return copyChars;
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
	return 	(*env)->CallIntMethod(env, UpCallsRFFIObject, IS_S4_OBJECTMethodID, x);
}

void SET_S4_OBJECT(SEXP x) {
	JNIEnv *env = getEnv();
	(*env)->CallVoidMethod(env, UpCallsRFFIObject, SET_S4_OBJECTMethodID, x);
}

void UNSET_S4_OBJECT(SEXP x) {
	JNIEnv *env = getEnv();
	(*env)->CallVoidMethod(env, UpCallsRFFIObject, UNSET_S4_OBJECTMethodID, x);
}

Rboolean R_ToplevelExec(void (*fun)(void *), void *data) {
	JNIEnv *env = getEnv();
	jobject handlerStacks = (*env)->CallObjectMethod(env, UpCallsRFFIObject, R_ToplevelExecMethodID);
	fun(data);
	(*env)->CallStaticVoidMethod(env, JNIUpCallsRFFIImplClass, restoreHandlerStacksMethodID, handlerStacks);
	// TODO how do we detect error
	return TRUE;
}

SEXP R_ExecWithCleanup(SEXP (*fun)(void *), void *data,
		       void (*cleanfun)(void *), void *cleandata) {
	return unimplemented("R_ExecWithCleanup");
}

/* Environment and Binding Features */
void R_RestoreHashCount(SEXP rho) {
	unimplemented("R_RestoreHashCount");
}

Rboolean R_IsPackageEnv(SEXP rho) {
	unimplemented("R_IsPackageEnv");
}

SEXP R_PackageEnvName(SEXP rho) {
	return unimplemented("R_PackageEnvName");
}

SEXP R_FindPackageEnv(SEXP info) {
	return unimplemented("R_FindPackageEnv");
}

Rboolean R_IsNamespaceEnv(SEXP rho) {
	return (Rboolean) unimplemented("R_IsNamespaceEnv");
}

SEXP R_NamespaceEnvSpec(SEXP rho) {
	return unimplemented("R_NamespaceEnvSpec");
}

SEXP R_FindNamespace(SEXP info) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, R_FindNamespaceMethodID, info);
	return checkRef(thisenv, result);
}

void R_LockEnvironment(SEXP env, Rboolean bindings) {
	unimplemented("R_LockEnvironment");
}

Rboolean R_EnvironmentIsLocked(SEXP env) {
	unimplemented("");
}

void R_LockBinding(SEXP sym, SEXP env) {
	unimplemented("R_LockBinding");
}

void R_unLockBinding(SEXP sym, SEXP env) {
	unimplemented("R_unLockBinding");
}

void R_MakeActiveBinding(SEXP sym, SEXP fun, SEXP env) {
	unimplemented("R_MakeActiveBinding");
}

Rboolean R_BindingIsLocked(SEXP sym, SEXP env) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallIntMethod(thisenv, UpCallsRFFIObject, R_BindingIsLockedID, sym, env);
}

Rboolean R_BindingIsActive(SEXP sym, SEXP env) {
    // TODO: for now, I believe all bindings are false
    return (Rboolean)0;
}

Rboolean R_HasFancyBindings(SEXP rho) {
	return (Rboolean) unimplemented("R_HasFancyBindings");
}

Rboolean Rf_isS4(SEXP x) {
    return IS_S4_OBJECT(x);
}

SEXP Rf_asS4(SEXP x, Rboolean b, int i) {
	unimplemented("Rf_asS4");
}

static SEXP R_tryEvalInternal(SEXP x, SEXP y, int *ErrorOccurred, jboolean silent) {
	JNIEnv *thisenv = getEnv();
    updateNativeArrays(thisenv);
	jobject tryResult =  (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, R_tryEvalMethodID, x, y, (int) silent);
	// If tryResult is NULL, an error occurred
	if (ErrorOccurred) {
		*ErrorOccurred = tryResult == NULL;
	}
	return checkRef(thisenv, tryResult);
}

SEXP R_tryEval(SEXP x, SEXP y, int *ErrorOccurred) {
	return R_tryEvalInternal(x, y, ErrorOccurred, JNI_FALSE);
}

SEXP R_tryEvalSilent(SEXP x, SEXP y, int *ErrorOccurred) {
	return R_tryEvalInternal(x, y, ErrorOccurred, JNI_TRUE);
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
    JNIEnv *thisenv = getEnv();
    return (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, R_PromiseExprMethodID, x);
}

SEXP R_ClosureExpr(SEXP x) {
	return unimplemented("R_ClosureExpr");
}

SEXP R_forceAndCall(SEXP e, int n, SEXP rho) {
	return unimplemented("R_forceAndCall");
}

SEXP R_MakeExternalPtr(void *p, SEXP tag, SEXP prot) {
	JNIEnv *thisenv = getEnv();
	SEXP result =  (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, R_MakeExternalPtrMethodID, (jlong) p, tag, prot);
    return checkRef(thisenv, result);
}

void *R_ExternalPtrAddr(SEXP s) {
	JNIEnv *thisenv = getEnv();
	return (void *) (*thisenv)->CallLongMethod(thisenv, UpCallsRFFIObject, R_ExternalPtrAddrMethodID, s);
}

SEXP R_ExternalPtrTag(SEXP s) {
	JNIEnv *thisenv = getEnv();
	SEXP result =  (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, R_ExternalPtrTagMethodID, s);
    return checkRef(thisenv, result);
}

SEXP R_ExternalPtrProt(SEXP s) {
	JNIEnv *thisenv = getEnv();
	SEXP result =  (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, R_ExternalPtrProtectedMethodID, s);
    return checkRef(thisenv, result);
}

void R_SetExternalPtrAddr(SEXP s, void *p) {
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallVoidMethod(thisenv, UpCallsRFFIObject, R_SetExternalPtrAddrMethodID, s, (jlong) p);
}

void R_SetExternalPtrTag(SEXP s, SEXP tag) {
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallVoidMethod(thisenv, UpCallsRFFIObject, R_SetExternalPtrTagMethodID, s, tag);
}

void R_SetExternalPtrProtected(SEXP s, SEXP p) {
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallVoidMethod(thisenv, UpCallsRFFIObject, R_SetExternalPtrProtMethodID, s, p);
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

SEXP R_MakeWeakRef(SEXP key, SEXP val, SEXP fin, Rboolean onexit) {
	unimplemented("R_MakeWeakRef");
}

SEXP R_MakeWeakRefC(SEXP key, SEXP val, R_CFinalizer_t fin, Rboolean onexit) {
	unimplemented("R_MakeWeakRefC");
}

SEXP R_WeakRefKey(SEXP w) {
	unimplemented("R_WeakRefKey");
}

SEXP R_WeakRefValue(SEXP w) {
	unimplemented("R_WeakRefValue");
}

void R_RunWeakRefFinalizer(SEXP w) {
	// TODO implement, but not fail for now
}

SEXP R_do_slot(SEXP obj, SEXP name) {
    TRACE(TARGp, obj, name);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, R_do_slot_MethodID, obj, name);
    return checkRef(thisenv, result);
}

SEXP R_do_slot_assign(SEXP obj, SEXP name, SEXP value) { // Same like R_set_slot
    TRACE(TARGp, obj, name, value);
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, R_do_slot_assign_MethodID, obj, name, value);
    return checkRef(thisenv, result);
}

int R_has_slot(SEXP obj, SEXP name) {
	return (int) unimplemented("R_has_slot");
}

SEXP R_do_MAKE_CLASS(const char *what) {
	JNIEnv *thisenv = getEnv();
	jstring string = (*thisenv)->NewStringUTF(thisenv, what);
	return (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, R_do_MAKE_CLASS_MethodID, string);
}

SEXP R_getClassDef (const char *what) {
	return unimplemented("R_getClassDef");
}

SEXP R_do_new_object(SEXP class_def) {
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, R_do_new_object_MethodID, class_def);
    return checkRef(thisenv, result);
}

SEXPTYPE Rf_str2type(const char *name) {
    JNIEnv *thisenv = getEnv();
    jstring jsName = (*thisenv)->NewStringUTF(thisenv, name);
    return (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, Rf_str2type_MethodID, jsName);
}

static SEXP jniGetMethodsNamespace() {
    JNIEnv *thisenv = getEnv();
    return (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, R_MethodsNamespaceMethodID);
}

int R_check_class_etc (SEXP x, const char **valid) {
	return R_check_class_etc_helper(x, valid, jniGetMethodsNamespace);
}

SEXP R_PreserveObject_FASTR(SEXP x) {
	// convert to a JNI global ref until explicitly released
	return createGlobalRef(getEnv(), x, 0);
}

void R_ReleaseObject(SEXP x) {
	releaseGlobalRef(getEnv(), x);
}

void R_dot_Last(void) {
	unimplemented("R_dot_Last");
}


Rboolean R_compute_identical(SEXP x, SEXP y, int flags) {
	JNIEnv *thisenv = getEnv();
	return (*thisenv)->CallIntMethod(thisenv, UpCallsRFFIObject, R_compute_identicalMethodID, x, y, flags);
}

void Rf_copyListMatrix(SEXP s, SEXP t, Rboolean byrow) {
    JNIEnv *thisenv = getEnv();
    (*thisenv)->CallIntMethod(thisenv, UpCallsRFFIObject, Rf_copyListMatrixMethodID, s, t, byrow);
}

void Rf_copyMatrix(SEXP s, SEXP t, Rboolean byrow) {
    JNIEnv *thisenv = getEnv();
    (*thisenv)->CallIntMethod(thisenv, UpCallsRFFIObject, Rf_copyMatrixMethodID, s, t, byrow);
}
