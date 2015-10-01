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
#include "rffiutils.h"
#include <string.h>

// Most of the functions with a Rf_ prefix
// TODO Lots missing yet

static jmethodID Rf_ScalarIntegerMethodID;
static jmethodID Rf_ScalarDoubleMethodID;
static jmethodID Rf_ScalarStringMethodID;
static jmethodID Rf_ScalarLogicalMethodID;
static jmethodID Rf_allocateVectorMethodID;
static jmethodID Rf_allocateArrayMethodID;
static jmethodID Rf_allocateMatrixMethodID;
static jmethodID Rf_duplicateMethodID;
static jmethodID Rf_consMethodID;
static jmethodID Rf_evalMethodID;
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

void init_rf_functions(JNIEnv *env) {
	Rf_ScalarIntegerMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_ScalarInteger", "(I)Lcom/oracle/truffle/r/runtime/data/RIntVector;", 1);
	Rf_ScalarDoubleMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_ScalarDouble", "(D)Lcom/oracle/truffle/r/runtime/data/RDoubleVector;", 1);
	Rf_ScalarStringMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_ScalarString", "(Ljava/lang/String;)Lcom/oracle/truffle/r/runtime/data/RStringVector;", 1);
	Rf_ScalarLogicalMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_ScalarLogical", "(I)Lcom/oracle/truffle/r/runtime/data/RLogicalVector;", 1);
	Rf_consMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_cons", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_evalMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_eval", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1);
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
	Rf_NewHashedEnvMethodID = checkGetMethodID(env, RDataFactoryClass, "createNewEnv", "(Lcom/oracle/truffle/r/runtime/env/REnvironment;Ljava/lang/String;ZI)Lcom/oracle/truffle/r/runtime/env/REnvironment;", 1);
	RprintfMethodID = checkGetMethodID(env, CallRFFIHelperClass, "printf", "(Ljava/lang/String;)V", 1);
	R_FindNamespaceMethodID = checkGetMethodID(env, CallRFFIHelperClass, "R_FindNamespace", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_GetOption1MethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_GetOption1", "(Ljava/lang/Object;)Ljava/lang/Object;", 1);
	Rf_gsetVarMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_gsetVar", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", 1);
	Rf_inheritsMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_inherits", "(Ljava/lang/Object;Ljava/lang/String;)I", 1);
//	Rf_rPsortMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_rPsort", "(Lcom/oracle/truffle/r/runtime/data/RDoubleVector;II)", 1);
//	Rf_iPsortMethodID = checkGetMethodID(env, CallRFFIHelperClass, "Rf_iPsort", "(Lcom/oracle/truffle/r/runtime/data/RIntVector;II)", 1);
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

SEXP Rf_cons(SEXP car, SEXP cdr) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_consMethodID, car, cdr);
    return checkRef(thisenv, result);
}

void Rf_defineVar(SEXP symbol, SEXP value, SEXP rho) {
	JNIEnv *thisenv = getEnv();
	(*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_defineVarMethodID, symbol, value, rho);
}

SEXP Rf_eval(SEXP expr, SEXP env) {
    JNIEnv *thisenv = getEnv();
    SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_evalMethodID, expr, env);
    return checkRef(thisenv, result);
}

SEXP Rf_findFun(SEXP symbol, SEXP rho) {
	JNIEnv *thisenv = getEnv();
	unimplemented("Rf_eval)");
	return NULL;
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
	unimplemented("Rf_any_duplicated)");
	return 0;
}

SEXP Rf_duplicated(SEXP x, Rboolean y) {
	unimplemented("Rf_duplicated)");
	return NULL;
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

SEXP Rf_install(const char *name) {
	JNIEnv *thisenv = getEnv();
	jstring string = (*thisenv)->NewStringUTF(thisenv, name);
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, createSymbolMethodID, string);
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

cetype_t Rf_getCharCE(SEXP x) {
    // unimplemented("Rf_getCharCE");
    // TODO: real implementation
    return CE_NATIVE;
}

SEXP Rf_mkChar(const char *x) {
	JNIEnv *thisenv = getEnv();
	// TODO encoding, assume UTF for now
	SEXP result = (*thisenv)->NewStringUTF(thisenv, x);
	return checkRef(thisenv, result);
}

SEXP Rf_mkCharCE(const char *x, cetype_t y) {
	unimplemented("Rf_mkCharCE");
	return NULL;
}

SEXP Rf_mkCharLenCE(const char *x, int len, cetype_t enc) {
	JNIEnv *thisenv = getEnv();
	char buf[len + 1];
	memcpy(buf, x, len);
	buf[len] = 0;
	// TODO encoding, assume UTF for now, zero terminated
	SEXP result = (*thisenv)->NewStringUTF(thisenv, buf);
	return checkRef(thisenv, result);
}

SEXP Rf_mkString(const char *s) {
	JNIEnv *thisenv = getEnv();
	jstring string = (*thisenv)->NewStringUTF(thisenv, s);
	return ScalarString(string);
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

void R_FlushConsole(void) {
	// ignored
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

const char *Rf_type2char(SEXPTYPE x) {
	unimplemented("Rf_type2char");
	return NULL;
}

SEXP Rf_type2str(SEXPTYPE x) {
	unimplemented("Rf_type2str");
	return R_NilValue;
	return NULL;
}

SEXP R_FindNamespace(SEXP info) {
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, R_FindNamespaceMethodID, info);
	return checkRef(thisenv, result);
}


SEXP GetOption1(SEXP tag)
{
	JNIEnv *thisenv = getEnv();
	SEXP result = (*thisenv)->CallStaticObjectMethod(thisenv, CallRFFIHelperClass, Rf_GetOption1MethodID, tag);
	return checkRef(thisenv, result);
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

