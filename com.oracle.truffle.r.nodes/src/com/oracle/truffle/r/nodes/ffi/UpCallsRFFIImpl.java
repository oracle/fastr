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

import static com.oracle.truffle.r.nodes.ffi.RFFIUtils.guaranteeInstanceOf;

import com.oracle.truffle.r.runtime.RErrorHandling;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.ffi.CharSXPWrapper;
import com.oracle.truffle.r.runtime.ffi.UpCallsRFFI;

public final class UpCallsRFFIImpl implements UpCallsRFFI {
    // Checkstyle: stop method name check

    private final UpCallsRFFI delegate;
    private final boolean tracing;

    public UpCallsRFFIImpl(UpCallsRFFI delegate) {
        this.delegate = delegate;
        FFIUpCallRootNode.register();
        tracing = RFFIUtils.traceEnabled();
    }

    @Override
    public RIntVector Rf_ScalarInteger(int value) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_ScalarInteger", value);
        }
        return delegate.Rf_ScalarInteger(value);
    }

    @Override
    public RLogicalVector Rf_ScalarLogical(int value) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_ScalarLogical", value);
        }
        return delegate.Rf_ScalarLogical(value);
    }

    @Override
    public RDoubleVector Rf_ScalarDouble(double value) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_ScalarDouble", value);
        }
        return delegate.Rf_ScalarDouble(value);
    }

    @Override
    public RStringVector Rf_ScalarString(Object value) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_ScalarString", value);
        }
        return delegate.Rf_ScalarString(value);
    }

    @Override
    public int Rf_asInteger(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_asInteger", x);
        }
        return delegate.Rf_asInteger(x);
    }

    @Override
    public double Rf_asReal(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_asReal", x);
        }
        return delegate.Rf_asReal(x);
    }

    @Override
    public int Rf_asLogical(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_asLogical", x);
        }
        return delegate.Rf_asLogical(x);
    }

    @Override
    public Object Rf_asChar(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_asChar", x);
        }
        return delegate.Rf_asChar(x);
    }

    @Override
    public Object Rf_mkCharLenCE(Object bytes, int len, int encoding) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_mkCharLenCE", bytes);
        }
        return delegate.Rf_mkCharLenCE(bytes, len, encoding);
    }

    @Override
    public Object Rf_cons(Object car, Object cdr) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_cons", car, cdr);
        }
        return delegate.Rf_cons(car, cdr);
    }

    @Override
    public void Rf_defineVar(Object symbolArg, Object value, Object envArg) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_defineVar", symbolArg, value, envArg);
        }
        delegate.Rf_defineVar(symbolArg, value, envArg);
    }

    @Override
    public Object R_do_MAKE_CLASS(Object clazz) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_do_MAKE_CLASS", clazz);
        }
        return delegate.R_do_MAKE_CLASS(clazz);
    }

    @Override
    public Object Rf_findVar(Object symbolArg, Object envArg) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_findVar", symbolArg, envArg);
        }
        return delegate.Rf_findVar(symbolArg, envArg);
    }

    @Override
    public Object Rf_findVarInFrame(Object envArg, Object symbolArg) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_findVarInFrame", envArg, symbolArg);
        }
        return delegate.Rf_findVarInFrame(envArg, symbolArg);
    }

    @Override
    public Object Rf_findVarInFrame3(Object envArg, Object symbolArg, int doGet) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_findVarInFrame3", envArg, symbolArg);
        }
        return delegate.Rf_findVarInFrame3(envArg, symbolArg, doGet);
    }

    @Override
    public Object Rf_getAttrib(Object obj, Object name) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_getAttrib", obj, name);
        }
        return delegate.Rf_getAttrib(obj, name);
    }

    @Override
    public void Rf_setAttrib(Object obj, Object name, Object val) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_setAttrib", obj, name, val);
        }
        delegate.Rf_setAttrib(obj, name, val);
    }

    @Override
    public int Rf_inherits(Object x, Object clazz) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_inherits", x, clazz);
        }
        return delegate.Rf_inherits(x, clazz);
    }

    @Override
    public Object Rf_install(Object name) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_install", name);
        }
        return delegate.Rf_install(name);
    }

    @Override
    public Object Rf_lengthgets(Object x, int newSize) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_lengthgets", x, newSize);
        }
        return delegate.Rf_lengthgets(x, newSize);
    }

    @Override
    public int Rf_isString(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_isString", x);
        }
        return delegate.Rf_isString(x);
    }

    @Override
    public int Rf_isNull(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_isNull", x);
        }
        return delegate.Rf_isNull(x);
    }

    @Override
    public Object Rf_PairToVectorList(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_PairToVectorList", x);
        }
        return delegate.Rf_PairToVectorList(x);
    }

    @Override
    public void Rf_error(Object msg) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_error", msg);
        }
        delegate.Rf_error(msg);
    }

    @Override
    public void Rf_warning(Object msg) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_warning", msg);
        }
        delegate.Rf_warning(msg);
    }

    @Override
    public void Rf_warningcall(Object call, Object msg) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_warningcall", call, msg);
        }
        delegate.Rf_warningcall(call, msg);
    }

    @Override
    public Object Rf_allocateVector(int mode, int n) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_allocateVector", mode, n);
        }
        return delegate.Rf_allocateVector(mode, n);
    }

    @Override
    public Object Rf_allocateArray(int mode, Object dimsObj) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_allocateArray", mode, dimsObj);
        }
        return null;
    }

    @Override
    public Object Rf_allocateMatrix(int mode, int nrow, int ncol) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_allocateMatrix", mode, ncol, nrow);
        }
        return delegate.Rf_allocateMatrix(mode, nrow, ncol);
    }

    @Override
    public int Rf_nrows(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_nrows", x);
        }
        return delegate.Rf_nrows(x);
    }

    @Override
    public int Rf_ncols(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_ncols", x);
        }
        return delegate.Rf_ncols(x);
    }

    @Override
    public int LENGTH(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("LENGTH", x);
        }
        return delegate.LENGTH(x);
    }

    @Override
    public void SET_STRING_ELT(Object x, int i, Object v) {
        if (tracing) {
            RFFIUtils.traceUpCall("SET_STRING_ELT", x, i, v);
        }
        delegate.SET_STRING_ELT(x, i, v);
    }

    @Override
    public void SET_VECTOR_ELT(Object x, int i, Object v) {
        if (tracing) {
            RFFIUtils.traceUpCall("SET_VECTOR_ELT", i, v);
        }
        delegate.SET_VECTOR_ELT(x, i, v);
    }

    @Override
    public Object RAW(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("RAW", x);
        }
        return delegate.RAW(x);
    }

    @Override
    public Object LOGICAL(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("LOGICAL", x);
        }
        return delegate.LOGICAL(x);
    }

    @Override
    public Object INTEGER(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("INTEGER", x);
        }
        return delegate.INTEGER(x);
    }

    @Override
    public Object REAL(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("REAL", x);
        }
        return delegate.REAL(x);
    }

    @Override
    public Object STRING_ELT(Object x, int i) {
        if (tracing) {
            RFFIUtils.traceUpCall("STRING_ELT", x, i);
        }
        return delegate.STRING_ELT(x, i);
    }

    @Override
    public Object VECTOR_ELT(Object x, int i) {
        if (tracing) {
            RFFIUtils.traceUpCall("VECTOR_ELT", x, i);
        }
        return delegate.VECTOR_ELT(x, i);
    }

    @Override
    public int NAMED(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("NAMED", x);
        }
        return delegate.NAMED(x);
    }

    @Override
    public Object SET_TYPEOF_FASTR(Object x, int v) {
        if (tracing) {
            RFFIUtils.traceUpCall("SET_TYPEOF_FASTR", x, v);
        }
        return delegate.SET_TYPEOF_FASTR(x, v);
    }

    @Override
    public int TYPEOF(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("TYPEOF", x);
        }
        return delegate.TYPEOF(x);
    }

    @Override
    public int OBJECT(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("OBJECT", x);
        }
        return delegate.OBJECT(x);
    }

    @Override
    public Object Rf_duplicate(Object x, int deep) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_duplicate", x, deep);
        }
        return delegate.Rf_duplicate(x, deep);
    }

    @Override
    public int Rf_anyDuplicated(Object x, int fromLast) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_anyDuplicated", x, fromLast);
        }
        return delegate.Rf_anyDuplicated(x, fromLast);
    }

    @Override
    public Object PRINTNAME(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("PRINTNAME", x);
        }
        return delegate.PRINTNAME(x);
    }

    @Override
    public Object TAG(Object e) {
        if (tracing) {
            RFFIUtils.traceUpCall("TAG", e);
        }
        return delegate.TAG(e);
    }

    @Override
    public Object CAR(Object e) {
        if (tracing) {
            RFFIUtils.traceUpCall("CAR", e);
        }
        return delegate.CAR(e);
    }

    @Override
    public Object CDR(Object e) {
        if (tracing) {
            RFFIUtils.traceUpCall("CDR", e);
        }
        return delegate.CDR(e);
    }

    @Override
    public Object CADR(Object e) {
        if (tracing) {
            RFFIUtils.traceUpCall("CADR", e);
        }
        return delegate.CADR(e);
    }

    @Override
    public Object CADDR(Object e) {
        if (tracing) {
            RFFIUtils.traceUpCall("CADDR", e);
        }
        return delegate.CADDR(e);
    }

    @Override
    public Object CDDR(Object e) {
        if (tracing) {
            RFFIUtils.traceUpCall("CDDR", e);
        }
        return delegate.CDDR(e);
    }

    @Override
    public Object SET_TAG(Object x, Object y) {
        if (tracing) {
            RFFIUtils.traceUpCall("SET_TAG", x, y);
        }
        return delegate.SET_TAG(x, y);
    }

    @Override
    public Object SETCAR(Object x, Object y) {
        if (tracing) {
            RFFIUtils.traceUpCall("SETCAR", x, y);
        }
        return delegate.SETCAR(x, y);
    }

    @Override
    public Object SETCDR(Object x, Object y) {
        if (tracing) {
            RFFIUtils.traceUpCall("SETCDR", x, y);
        }
        return delegate.SETCDR(x, y);
    }

    @Override
    public Object SETCADR(Object x, Object y) {
        if (tracing) {
            RFFIUtils.traceUpCall("SETCADR", x);
        }
        return delegate.SETCADR(x, y);
    }

    @Override
    public Object SYMVALUE(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("SYMVALUE", x);
        }
        return delegate.SYMVALUE(x);
    }

    @Override
    public void SET_SYMVALUE(Object x, Object v) {
        if (tracing) {
            RFFIUtils.traceUpCall("SET_SYMVALUE", x, v);
        }
        delegate.SET_SYMVALUE(x, v);
    }

    @Override
    public int R_BindingIsLocked(Object sym, Object env) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_BindingIsLocked", sym, env);
        }
        return delegate.R_BindingIsLocked(sym, env);
    }

    @Override
    public Object R_FindNamespace(Object name) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_FindNamespace", name);
        }
        return delegate.R_FindNamespace(name);
    }

    @Override
    public Object Rf_eval(Object expr, Object env) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_eval", expr, env);
        }
        return delegate.Rf_eval(expr, env);
    }

    @Override
    public Object Rf_findfun(Object symbolObj, Object envObj) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_findfun", symbolObj, envObj);
        }
        return delegate.Rf_findfun(symbolObj, envObj);
    }

    @Override
    public Object Rf_GetOption1(Object tag) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_GetOption1", tag);
        }
        return delegate.Rf_GetOption1(tag);
    }

    @Override
    public void Rf_gsetVar(Object symbol, Object value, Object rho) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_gsetVar", symbol, value, rho);
        }
        delegate.Rf_gsetVar(symbol, value, rho);
    }

    @Override
    public void DUPLICATE_ATTRIB(Object to, Object from) {
        if (tracing) {
            RFFIUtils.traceUpCall("DUPLICATE_ATTRIB", to, from);
        }
        delegate.DUPLICATE_ATTRIB(to, from);
    }

    @Override
    public int R_computeIdentical(Object x, Object y, int flags) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_computeIdentical", x, y, flags);
        }
        return delegate.R_computeIdentical(x, y, flags);
    }

    @Override
    public void Rf_copyListMatrix(Object s, Object t, int byrow) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_copyListMatrix", t, byrow);
        }
        delegate.Rf_copyListMatrix(s, t, byrow);
    }

    @Override
    public void Rf_copyMatrix(Object s, Object t, int byrow) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_copyMatrix", t, byrow);
        }
        delegate.Rf_copyMatrix(s, t, byrow);
    }

    @Override
    public Object R_tryEval(Object expr, Object env, boolean silent) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_tryEval", expr, env, silent);
        }
        return delegate.R_tryEval(expr, env, silent);
    }

    @Override
    public Object R_ToplevelExec() {
        if (tracing) {
            RFFIUtils.traceUpCall("R_TopLevelExec");
        }
        return delegate.R_ToplevelExec();
    }

    @Override
    public int RDEBUG(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("RDEBUG", x);
        }
        return delegate.RDEBUG(x);
    }

    @Override
    public void SET_RDEBUG(Object x, int v) {
        if (tracing) {
            RFFIUtils.traceUpCall("SET_RDEBUG", x, v);
        }
        delegate.SET_RDEBUG(x, v);
    }

    @Override
    public int RSTEP(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("RSTEP", x);
        }
        return delegate.RSTEP(x);
    }

    @Override
    public void SET_RSTEP(Object x, int v) {
        if (tracing) {
            RFFIUtils.traceUpCall("SET_RSTEP", x, v);
        }
        delegate.SET_RSTEP(x, v);
    }

    @Override
    public Object ENCLOS(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("ENCLOS", x);
        }
        return delegate.ENCLOS(x);
    }

    @Override
    public Object PRVALUE(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("PRVALUE", x);
        }
        return delegate.PRVALUE(x);
    }

    @Override
    public Object R_ParseVector(Object text, int n, Object srcFile) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_ParseVector", text, n, srcFile);
        }
        return delegate.R_ParseVector(text, n, srcFile);
    }

    @Override
    public Object R_lsInternal3(Object envArg, int allArg, int sortedArg) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_lsInternal3", envArg, allArg, sortedArg);
        }
        return delegate.R_lsInternal3(envArg, allArg, sortedArg);
    }

    @Override
    public String R_HomeDir() {
        if (tracing) {
            RFFIUtils.traceUpCall("R_HomeDir");
        }
        return delegate.R_HomeDir();
    }

    @Override
    public void R_CleanUp(int sa, int status, int runlast) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_Cleanup", sa, status, runlast);
        }
        delegate.R_CleanUp(sa, status, runlast);
    }

    @Override
    public Object R_GlobalContext() {
        if (tracing) {
            RFFIUtils.traceUpCall("R_GlobalContext");
        }
        return delegate.R_GlobalContext();
    }

    @Override
    public Object R_GlobalEnv() {
        if (tracing) {
            RFFIUtils.traceUpCall("R_GlobalEnv");
        }
        return delegate.R_GlobalEnv();
    }

    @Override
    public Object R_BaseEnv() {
        if (tracing) {
            RFFIUtils.traceUpCall("R_BaseEnv");
        }
        return delegate.R_BaseEnv();
    }

    @Override
    public Object R_BaseNamespace() {
        if (tracing) {
            RFFIUtils.traceUpCall("R_BaseNamespace");
        }
        return delegate.R_BaseNamespace();
    }

    @Override
    public Object R_NamespaceRegistry() {
        if (tracing) {
            RFFIUtils.traceUpCall("R_NamespaceRegistry");
        }
        return delegate.R_NamespaceRegistry();
    }

    @Override
    public int R_Interactive() {
        if (tracing) {
            RFFIUtils.traceUpCall("isInteractive");
        }
        return delegate.R_Interactive();
    }

    @Override
    public int IS_S4_OBJECT(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("isS4Object");
        }
        return delegate.IS_S4_OBJECT(x);
    }

    @Override
    public void Rprintf(Object message) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rprintf", message);
        }
        delegate.Rprintf(message);
    }

    @Override
    public void GetRNGstate() {
        if (tracing) {
            RFFIUtils.traceUpCall("GetRNGstate");
        }
        delegate.GetRNGstate();
    }

    @Override
    public void PutRNGstate() {
        if (tracing) {
            RFFIUtils.traceUpCall("PutRNGstate");
        }
        delegate.PutRNGstate();
    }

    @Override
    public double unif_rand() {
        if (tracing) {
            RFFIUtils.traceUpCall("unif_rand");
        }
        return delegate.unif_rand();
    }

    @Override
    public Object R_getGlobalFunctionContext() {
        if (tracing) {
            RFFIUtils.traceUpCall("R_getGlobalFunctionContext");
        }
        return delegate.R_getGlobalFunctionContext();
    }

    @Override
    public Object R_getParentFunctionContext(Object c) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_getParentFunctionContext");
        }
        return delegate.R_getParentFunctionContext(c);
    }

    @Override
    public Object R_getContextEnv(Object c) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_getContextEnv", c);
        }
        return delegate.R_getContextEnv(c);
    }

    @Override
    public Object R_getContextFun(Object c) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_getContextFun", c);
        }
        return delegate.R_getContextFun(c);
    }

    @Override
    public Object R_getContextCall(Object c) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_getContextCall", c);
        }
        return delegate.R_getContextCall(c);
    }

    @Override
    public Object R_getContextSrcRef(Object c) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_getContextSrcRef", c);
        }
        return delegate.R_getContextSrcRef(c);
    }

    @Override
    public int R_insideBrowser() {
        if (tracing) {
            RFFIUtils.traceUpCall("R_insideBrowser");
        }
        return delegate.R_insideBrowser();
    }

    @Override
    public int R_isGlobal(Object c) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_isGlobal", c);
        }
        return delegate.R_isGlobal(c);
    }

    @Override
    public int R_isEqual(Object x, Object y) {
        if (tracing) {
            RFFIUtils.traceUpCall("isEqual", x, y);
        }
        return delegate.R_isEqual(x, y);
    }

    @Override
    public Object Rf_classgets(Object x, Object y) {
        if (tracing) {
            RFFIUtils.traceUpCall("Rf_classgets", x, y);
        }
        return delegate.Rf_classgets(x, y);
    }

    @Override
    public RExternalPtr R_MakeExternalPtr(long addr, Object tag, Object prot) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_MakeExternalPtr", addr, tag, prot);
        }
        return delegate.R_MakeExternalPtr(addr, tag, prot);
    }

    @Override
    public long R_ExternalPtrAddr(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_ExternalPtrAddr", x);
        }
        return delegate.R_ExternalPtrAddr(x);
    }

    @Override
    public Object R_ExternalPtrTag(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_ExternalPtrTag", x);
        }
        return delegate.R_ExternalPtrTag(x);
    }

    @Override
    public Object R_ExternalPtrProt(Object x) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_ExternalPtrProt", x);
        }
        return delegate.R_ExternalPtrProt(x);
    }

    @Override
    public void R_SetExternalPtrAddr(Object x, long addr) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_SetExternalPtrAddr", x);
        }
        delegate.R_SetExternalPtrAddr(x, addr);
    }

    @Override
    public void R_SetExternalPtrTag(Object x, Object tag) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_SetExternalPtrTag", x);
        }
        delegate.R_SetExternalPtrTag(x, tag);
    }

    @Override
    public void R_SetExternalPtrProt(Object x, Object prot) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_ExternalPtrProt", x);
        }
        delegate.R_SetExternalPtrProt(x, prot);
    }

    @Override
    public REnvironment R_NewHashedEnv(REnvironment parent, int initialSize) {
        if (tracing) {
            RFFIUtils.traceUpCall("R_NewHashedEnv", parent, initialSize);
        }
        return delegate.R_NewHashedEnv(parent, initialSize);
    }

    // Implementation specific support

    /**
     * Helper function for {@code R_TopLevelExec}, see {@link #R_ToplevelExec()}, called after C
     * function returns.
     */
    @SuppressWarnings("static-method")
    public void R_ToplevelExecRestoreErrorHandlerStacks(Object stacks) {
        RErrorHandling.HandlerStacks handlerStacks = guaranteeInstanceOf(stacks, RErrorHandling.HandlerStacks.class);
        RErrorHandling.restoreHandlerStacks(handlerStacks);
    }

    /**
     * Called to possibly update the "complete" status on {@code x}. N.B. {@code x} may not be an
     * object with a concrete {@code setComplete} method, e.g. see {@link #INTEGER(Object)}.
     */
    @SuppressWarnings("static-method")
    public void setComplete(Object x, boolean complete) {
        // only care about concrete vectors
        if (x instanceof RVector) {
            ((RVector<?>) x).setComplete(complete);
        }
    }

    /**
     * Called when a {@link CharSXPWrapper} is expected and not found.
     */
    @SuppressWarnings("static-method")
    public void logNotCharSXPWrapper(Object x) {
        System.out.println("object " + x);
        System.out.println("class " + x.getClass());
    }

}
