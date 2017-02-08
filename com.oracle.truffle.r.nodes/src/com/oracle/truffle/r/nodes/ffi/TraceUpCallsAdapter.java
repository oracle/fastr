/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.ffi;

import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.ffi.UpCallsRFFI;

public class TraceUpCallsAdapter implements UpCallsRFFI {
    @Override
    public RIntVector Rf_ScalarInteger(int value) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_ScalarInteger", value);
        }
        return null;
    }

    @Override
    public RLogicalVector Rf_ScalarLogical(int value) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_ScalarLogical", value);
        }
        return null;
    }

    @Override
    public RDoubleVector Rf_ScalarDouble(double value) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_ScalarDouble", value);
        }
        return null;
    }

    @Override
    public RStringVector Rf_ScalarString(Object value) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_ScalarString", value);
        }
        return null;
    }

    @Override
    public int Rf_asInteger(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_asInteger", x);
        }
        return 0;
    }

    @Override
    public double Rf_asReal(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_asReal", x);
        }
        return 0;
    }

    @Override
    public int Rf_asLogical(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_asLogical", x);
        }
        return 0;
    }

    @Override
    public Object Rf_asChar(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_asChar", x);
        }
        return null;
    }

    @Override
    public Object Rf_mkCharLenCE(Object bytes, int len, int encoding) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_mkCharLenCE", bytes);
        }
        return null;
    }

    @Override
    public Object Rf_cons(Object car, Object cdr) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_cons", car, cdr);
        }
        return null;
    }

    @Override
    public void Rf_defineVar(Object symbolArg, Object value, Object envArg) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_defineVar", symbolArg, value, envArg);
        }
    }

    @Override
    public Object R_do_MAKE_CLASS(Object clazz) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_do_MAKE_CLASS", clazz);
        }
        return null;
    }

    @Override
    public Object Rf_findVar(Object symbolArg, Object envArg) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_findVar", symbolArg, envArg);
        }
        return null;
    }

    @Override
    public Object Rf_findVarInFrame(Object envArg, Object symbolArg) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_findVarInFrame", envArg, symbolArg);
        }
        return null;
    }

    @Override
    public Object Rf_findVarInFrame3(Object envArg, Object symbolArg, int doGet) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_findVarInFrame3", envArg, symbolArg);
        }
        return null;
    }

    @Override
    public Object Rf_getAttrib(Object obj, Object name) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_getAttrib", obj, name);
        }
        return null;
    }

    @Override
    public void Rf_setAttrib(Object obj, Object name, Object val) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_setAttrib", obj, name, val);
        }
    }

    @Override
    public int Rf_inherits(Object x, Object clazz) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_inherits", x, clazz);
        }
        return 0;
    }

    @Override
    public Object Rf_install(Object name) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_install", name);
        }
        return null;
    }

    @Override
    public Object Rf_lengthgets(Object x, int newSize) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_lengthgets", x, newSize);
        }
        return null;
    }

    @Override
    public int Rf_isString(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_isString", x);
        }
        return 0;
    }

    @Override
    public int Rf_isNull(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_isNull", x);
        }
        return 0;
    }

    @Override
    public Object Rf_PairToVectorList(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_PairToVectorList", x);
        }
        return null;
    }

    @Override
    public void Rf_error(Object msg) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_error", msg);
        }
    }

    @Override
    public void Rf_warning(Object msg) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_warning", msg);
        }
    }

    @Override
    public void Rf_warningcall(Object call, Object msg) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_warningcall", call, msg);
        }
    }

    @Override
    public Object Rf_allocateVector(int mode, int n) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_allocateVector", mode, n);
        }
        return null;
    }

    @Override
    public Object Rf_allocateArray(int mode, Object dimsObj) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_allocateArray", mode, dimsObj);
        }
        return null;
    }

    @Override
    public Object Rf_allocateMatrix(int mode, int nrow, int ncol) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_allocateMatrix", mode, ncol, nrow);
        }
        return null;
    }

    @Override
    public int Rf_nrows(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_nrows", x);
        }
        return 0;
    }

    @Override
    public int Rf_ncols(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_ncols", x);
        }
        return 0;
    }

    @Override
    public int LENGTH(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("LENGTH", x);
        }
        return 0;
    }

    @Override
    public void SET_STRING_ELT(Object x, int i, Object v) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("SET_STRING_ELT", x, i, v);
        }
    }

    @Override
    public void SET_VECTOR_ELT(Object x, int i, Object v) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("SET_VECTOR_ELT", i, v);
        }
    }

    @Override
    public Object RAW(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("RAW", x);
        }
        return null;
    }

    @Override
    public Object LOGICAL(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("LOGICAL", x);
        }
        return null;
    }

    @Override
    public Object INTEGER(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("INTEGER", x);
        }
        return null;
    }

    @Override
    public Object REAL(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("REAL", x);
        }
        return null;
    }

    @Override
    public Object STRING_ELT(Object x, int i) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("STRING_ELT", x, i);
        }
        return null;
    }

    @Override
    public Object VECTOR_ELT(Object x, int i) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("VECTOR_ELT", x, i);
        }
        return null;
    }

    @Override
    public int NAMED(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("NAMED", x);
        }
        return 0;
    }

    @Override
    public Object SET_TYPEOF_FASTR(Object x, int v) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("SET_TYPEOF_FASTR", x, v);
        }
        return null;
    }

    @Override
    public int TYPEOF(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("TYPEOF", x);
        }
        return 0;
    }

    @Override
    public int OBJECT(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("OBJECT", x);
        }
        return 0;
    }

    @Override
    public Object Rf_duplicate(Object x, int deep) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_duplicate", x, deep);
        }
        return null;
    }

    @Override
    public int Rf_anyDuplicated(Object x, int fromLast) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_anyDuplicated", x, fromLast);
        }
        return 0;
    }

    @Override
    public Object PRINTNAME(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("PRINTNAME", x);
        }
        return null;
    }

    @Override
    public Object TAG(Object e) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("TAG", e);
        }
        return null;
    }

    @Override
    public Object CAR(Object e) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("CAR", e);
        }
        return null;
    }

    @Override
    public Object CDR(Object e) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("CDR", e);
        }
        return null;
    }

    @Override
    public Object CADR(Object e) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("CADR", e);
        }
        return null;
    }

    @Override
    public Object CADDR(Object e) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("CADDR", e);
        }
        return null;
    }

    @Override
    public Object CDDR(Object e) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("CDDR", e);
        }
        return null;
    }

    @Override
    public Object SET_TAG(Object x, Object y) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("SET_TAG", x, y);
        }
        return null;
    }

    @Override
    public Object SETCAR(Object x, Object y) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("SETCAR", x, y);
        }
        return null;
    }

    @Override
    public Object SETCDR(Object x, Object y) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("SETCDR", x, y);
        }
        return null;
    }

    @Override
    public Object SETCADR(Object x, Object y) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("SETCADR", x);
        }
        return null;
    }

    @Override
    public Object SYMVALUE(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("SYMVALUE", x);
        }
        return null;
    }

    @Override
    public void SET_SYMVALUE(Object x, Object v) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("SET_SYMVALUE", x, v);
        }
    }

    @Override
    public int R_BindingIsLocked(Object sym, Object env) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_BindingIsLocked", sym, env);
        }
        return 0;
    }

    @Override
    public Object R_FindNamespace(Object name) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_FindNamespace", name);
        }
        return null;
    }

    @Override
    public Object Rf_eval(Object expr, Object env) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_eval", expr, env);
        }
        return null;
    }

    @Override
    public Object Rf_findfun(Object symbolObj, Object envObj) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_findfun", symbolObj, envObj);
        }
        return null;
    }

    @Override
    public Object Rf_GetOption1(Object tag) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_GetOption1", tag);
        }
        return null;
    }

    @Override
    public void Rf_gsetVar(Object symbol, Object value, Object rho) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_gsetVar", symbol, value, rho);
        }
    }

    @Override
    public void DUPLICATE_ATTRIB(Object to, Object from) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("DUPLICATE_ATTRIB", to, from);
        }
    }

    @Override
    public int R_computeIdentical(Object x, Object y, int flags) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_computeIdentical", x, y, flags);
        }
        return 0;
    }

    @Override
    public void Rf_copyListMatrix(Object s, Object t, int byrow) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_copyListMatrix", t, byrow);
        }
    }

    @Override
    public void Rf_copyMatrix(Object s, Object t, int byrow) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_copyMatrix", t, byrow);
        }
    }

    @Override
    public Object R_tryEval(Object expr, Object env, boolean silent) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_tryEval", expr, env, silent);
        }
        return null;
    }

    @Override
    public Object R_ToplevelExec() {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_TopLevelExec");
        }
        return null;
    }

    @Override
    public int RDEBUG(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("RDEBUG", x);
        }
        return 0;
    }

    @Override
    public void SET_RDEBUG(Object x, int v) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("SET_RDEBUG", x, v);
        }
    }

    @Override
    public int RSTEP(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("RSTEP", x);
        }
        return 0;
    }

    @Override
    public void SET_RSTEP(Object x, int v) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("SET_RSTEP", x, v);
        }
    }

    @Override
    public Object ENCLOS(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("ENCLOS", x);
        }
        return null;
    }

    @Override
    public Object PRVALUE(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("PRVALUE", x);
        }
        return null;
    }

    @Override
    public Object R_ParseVector(Object text, int n, Object srcFile) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_ParseVector", text, n, srcFile);
        }
        return null;
    }

    @Override
    public Object R_lsInternal3(Object envArg, int allArg, int sortedArg) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_lsInternal3", envArg, allArg, sortedArg);
        }
        return null;
    }

    @Override
    public String R_HomeDir() {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_HomeDir");
        }
        return null;
    }

    @Override
    public void R_CleanUp(int sa, int status, int runlast) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_Cleanup", sa, status, runlast);
        }
    }

    @Override
    public Object R_GlobalContext() {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_GlobalContext");
        }
        return null;
    }

    @Override
    public Object R_GlobalEnv() {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_GlobalEnv");
        }
        return null;
    }

    @Override
    public Object R_BaseEnv() {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_BaseEnv");
        }
        return null;
    }

    @Override
    public Object R_BaseNamespace() {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_BaseNamespace");
        }
        return null;
    }

    @Override
    public Object R_NamespaceRegistry() {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_NamespaceRegistry");
        }
        return null;
    }

    @Override
    public int R_Interactive() {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("isInteractive");
        }
        return 0;
    }

    @Override
    public int IS_S4_OBJECT(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("isS4Object");
        }
        return 0;
    }

    @Override
    public void Rprintf(Object message) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rprintf", message);
        }
    }

    @Override
    public void GetRNGstate() {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("GetRNGstate");
        }
    }

    @Override
    public void PutRNGstate() {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("PutRNGstate");
        }
    }

    @Override
    public double unif_rand() {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("unif_rand");
        }
        return 0;
    }

    @Override
    public Object R_getGlobalFunctionContext() {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_getGlobalFunctionContext");
        }
        return null;
    }

    @Override
    public Object R_getParentFunctionContext(Object c) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_getParentFunctionContext");
        }
        return null;
    }

    @Override
    public Object R_getContextEnv(Object c) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_getContextEnv", c);
        }
        return null;
    }

    @Override
    public Object R_getContextFun(Object c) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_getContextFun", c);
        }
        return null;
    }

    @Override
    public Object R_getContextCall(Object c) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_getContextCall", c);
        }
        return null;
    }

    @Override
    public Object R_getContextSrcRef(Object c) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_getContextSrcRef", c);
        }
        return null;
    }

    @Override
    public int R_insideBrowser() {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_insideBrowser");
        }
        return 0;
    }

    @Override
    public int R_isGlobal(Object c) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_isGlobal", c);
        }
        return 0;
    }

    @Override
    public int R_isEqual(Object x, Object y) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("isEqual", x, y);
        }
        return 0;
    }

    @Override
    public Object Rf_classgets(Object x, Object y) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("Rf_classgets", x, y);
        }
        return null;
    }

    @Override
    public RExternalPtr R_MakeExternalPtr(long addr, Object tag, Object prot) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_MakeExternalPtr", addr, tag, prot);
        }
        return null;
    }

    @Override
    public long R_ExternalPtrAddr(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_ExternalPtrAddr", x);
        }
        return 0;
    }

    @Override
    public Object R_ExternalPtrTag(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_ExternalPtrTag", x);
        }
        return null;
    }

    @Override
    public Object R_ExternalPtrProt(Object x) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_ExternalPtrProt", x);
        }
        return null;
    }

    @Override
    public void R_SetExternalPtrAddr(Object x, long addr) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_SetExternalPtrAddr", x);
        }
    }

    @Override
    public void R_SetExternalPtrTag(Object x, Object tag) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_SetExternalPtrTag", x);
        }
    }

    @Override
    public void R_SetExternalPtrProt(Object x, Object prot) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_ExternalPtrProt", x);
        }
    }

    @Override
    public REnvironment R_NewHashedEnv(REnvironment parent, int initialSize) {
        if (RFFIUtils.traceEnabled()) {
            RFFIUtils.traceUpCall("R_NewHashedEnv", parent, initialSize);
        }
        return null;
    }
}
