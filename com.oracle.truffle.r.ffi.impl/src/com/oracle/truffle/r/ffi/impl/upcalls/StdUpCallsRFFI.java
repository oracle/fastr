/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
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
import com.oracle.truffle.r.ffi.impl.nodes.AsS4;
import com.oracle.truffle.r.ffi.impl.nodes.AttributesAccessNodes.ATTRIB;
import com.oracle.truffle.r.ffi.impl.nodes.AttributesAccessNodes.CopyMostAttrib;
import com.oracle.truffle.r.ffi.impl.nodes.AttributesAccessNodes.GetAttrib;
import com.oracle.truffle.r.ffi.impl.nodes.AttributesAccessNodes.RfSetAttribNode;
import com.oracle.truffle.r.ffi.impl.nodes.AttributesAccessNodes.SetAttribNode;
import com.oracle.truffle.r.ffi.impl.nodes.AttributesAccessNodes.TAG;
import com.oracle.truffle.r.ffi.impl.nodes.CoerceNodes.AsCharacterFactor;
import com.oracle.truffle.r.ffi.impl.nodes.CoerceNodes.CoerceVectorNode;
import com.oracle.truffle.r.ffi.impl.nodes.CoerceNodes.VectorToPairListNode;
import com.oracle.truffle.r.ffi.impl.nodes.DispatchPrimFunNode;
import com.oracle.truffle.r.ffi.impl.nodes.DoMakeClassNode;
import com.oracle.truffle.r.ffi.impl.nodes.DuplicateNodes;
import com.oracle.truffle.r.ffi.impl.nodes.DuplicateNodes.RfDuplicated;
import com.oracle.truffle.r.ffi.impl.nodes.DuplicateNodes.RfAnyDuplicated;
import com.oracle.truffle.r.ffi.impl.nodes.DuplicateNodes.RfAnyDuplicated3;
import com.oracle.truffle.r.ffi.impl.nodes.EnvNodes.LockBindingNode;
import com.oracle.truffle.r.ffi.impl.nodes.EnvNodes.UnlockBindingNode;
import com.oracle.truffle.r.ffi.impl.nodes.GetClassDefNode;
import com.oracle.truffle.r.ffi.impl.nodes.IsObjectNode;
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
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodes.SETCAD4RNode;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodes.SETCADDDRNode;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodes.SETCADDRNode;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodes.SETCADRNode;
import com.oracle.truffle.r.ffi.impl.nodes.ListAccessNodes.SETCARNode;
import com.oracle.truffle.r.ffi.impl.nodes.MakeActiveBindingNode;
import com.oracle.truffle.r.ffi.impl.nodes.MatchNodes;
import com.oracle.truffle.r.ffi.impl.nodes.MathFunctionsNodes;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodes;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodes.LENGTHNode;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodes.SET_TRUELENGTHNode;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodes.SetObjectNode;
import com.oracle.truffle.r.ffi.impl.nodes.MiscNodes.TRUELENGTHNode;
import com.oracle.truffle.r.ffi.impl.nodes.NewCustomConnectionNode;
import com.oracle.truffle.r.ffi.impl.nodes.RForceAndCallNode;
import com.oracle.truffle.r.ffi.impl.nodes.RMakeExternalPtrNode;
import com.oracle.truffle.r.ffi.impl.nodes.RNCharNode;
import com.oracle.truffle.r.ffi.impl.nodes.RSetExternalPtrNode;
import com.oracle.truffle.r.ffi.impl.nodes.RandFunctionsNodes;
import com.oracle.truffle.r.ffi.impl.nodes.RfAllocVectorNode;
import com.oracle.truffle.r.ffi.impl.nodes.RfEvalNode;
import com.oracle.truffle.r.ffi.impl.nodes.RfFindFun;
import com.oracle.truffle.r.ffi.impl.nodes.Str2TypeNode;
import com.oracle.truffle.r.ffi.impl.nodes.TYPEOFNode;
import com.oracle.truffle.r.ffi.impl.nodes.TryRfEvalNode;
import com.oracle.truffle.r.ffi.impl.nodes.VectorElementGetterNode;
import com.oracle.truffle.r.ffi.processor.RFFICpointer;
import com.oracle.truffle.r.ffi.processor.RFFICstring;
import com.oracle.truffle.r.ffi.processor.RFFIResultOwner;
import com.oracle.truffle.r.ffi.processor.RFFIRunGC;
import com.oracle.truffle.r.ffi.processor.RFFIUpCallNode;
import com.oracle.truffle.r.runtime.nmath.distr.Cauchy;
import com.oracle.truffle.r.runtime.nmath.distr.Chisq;
import com.oracle.truffle.r.runtime.nmath.distr.DBeta;
import com.oracle.truffle.r.runtime.nmath.distr.DGamma;
import com.oracle.truffle.r.runtime.nmath.distr.DHyper;
import com.oracle.truffle.r.runtime.nmath.distr.DNBeta;
import com.oracle.truffle.r.runtime.nmath.distr.DNBinom;
import com.oracle.truffle.r.runtime.nmath.distr.DNChisq;
import com.oracle.truffle.r.runtime.nmath.distr.DNorm;
import com.oracle.truffle.r.runtime.nmath.distr.DPois;
import com.oracle.truffle.r.runtime.nmath.distr.Dbinom;
import com.oracle.truffle.r.runtime.nmath.distr.Df;
import com.oracle.truffle.r.runtime.nmath.distr.Dnf;
import com.oracle.truffle.r.runtime.nmath.distr.Dnt;
import com.oracle.truffle.r.runtime.nmath.distr.Dt;
import com.oracle.truffle.r.runtime.nmath.distr.Exp;
import com.oracle.truffle.r.runtime.nmath.distr.Geom;
import com.oracle.truffle.r.runtime.nmath.distr.LogNormal;
import com.oracle.truffle.r.runtime.nmath.distr.Logis;
import com.oracle.truffle.r.runtime.nmath.distr.PGamma;
import com.oracle.truffle.r.runtime.nmath.distr.PHyper;
import com.oracle.truffle.r.runtime.nmath.distr.PNBeta;
import com.oracle.truffle.r.runtime.nmath.distr.PNBinom;
import com.oracle.truffle.r.runtime.nmath.distr.PNChisq;
import com.oracle.truffle.r.runtime.nmath.distr.PPois;
import com.oracle.truffle.r.runtime.nmath.distr.PTukey;
import com.oracle.truffle.r.runtime.nmath.distr.Pbeta;
import com.oracle.truffle.r.runtime.nmath.distr.Pbinom;
import com.oracle.truffle.r.runtime.nmath.distr.Pf;
import com.oracle.truffle.r.runtime.nmath.distr.Pnf;
import com.oracle.truffle.r.runtime.nmath.distr.Pnorm;
import com.oracle.truffle.r.runtime.nmath.distr.Pnt;
import com.oracle.truffle.r.runtime.nmath.distr.Pt;
import com.oracle.truffle.r.runtime.nmath.distr.QBeta;
import com.oracle.truffle.r.runtime.nmath.distr.QGamma;
import com.oracle.truffle.r.runtime.nmath.distr.QHyper;
import com.oracle.truffle.r.runtime.nmath.distr.QNBeta;
import com.oracle.truffle.r.runtime.nmath.distr.QNBinom;
import com.oracle.truffle.r.runtime.nmath.distr.QNChisq;
import com.oracle.truffle.r.runtime.nmath.distr.QPois;
import com.oracle.truffle.r.runtime.nmath.distr.QTukey;
import com.oracle.truffle.r.runtime.nmath.distr.Qbinom;
import com.oracle.truffle.r.runtime.nmath.distr.Qf;
import com.oracle.truffle.r.runtime.nmath.distr.Qnf;
import com.oracle.truffle.r.runtime.nmath.distr.Qnorm;
import com.oracle.truffle.r.runtime.nmath.distr.Qnt;
import com.oracle.truffle.r.runtime.nmath.distr.Qt;
import com.oracle.truffle.r.runtime.nmath.distr.RBeta;
import com.oracle.truffle.r.runtime.nmath.distr.RGamma;
import com.oracle.truffle.r.runtime.nmath.distr.RHyper;
import com.oracle.truffle.r.runtime.nmath.distr.RNBinom;
import com.oracle.truffle.r.runtime.nmath.distr.RNchisq;
import com.oracle.truffle.r.runtime.nmath.distr.RPois;
import com.oracle.truffle.r.runtime.nmath.distr.Rbinom;
import com.oracle.truffle.r.runtime.nmath.distr.Rf;
import com.oracle.truffle.r.runtime.nmath.distr.Rnorm;
import com.oracle.truffle.r.runtime.nmath.distr.Rt;
import com.oracle.truffle.r.runtime.nmath.distr.Signrank;
import com.oracle.truffle.r.runtime.nmath.distr.Unif;
import com.oracle.truffle.r.runtime.nmath.distr.Weibull;
import com.oracle.truffle.r.runtime.nmath.distr.Wilcox;

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

    @RFFIRunGC
    Object Rf_ScalarComplex(double real, double imag);

    @RFFIRunGC
    Object Rf_ScalarInteger(int value);

    @RFFIRunGC
    Object Rf_ScalarLogical(int value);

    @RFFIRunGC
    Object Rf_ScalarRaw(int value);

    @RFFIRunGC
    Object Rf_ScalarReal(double value);

    @RFFIRunGC
    Object Rf_ScalarString(Object value);

    @RFFIUpCallNode(value = AsIntegerNode.class, needsCallTarget = true)
    int Rf_asInteger(Object x);

    @RFFIUpCallNode(value = AsRealNode.class, needsCallTarget = true)
    double Rf_asReal(Object x);

    @RFFIUpCallNode(value = AsLogicalNode.class, needsCallTarget = true)
    int Rf_asLogical(Object x);

    @RFFIUpCallNode(value = AsCharNode.class, needsCallTarget = true)
    Object Rf_asChar(Object x);

    @RFFIUpCallNode(CoerceVectorNode.class)
    Object Rf_coerceVector(Object x, int mode);

    Object Rf_mkCharLenCE(@RFFICpointer(isString = true) Object bytes, int len, int encoding);

    Object Rf_cons(Object car, Object cdr);

    void Rf_defineVar(Object symbolArg, Object value, Object envArg);

    @RFFIUpCallNode(GetClassDefNode.class)
    Object R_getClassDef(@RFFIResultOwner @RFFICpointer(isString = true) Object clazz);

    @RFFIUpCallNode(DoMakeClassNode.class)
    Object R_do_MAKE_CLASS(@RFFICpointer(isString = true) Object clazz);

    @RFFIUpCallNode(value = MiscNodes.RDoNewObjectNode.class, needsCallTarget = true)
    Object R_do_new_object(Object classDef);

    /**
     * WARNING: argument order reversed from Rf_findVarInFrame!
     */
    Object Rf_findVar(Object symbolArg, @RFFIResultOwner Object envArg);

    Object Rf_findVarInFrame(@RFFIResultOwner Object envArg, Object symbolArg);

    Object Rf_findVarInFrame3(@RFFIResultOwner Object envArg, Object symbolArg, int doGet);

    @RFFIUpCallNode(value = ATTRIB.class, needsCallTarget = true)
    Object ATTRIB(@RFFIResultOwner Object obj);

    @RFFIUpCallNode(value = GetAttrib.class, needsCallTarget = true)
    Object Rf_getAttrib(@RFFIResultOwner Object obj, Object name);

    @RFFIUpCallNode(value = RfSetAttribNode.class, needsCallTarget = true)
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

    @RFFIRunGC
    @RFFIUpCallNode(RfAllocVectorNode.class)
    Object Rf_allocVector(int mode, long n);

    @RFFIRunGC
    Object Rf_allocArray(int mode, Object dimsObj);

    @RFFIRunGC
    Object Rf_allocMatrix(int mode, int nrow, int ncol);

    @RFFIRunGC
    Object Rf_allocList(int length);

    @RFFIRunGC
    Object Rf_allocSExp(int type);

    int Rf_nrows(Object x);

    int Rf_ncols(Object x);

    @RFFIUpCallNode(LENGTHNode.class)
    int LENGTH(Object x);

    void SET_STRING_ELT(Object x, long i, Object v);

    void SETLENGTH(Object x, int l);

    @RFFIUpCallNode(SET_TRUELENGTHNode.class)
    void SET_TRUELENGTH(Object x, int l);

    @RFFIUpCallNode(TRUELENGTHNode.class)
    int TRUELENGTH(Object x);

    int LEVELS(Object x);

    void SETLEVELS(Object x, int gpbits);

    void SET_VECTOR_ELT(Object x, long i, Object v);

    @RFFIUpCallNode(SetAttribNode.class)
    void SET_ATTRIB(Object target, Object attributes);

    @RFFICpointer
    Object RAW(Object x);

    @RFFICpointer
    Object LOGICAL(Object x);

    @RFFICpointer
    Object INTEGER(Object x);

    @RFFICpointer
    Object REAL(Object x);

    @RFFICpointer
    Object COMPLEX(Object x);

    Object STRING_ELT(@RFFIResultOwner Object x, long i);

    @RFFIUpCallNode(value = VectorElementGetterNode.class)
    Object VECTOR_ELT(@RFFIResultOwner Object x, long i);

    int NAMED(Object x);

    @RFFIUpCallNode(SetObjectNode.class)
    void SET_OBJECT(Object x, int flag);

    void SET_NAMED_FASTR(Object x, int v);

    void SET_TYPEOF(Object x, int v);

    @RFFIUpCallNode(TYPEOFNode.class)
    int TYPEOF(Object x);

    int OBJECT(Object x);

    @RFFIUpCallNode(DuplicateNodes.DuplicateNode.class)
    Object Rf_duplicate(Object x, int deep);

    @RFFIUpCallNode(RfDuplicated.class)
    Object Rf_duplicated(Object x, int fromLast);

    @RFFIUpCallNode(RfAnyDuplicated.class)
    long Rf_any_duplicated(Object x, int fromLast);

    @RFFIUpCallNode(RfAnyDuplicated3.class)
    long Rf_any_duplicated3(Object x, Object incomparables, int fromLast);

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

    void SET_TAG(Object x, Object y);

    @RFFIUpCallNode(SETCARNode.class)
    Object SETCAR(Object x, Object y);

    Object SETCDR(Object x, Object y);

    @RFFIUpCallNode(MiscNodes.GetFunctionFormals.class)
    Object FORMALS(@RFFIResultOwner Object x);

    @RFFIUpCallNode(MiscNodes.GetFunctionBody.class)
    Object BODY(@RFFIResultOwner Object x);

    @RFFIUpCallNode(MiscNodes.GetFunctionEnvironment.class)
    Object CLOENV(Object x);

    @RFFIUpCallNode(MiscNodes.SetFunctionFormals.class)
    void SET_FORMALS(Object x, Object y);

    @RFFIUpCallNode(MiscNodes.SetFunctionBody.class)
    void SET_BODY(Object x, Object y);

    @RFFIUpCallNode(MiscNodes.SetFunctionEnvironment.class)
    void SET_CLOENV(Object x, Object y);

    @RFFIUpCallNode(SETCADRNode.class)
    Object SETCADR(Object x, Object y);

    @RFFIUpCallNode(SETCADDRNode.class)
    Object SETCADDR(Object x, Object y);

    @RFFIUpCallNode(SETCADDDRNode.class)
    Object SETCADDDR(Object x, Object y);

    @RFFIUpCallNode(SETCAD4RNode.class)
    Object SETCAD4R(Object x, Object y);

    Object SYMVALUE(Object x);

    void SET_SYMVALUE(Object x, Object v);

    int R_BindingIsLocked(Object sym, Object env);

    @RFFIUpCallNode(LockBindingNode.class)
    void R_LockBinding(Object sym, Object env);

    @RFFIUpCallNode(UnlockBindingNode.class)
    void R_unLockBinding(Object sym, Object env);

    Object R_FindNamespace(Object name);

    @RFFIUpCallNode(value = RfEvalNode.class, needsCallTarget = true)
    Object Rf_eval(Object expr, Object env);

    @RFFIUpCallNode(RfFindFun.class)
    Object Rf_findFun(Object symbolObj, Object envObj);

    Object Rf_GetOption1(Object tag);

    void Rf_gsetVar(Object symbol, Object value, Object rho);

    void Rf_setVar(Object symbol, Object value, Object rho);

    void DUPLICATE_ATTRIB(Object to, Object from);

    int R_compute_identical(Object x, Object y, int flags);

    void Rf_copyListMatrix(Object s, Object t, int byrow);

    void Rf_copyMatrix(Object s, Object t, int byrow);

    @RFFIUpCallNode(value = TryRfEvalNode.class, needsCallTarget = true)
    Object R_tryEval(Object expr, Object env, @RFFICpointer Object errorFlag, int silent);

    Object R_ToplevelExec();

    int RDEBUG(Object x);

    void SET_RDEBUG(Object x, int v);

    int RSTEP(Object x);

    void SET_RSTEP(Object x, int v);

    Object ENCLOS(Object x);

    void SET_ENCLOS(Object x, Object enc);

    Object PRVALUE(Object x);

    Object R_ParseVector(Object text, int n, Object srcFile);

    Object R_lsInternal3(Object envArg, int allArg, int sortedArg);

    String R_HomeDir();

    int IS_S4_OBJECT(Object x);

    @RFFIUpCallNode(value = AsS4.class, needsCallTarget = true)
    Object Rf_asS4(Object x, int b, int i);

    void SET_S4_OBJECT(Object x);

    void UNSET_S4_OBJECT(Object x);

    void Rprintf(@RFFICstring String message);

    void GetRNGstate();

    void PutRNGstate();

    double unif_rand();

    double norm_rand();

    double exp_rand();

    Object Rf_classgets(Object x, Object y);

    @RFFIUpCallNode(RMakeExternalPtrNode.class)
    Object R_MakeExternalPtr(@RFFICpointer Object addr, Object tag, Object prot);

    long R_ExternalPtrAddr(Object x);

    Object R_ExternalPtrTag(Object x);

    Object R_ExternalPtrProtected(Object x);

    @RFFIUpCallNode(RSetExternalPtrNode.class)
    void R_SetExternalPtrAddr(Object x, @RFFICpointer Object addr);

    void R_SetExternalPtrTag(Object x, Object tag);

    void R_SetExternalPtrProtected(Object x, Object prot);

    void R_CleanUp(int sa, int status, int runlast);

    Object R_NewHashedEnv(Object parent, Object initialSize);

    int PRSEEN(Object x);

    Object PRENV(@RFFIResultOwner Object x);

    Object R_PromiseExpr(@RFFIResultOwner Object x);

    Object PRCODE(@RFFIResultOwner Object x);

    @RFFICpointer
    Object R_CHAR(Object x);

    @RFFIUpCallNode(NewCustomConnectionNode.class)
    Object R_new_custom_connection(@RFFICpointer(isString = true) Object description, @RFFICpointer(isString = true) Object mode, @RFFICpointer(isString = true) Object className, Object readAddr);

    int R_ReadConnection(int fd, long bufAddress, int size);

    int R_WriteConnection(int fd, long bufAddress, int size);

    Object R_GetConnection(int fd);

    @RFFIUpCallNode(value = MiscNodes.RDoSlotNode.class, needsCallTarget = true)
    Object R_do_slot(Object o, Object name);

    @RFFIUpCallNode(MiscNodes.RDoSlotAssignNode.class)
    Object R_do_slot_assign(Object o, Object name, Object value);

    @RFFIUpCallNode(Str2TypeNode.class)
    int Rf_str2type(@RFFICpointer(isString = true) Object name);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_1Node.class, functionClass = Unif.DUnif.class)
    double Rf_dunif(double a, double b, double c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = Unif.QUnif.class)
    double Rf_qunif(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = Unif.PUnif.class)
    double Rf_punif(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2Node.class, functionClass = Unif.Runif.class)
    double Rf_runif(double a, double b);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2_1Node.class, functionClass = Chisq.DChisq.class)
    double Rf_dchisq(double a, double b, int c);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2_2Node.class, functionClass = Chisq.PChisq.class)
    double Rf_pchisq(double a, double b, int c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2_2Node.class, functionClass = Chisq.QChisq.class)
    double Rf_qchisq(double a, double b, int c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction1Node.class, functionClass = Chisq.RChisq.class)
    double Rf_rchisq(double a);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_1Node.class, functionClass = DNChisq.class)
    double Rf_dnchisq(double a, double b, double c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = PNChisq.class)
    double Rf_pnchisq(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = QNChisq.class)
    double Rf_qnchisq(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2Node.class, functionClass = RNchisq.class)
    double Rf_rnchisq(double a, double b);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_1Node.class, functionClass = DNorm.class)
    double Rf_dnorm4(double a, double b, double c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = Pnorm.class)
    double Rf_pnorm5(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = Qnorm.class)
    double Rf_qnorm5(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2Node.class, functionClass = Rnorm.class)
    double Rf_rnorm(double a, double b);

    @RFFIUpCallNode(value = MathFunctionsNodes.RfPnormBothNode.class)
    void Rf_pnorm_both(double a, @RFFICpointer Object b, @RFFICpointer Object c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_1Node.class, functionClass = LogNormal.DLNorm.class)
    double Rf_dlnorm(double a, double b, double c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = LogNormal.PLNorm.class)
    double Rf_plnorm(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = LogNormal.QLNorm.class)
    double Rf_qlnorm(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2Node.class, functionClass = LogNormal.RLNorm.class)
    double Rf_rlnorm(double a, double b);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_1Node.class, functionClass = DGamma.class)
    double Rf_dgamma(double a, double b, double c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = PGamma.class)
    double Rf_pgamma(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = QGamma.class)
    double Rf_qgamma(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2Node.class, functionClass = RGamma.class)
    double Rf_rgamma(double a, double b);

    @RFFIUpCallNode(MathFunctionsNodes.Log1pmxNode.class)
    double Rf_log1pmx(double a);

    @RFFIUpCallNode(MathFunctionsNodes.Log1pexpNode.class)
    double Rf_log1pexp(double a);

    @RFFIUpCallNode(MathFunctionsNodes.Lgamma1pNode.class)
    double Rf_lgamma1p(double a);

    @RFFIUpCallNode(MathFunctionsNodes.LogspaceAddNode.class)
    double Rf_logspace_add(double a, double b);

    @RFFIUpCallNode(MathFunctionsNodes.LogspaceSubNode.class)
    double Rf_logspace_sub(double a, double b);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_1Node.class, functionClass = DBeta.class)
    double Rf_dbeta(double a, double b, double c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = Pbeta.class)
    double Rf_pbeta(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = QBeta.class)
    double Rf_qbeta(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2Node.class, functionClass = RBeta.class)
    double Rf_rbeta(double a, double b);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_1Node.class, functionClass = Df.class)
    double Rf_df(double a, double b, double c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = Pf.class)
    double Rf_pf(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = Qf.class)
    double Rf_qf(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2Node.class, functionClass = Rf.class)
    double Rf_rf(double a, double b);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2_1Node.class, functionClass = Dt.class)
    double Rf_dt(double a, double b, int c);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2_2Node.class, functionClass = Pt.class)
    double Rf_pt(double a, double b, int c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2_2Node.class, functionClass = Qt.class)
    double Rf_qt(double a, double b, int c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction1Node.class, functionClass = Rt.class)
    double Rf_rt(double a);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_1Node.class, functionClass = Dbinom.class)
    double Rf_dbinom(double a, double b, double c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = Pbinom.class)
    double Rf_pbinom(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = Qbinom.class)
    double Rf_qbinom(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2Node.class, functionClass = Rbinom.class)
    double Rf_rbinom(double a, double b);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_1Node.class, functionClass = Cauchy.DCauchy.class)
    double Rf_dcauchy(double a, double b, double c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = Cauchy.PCauchy.class)
    double Rf_pcauchy(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = Cauchy.QCauchy.class)
    double Rf_qcauchy(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2Node.class, functionClass = Cauchy.RCauchy.class)
    double Rf_rcauchy(double a, double b);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2_1Node.class, functionClass = Exp.DExp.class)
    double Rf_dexp(double a, double b, int c);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2_2Node.class, functionClass = Exp.PExp.class)
    double Rf_pexp(double a, double b, int c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2_2Node.class, functionClass = Exp.QExp.class)
    double Rf_qexp(double a, double b, int c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction1Node.class, functionClass = Exp.RExp.class)
    double Rf_rexp(double a);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2_1Node.class, functionClass = Geom.DGeom.class)
    double Rf_dgeom(double a, double b, int c);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2_2Node.class, functionClass = Geom.PGeom.class)
    double Rf_pgeom(double a, double b, int c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2_2Node.class, functionClass = Geom.QGeom.class)
    double Rf_qgeom(double a, double b, int c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction1Node.class, functionClass = Geom.RGeom.class)
    double Rf_rgeom(double a);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction4_1Node.class, functionClass = DHyper.class)
    double Rf_dhyper(double a, double b, double c, double d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction4_2Node.class, functionClass = PHyper.class)
    double Rf_phyper(double a, double b, double c, double d, int e, int f);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction4_2Node.class, functionClass = QHyper.class)
    double Rf_qhyper(double a, double b, double c, double d, int e, int f);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3Node.class, functionClass = RHyper.class, needsCallTarget = true)
    double Rf_rhyper(double a, double b, double c);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_1Node.class, functionClass = DNBinom.DNBinomFunc.class)
    double Rf_dnbinom(double a, double b, double c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = PNBinom.PNBinomFunc.class)
    double Rf_pnbinom(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = QNBinom.QNBinomFunc.class)
    double Rf_qnbinom(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2Node.class, functionClass = RNBinom.RNBinomFunc.class)
    double Rf_rnbinom(double a, double b);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_1Node.class, functionClass = DNBinom.DNBinomMu.class)
    double Rf_dnbinom_mu(double a, double b, double c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = PNBinom.PNBinomMu.class)
    double Rf_pnbinom_mu(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = QNBinom.QNBinomMu.class)
    double Rf_qnbinom_mu(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2Node.class, functionClass = RNBinom.RNBinomMu.class)
    double Rf_rnbinom_mu(double a, double b);

    @RFFIUpCallNode(value = RandFunctionsNodes.RfRMultinomNode.class, needsCallTarget = true)
    void Rf_rmultinom(int a, @RFFICpointer Object b, int c, @RFFICpointer Object d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2_1Node.class, functionClass = DPois.class)
    double Rf_dpois(double a, double b, int c);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2_2Node.class, functionClass = PPois.class)
    double Rf_ppois(double a, double b, int c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2_2Node.class, functionClass = QPois.class)
    double Rf_qpois(double a, double b, int c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction1Node.class, functionClass = RPois.class)
    double Rf_rpois(double a);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_1Node.class, functionClass = Weibull.DWeibull.class)
    double Rf_dweibull(double a, double b, double c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = Weibull.PWeibull.class)
    double Rf_pweibull(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = Weibull.QWeibull.class)
    double Rf_qweibull(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2Node.class, functionClass = Weibull.RWeibull.class)
    double Rf_rweibull(double a, double b);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_1Node.class, functionClass = Logis.DLogis.class)
    double Rf_dlogis(double a, double b, double c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = Logis.PLogis.class)
    double Rf_plogis(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = Logis.QLogis.class)
    double Rf_qlogis(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2Node.class, functionClass = Logis.RLogis.class)
    double Rf_rlogis(double a, double b);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction4_1Node.class, functionClass = DNBeta.class)
    double Rf_dnbeta(double a, double b, double c, double d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction4_2Node.class, functionClass = PNBeta.class)
    double Rf_pnbeta(double a, double b, double c, double d, int e, int f);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction4_2Node.class, functionClass = QNBeta.class)
    double Rf_qnbeta(double a, double b, double c, double d, int e, int f);

    // Unable to find implementation of Rf_rnbeta
    // double Rf_rnbeta(double a, double b, double c);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction4_1Node.class, functionClass = Dnf.class)
    double Rf_dnf(double a, double b, double c, double d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction4_2Node.class, functionClass = Pnf.class)
    double Rf_pnf(double a, double b, double c, double d, int e, int f);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction4_2Node.class, functionClass = Qnf.class)
    double Rf_qnf(double a, double b, double c, double d, int e, int f);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_1Node.class, functionClass = Dnt.class)
    double Rf_dnt(double a, double b, double c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = Pnt.class)
    double Rf_pnt(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = Qnt.class)
    double Rf_qnt(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction4_2Node.class, functionClass = PTukey.class)
    double Rf_ptukey(double a, double b, double c, double d, int e, int f);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction4_2Node.class, functionClass = QTukey.class)
    double Rf_qtukey(double a, double b, double c, double d, int e, int f);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_1Node.class, functionClass = Wilcox.DWilcox.class)
    double Rf_dwilcox(double a, double b, double c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = Wilcox.PWilcox.class)
    double Rf_pwilcox(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction3_2Node.class, functionClass = Wilcox.QWilcox.class)
    double Rf_qwilcox(double a, double b, double c, int d, int e);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2Node.class, functionClass = Wilcox.RWilcox.class)
    double Rf_rwilcox(double a, double b);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2_1Node.class, functionClass = Signrank.DSignrank.class)
    double Rf_dsignrank(double a, double b, int c);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2_2Node.class, functionClass = Signrank.PSignrank.class)
    double Rf_psignrank(double a, double b, int c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction2_2Node.class, functionClass = Signrank.QSignrank.class)
    double Rf_qsignrank(double a, double b, int c, int d);

    @RFFIUpCallNode(value = RandFunctionsNodes.RandFunction1Node.class, functionClass = Signrank.RSignrank.class)
    double Rf_rsignrank(double a);

    @RFFIUpCallNode(MathFunctionsNodes.GammafnNode.class)
    double Rf_gammafn(double a);

    @RFFIUpCallNode(MathFunctionsNodes.LGammafnNode.class)
    double Rf_lgammafn(double a);

    @RFFIUpCallNode(MathFunctionsNodes.LGammafnSignNode.class)
    double Rf_lgammafn_sign(double a, @RFFICpointer Object b);

    @RFFIUpCallNode(MathFunctionsNodes.DpsiFnNode.class)
    void Rf_dpsifn(double a, int b, int c, int d, @RFFICpointer Object e, @RFFICpointer Object f, @RFFICpointer Object g);

    @RFFIUpCallNode(MathFunctionsNodes.PsiGammaNode.class)
    double Rf_psigamma(double a, double b);

    @RFFIUpCallNode(MathFunctionsNodes.DiGammaNode.class)
    double Rf_digamma(double a);

    @RFFIUpCallNode(MathFunctionsNodes.TriGammaNode.class)
    double Rf_trigamma(double a);

    @RFFIUpCallNode(MathFunctionsNodes.TetraGammaNode.class)
    double Rf_tetragamma(double a);

    @RFFIUpCallNode(MathFunctionsNodes.PentaGammaNode.class)
    double Rf_pentagamma(double a);

    @RFFIUpCallNode(MathFunctionsNodes.BetaNode.class)
    double Rf_beta(double a, double b);

    @RFFIUpCallNode(MathFunctionsNodes.LBetaNode.class)
    double Rf_lbeta(double a, double b);

    @RFFIUpCallNode(MathFunctionsNodes.ChooseNode.class)
    double Rf_choose(double a, double b);

    @RFFIUpCallNode(MathFunctionsNodes.LChooseNode.class)
    double Rf_lchoose(double a, double b);

    @RFFIUpCallNode(MathFunctionsNodes.BesselINode.class)
    double Rf_bessel_i(double a, double b, double c);

    @RFFIUpCallNode(MathFunctionsNodes.BesselJNode.class)
    double Rf_bessel_j(double a, double b);

    @RFFIUpCallNode(MathFunctionsNodes.BesselKNode.class)
    double Rf_bessel_k(double a, double b, double c);

    @RFFIUpCallNode(MathFunctionsNodes.BesselYNode.class)
    double Rf_bessel_y(double a, double b);

    @RFFIUpCallNode(value = MathFunctionsNodes.BesselIExNode.class, needsCallTarget = true)
    double Rf_bessel_i_ex(double a, double b, double c, @RFFICpointer Object d);

    @RFFIUpCallNode(value = MathFunctionsNodes.BesselJExNode.class, needsCallTarget = true)
    double Rf_bessel_j_ex(double a, double b, @RFFICpointer Object c);

    @RFFIUpCallNode(value = MathFunctionsNodes.BesselKExNode.class, needsCallTarget = true)
    double Rf_bessel_k_ex(double a, double b, double c, @RFFICpointer Object d);

    @RFFIUpCallNode(value = MathFunctionsNodes.BesselYExNode.class, needsCallTarget = true)
    double Rf_bessel_y_ex(double a, double b, @RFFICpointer Object c);

    @RFFIUpCallNode(MathFunctionsNodes.SignNode.class)
    double Rf_sign(double a);

    @RFFIUpCallNode(MathFunctionsNodes.FPrecNode.class)
    double Rf_fprec(double a, double b);

    double Rf_ftrunc(double a);

    @RFFIUpCallNode(MathFunctionsNodes.CospiNode.class)
    double Rf_cospi(double a);

    @RFFIUpCallNode(MathFunctionsNodes.SinpiNode.class)
    double Rf_sinpi(double a);

    @RFFIUpCallNode(MathFunctionsNodes.TanpiNode.class)
    double Rf_tanpi(double a);

    @RFFIUpCallNode(value = MiscNodes.NamesGetsNode.class, needsCallTarget = true)
    Object Rf_namesgets(Object vec, Object val);

    @RFFIUpCallNode(CopyMostAttrib.class)
    void Rf_copyMostAttrib(Object x, Object y);

    @RFFIUpCallNode(value = VectorToPairListNode.class, needsCallTarget = true)
    Object Rf_VectorToPairList(Object x);

    @RFFIUpCallNode(value = AsCharacterFactor.class, needsCallTarget = true)
    Object Rf_asCharacterFactor(Object x);

    @RFFIUpCallNode(value = MatchNodes.MatchNode.class, needsCallTarget = true)
    Object Rf_match(Object itables, Object ix, int nmatch);

    @RFFIUpCallNode(MatchNodes.NonNullStringMatchNode.class)
    boolean Rf_NonNullStringMatch(Object s, Object t);

    @RFFIUpCallNode(value = MiscNodes.RHasSlotNode.class, needsCallTarget = true)
    int R_has_slot(Object container, Object name);

    @RFFIUpCallNode(value = MiscNodes.OctSizeNode.class, needsCallTarget = true)
    Object octsize(Object size);

    @RFFIUpCallNode(MiscNodes.RfPrintValueNode.class)
    void Rf_PrintValue(Object value);

    @RFFIUpCallNode(RNCharNode.class)
    int R_nchar(Object string, int type, int allowNA, int keepNA, @RFFICstring String msgName);

    @RFFIUpCallNode(value = RForceAndCallNode.class, needsCallTarget = true)
    Object R_forceAndCall(Object e, Object f, int n, Object args);

    @RFFIUpCallNode(IsObjectNode.class)
    int Rf_isObject(Object x);

    @RFFIUpCallNode(MakeActiveBindingNode.class)
    void R_MakeActiveBinding(Object sym, Object fun, Object env);

    /**
     * <code>PRIMFUN(op)</code> returns a function pointer for the given function object (SEXP)
     * argument identifying a primitive builtin. Its main purpose is to be stored in a display list.
     * The FastR implementation uses the fact that the function argument is also passed to the
     * returned builtin function, which allows for <code>PRIMFUN(op)</code> in FastR to return a
     * generic <code>DispatchPRIMFUN</code>. The calls to a given primitive function are dispatched
     * in {@link DispatchPrimFunNode}.
     */
    @RFFIUpCallNode(DispatchPrimFunNode.class)
    Object DispatchPRIMFUN(Object call, Object op, Object args, Object rho);
}
