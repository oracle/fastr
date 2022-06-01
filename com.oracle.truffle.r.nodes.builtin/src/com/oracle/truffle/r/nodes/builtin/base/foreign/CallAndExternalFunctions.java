/*
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.nodes.builtin.base.foreign;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asStringVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.findFirst;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RVisibility.CUSTOM;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;
import static com.oracle.truffle.r.runtime.context.FastROptions.UseInternalGridGraphics;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.library.fastrGrid.FastRGridExternalLookup;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_M_setPrimitiveMethodsNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_externalPtrPrototypeObjectNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_getClassFromCacheNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_getGenericNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_identCNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_initMethodDispatchNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_methodsPackageMetaNameNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_nextMethodCallNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_set_method_dispatchNodeGen;
import com.oracle.truffle.r.library.methods.SlotFactory.R_getSlotNodeGen;
import com.oracle.truffle.r.library.methods.SlotFactory.R_hasSlotNodeGen;
import com.oracle.truffle.r.library.methods.SlotFactory.R_setSlotNodeGen;
import com.oracle.truffle.r.library.methods.SubstituteDirectNodeGen;
import com.oracle.truffle.r.library.parallel.ParallelFunctionsFactory.MCIsChildNodeGen;
import com.oracle.truffle.r.library.stats.Approx;
import com.oracle.truffle.r.library.stats.ApproxTest;
import com.oracle.truffle.r.library.stats.BinDist;
import com.oracle.truffle.r.library.stats.CdistNodeGen;
import com.oracle.truffle.r.library.stats.CompleteCases;
import com.oracle.truffle.r.library.stats.CovcorNodeGen;
import com.oracle.truffle.r.library.stats.CutreeNodeGen;
import com.oracle.truffle.r.library.stats.DoubleCentreNodeGen;
import com.oracle.truffle.r.library.stats.Fmin;
import com.oracle.truffle.r.library.stats.Influence;
import com.oracle.truffle.r.library.stats.PPSum;
import com.oracle.truffle.r.library.stats.PPSum.PPSumExternal;
import com.oracle.truffle.r.library.stats.RMultinomNode;
import com.oracle.truffle.r.library.stats.RandFunctionsNodes;
import com.oracle.truffle.r.library.stats.RandFunctionsNodes.RandFunction1Node;
import com.oracle.truffle.r.library.stats.RandFunctionsNodes.RandFunction2Node;
import com.oracle.truffle.r.library.stats.RandFunctionsNodes.RandFunction3Node;
import com.oracle.truffle.r.library.stats.SignrankFreeNode;
import com.oracle.truffle.r.library.stats.SplineFunctionsFactory.SplineCoefNodeGen;
import com.oracle.truffle.r.library.stats.SplineFunctionsFactory.SplineEvalNodeGen;
import com.oracle.truffle.r.library.stats.StatsFunctionsNodes;
import com.oracle.truffle.r.library.stats.WilcoxFreeNode;
import com.oracle.truffle.r.library.stats.Zeroin2;
import com.oracle.truffle.r.library.stats.deriv.D;
import com.oracle.truffle.r.library.stats.deriv.Deriv;
import com.oracle.truffle.r.library.tools.C_ParseRdNodeGen;
import com.oracle.truffle.r.library.tools.DirChmodNodeGen;
import com.oracle.truffle.r.library.tools.Rmd5NodeGen;
import com.oracle.truffle.r.library.tools.ToolsTextFactory.CodeFilesAppendNodeGen;
import com.oracle.truffle.r.library.tools.ToolsTextFactory.DoTabExpandNodeGen;
import com.oracle.truffle.r.library.utils.CountFieldsNodeGen;
import com.oracle.truffle.r.library.utils.Crc64NodeGen;
import com.oracle.truffle.r.library.utils.DownloadNodeGen;
import com.oracle.truffle.r.library.utils.MenuNodeGen;
import com.oracle.truffle.r.library.utils.ObjectSizeNodeGen;
import com.oracle.truffle.r.library.utils.OctSizeNode;
import com.oracle.truffle.r.library.utils.RprofNodeGen;
import com.oracle.truffle.r.library.utils.RprofmemNodeGen;
import com.oracle.truffle.r.library.utils.TypeConvertNodeGen;
import com.oracle.truffle.r.library.utils.UnzipNodeGen;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RInternalCodeBuiltinNode;
import com.oracle.truffle.r.nodes.objects.GetPrimNameNodeGen;
import com.oracle.truffle.r.nodes.objects.NewObjectNodeGen;
import com.oracle.truffle.r.runtime.RArguments.S3Args;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalCode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.builtins.RBehavior;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.nodes.MaterializeNode;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.ffi.CallRFFI.InvokeCallNode;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.NativeSymbolType;
import com.oracle.truffle.r.runtime.ffi.MiscRFFI.AbstractAfterGraphicsOpNode;
import com.oracle.truffle.r.runtime.ffi.MiscRFFI.AbstractBeforeGraphicsOpNode;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.nmath.distr.Cauchy;
import com.oracle.truffle.r.runtime.nmath.distr.Cauchy.DCauchy;
import com.oracle.truffle.r.runtime.nmath.distr.Cauchy.PCauchy;
import com.oracle.truffle.r.runtime.nmath.distr.Cauchy.RCauchy;
import com.oracle.truffle.r.runtime.nmath.distr.Chisq;
import com.oracle.truffle.r.runtime.nmath.distr.Chisq.RChisq;
import com.oracle.truffle.r.runtime.nmath.distr.DBeta;
import com.oracle.truffle.r.runtime.nmath.distr.DGamma;
import com.oracle.truffle.r.runtime.nmath.distr.DHyper;
import com.oracle.truffle.r.runtime.nmath.distr.DNBeta;
import com.oracle.truffle.r.runtime.nmath.distr.DNBinom.DNBinomFunc;
import com.oracle.truffle.r.runtime.nmath.distr.DNBinom.DNBinomMu;
import com.oracle.truffle.r.runtime.nmath.distr.DNChisq;
import com.oracle.truffle.r.runtime.nmath.distr.DNorm;
import com.oracle.truffle.r.runtime.nmath.distr.DPois;
import com.oracle.truffle.r.runtime.nmath.distr.Dbinom;
import com.oracle.truffle.r.runtime.nmath.distr.Df;
import com.oracle.truffle.r.runtime.nmath.distr.Dnf;
import com.oracle.truffle.r.runtime.nmath.distr.Dnt;
import com.oracle.truffle.r.runtime.nmath.distr.Dt;
import com.oracle.truffle.r.runtime.nmath.distr.Exp.DExp;
import com.oracle.truffle.r.runtime.nmath.distr.Exp.PExp;
import com.oracle.truffle.r.runtime.nmath.distr.Exp.QExp;
import com.oracle.truffle.r.runtime.nmath.distr.Exp.RExp;
import com.oracle.truffle.r.runtime.nmath.distr.Geom;
import com.oracle.truffle.r.runtime.nmath.distr.Geom.DGeom;
import com.oracle.truffle.r.runtime.nmath.distr.Geom.RGeom;
import com.oracle.truffle.r.runtime.nmath.distr.LogNormal.DLNorm;
import com.oracle.truffle.r.runtime.nmath.distr.LogNormal.PLNorm;
import com.oracle.truffle.r.runtime.nmath.distr.LogNormal.QLNorm;
import com.oracle.truffle.r.runtime.nmath.distr.LogNormal.RLNorm;
import com.oracle.truffle.r.runtime.nmath.distr.Logis;
import com.oracle.truffle.r.runtime.nmath.distr.Logis.DLogis;
import com.oracle.truffle.r.runtime.nmath.distr.Logis.RLogis;
import com.oracle.truffle.r.runtime.nmath.distr.PGamma;
import com.oracle.truffle.r.runtime.nmath.distr.PHyper;
import com.oracle.truffle.r.runtime.nmath.distr.PNBeta;
import com.oracle.truffle.r.runtime.nmath.distr.PNBinom.PNBinomFunc;
import com.oracle.truffle.r.runtime.nmath.distr.PNBinom.PNBinomMu;
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
import com.oracle.truffle.r.runtime.nmath.distr.QNBinom.QNBinomFunc;
import com.oracle.truffle.r.runtime.nmath.distr.QNBinom.QNBinomMu;
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
import com.oracle.truffle.r.runtime.nmath.distr.RNBinom.RNBinomFunc;
import com.oracle.truffle.r.runtime.nmath.distr.RNBinom.RNBinomMu;
import com.oracle.truffle.r.runtime.nmath.distr.RNchisq;
import com.oracle.truffle.r.runtime.nmath.distr.RPois;
import com.oracle.truffle.r.runtime.nmath.distr.Rbinom;
import com.oracle.truffle.r.runtime.nmath.distr.Rf;
import com.oracle.truffle.r.runtime.nmath.distr.Rnorm;
import com.oracle.truffle.r.runtime.nmath.distr.Rt;
import com.oracle.truffle.r.runtime.nmath.distr.Signrank.DSignrank;
import com.oracle.truffle.r.runtime.nmath.distr.Signrank.PSignrank;
import com.oracle.truffle.r.runtime.nmath.distr.Signrank.QSignrank;
import com.oracle.truffle.r.runtime.nmath.distr.Signrank.RSignrank;
import com.oracle.truffle.r.runtime.nmath.distr.Unif.DUnif;
import com.oracle.truffle.r.runtime.nmath.distr.Unif.PUnif;
import com.oracle.truffle.r.runtime.nmath.distr.Unif.QUnif;
import com.oracle.truffle.r.runtime.nmath.distr.Unif.Runif;
import com.oracle.truffle.r.runtime.nmath.distr.Weibull.DWeibull;
import com.oracle.truffle.r.runtime.nmath.distr.Weibull.PWeibull;
import com.oracle.truffle.r.runtime.nmath.distr.Weibull.QWeibull;
import com.oracle.truffle.r.runtime.nmath.distr.Weibull.RWeibull;
import com.oracle.truffle.r.runtime.nmath.distr.Wilcox.DWilcox;
import com.oracle.truffle.r.runtime.nmath.distr.Wilcox.PWilcox;
import com.oracle.truffle.r.runtime.nmath.distr.Wilcox.QWilcox;
import com.oracle.truffle.r.runtime.nmath.distr.Wilcox.RWilcox;

/**
 * {@code .Call}, {@code .Call.graphics}, {@code .External}, {@code .External2},
 * {@code External.graphics} functions, which share a common signature.
 *
 * The native function to be called can be specified in two ways:
 * <ol>
 * <li>as an object of R class {@code NativeSymbolInfo} (passed as an {@link RList}. In this case
 * {@code .PACKAGE} is ignored even if provided.</li>
 * <li>as a character string. If {@code .PACKAGE} is provided the search is restricted to that
 * package, else the symbol is searched in all loaded packages (evidently dangerous as the symbol
 * could be duplicated)</li>
 * </ol>
 *
 * Many of the functions in the builtin packages have been translated to Java "external builtins".
 * This is handled by specializations that call {@link LookupAdapter#lookupBuiltin(String)}.
 *
 * Another possiblity to override the native functions with R functions is via FastR specific
 * built-in {@code .fastr.register.functions}. It registers overrides for given native function
 * pointers. Before executing any native function, we check if there is an override for it.
 *
 * TODO Completeness (more types, more error checks), Performance (copying).
 *
 * See <a href="https://stat.ethz.ch/R-manual/R-devel/library/base/html/Foreign.html">here</a>.
 */
public class CallAndExternalFunctions {

    @TruffleBoundary
    private static Object encodeArgumentPairList(RArgsValuesAndNames args, Object handle) {
        Object list = RNull.instance;
        for (int i = args.getLength() - 1; i >= 0; i--) {
            String name = args.getSignature().getName(i);
            list = RDataFactory.createPairList(args.getArgument(i), list, name == null ? RNull.instance : RDataFactory.createSymbolInterned(name));
        }
        list = RDataFactory.createPairList(handle, list);
        return list;
    }

    /**
     * Base class with common logic for all the variants.
     */
    protected abstract static class Dot extends LookupAdapter {
        @Child private InvokeCallNode callRFFINode = RFFIFactory.getCallRFFI().createInvokeCallNode();

        protected Object dispatch(VirtualFrame frame, NativeCallInfo nativeCallInfo, Object[] args) {
            return callRFFINode.dispatch(frame, nativeCallInfo, args);
        }

        protected static void applyCommonCasts(Casts casts) {
            casts.arg(".NAME").mustBe(instanceOf(RList.class).or(instanceOf(RExternalPtr.class)).or(stringValue())).mapIf(stringValue(), asStringVector().setNext(findFirst().stringElement()));
            casts.arg("PACKAGE").allowMissing().mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        }

        // Note: we cannot declare "abstract" methods, because Truffle DSL

        /**
         * This is where the logic for various native call interfaces differs: in the way how the
         * arguments and pre/post processed and what the result is.
         */
        protected Object dispatch(@SuppressWarnings("unused") VirtualFrame frame, @SuppressWarnings("unused") Object originalHandle, @SuppressWarnings("unused") RArgsValuesAndNames args,
                        @SuppressWarnings("unused") NativeCallInfo nativeCallInfo) {
            throw RInternalError.shouldNotReachHere();
        }

        protected NativeSymbolType getNativeSymbolType() {
            throw RInternalError.shouldNotReachHere();
        }

        protected String getDotBuiltinName() {
            // may be OK to not implement, if you override symbolNotFoundError
            throw RInternalError.shouldNotReachHere();
        }

        protected RuntimeException symbolNotFoundError(String symbol, String packageName) {
            throw error(RError.Message.SYMBOL_NOT_IN_TABLE, symbol, getDotBuiltinName(), packageName);
        }

        @Override
        protected RExternalBuiltinNode lookupBuiltin(String symbol) {
            return null;
        }

        // ----------------------------------------------------------
        // Specializations for the case of FastR's "external builtins" -- native functions
        // overridden in Java

        /**
         * {@code .NAME = NativeSymbolInfo} implemented as a builtin.
         */
        @Specialization(limit = "99", guards = {"cachedName != null", "cachedName.equals(getSymbolName(symbol))", "builtin != null"})
        protected Object doExternalBuiltinSymbolInfo(VirtualFrame frame, @SuppressWarnings("unused") RList symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") Object packageName,
                        @SuppressWarnings("unused") @Cached("getSymbolNameSlowPath(symbol)") String cachedName,
                        @Cached("lookupBuiltinSlowPath(symbol)") RExternalBuiltinNode builtin) {
            return builtin.call(frame, args);
        }

        /**
         * {@code .NAME = String} implemented as a builtin.
         */
        @Specialization(limit = "99", guards = {"cachedName != null", "cachedName.equals(symbol)", "builtin != null"})
        protected Object doExternalBuiltinString(VirtualFrame frame, @SuppressWarnings("unused") String symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") Object packageName,
                        @SuppressWarnings("unused") @Cached("symbol") String cachedName,
                        @Cached("lookupBuiltin(symbol)") RExternalBuiltinNode builtin) {
            return builtin.call(frame, args);
        }

        // ----------------------------------------------------------
        // Specializations for the case of native call info in an RList (NativeSymbolInfo)

        @Specialization(limit = "getCacheSize(2)", guards = {"cached == symbol", "builtin == null"})
        protected Object callSymbolInfoFunction(VirtualFrame frame, @SuppressWarnings("unused") RList symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") Object packageName,
                        @SuppressWarnings("unused") @Cached("symbol") RList cached,
                        @SuppressWarnings("unused") @Cached("lookupBuiltinSlowPath(symbol)") RExternalBuiltinNode builtin,
                        @SuppressWarnings("unused") @Cached("new()") ExtractNativeCallInfoNode extractSymbolInfo,
                        @Cached("extractSymbolInfo.execute(symbol)") NativeCallInfo nativeCallInfo,
                        @Cached CallRegisteredROverride callRegisteredROverride) {
            // AST sharing TODO: NativeCallInfo caches runtime objects: restrict to single context
            if (callRegisteredROverride.isRegisteredRFunction(nativeCallInfo)) {
                return callRegisteredROverride.execute(frame, nativeCallInfo, args);
            } else {
                return dispatch(frame, symbol, args, nativeCallInfo);
            }
        }

        @Specialization(replaces = "callSymbolInfoFunction", guards = "lookupBuiltin(getSymbolName(symbol)) == null")
        protected Object callSymbolInfoFunctionGeneric(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, Object packageName,
                        @Cached("new()") ExtractNativeCallInfoNode extractSymbolInfo,
                        @Cached CallRegisteredROverride callRegisteredROverride) {
            return callSymbolInfoFunction(frame, symbol, args, packageName, symbol, null, extractSymbolInfo, extractSymbolInfo.execute(symbol), callRegisteredROverride);
        }

        // ----------------------------------------------------------
        // Specializations for the case of native call as a String: name of the native function

        // Note: from cast pipeline: packageName must be missing or String
        protected static boolean packageNameEqual(Object a, Object b) {
            if (a == b) {
                return true;
            } else if (a == RMissing.instance || b == RMissing.instance) {
                return false;
            }
            return CompilerDirectives.castExact(a, String.class).equals(CompilerDirectives.castExact(b, String.class));
        }

        protected int getNST() {
            return getNativeSymbolType().ordinal();
        }

        // Caching on the String helps us to avoid builtin lookup, we may consider caching the
        // native function lookup but that would have to be guarded by single context assumption!
        @Specialization(limit = "getCacheSize(2)", guards = {"cachedSymbol.equals(symbol)", "builtin == null", "packageNameEqual(cachePackageArg, packageNameIn)"})
        protected Object callNamedFunctionCached(VirtualFrame frame, String symbol, RArgsValuesAndNames args, Object packageNameIn,
                        @SuppressWarnings("unused") @Cached("symbol") String cachedSymbol,
                        @SuppressWarnings("unused") @Cached("packageNameIn") Object cachePackageArg,
                        @SuppressWarnings("unused") @Cached("lookupBuiltin(symbol)") RExternalBuiltinNode builtin,
                        @Cached("createRegisteredNativeSymbol(getNST())") DLL.RegisteredNativeSymbol rns,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode,
                        @Cached CallRegisteredROverride callRegisteredROverride) {
            String packageName = packageNameIn == RMissing.instance ? null : (String) packageNameIn;
            DLL.SymbolHandle func = findSymbolNode.execute(symbol, packageName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                throw symbolNotFoundError(symbol, packageName);
            }
            if (callRegisteredROverride.isRegisteredRFunction(func)) {
                return callRegisteredROverride.execute(frame, func, args);
            } else {
                return dispatch(frame, symbol, args, new NativeCallInfo(symbol, func, rns.getDllInfo()));
            }
        }

        @Specialization(replaces = "callNamedFunctionCached", guards = "lookupBuiltin(symbol) == null")
        protected Object callNamedFunction(VirtualFrame frame, String symbol, RArgsValuesAndNames args, Object packageNameIn,
                        @Cached("createRegisteredNativeSymbol(getNST())") DLL.RegisteredNativeSymbol rns,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode,
                        @Cached CallRegisteredROverride callRegisteredROverride) {
            return callNamedFunctionCached(frame, symbol, args, packageNameIn, symbol, packageNameIn, null, rns, findSymbolNode, callRegisteredROverride);
        }

        // ----------------------------------------------------------
        // External pointer:

        @Specialization
        protected Object callExternalPtrFunction(VirtualFrame frame, RExternalPtr symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") Object packageName,
                        @Cached CallRegisteredROverride callRegisteredROverride) {
            if (callRegisteredROverride.isRegisteredRFunction(symbol)) {
                return callRegisteredROverride.execute(frame, symbol, args);
            } else {
                return dispatch(frame, symbol, args, new NativeCallInfo("", symbol.getAddr(), null));
            }
        }

        // ----------------------------------------------------------
        // Fallbacks: we need few extra specializations to satisfy the DSL compiler

        @Specialization(replaces = {"doExternalBuiltinSymbolInfo", "callSymbolInfoFunction"})
        protected Object doInfoFallback(@SuppressWarnings("unused") VirtualFrame frame, @SuppressWarnings("unused") RList symbol, @SuppressWarnings("unused") RArgsValuesAndNames args,
                        @SuppressWarnings("unused") Object packageName) {
            throw LookupAdapter.fallback(this, symbol);
        }

        @Specialization(replaces = {"doExternalBuiltinString", "callNamedFunction"})
        protected Object doStringFallback(@SuppressWarnings("unused") VirtualFrame frame, String symbol, @SuppressWarnings("unused") RArgsValuesAndNames args,
                        @SuppressWarnings("unused") Object packageName) {
            throw LookupAdapter.fallback(this, symbol);
        }

        @Fallback
        protected Object dotCallFallback(Object symbol, @SuppressWarnings("unused") Object args, @SuppressWarnings("unused") Object packageName) {
            throw fallback(this, symbol);
        }
    }

    @RBuiltin(name = ".Call", kind = PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"}, behavior = COMPLEX)
    public abstract static class DotCall extends Dot {

        @Child private MaterializeNode materializeNode = MaterializeNode.create();

        static {
            applyCommonCasts(new Casts(DotCall.class));
        }

        @Override
        public final Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, RArgsValuesAndNames.EMPTY, RMissing.instance};
        }

        private Object[] materializeArgs(Object[] args) {
            Object[] materializedArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                materializedArgs[i] = materializeNode.execute(args[i]);
            }
            return materializedArgs;
        }

        @Override
        protected final String getDotBuiltinName() {
            return "Call";
        }

        @Override
        protected NativeSymbolType getNativeSymbolType() {
            return DLL.NativeSymbolType.Call;
        }

        @Override
        protected final Object dispatch(VirtualFrame frame, Object originalHandle, RArgsValuesAndNames args, NativeCallInfo nativeCallInfo) {
            return super.dispatch(frame, nativeCallInfo, materializeArgs(args.getArguments()));
        }

        @Override
        @TruffleBoundary
        public final RExternalBuiltinNode lookupBuiltin(String name) {
            assert name != null;
            if (getRContext().getOption(UseInternalGridGraphics)) {
                RExternalBuiltinNode gridExternal = FastRGridExternalLookup.lookupDotCall(getRContext(), name);
                if (gridExternal != null) {
                    return gridExternal;
                }
            }
            switch (name) {
                // methods
                case "R_initMethodDispatch":
                    return R_initMethodDispatchNodeGen.create();
                case "R_methodsPackageMetaName":
                    return R_methodsPackageMetaNameNodeGen.create();
                case "R_set_method_dispatch":
                    return R_set_method_dispatchNodeGen.create();
                case "R_M_setPrimitiveMethods":
                    return R_M_setPrimitiveMethodsNodeGen.create();
                case "R_getClassFromCache":
                    return R_getClassFromCacheNodeGen.create();
                case "R_clear_method_selection":
                case "R_dummy_extern_place":
                case "R_el_named":
                    return new UnimplementedExternal(name);
                case "R_externalptr_prototype_object":
                    return R_externalPtrPrototypeObjectNodeGen.create();
                case "R_getGeneric":
                    return R_getGenericNodeGen.create();
                case "R_get_slot":
                    return R_getSlotNodeGen.create();
                case "R_hasSlot":
                    return R_hasSlotNodeGen.create();
                case "R_identC":
                    return R_identCNodeGen.create();
                case "R_methods_test_MAKE_CLASS":
                case "R_methods_test_NEW":
                case "R_missingArg":
                case "R_nextMethodCall":
                    return R_nextMethodCallNodeGen.create();
                case "R_quick_method_check":
                case "R_selectMethod":
                case "R_set_el_named":
                    return new UnimplementedExternal(name);
                case "R_set_slot":
                    return R_setSlotNodeGen.create();
                case "R_standardGeneric":
                    return new UnimplementedExternal(name);
                case "do_substitute_direct":
                    return SubstituteDirectNodeGen.create();
                case "Rf_allocS4Object":
                    return new UnimplementedExternal(name);
                case "R_get_primname":
                    return GetPrimNameNodeGen.create();
                case "new_object":
                    return NewObjectNodeGen.create();

                // stats

                case "fft":
                    return FftNodeGen.create();
                case "cov":
                    return CovcorNodeGen.create(false);
                case "cor":
                    return CovcorNodeGen.create(true);
                case "SplineCoef":
                    return SplineCoefNodeGen.create();
                case "SplineEval":
                    return SplineEvalNodeGen.create();
                case "pnorm":
                    return StatsFunctionsNodes.Function3_2Node.create(new Pnorm());
                case "qnorm":
                    return StatsFunctionsNodes.Function3_2Node.create(new Qnorm());
                case "rnorm":
                    return RandFunction2Node.createDouble(Rnorm::create);
                case "runif":
                    return RandFunction2Node.createDouble(Runif::create);
                case "rbeta":
                    return RandFunction2Node.createDouble(RBeta::create);
                case "rgamma":
                    return RandFunction2Node.createDouble(RGamma::create);
                case "rcauchy":
                    return RandFunction2Node.createDouble(RCauchy::create);
                case "rf":
                    return RandFunction2Node.createDouble(Rf::create);
                case "rlogis":
                    return RandFunction2Node.createDouble(RLogis::create);
                case "rweibull":
                    return RandFunction2Node.createDouble(RWeibull::create);
                case "rnchisq":
                    return RandFunction2Node.createDouble(RNchisq::create);
                case "rnbinom_mu":
                    return RandFunction2Node.createDouble(RNBinomMu::create);
                case "rwilcox":
                    return RandFunction2Node.createInt(RWilcox::create);
                case "rchisq":
                    return RandFunction1Node.createDouble(RChisq::create);
                case "rexp":
                    return RandFunction1Node.createDouble(RExp::create);
                case "rgeom":
                    return RandFunction1Node.createInt(RGeom::create);
                case "rpois":
                    return RandFunction1Node.createInt(RPois::create);
                case "rnbinom":
                    return RandFunction2Node.createInt(RNBinomFunc::create);
                case "rt":
                    return RandFunction1Node.createDouble(Rt::create);
                case "rsignrank":
                    return RandFunction1Node.createInt(RSignrank::create);
                case "rhyper":
                    return RandFunction3Node.createInt(RHyper::new);
                case "phyper":
                    return StatsFunctionsNodes.Function4_2Node.create(new PHyper());
                case "dhyper":
                    return StatsFunctionsNodes.Function4_1Node.create(new DHyper());
                case "qhyper":
                    return StatsFunctionsNodes.Function4_2Node.create(new QHyper());
                case "pnchisq":
                    return StatsFunctionsNodes.Function3_2Node.create(new PNChisq());
                case "qnchisq":
                    return StatsFunctionsNodes.Function3_2Node.create(new QNChisq());
                case "dnchisq":
                    return StatsFunctionsNodes.Function3_1Node.create(new DNChisq());
                case "qt":
                    return StatsFunctionsNodes.Function2_2Node.create(new Qt());
                case "pt":
                    return StatsFunctionsNodes.Function2_2Node.create(new Pt());
                case "qgamma":
                    return StatsFunctionsNodes.Function3_2Node.create(new QGamma());
                case "dbinom":
                    return StatsFunctionsNodes.Function3_1Node.create(new Dbinom());
                case "qbinom":
                    return StatsFunctionsNodes.Function3_2Node.create(new Qbinom());
                case "punif":
                    return StatsFunctionsNodes.Function3_2Node.create(new PUnif());
                case "dunif":
                    return StatsFunctionsNodes.Function3_1Node.create(new DUnif());
                case "qunif":
                    return StatsFunctionsNodes.Function3_2Node.create(new QUnif());
                case "ppois":
                    return StatsFunctionsNodes.Function2_2Node.create(new PPois());
                case "qpois":
                    return StatsFunctionsNodes.Function2_2Node.create(new QPois());
                case "qweibull":
                    return StatsFunctionsNodes.Function3_2Node.create(new QWeibull());
                case "pweibull":
                    return StatsFunctionsNodes.Function3_2Node.create(new PWeibull());
                case "dweibull":
                    return StatsFunctionsNodes.Function3_1Node.create(new DWeibull());
                case "rbinom":
                    return RandFunction2Node.createInt(Rbinom::create);
                case "pbinom":
                    return StatsFunctionsNodes.Function3_2Node.create(new Pbinom());
                case "pbeta":
                    return StatsFunctionsNodes.Function3_2Node.create(new Pbeta());
                case "qbeta":
                    return StatsFunctionsNodes.Function3_2Node.create(new QBeta());
                case "dcauchy":
                    return StatsFunctionsNodes.Function3_1Node.create(new DCauchy());
                case "pcauchy":
                    return StatsFunctionsNodes.Function3_2Node.create(new PCauchy());
                case "qcauchy":
                    return StatsFunctionsNodes.Function3_2Node.create(new Cauchy.QCauchy());
                case "pf":
                    return StatsFunctionsNodes.Function3_2Node.create(new Pf());
                case "qf":
                    return StatsFunctionsNodes.Function3_2Node.create(new Qf());
                case "df":
                    return StatsFunctionsNodes.Function3_1Node.create(new Df());
                case "dgamma":
                    return StatsFunctionsNodes.Function3_1Node.create(new DGamma());
                case "pgamma":
                    return StatsFunctionsNodes.Function3_2Node.create(new PGamma());
                case "dchisq":
                    return StatsFunctionsNodes.Function2_1Node.create(new Chisq.DChisq());
                case "qchisq":
                    return StatsFunctionsNodes.Function2_2Node.create(new Chisq.QChisq());
                case "qgeom":
                    return StatsFunctionsNodes.Function2_2Node.create(new Geom.QGeom());
                case "pchisq":
                    return StatsFunctionsNodes.Function2_2Node.create(new Chisq.PChisq());
                case "dexp":
                    return StatsFunctionsNodes.Function2_1Node.create(new DExp());
                case "pexp":
                    return StatsFunctionsNodes.Function2_2Node.create(new PExp());
                case "qexp":
                    return StatsFunctionsNodes.Function2_2Node.create(new QExp());
                case "dgeom":
                    return StatsFunctionsNodes.Function2_1Node.create(new DGeom());
                case "dpois":
                    return StatsFunctionsNodes.Function2_1Node.create(new DPois());
                case "dbeta":
                    return StatsFunctionsNodes.Function3_1Node.create(new DBeta());
                case "dnbeta":
                    return StatsFunctionsNodes.Function4_1Node.create(new DNBeta());
                case "qnbeta":
                    return StatsFunctionsNodes.Function4_2Node.create(new QNBeta());
                case "dnf":
                    return StatsFunctionsNodes.Function4_1Node.create(new Dnf());
                case "qnf":
                    return StatsFunctionsNodes.Function4_2Node.create(new Qnf());
                case "pnf":
                    return StatsFunctionsNodes.Function4_2Node.create(new Pnf());
                case "pnbeta":
                    return StatsFunctionsNodes.Function4_2Node.create(new PNBeta());
                case "dt":
                    return StatsFunctionsNodes.Function2_1Node.create(new Dt());
                case "rlnorm":
                    return RandFunction2Node.createDouble(RLNorm::create);
                case "dlnorm":
                    return StatsFunctionsNodes.Function3_1Node.create(new DLNorm());
                case "qlnorm":
                    return StatsFunctionsNodes.Function3_2Node.create(new QLNorm());
                case "plnorm":
                    return StatsFunctionsNodes.Function3_2Node.create(new PLNorm());
                case "dlogis":
                    return StatsFunctionsNodes.Function3_1Node.create(new DLogis());
                case "qlogis":
                    return StatsFunctionsNodes.Function3_2Node.create(new Logis.QLogis());
                case "plogis":
                    return StatsFunctionsNodes.Function3_2Node.create(new Logis.PLogis());
                case "pgeom":
                    return StatsFunctionsNodes.Function2_2Node.create(new Geom.PGeom());
                case "qnbinom":
                    return StatsFunctionsNodes.Function3_2Node.create(new QNBinomFunc());
                case "dnbinom":
                    return StatsFunctionsNodes.Function3_1Node.create(new DNBinomFunc());
                case "pnbinom":
                    return StatsFunctionsNodes.Function3_2Node.create(new PNBinomFunc());
                case "qnbinom_mu":
                    return StatsFunctionsNodes.Function3_2Node.create(new QNBinomMu());
                case "dnbinom_mu":
                    return StatsFunctionsNodes.Function3_1Node.create(new DNBinomMu());
                case "pnbinom_mu":
                    return StatsFunctionsNodes.Function3_2Node.create(new PNBinomMu());
                case "qwilcox":
                    return StatsFunctionsNodes.Function3_2Node.create(new QWilcox());
                case "pwilcox":
                    return StatsFunctionsNodes.Function3_2Node.create(new PWilcox());
                case "dwilcox":
                    return StatsFunctionsNodes.Function3_1Node.create(new DWilcox());
                case "dsignrank":
                    return StatsFunctionsNodes.Function2_1Node.create(new DSignrank());
                case "psignrank":
                    return StatsFunctionsNodes.Function2_2Node.create(new PSignrank());
                case "dnt":
                    return StatsFunctionsNodes.Function3_1Node.create(new Dnt());
                case "pnt":
                    return StatsFunctionsNodes.Function3_2Node.create(new Pnt());
                case "qnt":
                    return StatsFunctionsNodes.Function3_2Node.create(new Qnt());
                case "qsignrank":
                    return StatsFunctionsNodes.Function2_2Node.create(new QSignrank());
                case "qtukey":
                    return StatsFunctionsNodes.Function4_2Node.create(new QTukey());
                case "ptukey":
                    return StatsFunctionsNodes.Function4_2Node.create(new PTukey());
                case "rmultinom":
                    return RMultinomNode.create();
                case "Approx":
                    return Approx.create();
                case "ApproxTest":
                    return ApproxTest.create();
                case "Cdist":
                    return CdistNodeGen.create();
                case "DoubleCentre":
                    return DoubleCentreNodeGen.create();
                case "cutree":
                    return CutreeNodeGen.create();
                case "BinDist":
                    return BinDist.create();
                case "influence":
                    return Influence.create();
                case "mvfft":
                    // TODO: only transforms arguments and then calls already ported fft
                    return new UnimplementedExternal(name);
                case "nextn":
                    // TODO: do not want to pull in fourier.c, should be simple to port
                    return new UnimplementedExternal(name);
                case "r2dtable":
                    // TODO: do not want to pull in random.c + uses PutRNG(), we can pull in rcont.c
                    // and then this
                    // becomes simple wrapper around it.
                    return new UnimplementedExternal(name);
                case "dqagi":
                case "dqags":
                case "Rsm":
                    return new UnimplementedExternal(name);
                case "pp_sum":
                    return PPSumExternal.create();
                case "intgrt_vec":
                    return PPSum.IntgrtVecNode.create();

                case "updateform":
                    return getExternalModelBuiltinNode(getRContext(), "updateform");

                case "Cdqrls":
                    return new RInternalCodeBuiltinNode("stats", RInternalCode.loadSourceRelativeTo(getRContext(), RandFunctionsNodes.class, "lm.R"), "Cdqrls");

                case "dnorm":
                    return StatsFunctionsNodes.Function3_1Node.create(new DNorm());

                // tools
                case "doTabExpand":
                    return DoTabExpandNodeGen.create();
                case "codeFilesAppend":
                    return CodeFilesAppendNodeGen.create();
                case "Rmd5":
                    return Rmd5NodeGen.create();
                case "dirchmod":
                    return DirChmodNodeGen.create();
                case "delim_match":
                case "C_getfmts":
                case "check_nonASCII":
                case "check_nonASCII2":
                case "ps_kill":
                case "ps_sigs":
                case "ps_priority":
                case "startHTTPD":
                case "stopHTTPD":
                case "C_deparseRd":
                    return new UnimplementedExternal(name);

                // utils
                case "crc64":
                    return Crc64NodeGen.create();
                case "flushconsole":
                    return new Flushconsole();
                case "menu":
                    return MenuNodeGen.create();
                case "nsl":
                    return new UnimplementedExternal(name);
                case "objectSize":
                    return ObjectSizeNodeGen.create();
                case "octsize":
                    return OctSizeNode.create();
                case "processevents":
                case "sockconnect":
                case "sockread":
                case "sockclose":
                case "sockopen":
                case "socklisten":
                case "sockwrite":
                    return new UnimplementedExternal(name);

                // parallel
                case "mc_is_child":
                    return MCIsChildNodeGen.create();
                default:
                    return null;
            }
            // Note: some externals that may be ported with reasonable effort
            // tukeyline, rfilter, SWilk, acf, Burg, d2x2xk, pRho
        }
    }

    @com.oracle.truffle.r.runtime.builtins.RBuiltin(name = ".External", kind = RBuiltinKind.PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"}, behavior = RBehavior.COMPLEX)
    public abstract static class DotExternal extends Dot {

        static {
            applyCommonCasts(new Casts(DotExternal.class));
        }

        @Override
        @TruffleBoundary
        public final RExternalBuiltinNode lookupBuiltin(String name) {
            if (getRContext().getOption(UseInternalGridGraphics)) {
                RExternalBuiltinNode gridExternal = FastRGridExternalLookup.lookupDotExternal(name);
                if (gridExternal != null) {
                    return gridExternal;
                }
            }
            switch (name) {
                case "compcases":
                    return new CompleteCases();
                // stats
                case "doD":
                    return D.create();
                case "deriv":
                    return Deriv.create();
                // utils
                case "countfields":
                    return CountFieldsNodeGen.create();
                case "readtablehead":
                    return ReadTableHeadNodeGen.create();
                case "download":
                    return DownloadNodeGen.create();
                case "termsform":
                    return getExternalModelBuiltinNode(getRContext(), "termsform");
                case "Rprof":
                    return RprofNodeGen.create();
                case "Rprofmem":
                    return RprofmemNodeGen.create();
                case "wilcox_free":
                    return new WilcoxFreeNode();
                case "signrank_free":
                    return new SignrankFreeNode();
                case "unzip":
                    return UnzipNodeGen.create();
                case "addhistory":
                case "loadhistory":
                case "savehistory":
                case "dataentry":
                case "dataviewer":
                case "edit":
                case "fileedit":
                case "selectlist":
                    return new UnimplementedExternal(name);
                default:
                    return null;
            }
        }

        @Override
        protected final String getDotBuiltinName() {
            return "External";
        }

        @Override
        protected NativeSymbolType getNativeSymbolType() {
            return NativeSymbolType.External;
        }

        @Override
        protected final Object dispatch(VirtualFrame frame, Object originalHandle, RArgsValuesAndNames args, NativeCallInfo nativeCallInfo) {
            Object list = encodeArgumentPairList(args, originalHandle);
            return dispatch(frame, nativeCallInfo, new Object[]{list});
        }
    }

    @RBuiltin(name = ".External2", visibility = CUSTOM, kind = PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"}, behavior = COMPLEX)
    public abstract static class DotExternal2 extends Dot {
        private static final Object CALL = "call";

        // AST sharing TODO: this is a runtime object cached in AST
        /**
         * This argument for the native function should be SPECIALSXP reprenting the .External2
         * builtin. In GnuR SPECIALSXP is index into the table of builtins. External2 and External
         * are in fact one native function with two entries in this table, the "op" argument is used
         * to determine whether the call was made to .External or .External2. The actual code of the
         * native function that is eventually invoked will always get SPECIALSXP reprenting the
         * .External2, becuase functions exported as .External do not take the "op" argument.
         */
        @CompilationFinal private Object op = null;

        static {
            applyCommonCasts(new Casts(DotExternal2.class));
        }

        private Object getOp() {
            if (op == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                op = getRContext().lookupBuiltin(".External2");
            }
            return op;
        }

        @Override
        protected final String getDotBuiltinName() {
            return "External2";
        }

        @Override
        protected NativeSymbolType getNativeSymbolType() {
            return NativeSymbolType.External;
        }

        @Override
        protected final Object dispatch(VirtualFrame frame, Object originalHandle, RArgsValuesAndNames args, NativeCallInfo nativeCallInfo) {
            Object list = encodeArgumentPairList(args, originalHandle);
            REnvironment rho = REnvironment.frameToEnvironment(frame.materialize());
            return dispatch(frame, nativeCallInfo, new Object[]{CALL, getOp(), list, rho});
        }

        @Override
        @TruffleBoundary
        public final RExternalBuiltinNode lookupBuiltin(String name) {
            if (getRContext().getOption(UseInternalGridGraphics)) {
                RExternalBuiltinNode gridExternal = FastRGridExternalLookup.lookupDotExternal2(name);
                if (gridExternal != null) {
                    return gridExternal;
                }
            }
            switch (name) {
                // tools
                case "writetable":
                    return WriteTableNodeGen.create();
                case "typeconvert":
                    return TypeConvertNodeGen.create();
                case "C_parseRd":
                    return C_ParseRdNodeGen.create();
                case "modelmatrix":
                case "modelframe":
                    return getExternalModelBuiltinNode(getRContext(), name);
                case "zeroin2":
                    return Zeroin2.create();

                // stats:
                case "do_fmin":
                    return Fmin.create();

                default:
                    return null;
            }
        }
    }

    public abstract static class DisplayListRecordingDot extends Dot implements RBuiltinNode.WithSideEffect {

        @Child private AbstractBeforeGraphicsOpNode beforeGraphicsOpNode = RFFIFactory.getMiscRFFI().createBeforeGraphicsOpNode();
        @Child private AbstractAfterGraphicsOpNode afterGraphicsOpNode = RFFIFactory.getMiscRFFI().createAfterGraphicsOpNode();

        @Override
        public Object beforeCall(VirtualFrame frame, RFunction currentFunction, RArgsValuesAndNames orderedArguments, S3Args s3Args) {
            return beforeGraphicsOpNode.execute();
        }

        @Override
        public void afterCall(VirtualFrame frame, RFunction currentFunction, RArgsValuesAndNames orderedArguments, S3Args s3Args, Object savedReturn) {
            Object opArgs = orderedArguments.getArgument(1);
            assert opArgs instanceof RArgsValuesAndNames;
            Object opArgsPL = ((RArgsValuesAndNames) opArgs).toPairlist();
            Object op = orderedArguments.getArgument(0);
            RPairList opCall;
            if (opArgsPL == RMissing.instance) {
                opCall = RDataFactory.createPairList(op);
            } else {
                opCall = RDataFactory.createPairList(op, opArgsPL);
            }
            int res = afterGraphicsOpNode.execute(currentFunction, opCall, (int) savedReturn);
            if (res < 0) {
                throw RInternalError.shouldNotReachHere("invalid graphics state");
            }
        }
    }

    @RBuiltin(name = ".External.graphics", kind = PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"}, behavior = COMPLEX)
    public abstract static class DotExternalGraphics extends DisplayListRecordingDot {
        static {
            applyCommonCasts(new Casts(DotExternalGraphics.class));
        }

        @Override
        protected final Object dispatch(VirtualFrame frame, Object originalHandle, RArgsValuesAndNames args, NativeCallInfo nativeCallInfo) {
            Object list = encodeArgumentPairList(args, originalHandle);
            return dispatch(frame, nativeCallInfo, new Object[]{list});
        }

        @Override
        protected NativeSymbolType getNativeSymbolType() {
            return NativeSymbolType.External;
        }

        @Override
        protected final RuntimeException symbolNotFoundError(String symbol, String packageName) {
            throw error(RError.Message.C_SYMBOL_NOT_IN_TABLE, symbol);
        }
    }

    @RBuiltin(name = ".Call.graphics", kind = PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"}, behavior = COMPLEX)
    public abstract static class DotCallGraphics extends DisplayListRecordingDot {
        static {
            applyCommonCasts(new Casts(DotCallGraphics.class));
        }

        @Override
        public final Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, RArgsValuesAndNames.EMPTY, RMissing.instance};
        }

        @Override
        protected final Object dispatch(VirtualFrame frame, Object originalHandle, RArgsValuesAndNames args, NativeCallInfo nativeCallInfo) {
            return dispatch(frame, nativeCallInfo, args.getArguments());
        }

        @Override
        protected NativeSymbolType getNativeSymbolType() {
            return NativeSymbolType.Call;
        }

        @Override
        protected final RuntimeException symbolNotFoundError(String symbol, String packageName) {
            throw error(RError.Message.C_SYMBOL_NOT_IN_TABLE, symbol);
        }

        @Override
        public final RExternalBuiltinNode lookupBuiltin(String name) {
            if (getRContext().getOption(UseInternalGridGraphics)) {
                return FastRGridExternalLookup.lookupDotCallGraphics(name);
            } else {
                return null;
            }
        }
    }
}
