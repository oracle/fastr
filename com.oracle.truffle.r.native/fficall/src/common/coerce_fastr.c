/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

#include <Rinternals.h>

#define _(Source) (Source)

// selected functions from coerce.c:

/* Coercion warnings will be OR'ed : */
#define WARN_NA	   1
#define WARN_INACC 2
#define WARN_IMAG  4
#define WARN_RAW  8

void CoercionWarning(int warn)
{
/* FIXME: Use
   =====
   WarningMessage(R_NilValue, WARNING_....);
*/
    if (warn & WARN_NA)
	warning(_("NAs introduced by coercion"));
    if (warn & WARN_INACC)
	warning(_("inaccurate integer conversion in coercion"));
    if (warn & WARN_IMAG)
	warning(_("imaginary parts discarded in coercion"));
    if (warn & WARN_RAW)
	warning(_("out-of-range values treated as 0 in coercion to raw"));
}

double
RealFromLogical(int x, int *warn)
{
    return (x == NA_LOGICAL) ?
	NA_REAL : x;
}

double
RealFromInteger(int x, int *warn)
{
    if (x == NA_INTEGER)
	return NA_REAL;
    else
	return x;
}

int
IntegerFromLogical(int x, int *warn)
{
    return (x == NA_LOGICAL) ?
	NA_INTEGER : x;
}

int
IntegerFromReal(double x, int *warn)
{
    if (ISNAN(x))
	return NA_INTEGER;
    else if (x > INT_MAX || x <= INT_MIN ) {
	*warn |= WARN_NA;
	return NA_INTEGER;
    }
    return (int) x;
}

static SEXP coerceToInteger(SEXP v)
{
    SEXP ans;
    int warn = 0;
    R_xlen_t i, n;
    PROTECT(ans = allocVector(INTSXP, n = XLENGTH(v)));
#ifdef R_MEMORY_PROFILING
    if (RTRACE(v)){
       memtrace_report(v,ans);
       SET_RTRACE(ans,1);
    }
#endif
    DUPLICATE_ATTRIB(ans, v);
    switch (TYPEOF(v)) {
    case LGLSXP:
	for (i = 0; i < n; i++) {
//	    if ((i+1) % NINTERRUPT == 0) R_CheckUserInterrupt();
	    INTEGER(ans)[i] = IntegerFromLogical(LOGICAL(v)[i], &warn);
	}
	break;
    case REALSXP:
	for (i = 0; i < n; i++) {
//	    if ((i+1) % NINTERRUPT == 0) R_CheckUserInterrupt();
	    INTEGER(ans)[i] = IntegerFromReal(REAL(v)[i], &warn);
	}
	break;
    case CPLXSXP:
	for (i = 0; i < n; i++) {
//	    if ((i+1) % NINTERRUPT == 0) R_CheckUserInterrupt();
	    INTEGER(ans)[i] = IntegerFromComplex(COMPLEX(v)[i], &warn);
	}
	break;
    case STRSXP:
	for (i = 0; i < n; i++) {
//	    if ((i+1) % NINTERRUPT == 0) R_CheckUserInterrupt();
	    INTEGER(ans)[i] = IntegerFromString(STRING_ELT(v, i), &warn);
	}
	break;
    case RAWSXP:
	for (i = 0; i < n; i++) {
//	    if ((i+1) % NINTERRUPT == 0) R_CheckUserInterrupt();
	    INTEGER(ans)[i] = (int)RAW(v)[i];
	}
	break;
    default:
	UNIMPLEMENTED_TYPE("coerceToInteger", v);
    }
    if (warn) CoercionWarning(warn);
    UNPROTECT(1);
    return ans;
}

static SEXP coerceToReal(SEXP v)
{
    SEXP ans;
    int warn = 0;
    R_xlen_t i, n;
    PROTECT(ans = allocVector(REALSXP, n = XLENGTH(v)));
#ifdef R_MEMORY_PROFILING
    if (RTRACE(v)){
       memtrace_report(v,ans);
       SET_RTRACE(ans,1);
    }
#endif
    DUPLICATE_ATTRIB(ans, v);
    switch (TYPEOF(v)) {
    case LGLSXP:
	for (i = 0; i < n; i++) {
//	    if ((i+1) % NINTERRUPT == 0) R_CheckUserInterrupt();
	    REAL(ans)[i] = RealFromLogical(LOGICAL(v)[i], &warn);
	}
	break;
    case INTSXP:
	for (i = 0; i < n; i++) {
//	    if ((i+1) % NINTERRUPT == 0) R_CheckUserInterrupt();
	    REAL(ans)[i] = RealFromInteger(INTEGER(v)[i], &warn);
	}
	break;
    case CPLXSXP:
	for (i = 0; i < n; i++) {
//	    if ((i+1) % NINTERRUPT == 0) R_CheckUserInterrupt();
	    REAL(ans)[i] = RealFromComplex(COMPLEX(v)[i], &warn);
	}
	break;
    case STRSXP:
	for (i = 0; i < n; i++) {
//	    if ((i+1) % NINTERRUPT == 0) R_CheckUserInterrupt();
	    REAL(ans)[i] = RealFromString(STRING_ELT(v, i), &warn);
	}
	break;
    case RAWSXP:
	for (i = 0; i < n; i++) {
//	    if ((i+1) % NINTERRUPT == 0) R_CheckUserInterrupt();
	    REAL(ans)[i] = RealFromInteger((int)RAW(v)[i], &warn);
	}
	break;
    default:
	UNIMPLEMENTED_TYPE("coerceToReal", v);
    }
    if (warn) CoercionWarning(warn);
    UNPROTECT(1);
    return ans;
}

static SEXP coerceToPairList(SEXP v)
{
    SEXP ans, ansp;
    int i, n;
    n = LENGTH(v); /* limited to len */
    PROTECT(ansp = ans = allocList(n));
    for (i = 0; i < n; i++) {
	switch (TYPEOF(v)) {
	case LGLSXP:
	    SETCAR(ansp, allocVector(LGLSXP, 1));
	    INTEGER(CAR(ansp))[0] = INTEGER(v)[i];
	    break;
	case INTSXP:
	    SETCAR(ansp, allocVector(INTSXP, 1));
	    INTEGER(CAR(ansp))[0] = INTEGER(v)[i];
	    break;
	case REALSXP:
	    SETCAR(ansp, allocVector(REALSXP, 1));
	    REAL(CAR(ansp))[0] = REAL(v)[i];
	    break;
	case CPLXSXP:
	    SETCAR(ansp, allocVector(CPLXSXP, 1));
	    COMPLEX(CAR(ansp))[0] = COMPLEX(v)[i];
	    break;
	case STRSXP:
	    SETCAR(ansp, ScalarString(STRING_ELT(v, i)));
	    break;
	case RAWSXP:
	    SETCAR(ansp, allocVector(RAWSXP, 1));
	    RAW(CAR(ansp))[0] = RAW(v)[i];
	    break;
	case VECSXP:
	    SETCAR(ansp, VECTOR_ELT(v, i));
	    break;
	case EXPRSXP:
	    SETCAR(ansp, VECTOR_ELT(v, i));
	    break;
	default:
	    UNIMPLEMENTED_TYPE("coerceToPairList", v);
	}
	ansp = CDR(ansp);
    }
    ansp = getAttrib(v, R_NamesSymbol);
    if (ansp != R_NilValue)
	setAttrib(ans, R_NamesSymbol, ansp);
    UNPROTECT(1);
    return (ans);
}

/* Coerce a pairlist to the given type */
static SEXP coercePairList(SEXP v, SEXPTYPE type)
{
    int i, n=0;
    SEXP rval= R_NilValue, vp, names;

    /* Hmm, this is also called to LANGSXP, and coerceVector already
       did the check of TYPEOF(v) == type */
    if(type == LISTSXP) return v;/* IS pairlist */

    names = v;
    if (type == EXPRSXP) {
	PROTECT(rval = allocVector(type, 1));
	SET_VECTOR_ELT(rval, 0, v);
	UNPROTECT(1);
	return rval;
    }
    else if (type == STRSXP) {
	n = length(v);
	PROTECT(rval = allocVector(type, n));
	for (vp = v, i = 0; vp != R_NilValue; vp = CDR(vp), i++) {
	    if (isString(CAR(vp)) && length(CAR(vp)) == 1)
		SET_STRING_ELT(rval, i, STRING_ELT(CAR(vp), 0));
	    else
		SET_STRING_ELT(rval, i, STRING_ELT(deparse1line(CAR(vp), 0), 0));
	}
    }
    else if (type == VECSXP) {
	rval = PairToVectorList(v);
	return rval;
    }
    else if (isVectorizable(v)) {
	n = length(v);
	PROTECT(rval = allocVector(type, n));
	switch (type) {
	case LGLSXP:
	    for (i = 0, vp = v; i < n; i++, vp = CDR(vp))
		LOGICAL(rval)[i] = asLogical(CAR(vp));
	    break;
	case INTSXP:
	    for (i = 0, vp = v; i < n; i++, vp = CDR(vp))
		INTEGER(rval)[i] = asInteger(CAR(vp));
	    break;
	case REALSXP:
	    for (i = 0, vp = v; i < n; i++, vp = CDR(vp))
		REAL(rval)[i] = asReal(CAR(vp));
	    break;
	case CPLXSXP:
	    for (i = 0, vp = v; i < n; i++, vp = CDR(vp))
		COMPLEX(rval)[i] = asComplex(CAR(vp));
	    break;
	case RAWSXP:
	    for (i = 0, vp = v; i < n; i++, vp = CDR(vp))
		RAW(rval)[i] = (Rbyte) asInteger(CAR(vp));
	    break;
	default:
	    UNIMPLEMENTED_TYPE("coercePairList", v);
	}
    }
    else
	error(_("'pairlist' object cannot be coerced to type '%s'"),
	      type2char(type));

    /* If any tags are non-null then we */
    /* need to add a names attribute. */
    for (vp = v, i = 0; vp != R_NilValue; vp = CDR(vp))
	if (TAG(vp) != R_NilValue)
	    i = 1;

    if (i) {
	i = 0;
	names = allocVector(STRSXP, n);
	for (vp = v; vp != R_NilValue; vp = CDR(vp), i++)
	    if (TAG(vp) != R_NilValue)
		SET_STRING_ELT(names, i, PRINTNAME(TAG(vp)));
	setAttrib(rval, R_NamesSymbol, names);
    }
    UNPROTECT(1);
    return rval;
}

SEXP Rf_coerceVector(SEXP v, SEXPTYPE type)
{
    SEXP op, vp, ans = R_NilValue;	/* -Wall */
    int i,n;

    if (TYPEOF(v) == type)
	return v;
    /* code to allow classes to extend ENVSXP, SYMSXP, etc */
    if(IS_S4_OBJECT(v) && TYPEOF(v) == S4SXP) {
	SEXP vv = R_getS4DataSlot(v, ANYSXP);
	if(vv == R_NilValue)
	  error(_("no method for coercing this S4 class to a vector"));
	else if(TYPEOF(vv) == type)
	  return vv;
	v = vv;
    }

    switch (TYPEOF(v)) {
#ifdef NOTYET
    case NILSXP:
	ans = coerceNull(v, type);
	break;
#endif
    case SYMSXP:
	ans = coerceSymbol(v, type);
	break;
    case NILSXP:
    case LISTSXP:
	ans = coercePairList(v, type);
	break;
    case LANGSXP:
	if (type != STRSXP) {
	    ans = coercePairList(v, type);
	    break;
	}

	/* This is mostly copied from coercePairList, but we need to
	 * special-case the first element so as not to get operators
	 * put in backticks. */
	n = length(v);
	PROTECT(ans = allocVector(type, n));
	if (n == 0) break; /* Can this actually happen? */
	i = 0;
	op = CAR(v);
	/* The case of practical relevance is "lhs ~ rhs", which
	 * people tend to split using as.character(), modify, and
	 * paste() back together. However, we might as well
	 * special-case all symbolic operators here. */
	if (TYPEOF(op) == SYMSXP) {
	    SET_STRING_ELT(ans, i, PRINTNAME(op));
	    i++;
	    v = CDR(v);
	}

	/* The distinction between strings and other elements was
	 * here "always", but is really dubious since it makes x <- a
	 * and x <- "a" come out identical. Won't fix just now. */
	for (vp = v;  vp != R_NilValue; vp = CDR(vp), i++) {
	    if (isString(CAR(vp)) && length(CAR(vp)) == 1)
		SET_STRING_ELT(ans, i, STRING_ELT(CAR(vp), 0));
	    else
		SET_STRING_ELT(ans, i, STRING_ELT(deparse1line(CAR(vp), 0), 0));
	}
	UNPROTECT(1);
	break;
    case VECSXP:
    case EXPRSXP:
	ans = coerceVectorList(v, type);
	break;
    case ENVSXP:
	error(_("environments cannot be coerced to other types"));
	break;
    case LGLSXP:
    case INTSXP:
    case REALSXP:
    case CPLXSXP:
    case STRSXP:
    case RAWSXP:

#define COERCE_ERROR_STRING "cannot coerce type '%s' to vector of type '%s'"

#define COERCE_ERROR							\
	error(_(COERCE_ERROR_STRING), type2char(TYPEOF(v)), type2char(type))

	switch (type) {
	case SYMSXP:
	    ans = coerceToSymbol(v);	    break;
	case LGLSXP:
	    ans = coerceToLogical(v);	    break;
	case INTSXP:
	    ans = coerceToInteger(v);	    break;
	case REALSXP:
	    ans = coerceToReal(v);	    break;
	case CPLXSXP:
	    ans = coerceToComplex(v);	    break;
	case RAWSXP:
	    ans = coerceToRaw(v);	    break;
	case STRSXP:
	    ans = coerceToString(v);	    break;
	case EXPRSXP:
	    ans = coerceToExpression(v);    break;
	case VECSXP:
	    ans = coerceToVectorList(v);    break;
	case LISTSXP:
	    ans = coerceToPairList(v);	    break;
	default:
	    COERCE_ERROR;
	}
	break;
    default:
	COERCE_ERROR;
    }
    return ans;
}
#undef COERCE_ERROR


