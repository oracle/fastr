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
#include <Rinterface.h>
#include <rffiutils.h>
#include <rffi_callbacks.h>

void *callbacks[CALLBACK_TABLE_SIZE];

void Rinternals_addCallback(int index, void *closure) {
	newClosureRef(closure);
	callbacks[index] = closure;
}

static SEXP unimplemented(char *f) {
	printf("unimplemented %s\n", f);
	exit(1);
	return NULL;
}

static int* return_int;
static double* return_double;
static char* return_byte;

long return_INTEGER_CREATE(int *value, int len) {
	int* idata = malloc(len * sizeof(int));
	memcpy(idata, value, len * sizeof(int));
	return_int = idata;
	return (long) idata;
}

long return_DOUBLE_CREATE(double *value, int len) {
	double* ddata = malloc(len * sizeof(double));
	memcpy(ddata, value, len * sizeof(double));
	return_double = ddata;
	return (long) ddata;
}

long return_BYTE_CREATE(char *value, int len, int isString) {
	if (isString) {
		len += 1;
	}
	char* bdata = malloc(len * sizeof(char));
	memcpy(bdata, value, len * sizeof(char));
	if (isString) {
		bdata[len] = 0;
	}
	return_byte = bdata;
	return (long) bdata;
}

void return_INTEGER_EXISTING(long address) {
	return_int = (int*) address;
}

void return_DOUBLE_EXISTING(long address) {
	return_double = (double*) address;
}

void return_BYTE_EXISTING(long address) {
	return_byte = (char*) address;
}

void return_FREE(void *address) {
//	free(address);
}

// R_GlobalEnv et al are not a variables in FASTR as they are RContext specific
SEXP FASTR_R_GlobalEnv() {
	return unimplemented("FASTR_R_GlobalEnv");
}

SEXP FASTR_R_BaseEnv() {
	return ((call_R_BaseEnv) callbacks[R_BaseEnv_x])();
}

SEXP FASTR_R_BaseNamespace() {
	return unimplemented("FASTR_R_BaseNamespace");
}

SEXP FASTR_R_NamespaceRegistry() {
	return unimplemented("FASTR_R_NamespaceRegistry");
}

CTXT FASTR_GlobalContext() {
	return unimplemented("FASTR_GlobalContext");
}

Rboolean FASTR_R_Interactive() {
	return (int) unimplemented("FASTR_R_Interactive");
}

SEXP CAR(SEXP e) {
	return ((call_CAR) callbacks[CAR_x])(e);
}

SEXP CDR(SEXP e) {
	return ((call_CDR) callbacks[CDR_x])(e);
}

int *INTEGER(SEXP x) {
	((call_INTEGER) callbacks[INTEGER_x])(x);
	return return_int;
}

int *LOGICAL(SEXP x){
	((call_LOGICAL) callbacks[LOGICAL_x])(x);
	return return_int;
}

double *REAL(SEXP x){
	((call_REAL) callbacks[REAL_x])(x);
	return return_double;
}

Rbyte *RAW(SEXP x) {
	((call_RAW) callbacks[RAW_x])(x);
		return return_byte;
}

int LENGTH(SEXP x) {
	return ((call_LENGTH) callbacks[LENGTH_x])(x);
}

const char * R_CHAR(SEXP x) {
	((call_R_CHAR) callbacks[R_CHAR_x])(x);
	return return_byte;
}

SEXP Rf_ScalarString(SEXP value) {
	return ((call_Rf_ScalarString) callbacks[Rf_ScalarString_x])(value);
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
	return ((call_Rf_mkCharLenCE) callbacks[Rf_mkCharLenCE_x])(x,len, enc);
}

SEXP Rf_mkString(const char *s) {
	return ScalarString(Rf_mkChar(s));
}

void Rf_gsetVar(SEXP symbol, SEXP value, SEXP rho) {
	((call_Rf_gsetVar) callbacks[Rf_gsetVar_x])(symbol, value, rho);
}

SEXP Rf_coerceVector(SEXP x, SEXPTYPE mode) {
	return ((call_Rf_coerceVector) callbacks[Rf_coerceVector_x])(x, mode);
}

SEXP Rf_protect(SEXP x) {
	return x;
}

void Rf_unprotect(int x) {
	// nothing to do
}

SEXP Rf_cons(SEXP car, SEXP cdr) {
	return ((call_Rf_cons) callbacks[Rf_cons_x])(car, cdr);
}

SEXP R_FindNamespace(SEXP info) {
	return ((call_R_FindNamespace) callbacks[R_FindNamespace_x])(info);
}

SEXP Rf_GetOption1(SEXP tag) {
	return ((call_Rf_GetOption1) callbacks[Rf_GetOption1_x])(tag);
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
	// TODO fix this
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
	// This will set a pending exception (in JNI)
	((call_Rf_error) callbacks[Rf_error_x])(buf);
	// just transfer back which will cleanup and exit the entire JNI call
//	longjmp(*getErrorJmpBuf(), 1);

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
	((call_Rf_warningcall) callbacks[Rf_warningcall_x])(x, buf);
}

void Rf_warning(const char *format, ...) {
	char buf[8192];
	va_list(ap);
	va_start(ap,format);
	Rvsnprintf(buf, BUFSIZE - 1, format, ap);
	va_end(ap);
	((call_Rf_warning) callbacks[Rf_warning_x])(buf);
}

void Rprintf(const char *format, ...) {
	char buf[8192];
	va_list(ap);
	va_start(ap,format);
	Rvsnprintf(buf, BUFSIZE - 1, format, ap);
	va_end(ap);
	((call_Rprintf) callbacks[Rprintf_x])(buf);
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
	// TODO
}

void Rvprintf(const char *format, va_list args) {
	unimplemented("Rvprintf");
}
void REvprintf(const char *format, va_list args) {
	unimplemented("REvprintf");
}


SEXP Rf_ScalarInteger(int value) {
	return ((call_Rf_ScalarInteger) callbacks[Rf_ScalarInteger_x])(value);
}

SEXP Rf_ScalarReal(double value) {
	return ((call_Rf_ScalarReal) callbacks[Rf_ScalarDouble_x])(value);
}

SEXP Rf_ScalarLogical(int value) {
	return ((call_Rf_ScalarLogical) callbacks[Rf_ScalarLogical_x])(value);
}

SEXP Rf_allocVector3(SEXPTYPE t, R_xlen_t len, R_allocator_t* allocator) {
    if (allocator != NULL) {
  	    unimplemented("RF_allocVector with custom allocator");
	    return NULL;
    }
    return ((call_Rf_allocateVector) callbacks[Rf_allocateVector_x])(t, len);
}

SEXP Rf_allocArray(SEXPTYPE t, SEXP dims) {
	unimplemented("Rf_allocArray");
}

SEXP Rf_alloc3DArray(SEXPTYPE t, int x, int y, int z) {
	return unimplemented("Rf_alloc3DArray");
}

SEXP Rf_allocMatrix(SEXPTYPE mode, int nrow, int ncol) {
	unimplemented("Rf_allocMatrix");
}

SEXP Rf_allocList(int x) {
	unimplemented("Rf_allocList)");
	return NULL;
}

SEXP Rf_allocSExp(SEXPTYPE t) {
	return unimplemented("Rf_allocSExp");
}

void Rf_defineVar(SEXP symbol, SEXP value, SEXP rho) {
	((call_Rf_defineVar) callbacks[Rf_defineVar_x])(symbol, value, rho);
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

SEXP Rf_findVar(SEXP sym, SEXP rho) {
	return ((call_Rf_findVar) callbacks[Rf_findVar_x])(sym, rho);
}

SEXP Rf_findVarInFrame(SEXP rho, SEXP sym) {
	return unimplemented("Rf_findVarInFrame");
}

SEXP Rf_findVarInFrame3(SEXP rho, SEXP sym, Rboolean b) {
	return unimplemented("Rf_findVarInFrame");
}

SEXP Rf_getAttrib(SEXP vec, SEXP name) {
	SEXP result = ((call_Rf_getAttrib) callbacks[Rf_getAttrib_x])(vec, name);
//	printf("Rf_getAttrib: %p\n", result);
	return result;
}

SEXP Rf_setAttrib(SEXP vec, SEXP name, SEXP val) {
	return ((call_Rf_setAttrib) callbacks[Rf_setAttrib_x])(vec, name, val);
}

SEXP Rf_duplicate(SEXP x) {
	return unimplemented("Rf_duplicate");
}

SEXP Rf_shallow_duplicate(SEXP x) {
	return unimplemented("Rf_shallow_duplicate");
}

R_xlen_t Rf_any_duplicated(SEXP x, Rboolean from_last) {
	return (R_xlen_t) unimplemented("Rf_any_duplicated");
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
	return (Rboolean) unimplemented("Rf_inherits");
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
	((call_Rf_install) callbacks[Rf_install_x])(name);
}

SEXP Rf_installChar(SEXP charsxp) {
	return ((call_Rf_installChar) callbacks[Rf_installChar_x])(charsxp);
}

Rboolean Rf_isNull(SEXP s) {
	return (Rboolean) ((call_Rf_isNull) callbacks[Rf_isNull_x])(s);
}

Rboolean Rf_isString(SEXP s) {
	return (Rboolean) ((call_Rf_isString) callbacks[Rf_isString_x])(s);
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

const char *Rf_reEnc(const char *x, cetype_t ce_in, cetype_t ce_out, int subst) {
	// TODO proper implementation
	return x;
}

int Rf_ncols(SEXP x) {
	return (int) unimplemented("Rf_ncols");
}

int Rf_nrows(SEXP x) {
	return (int) unimplemented("Rf_nrows");
}


void R_ProtectWithIndex(SEXP x, PROTECT_INDEX *y) {
	//
}

void R_Reprotect(SEXP x, PROTECT_INDEX y) {
	//
}


void Rf_unprotect_ptr(SEXP x) {
	//
}


void R_FlushConsole(void) {
	// ignored
}

void R_ProcessEvents(void) {
	unimplemented("R_ProcessEvents");
}

// Tools package support, not in public API
SEXP R_NewHashedEnv(SEXP parent, SEXP size) {
	return ((call_R_NewHashedEnv) callbacks[R_NewHashedEnv_x])(parent, size);
}

SEXP Rf_classgets(SEXP vec, SEXP klass) {
	return unimplemented("Rf_classgets");
}

const char *Rf_translateChar(SEXP x) {
	// TODO: proper implementation
	const char *result = CHAR(x);
	return result;
}

const char *Rf_translateChar0(SEXP x) {
	// TODO: proper implementation
	const char *result = CHAR(x);
	return result;
}

const char *Rf_translateCharUTF8(SEXP x) {
	// TODO: proper implementation
	const char *result = CHAR(x);
	return result;
}

SEXP Rf_lengthgets(SEXP x, R_len_t y) {
	return unimplemented("Rf_lengthgets");
}

SEXP Rf_xlengthgets(SEXP x, R_xlen_t y) {
	return unimplemented("Rf_xlengthgets");
}

SEXP R_lsInternal(SEXP env, Rboolean all) {
	return R_lsInternal3(env, all, TRUE);
}

SEXP R_lsInternal3(SEXP env, Rboolean all, Rboolean sorted) {
	return unimplemented("R_lsInternal3");
}

SEXP Rf_namesgets(SEXP x, SEXP y) {
	return unimplemented("Rf_namesgets");
}

SEXP GetOption(SEXP tag, SEXP rho) {
    return GetOption1(tag);
}

int GetOptionCutoff(void) {
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

int GetOptionWidth(void) {
    int w;
    w = asInteger(GetOption1(install("width")));
    if (w < R_MIN_WIDTH_OPT || w > R_MAX_WIDTH_OPT) {
	warning(_("invalid printing width, used 80"));
	return 80;
    }
    return w;
}

int GetOptionDigits(void) {
    int d;
    d = asInteger(GetOption1(install("digits")));
    if (d < R_MIN_DIGITS_OPT || d > R_MAX_DIGITS_OPT) {
	warning(_("invalid printing digits, used 7"));
	return 7;
    }
    return d;
}

Rboolean Rf_GetOptionDeviceAsk(void) {
    int ask;
    ask = asLogical(GetOption1(install("device.ask.default")));
    if(ask == NA_LOGICAL) {
	warning(_("invalid value for \"device.ask.default\", using FALSE"));
	return FALSE;
    }
    return ask != 0;
}

SEXP TAG(SEXP e) {
	return ((call_TAG) callbacks[TAG_x])(e);
}

SEXP PRINTNAME(SEXP e) {
	return unimplemented("PRINTNAME");
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
	return ((call_CADR) callbacks[CADR_x])(e);
}

SEXP CDDR(SEXP e) {
	return ((call_CDDR) callbacks[CDDR_x])(e);
}

SEXP CDDDR(SEXP e) {
    unimplemented("CDDDR");
    return NULL;
}

SEXP CADDR(SEXP e) {
	return unimplemented("CADDR");
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
	((call_SET_TAG) callbacks[SET_TAG_x])(x, y);
}

SEXP SETCAR(SEXP x, SEXP y) {
	return ((call_SETCAR) callbacks[SETCAR_x])(x, y);
}

SEXP SETCDR(SEXP x, SEXP y) {
	return ((call_SETCDR) callbacks[SETCDR_x])(x, y);
}

SEXP SETCADR(SEXP x, SEXP y) {
	return unimplemented("SETCADR");
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
}

int RSTEP(SEXP x) {
	unimplemented("RSTEP");
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
	return unimplemented("PRENV");
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

Rcomplex *COMPLEX(SEXP x){
	unimplemented("COMPLEX");
	return NULL;
}

SEXP STRING_ELT(SEXP x, R_xlen_t i) {
	return ((call_STRING_ELT) callbacks[STRING_ELT_x])(x, i);
}


SEXP VECTOR_ELT(SEXP x, R_xlen_t i){
	return ((call_VECTOR_ELT) callbacks[VECTOR_ELT_x])(x, i);
}

void SET_STRING_ELT(SEXP x, R_xlen_t i, SEXP v){
    ((call_SET_STRING_ELT) callbacks[SET_STRING_ELT_x])(x, i, v);
}


SEXP SET_VECTOR_ELT(SEXP x, R_xlen_t i, SEXP v){
	return ((call_SET_VECTOR_ELT) callbacks[SET_VECTOR_ELT_x])(x, i, v);
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
	return ((call_Rf_asChar) callbacks[Rf_asChar_x])(x);
}

SEXP Rf_PairToVectorList(SEXP x){
	return ((call_Rf_PairToVectorList) callbacks[Rf_PairToVectorList_x])(x);
}

SEXP Rf_VectorToPairList(SEXP x){
	return unimplemented("Rf_VectorToPairList");
}

SEXP Rf_asCharacterFactor(SEXP x){
	unimplemented("Rf_VectorToPairList");
	return NULL;
}

int Rf_asLogical(SEXP x){
	return ((call_Rf_asLogical) callbacks[Rf_asLogical_x])(x);
}

int Rf_asInteger(SEXP x) {
	return ((call_Rf_asInteger) callbacks[Rf_asInteger_x])(x);
}

double Rf_asReal(SEXP x) {
	return ((call_Rf_asReal) callbacks[Rf_asReal_x])(x);
}

Rcomplex Rf_asComplex(SEXP x){
	unimplemented("Rf_asLogical");
	Rcomplex c; return c;
}

int TYPEOF(SEXP x) {
	return (int) ((call_TYPEOF) callbacks[TYPEOF_x])(x);
}

SEXP ATTRIB(SEXP x){
    unimplemented("ATTRIB");
    return NULL;
}

int OBJECT(SEXP x){
	return (int) unimplemented("OBJECT");
}

int MARK(SEXP x){
    unimplemented("MARK");
    return 0;
}

int NAMED(SEXP x){
	return (int) unimplemented("NAMED");
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
}

void SET_NAMED(SEXP x, int v){
    unimplemented("SET_NAMED");
}

void SET_ATTRIB(SEXP x, SEXP v){
    unimplemented("SET_ATTRIB");
}

void DUPLICATE_ATTRIB(SEXP to, SEXP from){
	unimplemented("DUPLICATE_ATTRIB");
}

char *dgettext(const char *domainname, const char *msgid) {
	printf("dgettext: '%s'\n", msgid);
	return (char*) msgid;
}

char *libintl_dgettext(const char *domainname, const char *msgid) {
	return dgettext(domainname, msgid);
}

char *dngettext(const char *domainname, const char *msgid, const char * msgid_plural, unsigned long int n) {
    printf("dngettext: singular - '%s' ; plural - '%s'\n", msgid, msgid_plural);
    return (char*) (n == 1 ? msgid : msgid_plural);
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
	return (Rboolean) unimplemented("R_BindingIsLocked");
}

Rboolean R_BindingIsActive(SEXP sym, SEXP env) {
    // TODO: for now, I belive all bindings are false
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

static SEXP R_tryEvalInternal(SEXP x, SEXP y, int *ErrorOccurred, int silent) {
}

SEXP R_tryEval(SEXP x, SEXP y, int *ErrorOccurred) {
	return R_tryEvalInternal(x, y, ErrorOccurred, 0);
}

SEXP R_tryEvalSilent(SEXP x, SEXP y, int *ErrorOccurred) {
	return R_tryEvalInternal(x, y, ErrorOccurred, 1);
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
	return unimplemented("R_MakeExternalPtr");
}

void *R_ExternalPtrAddr(SEXP s) {
	return unimplemented("R_ExternalPtrAddr");
}

SEXP R_ExternalPtrTag(SEXP s) {
	return unimplemented("R_ExternalPtrTag");
}

SEXP R_ExternalPtrProt(SEXP s) {
	return unimplemented("R_ExternalPtrProt");
}

void R_SetExternalPtrAddr(SEXP s, void *p) {
	unimplemented("R_SetExternalPtrAddr");
}

void R_SetExternalPtrTag(SEXP s, SEXP tag) {
	unimplemented("R_SetExternalPtrTag");
}

void R_SetExternalPtrProtected(SEXP s, SEXP p) {
	unimplemented("R_SetExternalPtrProtected");
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

SEXP R_PreserveObject(SEXP x) {
	return newObjectRef(x);
}

void R_ReleaseObject(SEXP x) {
	releaseObjectRef(x);
}

void R_dot_Last(void) {
	unimplemented("R_dot_Last");
}


Rboolean R_compute_identical(SEXP x, SEXP y, int flags) {
	return (Rboolean) unimplemented("R_compute_identical");
}

void Rf_copyListMatrix(SEXP s, SEXP t, Rboolean byrow) {
	unimplemented("Rf_copyListMatrix");
}

void Rf_copyMatrix(SEXP s, SEXP t, Rboolean byrow) {
	unimplemented("Rf_copyMatrix");
}

