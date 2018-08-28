/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
#ifndef CALLBACKS_H
#define CALLBACKS_H

#include "rffi_upcallsindex.h"
#include <Rdynload.h>

extern __thread void* *callbacks;

// This is the complete set , including those not yet implemented

typedef SEXP (*call_Rf_ScalarComplex)(double real, double imag);
typedef SEXP (*call_Rf_ScalarInteger)(int value);
typedef SEXP (*call_Rf_ScalarLogical)(int value);
typedef SEXP (*call_Rf_ScalarRaw)(int value);
typedef SEXP (*call_Rf_ScalarReal)(double value);
typedef SEXP (*call_Rf_ScalarString)(SEXP value);
typedef SEXP (*call_Rf_allocVector)(SEXPTYPE t, R_xlen_t len);
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
typedef SEXP (*call_Rf_duplicate)(SEXP x, int v);
typedef SEXP (*call_Rf_shallow_duplicate)(SEXP x);
typedef SEXP (*call_Rf_coerceVector)(SEXP x, SEXPTYPE mode);
typedef R_xlen_t (*call_Rf_any_duplicated)(SEXP x, Rboolean from_last);
typedef R_xlen_t (*call_Rf_any_duplicated3)(SEXP x, SEXP y, Rboolean from_last);
typedef SEXP (*call_Rf_duplicated)(SEXP x, Rboolean y);
typedef SEXP (*call_Rf_applyClosure)(SEXP x, SEXP y, SEXP z, SEXP a, SEXP b);
typedef int (*call_Rf_copyMostAttrib)(SEXP x, SEXP y);
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
typedef int (*call_LEVELS)(SEXP x);
typedef int (*call_IS_LONG_VEC)(SEXP x);
typedef int (*call_LEVELS)(SEXP x);
typedef int (*call_SETLEVELS)(SEXP x, int v);
typedef int *(*call_LOGICAL)(SEXP x);
typedef int *(*call_INTEGER)(SEXP x);
typedef void *(*call_FASTR_DATAPTR)(SEXP x);
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
typedef void (*call_SET_NAMED)(SEXP x, int v);
typedef void (*call_SET_ATTRIB)(SEXP x, SEXP v);
typedef void (*call_DUPLICATE_ATTRIB)(SEXP to, SEXP from);
typedef int (*call_IS_S4_OBJECT)(SEXP x);
typedef void (*call_SET_S4_OBJECT)(SEXP x);
typedef void (*call_UNSET_S4_OBJECT)(SEXP x);
typedef SEXP (*call_R_ToplevelExec)();
typedef void (*call_restoreHandlerStack)(SEXP saved_handler_stack);
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
typedef SEXP (*call_R_tryEval)(SEXP x, SEXP y, int *ErrorOccurred, int silent);
typedef double (*call_R_atof)(const char *str);
typedef double (*call_R_strtod)(const char *c, char **end);
typedef SEXP (*call_R_PromiseExpr)(SEXP x);
typedef SEXP (*call_R_ClosureExpr)(SEXP x);
typedef SEXP (*call_R_forceAndCall)(SEXP e, SEXP f, int n, SEXP rho);
typedef SEXP (*call_R_MakeExternalPtr)(void *p, SEXP tag, SEXP prot);
typedef void *(*call_R_ExternalPtrAddr)(SEXP s);
typedef SEXP (*call_R_ExternalPtrTag)(SEXP s);
typedef SEXP (*call_R_ExternalPtrProtected)(SEXP s);
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
typedef void (*call_R_dot_Last)(void);
typedef Rboolean (*call_R_compute_identical)(SEXP x, SEXP y, int flags);
typedef void (*call_Rf_copyListMatrix)(SEXP s, SEXP t, Rboolean byrow);
typedef void (*call_Rf_copyMatrix)(SEXP s, SEXP t, Rboolean byrow);
typedef void (*call_GetRNGstate)();
typedef void (*call_PutRNGstate)();
typedef SEXP (*call_R_BaseEnv)();
typedef SEXP (*call_R_BaseNamespace)();
typedef SEXP (*call_R_MethodsNamespace)();
typedef SEXP (*call_R_GlobalEnv)();
typedef SEXP (*call_R_NamespaceRegistry)();
typedef int (*call_R_Interactive)();
typedef SEXP (*call_R_GlobalContext)();
typedef SEXP (*call_R_CHAR)(SEXP x);
typedef char *(*call_R_HomeDir)();
typedef void (*call_R_CleanUp)(int sa, int status, int runlast);
typedef void (*call_Rf_gsetVar)(SEXP symbol, SEXP value, SEXP rho);
typedef double (*call_unif_rand)();
typedef double (*call_Rf_qunif)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_dunif)(double a, double b, double c, int d);
typedef double (*call_Rf_punif)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_runif)(double x, double y);
typedef double (*call_Rf_dchisq)(double a, double b, int c);
typedef double (*call_Rf_pchisq)(double a, double b, int c, int d);
typedef double (*call_Rf_qchisq)(double a, double b, int c, int d);
typedef double (*call_Rf_rchisq)(double a);
typedef double (*call_Rf_dnchisq)(double a, double b, double c, int d);
typedef double (*call_Rf_pnchisq)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_qnchisq)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_rnchisq)(double a, double b);
typedef double (*call_Rf_dnorm)(double a, double b, double c, int d);
typedef double (*call_Rf_pnorm)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_qnorm)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_rnorm)(double a, double b);
typedef void (*call_Rf_pnorm_both)(double a, double* b, double* c, int d, int e);
typedef double (*call_Rf_dlnorm)(double a, double b, double c, int d);
typedef double (*call_Rf_plnorm)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_qlnorm)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_rlnorm)(double a, double b);
typedef double (*call_Rf_dgamma)(double a, double b, double c, int d);
typedef double (*call_Rf_pgamma)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_qgamma)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_rgamma)(double a, double b);
typedef double (*call_Rf_log1pmx)(double a);
typedef double (*call_Rf_log1pexp)(double a);
typedef double (*call_Rf_lgamma1p)(double a);
typedef double (*call_Rf_logspace_add)(double a, double b);
typedef double (*call_Rf_logspace_sub)(double a, double b);
typedef double (*call_Rf_dbeta)(double a, double b, double c, int d);
typedef double (*call_Rf_pbeta)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_qbeta)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_rbeta)(double a, double b);
typedef double (*call_Rf_df)(double a, double b, double c, int d);
typedef double (*call_Rf_pf)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_qf)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_rf)(double a, double b);
typedef double (*call_Rf_dt)(double a, double b, int c);
typedef double (*call_Rf_pt)(double a, double b, int c, int d);
typedef double (*call_Rf_qt)(double a, double b, int c, int d);
typedef double (*call_Rf_rt)(double a);
typedef double (*call_Rf_dbinom)(double a, double b, double c, int d);
typedef double (*call_Rf_pbinom)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_qbinom)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_rbinom)(double a, double b);
typedef double (*call_Rf_dcauchy)(double a, double b, double c, int d);
typedef double (*call_Rf_pcauchy)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_qcauchy)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_rcauchy)(double a, double b);
typedef double (*call_Rf_dexp)(double a, double b, int c);
typedef double (*call_Rf_pexp)(double a, double b, int c, int d);
typedef double (*call_Rf_qexp)(double a, double b, int c, int d);
typedef double (*call_Rf_rexp)(double a);
typedef double (*call_Rf_dgeom)(double a, double b, int c);
typedef double (*call_Rf_pgeom)(double a, double b, int c, int d);
typedef double (*call_Rf_qgeom)(double a, double b, int c, int d);
typedef double (*call_Rf_rgeom)(double a);
typedef double (*call_Rf_dhyper)(double a, double b, double c, double d, int e);
typedef double (*call_Rf_phyper)(double a, double b, double c, double d, int e, int f);
typedef double (*call_Rf_qhyper)(double a, double b, double c, double d, int e, int f);
typedef double (*call_Rf_rhyper)(double a, double b, double c);
typedef double (*call_Rf_dnbinom)(double a, double b, double c, int d);
typedef double (*call_Rf_pnbinom)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_qnbinom)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_rnbinom)(double a, double b);
typedef double (*call_Rf_dnbinom_mu)(double a, double b, double c, int d);
typedef double (*call_Rf_pnbinom_mu)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_qnbinom_mu)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_rnbinom_mu)(double a, double b);
typedef void (*call_Rf_rmultinom)(int a, double* b, int c, int* d);
typedef double (*call_Rf_dpois)(double a, double b, int c);
typedef double (*call_Rf_ppois)(double a, double b, int c, int d);
typedef double (*call_Rf_qpois)(double a, double b, int c, int d);
typedef double (*call_Rf_rpois)(double a);
typedef double (*call_Rf_dweibull)(double a, double b, double c, int d);
typedef double (*call_Rf_pweibull)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_qweibull)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_rweibull)(double a, double b);
typedef double (*call_Rf_dlogis)(double a, double b, double c, int d);
typedef double (*call_Rf_plogis)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_qlogis)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_rlogis)(double a, double b);
typedef double (*call_Rf_dnbeta)(double a, double b, double c, double d, int e);
typedef double (*call_Rf_pnbeta)(double a, double b, double c, double d, int e, int f);
typedef double (*call_Rf_qnbeta)(double a, double b, double c, double d, int e, int f);
typedef double (*call_Rf_dnf)(double a, double b, double c, double d, int e);
typedef double (*call_Rf_pnf)(double a, double b, double c, double d, int e, int f);
typedef double (*call_Rf_qnf)(double a, double b, double c, double d, int e, int f);
typedef double (*call_Rf_dnt)(double a, double b, double c, int d);
typedef double (*call_Rf_pnt)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_qnt)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_ptukey)(double a, double b, double c, double d, int e, int f);
typedef double (*call_Rf_qtukey)(double a, double b, double c, double d, int e, int f);
typedef double (*call_Rf_dwilcox)(double a, double b, double c, int d);
typedef double (*call_Rf_pwilcox)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_qwilcox)(double a, double b, double c, int d, int e);
typedef double (*call_Rf_rwilcox)(double a, double b);
typedef double (*call_Rf_dsignrank)(double a, double b, int c);
typedef double (*call_Rf_psignrank)(double a, double b, int c, int d);
typedef double (*call_Rf_qsignrank)(double a, double b, int c, int d);
typedef double (*call_Rf_rsignrank)(double a);
typedef double (*call_Rf_gammafn)(double a);
typedef double (*call_Rf_lgammafn)(double a);
typedef double (*call_Rf_lgammafn_sign)(double a, int* b);
typedef void (*call_Rf_dpsifn)(double a, int b, int c, int d, double* e, int* f, int* g);
typedef double (*call_Rf_psigamma)(double a, double b);
typedef double (*call_Rf_digamma)(double a);
typedef double (*call_Rf_trigamma)(double a);
typedef double (*call_Rf_tetragamma)(double a);
typedef double (*call_Rf_pentagamma)(double a);
typedef double (*call_Rf_beta)(double a, double b);
typedef double (*call_Rf_lbeta)(double a, double b);
typedef double (*call_Rf_choose)(double a, double b);
typedef double (*call_Rf_lchoose)(double a, double b);
typedef double (*call_Rf_bessel_i)(double a, double b, double c);
typedef double (*call_Rf_bessel_j)(double a, double b);
typedef double (*call_Rf_bessel_k)(double a, double b, double c);
typedef double (*call_Rf_bessel_y)(double a, double b);
typedef double (*call_Rf_bessel_i_ex)(double a, double b, double c, double * d);
typedef double (*call_Rf_bessel_j_ex)(double a, double b, double * c);
typedef double (*call_Rf_bessel_k_ex)(double a, double b, double c, double * d);
typedef double (*call_Rf_bessel_y_ex)(double a, double b, double * c);
typedef double (*call_Rf_sign)(double a);
typedef double (*call_Rf_fprec)(double a, double b);
typedef double (*call_Rf_ftrunc)(double a);
typedef double (*call_Rf_cospi)(double a);
typedef double (*call_Rf_sinpi)(double a);
typedef double (*call_Rf_tanpi)(double a);
typedef SEXP (*call_Rf_match)(SEXP itable, SEXP ix, int nmatch);
typedef Rboolean (*call_Rf_NonNullStringMatch)(SEXP s, SEXP t);
typedef SEXP (*call_getvar)();
typedef SEXP (*call_R_ParseVector)(SEXP text, int n, SEXP srcFile);
typedef SEXPTYPE (*call_Rf_str2type)(const char *s);
typedef SEXP (*call_octsize)(SEXP size);
typedef int (*call_R_nchar)(SEXP string, nchar_type type_, Rboolean allowNA, Rboolean keepNA, const char* msg_name);

// connections

typedef int (*call_FASTR_getConnectionChar)(SEXP connection);
typedef int (*call_R_ReadConnection)(int fd, long bufAddress, int size);
typedef int (*call_R_WriteConnection)(int fd, long bufAddress, int size);

typedef SEXP (*call_R_new_custom_connection)(const char *description, const char *mode, const char *className, SEXP connAddrObj);

typedef SEXP (*call_R_GetConnection)(int fd);
typedef char* (*call_getConnectionClassString)(SEXP conn);
typedef char* (*call_getSummaryDescription)(SEXP conn);
typedef char* (*call_getOpenModeString)(SEXP conn);
typedef int (*call_isSeekable)(SEXP conn);


// symbols, dlls, etc.

typedef void (*call_registerRoutines)(DllInfo *dllInfo, int nstOrd, int num, const void* routines);
typedef int (*call_useDynamicSymbols)(DllInfo *dllInfo, Rboolean value);
typedef void * (*call_setDotSymbolValues)(DllInfo *dllInfo, char *name, DL_FUNC fun, int numArgs);
typedef int (*call_forceSymbols)(DllInfo *dllInfo, Rboolean value);
typedef int (*call_registerCCallable)(const char *pkgname, const char *name, void *fun);
typedef void* (*call_getCCallable)(const char *pkgname, const char *name);
typedef DllInfo* (*call_getEmbeddingDLLInfo)(void);

// memory

typedef SEXP (*call_Rf_protect)(SEXP x);
typedef void (*call_Rf_unprotect)(int x);
typedef int (*call_R_ProtectWithIndex)(SEXP x);
typedef void (*call_R_Reprotect)(SEXP x, int y);
typedef void (*call_Rf_unprotect_ptr)(SEXP x);
typedef void (*call_R_PreserveObject)(SEXP x);
typedef void (*call_R_ReleaseObject)(SEXP x);

typedef void* (*call_R_alloc)(int n, int size);

// IDEs and Tools
typedef void* (*call_R_getGlobalFunctionContext)();
typedef void* (*call_R_getParentFunctionContext)(void* c);
typedef void* (*call_R_getContextEnv)(void* c);
typedef void* (*call_R_getContextFun)(void* c);
typedef void* (*call_R_getContextCall)(void* c);
typedef void* (*call_R_getContextSrcRef)(void* c);
typedef int (*call_R_insideBrowser)();
typedef int (*call_R_isGlobal)(void* c);
typedef int (*call_R_isEqual)(void* x, void* y);

#endif
