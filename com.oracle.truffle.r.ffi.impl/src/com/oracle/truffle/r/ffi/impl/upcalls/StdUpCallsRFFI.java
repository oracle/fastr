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
package com.oracle.truffle.r.ffi.impl.upcalls;

import com.oracle.truffle.r.ffi.impl.nodes.AsCharNode;
import com.oracle.truffle.r.ffi.impl.nodes.AsIntegerNode;
import com.oracle.truffle.r.ffi.impl.nodes.AsLogicalNode;
import com.oracle.truffle.r.ffi.impl.nodes.AsRealNode;
import com.oracle.truffle.r.ffi.impl.nodes.AttributesAccessNodes.ATTRIB;
import com.oracle.truffle.r.ffi.impl.nodes.AttributesAccessNodes.CopyMostAttrib;
import com.oracle.truffle.r.ffi.impl.nodes.AttributesAccessNodes.TAG;
import com.oracle.truffle.r.ffi.impl.nodes.CoerceNodes.CoerceVectorNode;
import com.oracle.truffle.r.ffi.impl.nodes.CoerceNodes.VectorToPairListNode;
import com.oracle.truffle.r.ffi.impl.nodes.DuplicateNodes;
import com.oracle.truffle.r.ffi.impl.nodes.EnvNodes.LockBindingNode;
import com.oracle.truffle.r.ffi.impl.nodes.EnvNodes.UnlockBindingNode;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodes.CAARNode;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodes.CAD4RNode;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodes.CADDDRNode;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodes.CADDRNode;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodes.CADRNode;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodes.CARNode;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodes.CDARNode;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodes.CDDDRNode;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodes.CDDRNode;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodes.CDRNode;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodes.SETCADRNode;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodes.SETCARNode;
import com.oracle.truffle.r.ffi.impl.nodes.MatchNodes;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodes;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodes.LENGTHNode;
import com.oracle.truffle.r.ffi.impl.nodes.RandFunctionsNodes;
import com.oracle.truffle.r.ffi.impl.nodes.RfEvalNode;
import com.oracle.truffle.r.ffi.processor.RFFICstring;
import com.oracle.truffle.r.ffi.processor.RFFIRunGC;
import com.oracle.truffle.r.ffi.processor.RFFIUpCallNode;

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
 * {@code String} here. In particular do not use array types as these are passed as custom Truffle
 * objects in some implementations.
 */
public interface StdUpCallsRFFI {
    // Checkstyle: stop method name check

    Object Rf_ScalarInteger(int value);

    Object Rf_ScalarLogical(int value);

    Object Rf_ScalarDouble(double value);

    Object Rf_ScalarString(Object value);

    @RFFIUpCallNode(AsIntegerNode.class)
    int Rf_asInteger(Object x);

    @RFFIUpCallNode(AsRealNode.class)
    double Rf_asReal(Object x);

    @RFFIUpCallNode(AsLogicalNode.class)
    int Rf_asLogical(Object x);

    @RFFIUpCallNode(AsCharNode.class)
    Object Rf_asChar(Object x);

    @RFFIUpCallNode(CoerceVectorNode.class)
    Object Rf_coerceVector(Object x, int mode);

    Object Rf_mkCharLenCE(@RFFICstring(convert = false) Object bytes, int len, int encoding);

    Object Rf_cons(Object car, Object cdr);

    void Rf_defineVar(Object symbolArg, Object value, Object envArg);

    Object R_getClassDef(@RFFICstring String clazz);

    Object R_do_MAKE_CLASS(@RFFICstring String clazz);

    @RFFIUpCallNode(MiscNodes.RDoNewObjectNode.class)
    Object R_do_new_object(Object classDef);

    /**
     * WARNING: argument order reversed from Rf_findVarInFrame!
     */
    Object Rf_findVar(Object symbolArg, Object envArg);

    Object Rf_findVarInFrame(Object envArg, Object symbolArg);

    Object Rf_findVarInFrame3(Object envArg, Object symbolArg, int doGet);

    @RFFIUpCallNode(ATTRIB.class)
    Object ATTRIB(Object obj);

    Object Rf_getAttrib(Object obj, Object name);

    void Rf_setAttrib(Object obj, Object name, Object val);

    int Rf_inherits(Object x, @RFFICstring String clazz);

    Object Rf_install(@RFFICstring String name);

    Object Rf_installChar(Object name);

    Object Rf_lengthgets(Object x, int newSize);

    int Rf_isString(Object x);

    int Rf_isNull(Object x);

    Object Rf_PairToVectorList(Object x);

    void Rf_error(@RFFICstring String msg);

    void Rf_warning(@RFFICstring String msg);

    void Rf_warningcall(Object call, @RFFICstring String msg);

    void Rf_errorcall(Object call, @RFFICstring String msg);

    Object Rf_allocVector(int mode, long n);

    Object Rf_allocArray(int mode, Object dimsObj);

    Object Rf_allocMatrix(int mode, int nrow, int ncol);

    int Rf_nrows(Object x);

    int Rf_ncols(Object x);

    @RFFIUpCallNode(LENGTHNode.class)
    int LENGTH(Object x);

    void SET_STRING_ELT(Object x, long i, Object v);

    void SET_VECTOR_ELT(Object x, long i, Object v);

    Object RAW(Object x);

    Object LOGICAL(Object x);

    Object INTEGER(Object x);

    Object REAL(Object x);

    Object COMPLEX(Object x);

    Object STRING_ELT(Object x, long i);

    Object VECTOR_ELT(Object x, long i);

    int NAMED(Object x);

    Object SET_NAMED_FASTR(Object x, int v);

    Object SET_TYPEOF_FASTR(Object x, int v);

    int TYPEOF(Object x);

    int OBJECT(Object x);

    @RFFIUpCallNode(DuplicateNodes.DuplicateNode.class)
    Object Rf_duplicate(Object x, int deep);

    long Rf_any_duplicated(Object x, int fromLast);

    Object PRINTNAME(Object x);

    @RFFIUpCallNode(TAG.class)
    Object TAG(Object e);

    @RFFIUpCallNode(CARNode.class)
    Object CAR(Object e);

    @RFFIUpCallNode(CAARNode.class)
    Object CAAR(Object e);

    @RFFIUpCallNode(CDRNode.class)
    Object CDR(Object e);

    @RFFIUpCallNode(CDARNode.class)
    Object CDAR(Object e);

    @RFFIUpCallNode(CADRNode.class)
    Object CADR(Object e);

    @RFFIUpCallNode(CADDRNode.class)
    Object CADDR(Object e);

    @RFFIUpCallNode(CADDDRNode.class)
    Object CADDDR(Object e);

    @RFFIUpCallNode(CAD4RNode.class)
    Object CAD4R(Object e);

    @RFFIUpCallNode(CDDRNode.class)
    Object CDDR(Object e);

    @RFFIUpCallNode(CDDDRNode.class)
    Object CDDDR(Object e);

    Object SET_TAG(Object x, Object y);

    @RFFIUpCallNode(SETCARNode.class)
    Object SETCAR(Object x, Object y);

    Object SETCDR(Object x, Object y);

    @RFFIUpCallNode(SETCADRNode.class)
    Object SETCADR(Object x, Object y);

    Object SYMVALUE(Object x);

    void SET_SYMVALUE(Object x, Object v);

    int R_BindingIsLocked(Object sym, Object env);

    @RFFIUpCallNode(LockBindingNode.class)
    void R_LockBinding(Object sym, Object env);

    @RFFIUpCallNode(UnlockBindingNode.class)
    void R_unLockBinding(Object sym, Object env);

    Object R_FindNamespace(Object name);

    @RFFIRunGC
    @RFFIUpCallNode(RfEvalNode.class)
    Object Rf_eval(Object expr, Object env);

    Object Rf_findFun(Object symbolObj, Object envObj);

    Object Rf_GetOption1(Object tag);

    void Rf_gsetVar(Object symbol, Object value, Object rho);

    void DUPLICATE_ATTRIB(Object to, Object from);

    int R_compute_identical(Object x, Object y, int flags);

    void Rf_copyListMatrix(Object s, Object t, int byrow);

    void Rf_copyMatrix(Object s, Object t, int byrow);

    Object R_tryEval(Object expr, Object env, int silent);

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

    void SET_S4_OBJECT(Object x);

    void UNSET_S4_OBJECT(Object x);

    void Rprintf(@RFFICstring String message);

    void GetRNGstate();

    void PutRNGstate();

    double unif_rand();

    Object Rf_classgets(Object x, Object y);

    Object R_MakeExternalPtr(long addr, Object tag, Object prot);

    long R_ExternalPtrAddr(Object x);

    Object R_ExternalPtrTag(Object x);

    Object R_ExternalPtrProtected(Object x);

    void R_SetExternalPtrAddr(Object x, long addr);

    void R_SetExternalPtrTag(Object x, Object tag);

    void R_SetExternalPtrProtected(Object x, Object prot);

    void R_CleanUp(int sa, int status, int runlast);

    Object R_NewHashedEnv(Object parent, Object initialSize);

    int PRSEEN(Object x);

    Object PRENV(Object x);

    Object R_PromiseExpr(Object x);

    Object PRCODE(Object x);

    Object R_CHAR(Object x);

    Object R_new_custom_connection(@RFFICstring String description, @RFFICstring String mode, @RFFICstring String className, Object readAddr);

    int R_ReadConnection(int fd, long bufAddress, int size);

    int R_WriteConnection(int fd, long bufAddress, int size);

    Object R_GetConnection(int fd);

    String getSummaryDescription(Object x);

    String getConnectionClassString(Object x);

    String getOpenModeString(Object x);

    boolean isSeekable(Object x);

    @RFFIUpCallNode(MiscNodes.RDoSlotNode.class)
    Object R_do_slot(Object o, Object name);

    @RFFIUpCallNode(MiscNodes.RDoSlotAssignNode.class)
    Object R_do_slot_assign(Object o, Object name, Object value);

    Object R_MethodsNamespace();

    int Rf_str2type(@RFFICstring String name);

    int FASTR_getConnectionChar(Object obj);

    @RFFIUpCallNode(RandFunctionsNodes.DunifNode.class)
    double Rf_dunif(double a, double b, double c, int d);

    @RFFIUpCallNode(RandFunctionsNodes.QunifNode.class)
    double Rf_qunif(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(RandFunctionsNodes.PunifNode.class)
    double Rf_punif(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(RandFunctionsNodes.RunifNode.class)
    double Rf_runif(double a, double b);

    @RFFIUpCallNode(MiscNodes.NamesGetsNode.class)
    Object Rf_namesgets(Object vec, Object val);

    @RFFIUpCallNode(CopyMostAttrib.class)
    void Rf_copyMostAttrib(Object x, Object y);

    @RFFIUpCallNode(VectorToPairListNode.class)
    Object Rf_VectorToPairList(Object x);

    @RFFIUpCallNode(CADDRNode.class)
    Object Rf_asCharacterFactor(Object x);

    @RFFIUpCallNode(MatchNodes.MatchNode.class)
    Object Rf_match(Object itables, Object ix, int nmatch);

    @RFFIUpCallNode(MatchNodes.NonNullStringMatchNode.class)
    Object Rf_NonNullStringMatch(Object s, Object t);

    @RFFIUpCallNode(MiscNodes.RHasSlotNode.class)
    int R_has_slot(Object container, Object name);

    @RFFIUpCallNode(MiscNodes.GetFunctionEnvironment.class)
    Object CLOENV(Object x);

    @RFFIUpCallNode(MiscNodes.OctSizeNode.class)
    Object octsize(Object size);

}
