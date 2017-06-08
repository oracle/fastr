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
#include <truffle.h>
#include "../common/rffi_upcalls.h"

// Most everything in RInternals.h

void **callbacks = NULL;

void Rinternals_addCallback(int index, void *callback) {
	if (callbacks == NULL) {
		callbacks = truffle_managed_malloc(UPCALLS_TABLE_SIZE * sizeof(void*));
	}
	callbacks[index] = callback;
}

static char *ensure_truffle_chararray_n(const char *x, int n);

// R_GlobalEnv et al are not a variables in FASTR as they are RContext specific
SEXP FASTR_R_GlobalEnv() {
	IMPORT_CALLHELPER_IMPL();
	return truffle_invoke(obj, "R_GlobalEnv");
}

SEXP FASTR_R_BaseEnv() {
	IMPORT_CALLHELPER_IMPL();
	return truffle_invoke(obj, "R_BaseEnv");
}

SEXP FASTR_R_BaseNamespace() {
	IMPORT_CALLHELPER_IMPL();
	return truffle_invoke(obj, "R_BaseNamespace");
}

SEXP FASTR_R_NamespaceRegistry() {
	IMPORT_CALLHELPER_IMPL();
	return truffle_invoke(obj, "R_NamespaceRegistry");
}

Rboolean FASTR_R_Interactive() {
	IMPORT_CALLHELPER_IMPL();
	return (Rboolean) truffle_invoke_i(obj, "R_Interactive");
}

SEXP Rf_ScalarInteger(int value) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "Rf_ScalarInteger", value);
}

SEXP Rf_ScalarReal(double value) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "Rf_ScalarDouble", value);
}

SEXP Rf_ScalarString(SEXP value) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "Rf_ScalarString", value);
}

SEXP Rf_ScalarLogical(int value) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "Rf_ScalarLogical", value);
}

SEXP Rf_allocVector3(SEXPTYPE t, R_xlen_t len, R_allocator_t* allocator) {
    if (allocator != NULL) {
	    return unimplemented("RF_allocVector with custom allocator");
    }
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "Rf_allocVector", t, len);
}

SEXP Rf_allocArray(SEXPTYPE t, SEXP dims) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "Rf_allocArray", t, dims);
}

SEXP Rf_alloc3DArray(SEXPTYPE t, int x, int y, int z) {
	return unimplemented("Rf_alloc3DArray");
}

SEXP Rf_allocMatrix(SEXPTYPE mode, int nrow, int ncol) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "Rf_allocMatrix", mode, nrow, ncol);
}

SEXP Rf_allocList(int x) {
	return unimplemented("Rf_allocList)");
}

SEXP Rf_allocSExp(SEXPTYPE t) {
	return unimplemented("Rf_allocSExp");
}

SEXP Rf_cons(SEXP car, SEXP cdr) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "Rf_cons", car, cdr);
}

void Rf_defineVar(SEXP symbol, SEXP value, SEXP rho) {
	IMPORT_CALLHELPER();
    truffle_invoke(obj, "Rf_defineVar", symbol, value, rho);
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
	return unimplemented("Rf_eval");
}

SEXP Rf_findFun(SEXP symbol, SEXP rho) {
	return unimplemented("Rf_findFun");
}

SEXP Rf_findVar(SEXP symbol, SEXP rho) {
	IMPORT_CALLHELPER();
    return truffle_invoke(obj, "Rf_findVar", symbol, rho);
}

SEXP Rf_findVarInFrame(SEXP symbol, SEXP rho) {
	return unimplemented("Rf_findVarInFrame");
}

SEXP Rf_getAttrib(SEXP vec, SEXP name) {
	IMPORT_CALLHELPER();
    return truffle_invoke(obj, "Rf_getAttrib", vec, name);
}

SEXP Rf_setAttrib(SEXP vec, SEXP name, SEXP val) {
	IMPORT_CALLHELPER();
    return truffle_invoke(obj, "Rf_setAttrib", vec, name, val);
}

SEXP Rf_duplicate(SEXP x) {
	IMPORT_CALLHELPER();
    return truffle_invoke(obj, "Rf_duplicate", x, 1);
}

SEXP Rf_shallow_duplicate(SEXP x) {
	IMPORT_CALLHELPER();
    return truffle_invoke(obj, "Rf_duplicate", x, 0);
}

R_xlen_t Rf_any_duplicated(SEXP x, Rboolean from_last) {
	IMPORT_CALLHELPER();
    return (R_xlen_t) truffle_invoke(obj, "Rf_any_duplicated", x, from_last);
}

SEXP Rf_duplicated(SEXP x, Rboolean y) {
	IMPORT_CALLHELPER();
    return truffle_invoke(obj, "Rf_duplicated", x, y);
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
	IMPORT_CALLHELPER();
    return (Rboolean) truffle_invoke(obj, "Rf_inherits", x, klass);
}

Rboolean Rf_isObject(SEXP s) {
	unimplemented("Rf_isObject");
	return FALSE;
}

void Rf_PrintValue(SEXP x) {
	unimplemented("Rf_PrintValue");
}

SEXP Rf_install(const char *name) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "Rf_install", ensure_truffle_chararray(name));
}

SEXP Rf_installChar(SEXP charsxp) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "Rf_installChar", charsxp);
}

Rboolean Rf_isNull(SEXP s) {
	IMPORT_CALLHELPER();
	return (Rboolean) truffle_invoke_i(obj, "Rf_isNull", s);
}

Rboolean Rf_isString(SEXP s) {
	IMPORT_CALLHELPER();
	return (Rboolean) truffle_invoke_i(obj, "Rf_isString", s);
}

Rboolean R_cycle_detected(SEXP s, SEXP child) {
	return (Rboolean) unimplemented("R_cycle_detected");
}

cetype_t Rf_getCharCE(SEXP x) {
    // unimplemented("Rf_getCharCE");
    // TODO: real implementation
    return CE_NATIVE;
}

char *ensure_truffle_chararray_n(const char *x, int n) {
	if (truffle_is_truffle_object(x)) {
		return x;
	} else {
		IMPORT_CALLHELPER_IMPL();
		return truffle_invoke(obj, "bytesToNativeCharArray", truffle_read_n_bytes(x, n));
	}
}

SEXP Rf_mkCharLenCE_truffle(const char *x, cetype_t enc) {
	// Assumes x is a NativeCharArray
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "Rf_mkCharLenCE", x, 0, enc);
}

SEXP Rf_mkChar(const char *x) {
	return Rf_mkCharLenCE_truffle(ensure_truffle_chararray(x), CE_NATIVE);
}

SEXP Rf_mkCharCE(const char *x, cetype_t y) {
	return Rf_mkCharLenCE_truffle(ensure_truffle_chararray(x), y);
}

SEXP Rf_mkCharLen(const char *x, int y) {
	return Rf_mkCharLenCE(x, y, CE_NATIVE);
}

SEXP Rf_mkCharLenCE(const char *x, int len, cetype_t enc) {
	return Rf_mkCharLenCE_truffle(ensure_truffle_chararray_n(x, len), enc);
}

const char *Rf_reEnc(const char *x, cetype_t ce_in, cetype_t ce_out, int subst) {
	// TODO proper implementation
	return x;
}

SEXP Rf_mkString(const char *s) {
	return ScalarString(Rf_mkChar(s));
}

int Rf_ncols(SEXP x) {
	IMPORT_CALLHELPER();
	return (int) truffle_invoke(obj, "Rf_ncols", x);
}

int Rf_nrows(SEXP x) {
	IMPORT_CALLHELPER();
	return (int) truffle_invoke(obj, "Rf_nrows", x);
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
	char buf[8192];
	va_list(ap);
	va_start(ap,format);
	Rvsnprintf(buf, BUFSIZE - 1, format, ap);
	va_end(ap);
	IMPORT_CALLHELPER();
	truffle_invoke(obj, "Rf_error", ensure_truffle_chararray(buf));
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
	unimplemented("Rf_warningcall");

}

void Rf_warning(const char *format, ...) {
	char buf[8192];
	va_list(ap);
	va_start(ap,format);
	Rvsnprintf(buf, BUFSIZE - 1, format, ap);
	va_end(ap);
	unimplemented("Rf_warning");

}

void Rprintf(const char *format, ...) {
	char buf[8192];
	va_list(ap);
	va_start(ap,format);
	Rvsnprintf(buf, BUFSIZE - 1, format, ap);
	va_end(ap);
	void *str = truffle_read_string(buf);
	IMPORT_CALLHELPER();
	truffle_invoke(obj, "printf", str);
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
	unimplemented("REprintf");

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
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "R_NewHashedEnv", parent, size);
}

SEXP Rf_classgets(SEXP x, SEXP y) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "Rf_classgets", x, y);
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
	return unimplemented("R_FindNamespace");
}

SEXP Rf_lengthgets(SEXP x, R_len_t y) {
	return unimplemented("Rf_lengthgets");
}

SEXP Rf_xlengthgets(SEXP x, R_xlen_t y) {
	return unimplemented("Rf_xlengthgets");

}

SEXP Rf_namesgets(SEXP x, SEXP y) {
	return unimplemented("Rf_namesgets");
}

SEXP GetOption1(SEXP tag){
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "GetOption1", tag);
}

void Rf_gsetVar(SEXP symbol, SEXP value, SEXP rho) {
	IMPORT_CALLHELPER();
    truffle_invoke(obj, "Rf_gsetVar", symbol, value, rho);
}

SEXP TAG(SEXP e) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "TAG", e);
}

SEXP PRINTNAME(SEXP e) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "PRINTNAME", e);
}

SEXP CAR(SEXP e) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "CAR", e);
}

SEXP CDR(SEXP e) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "CDR", e);
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
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "CADR", e);
}

SEXP CDDR(SEXP e) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "CDDR", e);
}

SEXP CDDDR(SEXP e) {
    unimplemented("CDDDR");
    return NULL;
}

SEXP CADDR(SEXP e) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "CADDR", e);
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
	IMPORT_CALLHELPER();
 truffle_invoke(obj, "SET_TAG", x, y);
}

SEXP SETCAR(SEXP x, SEXP y) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "SETCAR", x, y);
}

SEXP SETCDR(SEXP x, SEXP y) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "SETCDR", x, y);
}

SEXP SETCADR(SEXP x, SEXP y) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "SETCADR", x, y);
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
	IMPORT_CALLHELPER();
	return (int) truffle_invoke(obj, "RDEBUG", x);
}

int RSTEP(SEXP x) {
	IMPORT_CALLHELPER();
	return (int) truffle_invoke(obj, "RSTEP", x);
}

int RTRACE(SEXP x) {
	unimplemented("RTRACE");
    return 0;
}

void SET_RDEBUG(SEXP x, int v) {
	IMPORT_CALLHELPER();
	truffle_invoke(obj, "SET_RDEBUG", x, v);
}

void SET_RSTEP(SEXP x, int v) {
	IMPORT_CALLHELPER();
	truffle_invoke(obj, "SET_RSTEP", x, v);
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
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "SYMVALUE", x);
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
	IMPORT_CALLHELPER();
    truffle_invoke(obj, "SET_SYMVALUE", x, v);
}

void SET_INTERNAL(SEXP x, SEXP v) {
    unimplemented("SET_INTERNAL");
}


SEXP FRAME(SEXP x) {
	return unimplemented("FRAME");
}

SEXP ENCLOS(SEXP x) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "ENCLOS", x);
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
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "PRCODE", x);
}

SEXP PRENV(SEXP x) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "PRENV", x);
}

SEXP PRVALUE(SEXP x) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "PRVALUE", x);
}

int PRSEEN(SEXP x) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "PRSEEN", x);
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
	IMPORT_CALLHELPER();
    return truffle_invoke_i(obj, "LENGTH", x);
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
	IMPORT_CALLHELPER();
	return (int*) truffle_invoke(obj, "LOGICAL", x);
}

int *INTEGER(SEXP x){
	IMPORT_CALLHELPER();
	return (int*) truffle_invoke(obj, "INTEGER", x);
}


Rbyte *RAW(SEXP x){
	IMPORT_CALLHELPER();
	return (int*) truffle_invoke(obj, "RAW", x);
}


double *REAL(SEXP x){
	IMPORT_CALLHELPER();
	return (double*) truffle_invoke(obj, "REAL", x);
}


Rcomplex *COMPLEX(SEXP x){
	return (Rcomplex*) unimplemented("COMPLEX");
}


SEXP STRING_ELT(SEXP x, R_xlen_t i){
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "STRING_ELT", x, i);
}


SEXP VECTOR_ELT(SEXP x, R_xlen_t i){
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "VECTOR_ELT", x, i);
}

void SET_STRING_ELT(SEXP x, R_xlen_t i, SEXP v){
	IMPORT_CALLHELPER();
	truffle_invoke(obj, "SET_STRING_ELT", x, i, v);
}


SEXP SET_VECTOR_ELT(SEXP x, R_xlen_t i, SEXP v){
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "SET_VECTOR_ELT", x, i, v);
}


SEXP *STRING_PTR(SEXP x){
	return unimplemented("STRING_PTR");
}


SEXP *VECTOR_PTR(SEXP x){
	return unimplemented("VECTOR_PTR");
}

SEXP Rf_asChar(SEXP x){
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "Rf_asChar", x);
}

SEXP Rf_PairToVectorList(SEXP x){
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "Rf_PairToVectorList", x);
}

SEXP Rf_VectorToPairList(SEXP x){
	return unimplemented("Rf_VectorToPairList");
}

SEXP Rf_asCharacterFactor(SEXP x){
	return unimplemented("Rf_VectorToPairList");
}

int Rf_asLogical(SEXP x){
	IMPORT_CALLHELPER();
    return truffle_invoke_i(obj, "Rf_asLogical", x);
}

int Rf_asInteger(SEXP x) {
	IMPORT_CALLHELPER();
    return truffle_invoke_i(obj, "Rf_asInteger", x);
}

double Rf_asReal(SEXP x) {
	IMPORT_CALLHELPER();
	return (double) truffle_invoke_d(obj, "Rf_asReal", x);
}

Rcomplex Rf_asComplex(SEXP x){
	unimplemented("Rf_asLogical");
	Rcomplex c; return c;
}

int TYPEOF(SEXP x) {
	IMPORT_CALLHELPER();
	return truffle_invoke_i(obj, "TYPEOF", x);
}

SEXP ATTRIB(SEXP x){
    unimplemented("ATTRIB");
    return NULL;
}

int OBJECT(SEXP x){
	return (int) unimplemented("OBJECT");
}

int MARK(SEXP x){
	IMPORT_CALLHELPER();
	return (int) truffle_invoke(obj, "MARK", x);
}

int NAMED(SEXP x){
	IMPORT_CALLHELPER();
	return truffle_invoke_i(obj, "NAMED", x);
}

int REFCNT(SEXP x){
	return (int) unimplemented("REFCNT");
}

void SET_OBJECT(SEXP x, int v){
    unimplemented("SET_OBJECT");
}

void SET_TYPEOF(SEXP x, int v){
    unimplemented("SET_TYPEOF");
}

SEXP SET_TYPEOF_FASTR(SEXP x, int v){
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "SET_TYPEOF_FASTR", x, v);
}

void SET_NAMED(SEXP x, int v){
    unimplemented("SET_NAMED");
}

void SET_ATTRIB(SEXP x, SEXP v){
    unimplemented("SET_ATTRIB");
}

void DUPLICATE_ATTRIB(SEXP to, SEXP from){
	IMPORT_CALLHELPER();
 truffle_invoke(obj, "DUPLICATE_ATTRIB", to, from);
}

const char *R_CHAR(SEXP charsxp) {
	IMPORT_CALLHELPER_IMPL();
	return (char *)truffle_invoke(obj, "charSXPToNativeCharArray", charsxp);
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
	return (int) unimplemented("IS_S4_OBJECT");
}

void SET_S4_OBJECT(SEXP x) {
	IMPORT_CALLHELPER();
 truffle_invoke(obj, "SET_S4_OBJECT", x);
}

void UNSET_S4_OBJECT(SEXP x) {
	IMPORT_CALLHELPER();
     truffle_invoke(obj, "UNSET_S4_OBJECT", x);
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
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "R_PromiseExpr", x);
}

SEXP R_ClosureExpr(SEXP x) {
	return unimplemented("R_ClosureExpr");
}

SEXP R_forceAndCall(SEXP e, int n, SEXP rho) {
	return unimplemented("R_forceAndCall");
}

SEXP R_MakeExternalPtr(void *p, SEXP tag, SEXP prot) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "R_MakeExternalPtr", (long) p, tag, prot);
}

void *R_ExternalPtrAddr(SEXP s) {
	IMPORT_CALLHELPER();
	return (void*) truffle_invoke_l(obj, "R_ExternalPtrAddr", s);
}

SEXP R_ExternalPtrTag(SEXP s) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "R_ExternalPtrTag", s);
}

SEXP R_ExternalPtrProt(SEXP s) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "R_ExternalPtrProt", s);
}

void R_SetExternalPtrAddr(SEXP s, void *p) {
	IMPORT_CALLHELPER();
	truffle_invoke(obj, "R_SetExternalPtrAddr", s, p);
}

void R_SetExternalPtrTag(SEXP s, SEXP tag) {
	IMPORT_CALLHELPER();
	truffle_invoke(obj, "R_SetExternalPtrTag", s, tag);
}

void R_SetExternalPtrProt(SEXP s, SEXP p) {
	IMPORT_CALLHELPER();
	truffle_invoke(obj, "R_SetExternalPtrProt", s, p);
}

void R_ClearExternalPtr(SEXP s) {
	IMPORT_CALLHELPER();
	truffle_invoke(obj, "R_ClearExternalPtr", s);
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

SEXP R_do_slot(SEXP objx, SEXP name) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "R_do_slot", objx, name);
}

SEXP R_do_slot_assign(SEXP objx, SEXP name, SEXP value) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "R_do_slot_assign", objx, name, value);
}

int R_has_slot(SEXP obj, SEXP name) {
	return (int) unimplemented("R_has_slot");
}

SEXP R_do_MAKE_CLASS(const char *what) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "R_do_MAKE_CLASS", what);
}

SEXP R_getClassDef (const char *what) {
	return unimplemented("R_getClassDef");
}

SEXP R_do_new_object(SEXP class_def) {
	IMPORT_CALLHELPER();
	return truffle_invoke(obj, "R_do_new_object", class_def);
}

int R_check_class_and_super(SEXP x, const char **valid, SEXP rho) {
	return (int) unimplemented("R_check_class_and_super");
}

int R_check_class_etc (SEXP x, const char **valid) {
	return (int) unimplemented("R_check_class_etc");
}

SEXP R_PreserveObject(SEXP x) {
	return unimplemented("R_PreserveObject");
}

void R_ReleaseObject(SEXP x) {
	unimplemented("R_ReleaseObject");
}

Rboolean R_compute_identical(SEXP x, SEXP y, int flags) {
	IMPORT_CALLHELPER();
	return (Rboolean) truffle_invoke(obj, "R_compute_identical", x, y, flags);
}

void Rf_copyListMatrix(SEXP s, SEXP t, Rboolean byrow) {
	IMPORT_CALLHELPER();
	truffle_invoke(obj, "Rf_copyListMatrix", s, t, byrow);
}

void Rf_copyMatrix(SEXP s, SEXP t, Rboolean byrow) {
	IMPORT_CALLHELPER();
	truffle_invoke(obj, "Rf_copyMatrix", s, t, byrow);
}
