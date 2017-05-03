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
#include <Rinternals.h>

// This file includes all implementations that arise from Rinternals.h that
// are independent, or largely independent, of the RFFI implementation.

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

SEXP GetOption(SEXP tag, SEXP rho)
{
    return GetOption1(tag); // RFFI impl dependent
}

int GetOptionCutoff(void)
{
    int w;
    w = asInteger(GetOption1(install("deparse.cutoff")));
    if (w == NA_INTEGER || w <= 0) {
	warning("invalid 'deparse.cutoff', used 60");
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
	warning("invalid printing width, used 80");
	return 80;
    }
    return w;
}

int GetOptionDigits(void)
{
    int d;
    d = asInteger(GetOption1(install("digits")));
    if (d < R_MIN_DIGITS_OPT || d > R_MAX_DIGITS_OPT) {
	warning("invalid printing digits, used 7");
	return 7;
    }
    return d;
}

Rboolean Rf_GetOptionDeviceAsk(void)
{
    int ask;
    ask = asLogical(GetOption1(install("device.ask.default")));
    if(ask == NA_LOGICAL) {
	warning("invalid value for \"device.ask.default\", using FALSE");
	return FALSE;
    }
    return ask != 0;
}

void *DATAPTR(SEXP x) {
	int type = TYPEOF(x);
	if (type == INTSXP) {
		return INTEGER(x);
	} else if (type == REALSXP) {
		return REAL(x);
	} else if (type == LGLSXP) {
		return LOGICAL(x);
	} else {
		printf("DATAPTR %d\n", type);
		unimplemented("R_DATAPTR");
		return NULL;
	}
}

int R_check_class_and_super(SEXP x, const char **valid, SEXP rho) {
    int ans;
    SEXP cl = PROTECT(asChar(getAttrib(x, R_ClassSymbol)));
    const char *class = CHAR(cl);
    for (ans = 0; ; ans++) {
	if (!strlen(valid[ans])) // empty string
	    break;
	if (!strcmp(class, valid[ans])) {
	    UNPROTECT(1); /* cl */
	    return ans;
	}
    }
    /* if not found directly, now search the non-virtual super classes :*/
    if(IS_S4_OBJECT(x)) {
	/* now try the superclasses, i.e.,  try   is(x, "....");  superCl :=
	   .selectSuperClasses(getClass("....")@contains, dropVirtual=TRUE)  */
	SEXP classExts, superCl, _call;
        // install() results cached anyway so the following variables could be non-static if needed
	static SEXP s_contains = NULL, s_selectSuperCl = NULL;
	int i;
	if(!s_contains) {
	    s_contains      = install("contains");
	    s_selectSuperCl = install(".selectSuperClasses");
	}
	SEXP classDef = PROTECT(R_getClassDef(class));
	PROTECT(classExts = R_do_slot(classDef, s_contains));
	PROTECT(_call = lang3(s_selectSuperCl, classExts,
			      /* dropVirtual = */ ScalarLogical(1)));
	superCl = eval(_call, rho);
	UNPROTECT(3); /* _call, classExts, classDef */
	PROTECT(superCl);
	for(i=0; i < LENGTH(superCl); i++) {
	    const char *s_class = CHAR(STRING_ELT(superCl, i));
	    for (ans = 0; ; ans++) {
		if (!strlen(valid[ans]))
		    break;
		if (!strcmp(s_class, valid[ans])) {
		    UNPROTECT(2); /* superCl, cl */
		    return ans;
		}
	    }
	}
	UNPROTECT(1); /* superCl */
    }
    UNPROTECT(1); /* cl */
    return -1;
}

int R_check_class_etc_helper (SEXP x, const char **valid, SEXP (*getMethodsNamespace)()) {
    // install() results cached anyway so the following variables could be non-static if needed
    static SEXP meth_classEnv = NULL;
    SEXP cl = getAttrib(x, R_ClassSymbol), rho = R_GlobalEnv, pkg;
    if (!meth_classEnv)
        meth_classEnv = install(".classEnv");

    pkg = getAttrib(cl, R_PackageSymbol); /* ==R== packageSlot(class(x)) */
    if (!isNull(pkg)) { /* find  rho := correct class Environment */
        SEXP clEnvCall;
        // FIXME: fails if 'methods' is not loaded.
        PROTECT(clEnvCall = lang2(meth_classEnv, cl));
        SEXP methodsNamespace = getMethodsNamespace();
        rho = eval(clEnvCall, methodsNamespace);
        UNPROTECT(1);
        if (!isEnvironment(rho))
            error(_("could not find correct environment; please report!"));
    }
    PROTECT(rho);
    int res = R_check_class_and_super(x, valid, rho);
    UNPROTECT(1);
    return res;
}
