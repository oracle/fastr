/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
#ifndef CALLBACKS_H
#define CALLBACKS_H

#define CADDR_x 0
#define CADR_x 1
#define CAR_x 2
#define CDDR_x 3
#define CDR_x 4
#define DUPLICATE_ATTRIB_x 5
#define ENCLOS_x 6
#define GetRNGstate_x 7
#define INTEGER_x 8
#define IS_S4_OBJECT_x 9
#define LENGTH_x 10
#define LOGICAL_x 11
#define NAMED_x 12
#define OBJECT_x 13
#define PRCODE_x 14
#define PRENV_x 15
#define PRINTNAME_x 16
#define PRSEEN_x 17
#define PRVALUE_x 18
#define PutRNGstate_x 19
#define RAW_x 20
#define RDEBUG_x 21
#define REAL_x 22
#define RSTEP_x 23
#define R_BaseEnv_x 24
#define R_BaseNamespace_x 25
#define R_BindingIsLocked_x 26
#define R_CHAR_x 27
#define R_CleanUp_x 28
#define R_ExternalPtrAddr_x 29
#define R_ExternalPtrProt_x 30
#define R_ExternalPtrTag_x 31
#define R_FindNamespace_x 32
#define R_GetConnection_x 33
#define R_GlobalContext_x 34
#define R_GlobalEnv_x 35
#define R_HomeDir_x 36
#define R_Interactive_x 37
#define R_MakeExternalPtr_x 38
#define R_NamespaceRegistry_x 39
#define R_NewHashedEnv_x 40
#define R_ParseVector_x 41
#define R_PromiseExpr_x 42
#define R_ReadConnection_x 43
#define R_SetExternalPtrAddr_x 44
#define R_SetExternalPtrProt_x 45
#define R_SetExternalPtrTag_x 46
#define R_ToplevelExec_x 47
#define R_WriteConnection_x 48
#define R_computeIdentical_x 49
#define R_do_MAKE_CLASS_x 50
#define R_getContextCall_x 51
#define R_getContextEnv_x 52
#define R_getContextFun_x 53
#define R_getContextSrcRef_x 54
#define R_getGlobalFunctionContext_x 55
#define R_getParentFunctionContext_x 56
#define R_insideBrowser_x 57
#define R_isEqual_x 58
#define R_isGlobal_x 59
#define R_lsInternal3_x 60
#define R_new_custom_connection_x 61
#define R_tryEval_x 62
#define Rf_GetOption1_x 63
#define Rf_PairToVectorList_x 64
#define Rf_ScalarDouble_x 65
#define Rf_ScalarInteger_x 66
#define Rf_ScalarLogical_x 67
#define Rf_ScalarString_x 68
#define Rf_allocateArray_x 69
#define Rf_allocateMatrix_x 70
#define Rf_allocateVector_x 71
#define Rf_anyDuplicated_x 72
#define Rf_asChar_x 73
#define Rf_asInteger_x 74
#define Rf_asLogical_x 75
#define Rf_asReal_x 76
#define Rf_classgets_x 77
#define Rf_coerceVector_x 78
#define Rf_cons_x 79
#define Rf_copyListMatrix_x 80
#define Rf_copyMatrix_x 81
#define Rf_defineVar_x 82
#define Rf_duplicate_x 83
#define Rf_error_x 84
#define Rf_eval_x 85
#define Rf_findVar_x 86
#define Rf_findVarInFrame_x 87
#define Rf_findVarInFrame3_x 88
#define Rf_findfun_x 89
#define Rf_getAttrib_x 90
#define Rf_gsetVar_x 91
#define Rf_inherits_x 92
#define Rf_install_x 93
#define Rf_installChar_x 94
#define Rf_isNull_x 95
#define Rf_isString_x 96
#define Rf_lengthgets_x 97
#define Rf_mkCharLenCE_x 98
#define Rf_ncols_x 99
#define Rf_nrows_x 100
#define Rf_setAttrib_x 101
#define Rf_warning_x 102
#define Rf_warningcall_x 103
#define Rprintf_x 104
#define SETCADR_x 105
#define SETCAR_x 106
#define SETCDR_x 107
#define SET_RDEBUG_x 108
#define SET_RSTEP_x 109
#define SET_STRING_ELT_x 110
#define SET_SYMVALUE_x 111
#define SET_TAG_x 112
#define SET_TYPEOF_FASTR_x 113
#define SET_VECTOR_ELT_x 114
#define STRING_ELT_x 115
#define SYMVALUE_x 116
#define TAG_x 117
#define TYPEOF_x 118
#define VECTOR_ELT_x 119
#define getConnectionClassString_x 120
#define getOpenModeString_x 121
#define getSummaryDescription_x 122
#define isSeekable_x 123
#define unif_rand_x 124

#define CALLBACK_TABLE_SIZE 125

extern void* callbacks[];

// TODO Use an array indexed by above

// This is the complete set , including those not yet implemented

typedef SEXP (*call_Rf_ScalarInteger)(int value);
typedef SEXP (*call_Rf_ScalarReal)(double value);
typedef SEXP (*call_Rf_ScalarString)(SEXP value);
typedef SEXP (*call_Rf_ScalarLogical)(int value);
typedef SEXP (*call_Rf_allocateVector)(SEXPTYPE t, R_xlen_t len);
typedef SEXP (*call_Rf_allocArray)(SEXPTYPE t, SEXP dims);
typedef SEXP (*call_Rf_alloc3DArray)(SEXPTYPE t, int x, int y, int z);
typedef SEXP (*call_Rf_allocMatrix)(SEXPTYPE mode, int nrow, int ncol);
typedef SEXP (*call_Rf_allocList)(int x);
typedef SEXP (*call_Rf_allocSExp)(SEXPTYPE t);
typedef SEXP (*call_Rf_cons)(SEXP car, SEXP cdr);
typedef void (*call_Rf_defineVar)(SEXP symbol, SEXP value, SEXP rho);
typedef void (*call_Rf_setVar)(SEXP x, SEXP y, SEXP z);
typedef SEXP (*call_Rf_dimgets)(SEXP x, SEXP y);
typedef SEXP (*call_Rf_dimnamesgets)(SEXP x, SEXP y);
typedef SEXP (*call_Rf_eval)(SEXP expr, SEXP env);
typedef SEXP (*call_Rf_findFun)(SEXP symbol, SEXP rho);
typedef SEXP (*call_Rf_findVar)(SEXP sym, SEXP rho);
typedef SEXP (*call_Rf_findVarInFrame)(SEXP rho, SEXP sym);
typedef SEXP (*call_Rf_findVarInFrame3)(SEXP rho, SEXP sym, Rboolean b);
typedef SEXP (*call_Rf_getAttrib)(SEXP vec, SEXP name);
typedef SEXP (*call_Rf_GetOption1)(SEXP tag);
typedef SEXP (*call_Rf_setAttrib)(SEXP vec, SEXP name, SEXP val);
typedef SEXP (*call_Rf_duplicate)(SEXP x);
typedef SEXP (*call_Rf_shallow_duplicate)(SEXP x);
typedef SEXP (*call_Rf_coerceVector)(SEXP x, SEXPTYPE mode);
typedef R_xlen_t (*call_Rf_any_duplicated)(SEXP x, Rboolean from_last);
typedef SEXP (*call_Rf_duplicated)(SEXP x, Rboolean y);
typedef SEXP (*call_Rf_applyClosure)(SEXP x, SEXP y, SEXP z, SEXP a, SEXP b);
typedef void (*call_Rf_copyMostAttrib)(SEXP x, SEXP y);
typedef void (*call_Rf_copyVector)(SEXP x, SEXP y);
typedef int (*call_Rf_countContexts)(int x, int y);
typedef Rboolean (*call_Rf_inherits)(SEXP x, const char * klass);
typedef Rboolean (*call_Rf_isReal)(SEXP x);
typedef Rboolean (*call_Rf_isSymbol)(SEXP x);
typedef Rboolean (*call_Rf_isComplex)(SEXP x);
typedef Rboolean (*call_Rf_isEnvironment)(SEXP x);
typedef Rboolean (*call_Rf_isExpression)(SEXP x);
typedef Rboolean (*call_Rf_isLogical)(SEXP x);
typedef Rboolean (*call_Rf_isObject)(SEXP s);
typedef void (*call_Rf_PrintValue)(SEXP x);
typedef SEXP (*call_Rf_install)(const char *name);
typedef SEXP (*call_Rf_installChar)(SEXP charsxp);
typedef Rboolean (*call_Rf_isNull)(SEXP s);
typedef Rboolean (*call_Rf_isString)(SEXP s);
typedef Rboolean (*call_R_cycle_detected)(SEXP s, SEXP child);
typedef cetype_t (*call_Rf_getCharCE)(SEXP x);
typedef SEXP (*call_Rf_mkChar)(const char *x);
typedef SEXP (*call_Rf_mkCharCE)(const char *x, cetype_t y);
typedef SEXP (*call_Rf_mkCharLen)(const char *x, int y);
typedef SEXP (*call_Rf_mkCharLenCE)(const char *x, int len, cetype_t enc);
typedef const char * (*call_Rf_reEnc)(const char *x, cetype_t ce_in, cetype_t ce_out, int subst);
typedef SEXP (*call_Rf_mkString)(const char *s);
typedef int (*call_Rf_ncols)(SEXP x);
typedef int (*call_Rf_nrows)(SEXP x);
typedef SEXP (*call_Rf_protect)(SEXP x);
typedef void (*call_Rf_unprotect)(int x);
typedef void (*call_R_ProtectWithIndex)(SEXP x, PROTECT_INDEX *y);
typedef void (*call_R_Reprotect)(SEXP x, PROTECT_INDEX y);
typedef void (*call_Rf_unprotect_ptr)(SEXP x);
typedef void (*call_Rf_error)(const char *format, ...);
typedef void (*call_Rf_errorcall)(SEXP x, const char *format, ...);
typedef void (*call_Rf_warningcall)(SEXP x, const char *format, ...);
typedef void (*call_Rf_warning)(const char *format, ...);
typedef void (*call_Rprintf)(const char *format, ...);
typedef void (*call_Rvprintf)(const char *format, va_list args);
typedef void (*call_REvprintf)(const char *format, va_list args);
typedef void (*call_R_FlushConsole)(void);
typedef void (*call_R_ProcessEvents)(void);
typedef SEXP (*call_R_NewHashedEnv)(SEXP parent, SEXP size);
typedef SEXP (*call_Rf_classgets)(SEXP vec, SEXP klass);
typedef const char *(*call_Rf_translateChar)(SEXP x);
typedef const char *(*call_Rf_translateChar0)(SEXP x);
typedef const char *(*call_Rf_translateCharUTF8)(SEXP x);
typedef SEXP (*call_Rf_lengthgets)(SEXP x, R_len_t y);
typedef SEXP (*call_Rf_xlengthgets)(SEXP x, R_xlen_t y);
typedef SEXP (*call_R_lsInternal)(SEXP env, Rboolean all);
typedef SEXP (*call_R_lsInternal3)(SEXP env, Rboolean all, Rboolean sorted);
typedef SEXP (*call_Rf_namesgets)(SEXP x, SEXP y);
typedef SEXP (*call_TAG)(SEXP e);
typedef SEXP (*call_PRINTNAME)(SEXP e);
typedef SEXP (*call_CAR)(SEXP e);
typedef SEXP (*call_CDR)(SEXP e);
typedef SEXP (*call_CAAR)(SEXP e);
typedef SEXP (*call_CDAR)(SEXP e);
typedef SEXP (*call_CADR)(SEXP e);
typedef SEXP (*call_CDDR)(SEXP e);
typedef SEXP (*call_CDDDR)(SEXP e);
typedef SEXP (*call_CADDR)(SEXP e);
typedef SEXP (*call_CADDDR)(SEXP e);
typedef SEXP (*call_CAD4R)(SEXP e);
typedef int (*call_MISSING)(SEXP x);
typedef void (*call_SET_MISSING)(SEXP x, int v);
typedef void (*call_SET_TAG)(SEXP x, SEXP y);
typedef SEXP (*call_SETCAR)(SEXP x, SEXP y);
typedef SEXP (*call_SETCDR)(SEXP x, SEXP y);
typedef SEXP (*call_SETCADR)(SEXP x, SEXP y);
typedef SEXP (*call_SETCADDR)(SEXP x, SEXP y);
typedef SEXP (*call_SETCADDDR)(SEXP x, SEXP y);
typedef SEXP (*call_SETCAD4R)(SEXP e, SEXP y);
typedef SEXP (*call_FORMALS)(SEXP x);
typedef SEXP (*call_BODY)(SEXP x);
typedef SEXP (*call_CLOENV)(SEXP x);
typedef int (*call_RDEBUG)(SEXP x);
typedef int (*call_RSTEP)(SEXP x);
typedef int (*call_RTRACE)(SEXP x);
typedef void (*call_SET_RDEBUG)(SEXP x, int v);
typedef void (*call_SET_RSTEP)(SEXP x, int v);
typedef void (*call_SET_RTRACE)(SEXP x, int v);
typedef void (*call_SET_FORMALS)(SEXP x, SEXP v);
typedef void (*call_SET_BODY)(SEXP x, SEXP v);
typedef void (*call_SET_CLOENV)(SEXP x, SEXP v);
typedef SEXP (*call_SYMVALUE)(SEXP x);
typedef SEXP (*call_INTERNAL)(SEXP x);
typedef int (*call_DDVAL)(SEXP x);
typedef void (*call_SET_DDVAL)(SEXP x, int v);
typedef void (*call_SET_SYMVALUE)(SEXP x, SEXP v);
typedef void (*call_SET_INTERNAL)(SEXP x, SEXP v);
typedef SEXP (*call_FRAME)(SEXP x);
typedef SEXP (*call_ENCLOS)(SEXP x);
typedef SEXP (*call_HASHTAB)(SEXP x);
typedef int (*call_ENVFLAGS)(SEXP x);
typedef void (*call_SET_ENVFLAGS)(SEXP x, int v);
typedef void (*call_SET_FRAME)(SEXP x, SEXP v);
typedef void (*call_SET_ENCLOS)(SEXP x, SEXP v);
typedef void (*call_SET_HASHTAB)(SEXP x, SEXP v);
typedef SEXP (*call_PRCODE)(SEXP x);
typedef SEXP (*call_PRENV)(SEXP x);
typedef SEXP (*call_PRVALUE)(SEXP x);
typedef int (*call_PRSEEN)(SEXP x);
typedef void (*call_SET_PRSEEN)(SEXP x, int v);
typedef void (*call_SET_PRENV)(SEXP x, SEXP v);
typedef void (*call_SET_PRVALUE)(SEXP x, SEXP v);
typedef void (*call_SET_PRCODE)(SEXP x, SEXP v);
typedef int (*call_LENGTH)(SEXP x);
typedef int (*call_TRUELENGTH)(SEXP x);
typedef void (*call_SETLENGTH)(SEXP x, int v);
typedef void (*call_SET_TRUELENGTH)(SEXP x, int v);
typedef R_xlen_t (*call_XLENGTH)(SEXP x);
typedef R_xlen_t (*call_XTRUELENGTH)(SEXP x);
typedef int (*call_IS_LONG_VEC)(SEXP x);
typedef int (*call_LEVELS)(SEXP x);
typedef int (*call_SETLEVELS)(SEXP x, int v);
typedef int *(*call_LOGICAL)(SEXP x);
typedef int *(*call_INTEGER)(SEXP x);
typedef Rbyte *(*call_RAW)(SEXP x);
typedef double *(*call_REAL)(SEXP x);
typedef Rcomplex *(*call_COMPLEX)(SEXP x);
typedef SEXP (*call_STRING_ELT)(SEXP x, R_xlen_t i);
typedef SEXP (*call_VECTOR_ELT)(SEXP x, R_xlen_t i);
typedef void (*call_SET_STRING_ELT)(SEXP x, R_xlen_t i, SEXP v);
typedef SEXP (*call_SET_VECTOR_ELT)(SEXP x, R_xlen_t i, SEXP v);
typedef SEXP *(*call_STRING_PTR)(SEXP x);
typedef SEXP *(*call_VECTOR_PTR)(SEXP x);
typedef SEXP (*call_Rf_asChar)(SEXP x);
typedef SEXP (*call_Rf_PairToVectorList)(SEXP x);
typedef SEXP (*call_Rf_VectorToPairList)(SEXP x);
typedef SEXP (*call_Rf_asCharacterFactor)(SEXP x);
typedef int (*call_Rf_asLogical)(SEXP x);
typedef int (*call_Rf_asInteger)(SEXP x);
typedef double (*call_Rf_asReal)(SEXP x);
typedef Rcomplex (*call_Rf_asComplex)(SEXP x);
typedef int (*call_TYPEOF)(SEXP x);
typedef SEXP (*call_ATTRIB)(SEXP x);
typedef int (*call_OBJECT)(SEXP x);
typedef int (*call_MARK)(SEXP x);
typedef int (*call_NAMED)(SEXP x);
typedef int (*call_REFCNT)(SEXP x);
typedef void (*call_SET_OBJECT)(SEXP x, int v);
typedef void (*call_SET_TYPEOF)(SEXP x, int v);
typedef SEXP (*call_SET_TYPEOF_FASTR)(SEXP x, int v);
typedef void (*call_SET_NAMED)(SEXP x, int v);
typedef void (*call_SET_ATTRIB)(SEXP x, SEXP v);
typedef void (*call_DUPLICATE_ATTRIB)(SEXP to, SEXP from);
typedef int (*call_IS_S4_OBJECT)(SEXP x);
typedef void (*call_SET_S4_OBJECT)(SEXP x);
typedef void (*call_UNSET_S4_OBJECT)(SEXP x);
typedef Rboolean (*call_R_ToplevelExec)(void (*fun)(void *), void *data);
typedef void (*call_R_RestoreHashCount)(SEXP rho);
typedef Rboolean (*call_R_IsPackageEnv)(SEXP rho);
typedef SEXP (*call_R_PackageEnvName)(SEXP rho);
typedef SEXP (*call_R_FindPackageEnv)(SEXP info);
typedef Rboolean (*call_R_IsNamespaceEnv)(SEXP rho);
typedef SEXP (*call_R_NamespaceEnvSpec)(SEXP rho);
typedef SEXP (*call_R_FindNamespace)(SEXP info);
typedef void (*call_R_LockEnvironment)(SEXP env, Rboolean bindings);
typedef Rboolean (*call_R_EnvironmentIsLocked)(SEXP env);
typedef void (*call_R_LockBinding)(SEXP sym, SEXP env);
typedef void (*call_R_unLockBinding)(SEXP sym, SEXP env);
typedef void (*call_R_MakeActiveBinding)(SEXP sym, SEXP fun, SEXP env);
typedef Rboolean (*call_R_BindingIsLocked)(SEXP sym, SEXP env);
typedef Rboolean (*call_R_BindingIsActive)(SEXP sym, SEXP env);
typedef Rboolean (*call_R_HasFancyBindings)(SEXP rho);
typedef Rboolean (*call_Rf_isS4)(SEXP x);
typedef SEXP (*call_Rf_asS4)(SEXP x, Rboolean b, int i);
typedef SEXP (*call_R_tryEval)(SEXP x, SEXP y, int *ErrorOccurred);
typedef SEXP (*call_R_tryEvalSilent)(SEXP x, SEXP y, int *ErrorOccurred);
typedef double (*call_R_atof)(const char *str);
typedef double (*call_R_strtod)(const char *c, char **end);
typedef SEXP (*call_R_PromiseExpr)(SEXP x);
typedef SEXP (*call_R_ClosureExpr)(SEXP x);
typedef SEXP (*call_R_forceAndCall)(SEXP e, int n, SEXP rho);
typedef SEXP (*call_R_MakeExternalPtr)(void *p, SEXP tag, SEXP prot);
typedef void *(*call_R_ExternalPtrAddr)(SEXP s);
typedef SEXP (*call_R_ExternalPtrTag)(SEXP s);
typedef SEXP (*call_R_ExternalPtrProt)(SEXP s);
typedef void (*call_R_SetExternalPtrAddr)(SEXP s, void *p);
typedef void (*call_R_SetExternalPtrTag)(SEXP s, SEXP tag);
typedef void (*call_R_SetExternalPtrProtected)(SEXP s, SEXP p);
typedef void (*call_R_ClearExternalPtr)(SEXP s);
typedef void (*call_R_RegisterFinalizer)(SEXP s, SEXP fun);
typedef void (*call_R_RegisterCFinalizer)(SEXP s, R_CFinalizer_t fun);
typedef void (*call_R_RegisterFinalizerEx)(SEXP s, SEXP fun, Rboolean onexit);
typedef void (*call_R_RegisterCFinalizerEx)(SEXP s, R_CFinalizer_t fun, Rboolean onexit);
typedef void (*call_R_RunPendingFinalizers)(void);
typedef SEXP (*call_R_MakeWeakRef)(SEXP key, SEXP val, SEXP fin, Rboolean onexit);
typedef SEXP (*call_R_MakeWeakRefC)(SEXP key, SEXP val, R_CFinalizer_t fin, Rboolean onexit);
typedef SEXP (*call_R_WeakRefKey)(SEXP w);
typedef SEXP (*call_R_WeakRefValue)(SEXP w);
typedef void (*call_R_RunWeakRefFinalizer)(SEXP w);
typedef SEXP (*call_R_do_slot)(SEXP obj, SEXP name);
typedef SEXP (*call_R_do_slot_assign)(SEXP obj, SEXP name, SEXP value);
typedef int (*call_R_has_slot)(SEXP obj, SEXP name);
typedef SEXP (*call_R_do_MAKE_CLASS)(const char *what);
typedef SEXP (*call_R_getClassDef )(const char *what);
typedef SEXP (*call_R_do_new_object)(SEXP class_def);
typedef int (*call_R_check_class_and_super)(SEXP x, const char **valid, SEXP rho);
typedef int (*call_R_check_class_etc )(SEXP x, const char **valid);
typedef SEXP (*call_R_PreserveObject)(SEXP x);
typedef void (*call_R_ReleaseObject)(SEXP x);
typedef void (*call_R_dot_Last)(void);
typedef Rboolean (*call_R_compute_identical)(SEXP x, SEXP y, int flags);
typedef void (*call_Rf_copyListMatrix)(SEXP s, SEXP t, Rboolean byrow);
typedef void (*call_Rf_copyMatrix)(SEXP s, SEXP t, Rboolean byrow);
typedef void (*call_GetRNGstate)();
typedef void (*call_PutRNGstate)();
typedef SEXP (*call_R_BaseEnv)();
typedef SEXP (*call_R_BaseNamespace)();
typedef SEXP (*call_R_GlobalEnv)();
typedef SEXP (*call_R_NamespaceRegistry)();
typedef SEXP (*call_R_Interactive)();
typedef SEXP (*call_R_GlobalContext)();
typedef SEXP (*call_R_CHAR)(SEXP x);
typedef char *(*call_R_HomeDir)();
typedef void (*call_R_CleanUp)(int sa, int status, int runlast);
typedef void (*call_Rf_gsetVar)(SEXP symbol, SEXP value, SEXP rho);
typedef double (*call_unif_rand)();

#endif

