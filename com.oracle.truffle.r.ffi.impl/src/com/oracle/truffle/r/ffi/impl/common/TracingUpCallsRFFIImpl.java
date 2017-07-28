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
package com.oracle.truffle.r.ffi.impl.common;

import com.oracle.truffle.r.ffi.impl.upcalls.UpCallsRFFI;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RStringVector;

final class TracingUpCallsRFFIImpl implements UpCallsRFFI {
    // Checkstyle: stop method name check

    private final UpCallsRFFI delegate;

    public TracingUpCallsRFFIImpl(UpCallsRFFI delegate) {
        this.delegate = delegate;
    }

    @Override
    public RIntVector Rf_ScalarInteger(int value) {
        RFFIUtils.traceUpCall("Rf_ScalarInteger", value);
        return delegate.Rf_ScalarInteger(value);
    }

    @Override
    public RLogicalVector Rf_ScalarLogical(int value) {
        RFFIUtils.traceUpCall("Rf_ScalarLogical", value);
        return delegate.Rf_ScalarLogical(value);
    }

    @Override
    public RDoubleVector Rf_ScalarDouble(double value) {
        RFFIUtils.traceUpCall("Rf_ScalarDouble", value);
        return delegate.Rf_ScalarDouble(value);
    }

    @Override
    public RStringVector Rf_ScalarString(Object value) {
        RFFIUtils.traceUpCall("Rf_ScalarString", value);
        return delegate.Rf_ScalarString(value);
    }

    @Override
    public int Rf_asInteger(Object x) {
        RFFIUtils.traceUpCall("Rf_asInteger", x);
        return delegate.Rf_asInteger(x);
    }

    @Override
    public double Rf_asReal(Object x) {
        RFFIUtils.traceUpCall("Rf_asReal", x);
        return delegate.Rf_asReal(x);
    }

    @Override
    public int Rf_asLogical(Object x) {
        RFFIUtils.traceUpCall("Rf_asLogical", x);
        return delegate.Rf_asLogical(x);
    }

    @Override
    public Object Rf_asChar(Object x) {
        RFFIUtils.traceUpCall("Rf_asChar", x);
        return delegate.Rf_asChar(x);
    }

    @Override
    public Object Rf_coerceVector(Object x, int mode) {
        RFFIUtils.traceUpCall("Rf_coerceVector", x, mode);
        return delegate.Rf_coerceVector(x, mode);
    }

    @Override
    public Object Rf_mkCharLenCE(Object bytes, int len, int encoding) {
        RFFIUtils.traceUpCall("Rf_mkCharLenCE", bytes);
        return delegate.Rf_mkCharLenCE(bytes, len, encoding);
    }

    @Override
    public Object Rf_cons(Object car, Object cdr) {
        RFFIUtils.traceUpCall("Rf_cons", car, cdr);
        return delegate.Rf_cons(car, cdr);
    }

    @Override
    public int Rf_defineVar(Object symbolArg, Object value, Object envArg) {
        RFFIUtils.traceUpCall("Rf_defineVar", symbolArg, value, envArg);
        return delegate.Rf_defineVar(symbolArg, value, envArg);
    }

    @Override
    public Object R_do_MAKE_CLASS(Object clazz) {
        RFFIUtils.traceUpCall("R_do_MAKE_CLASS", clazz);
        return delegate.R_do_MAKE_CLASS(clazz);
    }

    @Override
    public Object R_do_new_object(Object classDef) {
        RFFIUtils.traceUpCall("R_do_new_object", classDef);
        return delegate.R_do_new_object(classDef);
    }

    @Override
    public Object Rf_findVar(Object symbolArg, Object envArg) {
        RFFIUtils.traceUpCall("Rf_findVar", symbolArg, envArg);
        return delegate.Rf_findVar(symbolArg, envArg);
    }

    @Override
    public Object Rf_findVarInFrame(Object envArg, Object symbolArg) {
        RFFIUtils.traceUpCall("Rf_findVarInFrame", envArg, symbolArg);
        return delegate.Rf_findVarInFrame(envArg, symbolArg);
    }

    @Override
    public Object Rf_findVarInFrame3(Object envArg, Object symbolArg, int doGet) {
        RFFIUtils.traceUpCall("Rf_findVarInFrame3", envArg, symbolArg);
        return delegate.Rf_findVarInFrame3(envArg, symbolArg, doGet);
    }

    @Override
    public Object ATTRIB(Object obj) {
        RFFIUtils.traceUpCall("ATTRIB");
        return delegate.ATTRIB(obj);
    }

    @Override
    public Object Rf_getAttrib(Object obj, Object name) {
        RFFIUtils.traceUpCall("Rf_getAttrib", obj, name);
        return delegate.Rf_getAttrib(obj, name);
    }

    @Override
    public int Rf_setAttrib(Object obj, Object name, Object val) {
        RFFIUtils.traceUpCall("Rf_setAttrib", obj, name, val);
        return delegate.Rf_setAttrib(obj, name, val);
    }

    @Override
    public int Rf_inherits(Object x, Object clazz) {
        RFFIUtils.traceUpCall("Rf_inherits", x, clazz);
        return delegate.Rf_inherits(x, clazz);
    }

    @Override
    public Object Rf_install(Object name) {
        RFFIUtils.traceUpCall("Rf_install", name);
        return delegate.Rf_install(name);
    }

    @Override
    public Object Rf_installChar(Object name) {
        RFFIUtils.traceUpCall("Rf_installChar", name);
        return delegate.Rf_installChar(name);
    }

    @Override
    public Object Rf_lengthgets(Object x, int newSize) {
        RFFIUtils.traceUpCall("Rf_lengthgets", x, newSize);
        return delegate.Rf_lengthgets(x, newSize);
    }

    @Override
    public int Rf_isString(Object x) {
        RFFIUtils.traceUpCall("Rf_isString", x);
        return delegate.Rf_isString(x);
    }

    @Override
    public int Rf_isNull(Object x) {
        RFFIUtils.traceUpCall("Rf_isNull", x);
        return delegate.Rf_isNull(x);
    }

    @Override
    public Object Rf_PairToVectorList(Object x) {
        RFFIUtils.traceUpCall("Rf_PairToVectorList", x);
        return delegate.Rf_PairToVectorList(x);
    }

    @Override
    public int Rf_error(Object msg) {
        RFFIUtils.traceUpCall("Rf_error", msg);
        return delegate.Rf_error(msg);
    }

    @Override
    public int Rf_warning(Object msg) {
        RFFIUtils.traceUpCall("Rf_warning", msg);
        return delegate.Rf_warning(msg);
    }

    @Override
    public int Rf_warningcall(Object call, Object msg) {
        RFFIUtils.traceUpCall("Rf_warningcall", call, msg);
        return delegate.Rf_warningcall(call, msg);
    }

    @Override
    public Object Rf_allocVector(int mode, long n) {
        RFFIUtils.traceUpCall("Rf_allocateVector", mode, n);
        return delegate.Rf_allocVector(mode, n);
    }

    @Override
    public Object Rf_allocArray(int mode, Object dimsObj) {
        RFFIUtils.traceUpCall("Rf_allocateArray", mode, dimsObj);
        return null;
    }

    @Override
    public Object Rf_allocMatrix(int mode, int nrow, int ncol) {
        RFFIUtils.traceUpCall("Rf_allocateMatrix", mode, ncol, nrow);
        return delegate.Rf_allocMatrix(mode, nrow, ncol);
    }

    @Override
    public int Rf_nrows(Object x) {
        RFFIUtils.traceUpCall("Rf_nrows", x);
        return delegate.Rf_nrows(x);
    }

    @Override
    public int Rf_ncols(Object x) {
        RFFIUtils.traceUpCall("Rf_ncols", x);
        return delegate.Rf_ncols(x);
    }

    @Override
    public int LENGTH(Object x) {
        RFFIUtils.traceUpCall("LENGTH", x);
        return delegate.LENGTH(x);
    }

    @Override
    public int SET_STRING_ELT(Object x, long i, Object v) {
        RFFIUtils.traceUpCall("SET_STRING_ELT", x, i, v);
        return delegate.SET_STRING_ELT(x, i, v);
    }

    @Override
    public int SET_VECTOR_ELT(Object x, long i, Object v) {
        RFFIUtils.traceUpCall("SET_VECTOR_ELT", i, v);
        return delegate.SET_VECTOR_ELT(x, i, v);
    }

    @Override
    public Object RAW(Object x) {
        RFFIUtils.traceUpCall("RAW", x);
        return delegate.RAW(x);
    }

    @Override
    public Object LOGICAL(Object x) {
        RFFIUtils.traceUpCall("LOGICAL", x);
        return delegate.LOGICAL(x);
    }

    @Override
    public Object INTEGER(Object x) {
        RFFIUtils.traceUpCall("INTEGER", x);
        return delegate.INTEGER(x);
    }

    @Override
    public Object REAL(Object x) {
        RFFIUtils.traceUpCall("REAL", x);
        return delegate.REAL(x);
    }

    @Override
    public Object STRING_ELT(Object x, long i) {
        RFFIUtils.traceUpCall("STRING_ELT", x, i);
        return delegate.STRING_ELT(x, i);
    }

    @Override
    public Object VECTOR_ELT(Object x, long i) {
        RFFIUtils.traceUpCall("VECTOR_ELT", x, i);
        return delegate.VECTOR_ELT(x, i);
    }

    @Override
    public int NAMED(Object x) {
        RFFIUtils.traceUpCall("NAMED", x);
        return delegate.NAMED(x);
    }

    @Override
    public Object SET_NAMED_FASTR(Object x, int v) {
        RFFIUtils.traceUpCall("SET_NAMED_FASTR", x, v);
        return delegate.SET_NAMED_FASTR(x, v);
    }

    @Override
    public Object SET_TYPEOF_FASTR(Object x, int v) {
        RFFIUtils.traceUpCall("SET_TYPEOF_FASTR", x, v);
        return delegate.SET_TYPEOF_FASTR(x, v);
    }

    @Override
    public int TYPEOF(Object x) {
        RFFIUtils.traceUpCall("TYPEOF", x);
        return delegate.TYPEOF(x);
    }

    @Override
    public int OBJECT(Object x) {
        RFFIUtils.traceUpCall("OBJECT", x);
        return delegate.OBJECT(x);
    }

    @Override
    public Object Rf_duplicate(Object x, int deep) {
        RFFIUtils.traceUpCall("Rf_duplicate", x, deep);
        return delegate.Rf_duplicate(x, deep);
    }

    @Override
    public long Rf_any_duplicated(Object x, int fromLast) {
        RFFIUtils.traceUpCall("Rf_anyDuplicated", x, fromLast);
        return delegate.Rf_any_duplicated(x, fromLast);
    }

    @Override
    public Object PRINTNAME(Object x) {
        RFFIUtils.traceUpCall("PRINTNAME", x);
        return delegate.PRINTNAME(x);
    }

    @Override
    public Object TAG(Object e) {
        RFFIUtils.traceUpCall("TAG", e);
        return delegate.TAG(e);
    }

    @Override
    public Object CAR(Object e) {
        RFFIUtils.traceUpCall("CAR", e);
        return delegate.CAR(e);
    }

    @Override
    public Object CDR(Object e) {
        RFFIUtils.traceUpCall("CDR", e);
        return delegate.CDR(e);
    }

    @Override
    public Object CADR(Object e) {
        RFFIUtils.traceUpCall("CADR", e);
        return delegate.CADR(e);
    }

    @Override
    public Object CADDR(Object e) {
        RFFIUtils.traceUpCall("CADDR", e);
        return delegate.CADDR(e);
    }

    @Override
    public Object CDDR(Object e) {
        RFFIUtils.traceUpCall("CDDR", e);
        return delegate.CDDR(e);
    }

    @Override
    public Object SET_TAG(Object x, Object y) {
        RFFIUtils.traceUpCall("SET_TAG", x, y);
        return delegate.SET_TAG(x, y);
    }

    @Override
    public Object SETCAR(Object x, Object y) {
        RFFIUtils.traceUpCall("SETCAR", x, y);
        return delegate.SETCAR(x, y);
    }

    @Override
    public Object SETCDR(Object x, Object y) {
        RFFIUtils.traceUpCall("SETCDR", x, y);
        return delegate.SETCDR(x, y);
    }

    @Override
    public Object SETCADR(Object x, Object y) {
        RFFIUtils.traceUpCall("SETCADR", x);
        return delegate.SETCADR(x, y);
    }

    @Override
    public Object SYMVALUE(Object x) {
        RFFIUtils.traceUpCall("SYMVALUE", x);
        return delegate.SYMVALUE(x);
    }

    @Override
    public int SET_SYMVALUE(Object x, Object v) {
        RFFIUtils.traceUpCall("SET_SYMVALUE", x, v);
        return delegate.SET_SYMVALUE(x, v);
    }

    @Override
    public int R_BindingIsLocked(Object sym, Object env) {
        RFFIUtils.traceUpCall("R_BindingIsLocked", sym, env);
        return delegate.R_BindingIsLocked(sym, env);
    }

    @Override
    public Object R_FindNamespace(Object name) {
        RFFIUtils.traceUpCall("R_FindNamespace", name);
        return delegate.R_FindNamespace(name);
    }

    @Override
    public Object Rf_eval(Object expr, Object env) {
        RFFIUtils.traceUpCall("Rf_eval", expr, env);
        return delegate.Rf_eval(expr, env);
    }

    @Override
    public Object Rf_findFun(Object symbolObj, Object envObj) {
        RFFIUtils.traceUpCall("Rf_findfun", symbolObj, envObj);
        return delegate.Rf_findFun(symbolObj, envObj);
    }

    @Override
    public Object Rf_GetOption1(Object tag) {
        RFFIUtils.traceUpCall("Rf_GetOption1", tag);
        return delegate.Rf_GetOption1(tag);
    }

    @Override
    public int Rf_gsetVar(Object symbol, Object value, Object rho) {
        RFFIUtils.traceUpCall("Rf_gsetVar", symbol, value, rho);
        return delegate.Rf_gsetVar(symbol, value, rho);
    }

    @Override
    public int DUPLICATE_ATTRIB(Object to, Object from) {
        RFFIUtils.traceUpCall("DUPLICATE_ATTRIB", to, from);
        return delegate.DUPLICATE_ATTRIB(to, from);
    }

    @Override
    public int R_compute_identical(Object x, Object y, int flags) {
        RFFIUtils.traceUpCall("R_computeIdentical", x, y, flags);
        return delegate.R_compute_identical(x, y, flags);
    }

    @Override
    public int Rf_copyListMatrix(Object s, Object t, int byrow) {
        RFFIUtils.traceUpCall("Rf_copyListMatrix", t, byrow);
        return delegate.Rf_copyListMatrix(s, t, byrow);
    }

    @Override
    public int Rf_copyMatrix(Object s, Object t, int byrow) {
        RFFIUtils.traceUpCall("Rf_copyMatrix", t, byrow);
        return delegate.Rf_copyMatrix(s, t, byrow);
    }

    @Override
    public Object R_tryEval(Object expr, Object env, int silent) {
        RFFIUtils.traceUpCall("R_tryEval", expr, env, silent);
        return delegate.R_tryEval(expr, env, silent);
    }

    @Override
    public Object R_ToplevelExec() {
        RFFIUtils.traceUpCall("R_TopLevelExec");
        return delegate.R_ToplevelExec();
    }

    @Override
    public int RDEBUG(Object x) {
        RFFIUtils.traceUpCall("RDEBUG", x);
        return delegate.RDEBUG(x);
    }

    @Override
    public int SET_RDEBUG(Object x, int v) {
        RFFIUtils.traceUpCall("SET_RDEBUG", x, v);
        return delegate.SET_RDEBUG(x, v);
    }

    @Override
    public int RSTEP(Object x) {
        RFFIUtils.traceUpCall("RSTEP", x);
        return delegate.RSTEP(x);
    }

    @Override
    public int SET_RSTEP(Object x, int v) {
        RFFIUtils.traceUpCall("SET_RSTEP", x, v);
        return delegate.SET_RSTEP(x, v);
    }

    @Override
    public Object ENCLOS(Object x) {
        RFFIUtils.traceUpCall("ENCLOS", x);
        return delegate.ENCLOS(x);
    }

    @Override
    public Object PRVALUE(Object x) {
        RFFIUtils.traceUpCall("PRVALUE", x);
        return delegate.PRVALUE(x);
    }

    @Override
    public Object R_ParseVector(Object text, int n, Object srcFile) {
        RFFIUtils.traceUpCall("R_ParseVector", text, n, srcFile);
        return delegate.R_ParseVector(text, n, srcFile);
    }

    @Override
    public Object R_lsInternal3(Object envArg, int allArg, int sortedArg) {
        RFFIUtils.traceUpCall("R_lsInternal3", envArg, allArg, sortedArg);
        return delegate.R_lsInternal3(envArg, allArg, sortedArg);
    }

    @Override
    public String R_HomeDir() {
        RFFIUtils.traceUpCall("R_HomeDir");
        return delegate.R_HomeDir();
    }

    @Override
    public int R_CleanUp(int sa, int status, int runlast) {
        RFFIUtils.traceUpCall("R_Cleanup", sa, status, runlast);
        return delegate.R_CleanUp(sa, status, runlast);
    }

    @Override
    public Object R_GlobalContext() {
        RFFIUtils.traceUpCall("R_GlobalContext");
        return delegate.R_GlobalContext();
    }

    @Override
    public Object R_GlobalEnv() {
        RFFIUtils.traceUpCall("R_GlobalEnv");
        return delegate.R_GlobalEnv();
    }

    @Override
    public Object R_BaseEnv() {
        RFFIUtils.traceUpCall("R_BaseEnv");
        return delegate.R_BaseEnv();
    }

    @Override
    public Object R_BaseNamespace() {
        RFFIUtils.traceUpCall("R_BaseNamespace");
        return delegate.R_BaseNamespace();
    }

    @Override
    public Object R_NamespaceRegistry() {
        RFFIUtils.traceUpCall("R_NamespaceRegistry");
        return delegate.R_NamespaceRegistry();
    }

    @Override
    public int R_Interactive() {
        RFFIUtils.traceUpCall("isInteractive");
        return delegate.R_Interactive();
    }

    @Override
    public int IS_S4_OBJECT(Object x) {
        RFFIUtils.traceUpCall("isS4Object");
        return delegate.IS_S4_OBJECT(x);
    }

    @Override
    public int SET_S4_OBJECT(Object x) {
        RFFIUtils.traceUpCall("setS4Object");
        return delegate.SET_S4_OBJECT(x);
    }

    @Override
    public int UNSET_S4_OBJECT(Object x) {
        RFFIUtils.traceUpCall("unsetS4Object");
        return delegate.UNSET_S4_OBJECT(x);
    }

    @Override
    public int Rprintf(Object message) {
        RFFIUtils.traceUpCall("Rprintf", message);
        return delegate.Rprintf(message);
    }

    @Override
    public int GetRNGstate() {
        RFFIUtils.traceUpCall("GetRNGstate");
        return delegate.GetRNGstate();
    }

    @Override
    public int PutRNGstate() {
        RFFIUtils.traceUpCall("PutRNGstate");
        return delegate.PutRNGstate();
    }

    @Override
    public double unif_rand() {
        RFFIUtils.traceUpCall("unif_rand");
        return delegate.unif_rand();
    }

    @Override
    public Object R_getGlobalFunctionContext() {
        RFFIUtils.traceUpCall("R_getGlobalFunctionContext");
        return delegate.R_getGlobalFunctionContext();
    }

    @Override
    public Object R_getParentFunctionContext(Object c) {
        RFFIUtils.traceUpCall("R_getParentFunctionContext");
        return delegate.R_getParentFunctionContext(c);
    }

    @Override
    public Object R_getContextEnv(Object c) {
        RFFIUtils.traceUpCall("R_getContextEnv", c);
        return delegate.R_getContextEnv(c);
    }

    @Override
    public Object R_getContextFun(Object c) {
        RFFIUtils.traceUpCall("R_getContextFun", c);
        return delegate.R_getContextFun(c);
    }

    @Override
    public Object R_getContextCall(Object c) {
        RFFIUtils.traceUpCall("R_getContextCall", c);
        return delegate.R_getContextCall(c);
    }

    @Override
    public Object R_getContextSrcRef(Object c) {
        RFFIUtils.traceUpCall("R_getContextSrcRef", c);
        return delegate.R_getContextSrcRef(c);
    }

    @Override
    public int R_insideBrowser() {
        RFFIUtils.traceUpCall("R_insideBrowser");
        return delegate.R_insideBrowser();
    }

    @Override
    public int R_isGlobal(Object c) {
        RFFIUtils.traceUpCall("R_isGlobal", c);
        return delegate.R_isGlobal(c);
    }

    @Override
    public int R_isEqual(Object x, Object y) {
        RFFIUtils.traceUpCall("isEqual", x, y);
        return delegate.R_isEqual(x, y);
    }

    @Override
    public Object Rf_classgets(Object x, Object y) {
        RFFIUtils.traceUpCall("Rf_classgets", x, y);
        return delegate.Rf_classgets(x, y);
    }

    @Override
    public RExternalPtr R_MakeExternalPtr(long addr, Object tag, Object prot) {
        RFFIUtils.traceUpCall("R_MakeExternalPtr", addr, tag, prot);
        return delegate.R_MakeExternalPtr(addr, tag, prot);
    }

    @Override
    public long R_ExternalPtrAddr(Object x) {
        RFFIUtils.traceUpCall("R_ExternalPtrAddr", x);
        return delegate.R_ExternalPtrAddr(x);
    }

    @Override
    public Object R_ExternalPtrTag(Object x) {
        RFFIUtils.traceUpCall("R_ExternalPtrTag", x);
        return delegate.R_ExternalPtrTag(x);
    }

    @Override
    public Object R_ExternalPtrProtected(Object x) {
        RFFIUtils.traceUpCall("R_ExternalPtrProt", x);
        return delegate.R_ExternalPtrProtected(x);
    }

    @Override
    public int R_SetExternalPtrAddr(Object x, long addr) {
        RFFIUtils.traceUpCall("R_SetExternalPtrAddr", x);
        return delegate.R_SetExternalPtrAddr(x, addr);
    }

    @Override
    public int R_SetExternalPtrTag(Object x, Object tag) {
        RFFIUtils.traceUpCall("R_SetExternalPtrTag", x);
        return delegate.R_SetExternalPtrTag(x, tag);
    }

    @Override
    public int R_SetExternalPtrProtected(Object x, Object prot) {
        RFFIUtils.traceUpCall("R_ExternalPtrProt", x);
        return delegate.R_SetExternalPtrProtected(x, prot);
    }

    @Override
    public Object R_NewHashedEnv(Object parent, Object initialSize) {
        RFFIUtils.traceUpCall("R_NewHashedEnv", parent, initialSize);
        return delegate.R_NewHashedEnv(parent, initialSize);
    }

    @Override
    public int PRSEEN(Object x) {
        RFFIUtils.traceUpCall("PRSEEN", x);
        return delegate.PRSEEN(x);
    }

    @Override
    public Object PRENV(Object x) {
        RFFIUtils.traceUpCall("PRENV", x);
        return delegate.PRENV(x);
    }

    @Override
    public Object R_PromiseExpr(Object x) {
        RFFIUtils.traceUpCall("R_PromiseExpr", x);
        return delegate.R_PromiseExpr(x);
    }

    @Override
    public Object PRCODE(Object x) {
        RFFIUtils.traceUpCall("PRCODE", x);
        return delegate.PRCODE(x);
    }

    @Override
    public Object R_CHAR(Object x) {
        RFFIUtils.traceUpCall("R_CHAR", x);
        return delegate.R_CHAR(x);
    }

    @Override
    public Object R_new_custom_connection(Object description, Object mode, Object className, Object readAddr) {
        RFFIUtils.traceUpCall("R_new_custom_connection", description, mode, className, readAddr);
        return delegate.R_new_custom_connection(description, mode, className, readAddr);
    }

    @Override
    public int R_ReadConnection(int fd, Object buf) {
        RFFIUtils.traceUpCall("R_ReadConnection", fd, buf);
        return delegate.R_ReadConnection(fd, buf);
    }

    @Override
    public int R_WriteConnection(int fd, Object buf) {
        RFFIUtils.traceUpCall("R_WriteConnection", fd, buf);
        return delegate.R_WriteConnection(fd, buf);
    }

    @Override
    public Object R_GetConnection(int fd) {
        RFFIUtils.traceUpCall("R_GetConnection", fd);
        return delegate.R_GetConnection(fd);
    }

    @Override
    public String getSummaryDescription(Object x) {
        RFFIUtils.traceUpCall("getSummaryDescription", x);
        return delegate.getSummaryDescription(x);
    }

    @Override
    public String getConnectionClassString(Object x) {
        RFFIUtils.traceUpCall("getConnectionClassString", x);
        return delegate.getConnectionClassString(x);
    }

    @Override
    public String getOpenModeString(Object x) {
        RFFIUtils.traceUpCall("getOpenModeString", x);
        return delegate.getOpenModeString(x);
    }

    @Override
    public boolean isSeekable(Object x) {
        RFFIUtils.traceUpCall("isSeekable", x);
        return delegate.isSeekable(x);
    }

    @Override
    public Object R_do_slot(Object o, Object name) {
        RFFIUtils.traceUpCall("R_do_slot", o, name);
        return delegate.R_do_slot(o, name);
    }

    @Override
    public Object R_do_slot_assign(Object o, Object name, Object value) {
        RFFIUtils.traceUpCall("R_do_slot", o, name, value);
        return delegate.R_do_slot_assign(o, name, value);
    }

    @Override
    public Object R_MethodsNamespace() {
        RFFIUtils.traceUpCall("R_MethodsNamespace");
        return delegate.R_MethodsNamespace();
    }

    @Override
    public int Rf_str2type(Object name) {
        RFFIUtils.traceUpCall("Rf_str2type");
        return delegate.Rf_str2type(name);
    }
}
