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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * This class defines methods that match the functionality of the macro/function definitions in the
 * R header files, e.g. {@code Rinternals.h} that are used by C/C++ code to call into the R
 * implementation. For ease of identification, we use method names that match the names in the R
 * header files. These methods should never be called from normal FastR code.
 *
 * The set is incomplete; these are the functions that have been found to be used to at this time of
 * writing. From the GNU R perspective all {@code Object} parameters are {@code SEXP} instances.
 * Some of the functions are typed with a specific return type but, again, this is a {@code SEXP} in
 * GNU R terms. The native side does not require a specific Java type.
 *
 * N.B. It is important not to be too specific about types owing the support for Truffle interop
 * implementations. For example, many arguments are "strings" but we do not specify them as
 * {@code String} here.
 */
public interface StdUpCallsRFFI {
    // Checkstyle: stop method name check

    RIntVector Rf_ScalarInteger(int value);

    RLogicalVector Rf_ScalarLogical(int value);

    RDoubleVector Rf_ScalarDouble(double value);

    RStringVector Rf_ScalarString(Object value);

    int Rf_asInteger(Object x);

    double Rf_asReal(Object x);

    int Rf_asLogical(Object x);

    Object Rf_asChar(Object x);

    Object Rf_coerceVector(Object x, int mode);

    Object Rf_mkCharLenCE(@RFFICstring(convert = false) Object bytes, int len, int encoding);

    Object Rf_cons(Object car, Object cdr);

    void Rf_defineVar(Object symbolArg, Object value, Object envArg);

    Object R_do_MAKE_CLASS(@RFFICstring Object clazz);

    /**
     * WARNING: argument order reversed from Rf_findVarInFrame!
     */
    Object Rf_findVar(Object symbolArg, Object envArg);

    Object Rf_findVarInFrame(Object envArg, Object symbolArg);

    Object Rf_findVarInFrame3(Object envArg, Object symbolArg, int doGet);

    Object Rf_getAttrib(Object obj, Object name);

    void Rf_setAttrib(Object obj, Object name, Object val);

    int Rf_inherits(@RFFICstring Object x, Object clazz);

    Object Rf_install(@RFFICstring Object name);

    Object Rf_installChar(Object name);

    Object Rf_lengthgets(Object x, int newSize);

    int Rf_isString(Object x);

    int Rf_isNull(Object x);

    Object Rf_PairToVectorList(Object x);

    void Rf_error(@RFFICstring Object msg);

    void Rf_warning(@RFFICstring Object msg);

    void Rf_warningcall(Object call, @RFFICstring Object msg);

    Object Rf_allocateVector(int mode, int n);

    Object Rf_allocateArray(int mode, Object dimsObj);

    Object Rf_allocateMatrix(int mode, int nrow, int ncol);

    int Rf_nrows(Object x);

    int Rf_ncols(Object x);

    int LENGTH(Object x);

    void SET_STRING_ELT(Object x, int i, Object v);

    void SET_VECTOR_ELT(Object x, int i, Object v);

    Object RAW(Object x);

    Object LOGICAL(Object x);

    Object INTEGER(Object x);

    Object REAL(Object x);

    Object STRING_ELT(Object x, int i);

    Object VECTOR_ELT(Object x, int i);

    int NAMED(Object x);

    Object SET_TYPEOF_FASTR(Object x, int v);

    int TYPEOF(Object x);

    int OBJECT(Object x);

    Object Rf_duplicate(Object x, int deep);

    int Rf_anyDuplicated(Object x, int fromLast);

    Object PRINTNAME(Object x);

    Object TAG(Object e);

    Object CAR(Object e);

    Object CDR(Object e);

    Object CADR(Object e);

    Object CADDR(Object e);

    Object CDDR(Object e);

    Object SET_TAG(Object x, Object y);

    Object SETCAR(Object x, Object y);

    Object SETCDR(Object x, Object y);

    Object SETCADR(Object x, Object y);

    Object SYMVALUE(Object x);

    void SET_SYMVALUE(Object x, Object v);

    int R_BindingIsLocked(Object sym, Object env);

    Object R_FindNamespace(Object name);

    Object Rf_eval(Object expr, Object env);

    Object Rf_findfun(Object symbolObj, Object envObj);

    Object Rf_GetOption1(Object tag);

    void Rf_gsetVar(Object symbol, Object value, Object rho);

    void DUPLICATE_ATTRIB(Object to, Object from);

    int R_computeIdentical(Object x, Object y, int flags);

    void Rf_copyListMatrix(Object s, Object t, int byrow);

    void Rf_copyMatrix(Object s, Object t, int byrow);

    Object R_tryEval(Object expr, Object env, boolean silent);

    Object R_ToplevelExec();

    int RDEBUG(Object x);

    void SET_RDEBUG(Object x, int v);

    int RSTEP(Object x);

    void SET_RSTEP(Object x, int v);

    Object ENCLOS(Object x);

    Object PRVALUE(Object x);

    Object R_ParseVector(Object text, int n, Object srcFile);

    Object R_lsInternal3(Object envArg, int allArg, int sortedArg);

    String R_HomeDir();

    int IS_S4_OBJECT(Object x);

    void Rprintf(@RFFICstring Object message);

    void GetRNGstate();

    void PutRNGstate();

    double unif_rand();

    Object Rf_classgets(Object x, Object y);

    RExternalPtr R_MakeExternalPtr(long addr, Object tag, Object prot);

    long R_ExternalPtrAddr(Object x);

    Object R_ExternalPtrTag(Object x);

    Object R_ExternalPtrProt(Object x);

    void R_SetExternalPtrAddr(Object x, long addr);

    void R_SetExternalPtrTag(Object x, Object tag);

    void R_SetExternalPtrProt(Object x, Object prot);

    void R_CleanUp(int sa, int status, int runlast);

    REnvironment R_NewHashedEnv(REnvironment parent, Object initialSize);

    int PRSEEN(Object x);

    Object PRENV(Object x);

    Object R_PromiseExpr(Object x);

    Object PRCODE(Object x);

    Object R_CHAR(Object x);

}
