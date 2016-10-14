/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base.foreign;

import static com.oracle.truffle.r.runtime.RVisibility.CUSTOM;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.library.grDevices.DevicesCCalls;
import com.oracle.truffle.r.library.graphics.GraphicsCCalls;
import com.oracle.truffle.r.library.graphics.GraphicsCCalls.C_Par;
import com.oracle.truffle.r.library.graphics.GraphicsCCalls.C_PlotXY;
import com.oracle.truffle.r.library.grid.GridFunctionsFactory.InitGridNodeGen;
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
import com.oracle.truffle.r.library.methods.SlotFactory.R_setSlotNodeGen;
import com.oracle.truffle.r.library.methods.SubstituteDirectNodeGen;
import com.oracle.truffle.r.library.parallel.ParallelFunctionsFactory.MCIsChildNodeGen;
import com.oracle.truffle.r.library.stats.CompleteCases;
import com.oracle.truffle.r.library.stats.Covcor;
import com.oracle.truffle.r.library.stats.Dbinom;
import com.oracle.truffle.r.library.stats.GammaFunctionsFactory.QgammaNodeGen;
import com.oracle.truffle.r.library.stats.Pbinom;
import com.oracle.truffle.r.library.stats.Pf;
import com.oracle.truffle.r.library.stats.Pnorm;
import com.oracle.truffle.r.library.stats.Qbinom;
import com.oracle.truffle.r.library.stats.Qnorm;
import com.oracle.truffle.r.library.stats.RbinomNodeGen;
import com.oracle.truffle.r.library.stats.RnormNodeGen;
import com.oracle.truffle.r.library.stats.RunifNodeGen;
import com.oracle.truffle.r.library.stats.SplineFunctionsFactory.SplineCoefNodeGen;
import com.oracle.truffle.r.library.stats.SplineFunctionsFactory.SplineEvalNodeGen;
import com.oracle.truffle.r.library.stats.StatsFunctionsFactory;
import com.oracle.truffle.r.library.stats.StatsUtil;
import com.oracle.truffle.r.library.tools.C_ParseRdNodeGen;
import com.oracle.truffle.r.library.tools.DirChmodNodeGen;
import com.oracle.truffle.r.library.tools.Rmd5NodeGen;
import com.oracle.truffle.r.library.tools.ToolsTextFactory.CodeFilesAppendNodeGen;
import com.oracle.truffle.r.library.tools.ToolsTextFactory.DoTabExpandNodeGen;
import com.oracle.truffle.r.library.utils.CountFields;
import com.oracle.truffle.r.library.utils.Crc64NodeGen;
import com.oracle.truffle.r.library.utils.Download;
import com.oracle.truffle.r.library.utils.MenuNodeGen;
import com.oracle.truffle.r.library.utils.ObjectSizeNodeGen;
import com.oracle.truffle.r.library.utils.RprofNodeGen;
import com.oracle.truffle.r.library.utils.RprofmemNodeGen;
import com.oracle.truffle.r.library.utils.TypeConvertNodeGen;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RInternalCodeBuiltinNode;
import com.oracle.truffle.r.nodes.objects.GetPrimNameNodeGen;
import com.oracle.truffle.r.nodes.objects.NewObjectNodeGen;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalCode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

/**
 * {@code .Call} {@code .Fortran}, {@code .External}, {@code .External2}, {@code External.graphics}
 * functions.
 *
 * TODO Completeness (more types, more error checks), Performance (copying). Especially all the
 * subtleties around copying.
 *
 * See <a href="https://stat.ethz.ch/R-manual/R-devel/library/base/html/Foreign.html">here</a>.
 */
public class ForeignFunctions {

    @TruffleBoundary
    protected static Object encodeArgumentPairList(RArgsValuesAndNames args, String symbolName) {
        Object list = RNull.instance;
        for (int i = args.getLength() - 1; i >= 0; i--) {
            String name = args.getSignature().getName(i);
            list = RDataFactory.createPairList(args.getArgument(i), list, name == null ? RNull.instance : RDataFactory.createSymbolInterned(name));
        }
        list = RDataFactory.createPairList(symbolName, list);
        return list;
    }

    /**
     * Locator for "builtin" package function implementations. The "builtin" packages contain many
     * functions that are called from R code via the FFI, e.g. {@code .Call}, but implemented
     * internally in GnuR, and not necessarily following the FFI API. The value passed to
     * {@code .Call} etc., is a symbol, created when the package is loaded and stored in the
     * namespace environment of the package, that is a list-valued object. Evidently these
     * "builtins" are somewhat similar to the {@code .Primitive} and {@code .Internal} builtins and,
     * similarly, most of these are re-implemented in Java in FastR. The
     * {@link #lookupBuiltin(RList)} method checks the name in the list object and returns the
     * {@link RExternalBuiltinNode} that implements the function, or {@code null}. A {@code null}
     * result implies that the builtin is not implemented in Java, but called directly via the FFI
     * interface, which is only possible for functions that use the FFI in a way that FastR can
     * handle.
     */
    protected abstract static class LookupAdapter extends RBuiltinNode {
        protected static class UnimplementedExternal extends RExternalBuiltinNode {
            private final String name;

            public UnimplementedExternal(String name) {
                this.name = name;
            }

            @Override
            public final Object call(RArgsValuesAndNames args) {
                throw RInternalError.unimplemented("unimplemented external builtin: " + name);
            }
        }

        protected abstract RExternalBuiltinNode lookupBuiltin(RList f);

        private static final String UNKNOWN_EXTERNAL_BUILTIN = "UNKNOWN_EXTERNAL_BUILTIN";

        protected static String lookupName(RList f) {
            CompilerAsserts.neverPartOfCompilation();
            if (f.getNames() != null) {
                RAbstractStringVector names = f.getNames();
                for (int i = 0; i < names.getLength(); i++) {
                    if (names.getDataAt(i).equals("name")) {
                        String name = RRuntime.asString(f.getDataAt(i));
                        return name != null ? name : UNKNOWN_EXTERNAL_BUILTIN;
                    }
                }
            }
            return UNKNOWN_EXTERNAL_BUILTIN;
        }

        @TruffleBoundary
        protected RuntimeException fallback(Object fobj) {
            String name = null;
            if (fobj instanceof RList) {
                name = lookupName((RList) fobj);
                name = name == UNKNOWN_EXTERNAL_BUILTIN ? null : name;
                if (name != null && lookupBuiltin((RList) fobj) != null) {
                    /*
                     * if we reach this point, then the cache saw a different value for f. the lists
                     * that contain the information about native calls are never expected to change.
                     */
                    throw RInternalError.shouldNotReachHere("fallback reached for " + getRBuiltin().name() + " " + name);
                }
            }
            throw RError.nyi(this, getRBuiltin().name() + " specialization failure: " + (name == null ? "<unknown>" : name));
        }

        @Child private ExtractVectorNode nameExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
        @Child private ExtractVectorNode addressExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);

        protected String getNameFromSymbolInfo(VirtualFrame frame, RList symbol) {
            if (nameExtract == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                nameExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
            }
            return RRuntime.asString(nameExtract.applyAccessField(frame, symbol, "name"));
        }

        protected long getAddressFromSymbolInfo(VirtualFrame frame, RList symbol) {
            if (addressExtract == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                addressExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
            }
            return ((RExternalPtr) addressExtract.applyAccessField(frame, symbol, "address")).getAddr();
        }

        protected String checkPackageArg(Object rPackage, BranchProfile errorProfile) {
            String libName = null;
            if (!(rPackage instanceof RMissing)) {
                libName = RRuntime.asString(rPackage);
                if (libName == null) {
                    errorProfile.enter();
                    throw RError.error(this, RError.Message.ARGUMENT_MUST_BE_STRING, "PACKAGE");
                }
            }
            return libName;
        }

        protected static RExternalBuiltinNode getExternalModelBuiltinNode(String name) {
            return new RInternalCodeBuiltinNode(RContext.getInstance(), "stats", RInternalCode.loadSourceRelativeTo(StatsUtil.class, "model.R"), name);
        }
    }

    /**
     * Interface to .Fortran native functions. Some functions have explicit implementations in
     * FastR, otherwise the .Fortran interface uses the machinery that implements the .C interface.
     */
    @RBuiltin(name = ".Fortran", kind = PRIMITIVE, parameterNames = {".NAME", "...", "NAOK", "DUP", "PACKAGE", "ENCODING"}, behavior = COMPLEX)
    public abstract static class Fortran extends LookupAdapter {

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, RArgsValuesAndNames.EMPTY, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_FALSE, RMissing.instance, RMissing.instance};
        }

        @Override
        @TruffleBoundary
        protected RExternalBuiltinNode lookupBuiltin(RList f) {
            switch (lookupName(f)) {
                case "dqrdc2":
                    return new Dqrdc2();
                case "dqrcf":
                    return new Dqrcf();
                default:
                    return null;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == f", "builtin != null"})
        protected Object doExternal(VirtualFrame frame, RList f, RArgsValuesAndNames args, byte naok, byte dup, Object rPackage, RMissing encoding, //
                        @Cached("f") RList cached, //
                        @Cached("lookupBuiltin(f)") RExternalBuiltinNode builtin) {
            return builtin.call(frame, args);
        }

        @Specialization(guards = "lookupBuiltin(symbol) == null")
        protected RList c(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, byte naok, byte dup, @SuppressWarnings("unused") Object rPackage,
                        @SuppressWarnings("unused") RMissing encoding) {
            return DotC.dispatch(this, getAddressFromSymbolInfo(frame, symbol), getNameFromSymbolInfo(frame, symbol), naok, dup, args);
        }

        @Specialization
        protected RList c(RAbstractStringVector f, RArgsValuesAndNames args, byte naok, byte dup, Object rPackage, @SuppressWarnings("unused") RMissing encoding, //
                        @Cached("create()") BranchProfile errorProfile) {
            String libName = checkPackageArg(rPackage, errorProfile);
            DLL.RegisteredNativeSymbol rns = new DLL.RegisteredNativeSymbol(DLL.NativeSymbolType.Fortran, null, null);
            long func = DLL.findSymbol(f.getDataAt(0), libName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.C_SYMBOL_NOT_IN_TABLE, f);
            }
            return DotC.dispatch(this, func, f.getDataAt(0), naok, dup, args);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object fallback(Object f, Object args, Object naok, Object dup, Object rPackage, Object encoding) {
            throw fallback(f);
        }
    }

    /**
     * Handles the generic case, but also many special case functions that are called from the
     * default packages.
     */
    @RBuiltin(name = ".Call", kind = PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"}, behavior = COMPLEX)
    public abstract static class DotCall extends LookupAdapter {

        private final BranchProfile errorProfile = BranchProfile.create();

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, RArgsValuesAndNames.EMPTY, RMissing.instance};
        }

        @Override
        @TruffleBoundary
        protected RExternalBuiltinNode lookupBuiltin(RList f) {
            String name = lookupName(f);
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
                    return new UnimplementedExternal(name);
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
                    return new Fft();
                case "cov":
                    return new Covcor(false);
                case "cor":
                    return new Covcor(true);
                case "SplineCoef":
                    return SplineCoefNodeGen.create();
                case "SplineEval":
                    return SplineEvalNodeGen.create();
                case "pnorm":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new Pnorm());
                case "qnorm":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new Qnorm());
                case "rnorm":
                    return RnormNodeGen.create();
                case "runif":
                    return RunifNodeGen.create();
                case "qgamma":
                    return QgammaNodeGen.create();
                case "dbinom":
                    return StatsFunctionsFactory.Function3_1NodeGen.create(new Dbinom());
                case "qbinom":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new Qbinom());
                case "rbinom":
                    return RbinomNodeGen.create();
                case "pbinom":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new Pbinom());
                case "pf":
                    return StatsFunctionsFactory.Function3_2NodeGen.create(new Pf());
                case "cutree":
                case "isoreg":
                case "monoFC_m":
                case "numeric_deriv":
                case "nls_iter":
                case "setup_starma":
                case "free_starma":
                case "set_trans":
                case "arma0fa":
                case "get_s2":
                case "get_resid":
                case "Dotrans":
                case "arma0_kfore":
                case "Starma_method":
                case "Invtrans":
                case "Gradtrans":
                case "ARMAtoMA":
                case "KalmanLike":
                case "KalmanFore":
                case "KalmanSmooth":
                case "ARIMA_undoPars":
                case "ARIMA_transPars":
                case "ARIMA_Invtrans":
                case "ARIMA_Gradtrans":
                case "ARIMA_Like":
                case "ARIMA_CSS":
                case "TSconv":
                case "getQ0":
                case "getQ0bis":
                case "port_ivset":
                case "port_nlminb":
                case "port_nlsb":
                case "logit_link":
                case "logit_linkinv":
                case "logit_mu_eta":
                case "binomial_dev_resids":
                case "rWishart":
                case "Cdist":
                case "mvfft":
                case "nextn":
                case "r2dtable":
                case "cfilter":
                case "rfilter":
                case "lowess":
                case "DoubleCentre":
                case "BinDist":
                case "Rsm":
                case "tukeyline":
                case "runmed":
                case "influence":
                case "pSmirnov2x":
                case "pKolmogorov2x":
                case "pKS2":
                case "ksmooth":
                case "Approx":
                case "ApproxTest":
                case "LogLin":
                case "pAnsari":
                case "qAnsari":
                case "pKendall":
                case "pRho":
                case "SWilk":
                case "bw_den":
                case "bw_ucv":
                case "bw_bcv":
                case "bw_phi4":
                case "bw_phi6":
                case "acf":
                case "pacf1":
                case "ar2ma":
                case "Burg":
                case "intgrt_vec":
                case "pp_sum":
                case "Fexact":
                case "Fisher_sim":
                case "chisq_sim":
                case "d2x2xk":
                    return new UnimplementedExternal(name);

                case "updateform":
                    return getExternalModelBuiltinNode("updateform");

                case "Cdqrls":
                    return new RInternalCodeBuiltinNode(RContext.getInstance(), "stats", RInternalCode.loadSourceRelativeTo(StatsUtil.class, "lm.R"), "Cdqrls");

                case "dnorm":
                    return StatsFunctionsFactory.Function3_1NodeGen.create(new Dnorm4());

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
                case "processevents":
                case "octsize":
                case "sockconnect":
                case "sockread":
                case "sockclose":
                case "sockopen":
                case "socklisten":
                case "sockwrite":
                    return new UnimplementedExternal(name);

                // grDevices
                case "cairoProps":
                    return CairoPropsNodeGen.create();
                case "makeQuartzDefault":
                    return new MakeQuartzDefault();

                // grid
                case "L_initGrid":
                    return InitGridNodeGen.create();

                // parallel
                case "mc_is_child":
                    return MCIsChildNodeGen.create();
                default:
                    return null;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == f", "builtin != null"})
        protected Object doExternal(VirtualFrame frame, RList f, RArgsValuesAndNames args, RMissing packageName, //
                        @Cached("f") RList cached, //
                        @Cached("lookupBuiltin(f)") RExternalBuiltinNode builtin) {
            return builtin.call(frame, args);
        }

        @Specialization
        protected Object callNamedFunction(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") Object packageName) {
            return RFFIFactory.getRFFI().getCallRFFI().invokeCall(getAddressFromSymbolInfo(frame, symbol), getNameFromSymbolInfo(frame, symbol), args.getArguments());
        }

        @Specialization
        protected Object callNamedFunction(String name, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName) {
            return callNamedFunctionWithPackage(name, args, null);
        }

        @Specialization
        protected Object callNamedFunctionWithPackage(String name, RArgsValuesAndNames args, String packageName) {
            DLL.RegisteredNativeSymbol rns = new DLL.RegisteredNativeSymbol(DLL.NativeSymbolType.Call, null, null);
            long func = DLL.findSymbol(name, packageName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.C_SYMBOL_NOT_IN_TABLE, name);
            }
            return RFFIFactory.getRFFI().getCallRFFI().invokeCall(func, name, args.getArguments());
        }

        @Fallback
        protected Object dotCallFallback(Object fobj, @SuppressWarnings("unused") Object args, @SuppressWarnings("unused") Object packageName) {
            throw fallback(fobj);
        }
    }

    @RBuiltin(name = ".External", kind = PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"}, behavior = COMPLEX)
    public abstract static class DotExternal extends LookupAdapter {

        private final BranchProfile errorProfile = BranchProfile.create();

        @Override
        @TruffleBoundary
        protected RExternalBuiltinNode lookupBuiltin(RList f) {
            String name = lookupName(f);
            if (FastROptions.UseInternalGraphics.getBooleanValue()) {
                switch (name) {
                    case "PDF":
                        return new DevicesCCalls.C_PDF();
                    case "devoff":
                        return new DevicesCCalls.C_DevOff();
                    case "devcur":
                        return new DevicesCCalls.C_DevCur();
                }
            }
            switch (name) {
                case "compcases":
                    return new CompleteCases();
                // utils
                case "countfields":
                    return new CountFields();
                case "readtablehead":
                    return new ReadTableHead();
                case "download":
                    return new Download();
                case "termsform":
                    return getExternalModelBuiltinNode("termsform");
                case "Rprof":
                    return RprofNodeGen.create();
                case "Rprofmem":
                    return RprofmemNodeGen.create();
                case "unzip":
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

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == f", "builtin != null"})
        protected Object doExternal(VirtualFrame frame, RList f, RArgsValuesAndNames args, RMissing packageName, //
                        @Cached("f") RList cached, //
                        @Cached("lookupBuiltin(f)") RExternalBuiltinNode builtin) {
            return builtin.call(frame, args);
        }

        @Specialization
        protected Object callNamedFunction(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") Object packageName) {
            String name = getNameFromSymbolInfo(frame, symbol);
            Object list = encodeArgumentPairList(args, name);
            return RFFIFactory.getRFFI().getCallRFFI().invokeCall(getAddressFromSymbolInfo(frame, symbol), name, new Object[]{list});
        }

        @Specialization
        protected Object callNamedFunction(String name, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName) {
            return callNamedFunctionWithPackage(name, args, null);
        }

        @Specialization
        protected Object callNamedFunctionWithPackage(String name, RArgsValuesAndNames args, String packageName) {
            DLL.RegisteredNativeSymbol rns = new DLL.RegisteredNativeSymbol(DLL.NativeSymbolType.External, null, null);
            long func = DLL.findSymbol(name, packageName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.C_SYMBOL_NOT_IN_TABLE, name);
            }
            Object list = encodeArgumentPairList(args, name);
            return RFFIFactory.getRFFI().getCallRFFI().invokeCall(func, name, new Object[]{list});
        }

        @Fallback
        protected Object fallback(Object f, @SuppressWarnings("unused") Object args, @SuppressWarnings("unused") Object packageName) {
            throw fallback(f);
        }
    }

    @RBuiltin(name = ".External2", visibility = CUSTOM, kind = PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"}, behavior = COMPLEX)
    public abstract static class DotExternal2 extends LookupAdapter {
        private static final Object CALL = "call";
        private static final Object OP = "op";
        private static final Object RHO = "rho";

        private final BranchProfile errorProfile = BranchProfile.create();

        @Override
        @TruffleBoundary
        protected RExternalBuiltinNode lookupBuiltin(RList f) {
            if (FastROptions.UseInternalGraphics.getBooleanValue()) {
                switch (lookupName(f)) {
                    case "C_par":
                        return new C_Par();
                }
            }
            String name = lookupName(f);
            switch (name) {
                // tools
                case "writetable":
                    return new WriteTable();
                case "typeconvert":
                    return TypeConvertNodeGen.create();
                case "C_parseRd":
                    return C_ParseRdNodeGen.create();
                case "modelmatrix":
                case "modelframe":
                    return getExternalModelBuiltinNode(name);
                default:
                    return null;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == f", "builtin != null"})
        protected Object doExternal(VirtualFrame frame, RList f, RArgsValuesAndNames args, RMissing packageName, //
                        @Cached("f") RList cached, //
                        @Cached("lookupBuiltin(f)") RExternalBuiltinNode builtin) {
            return builtin.call(frame, args);
        }

        @Specialization
        protected Object callNamedFunction(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") Object packageName) {
            String name = getNameFromSymbolInfo(frame, symbol);
            Object list = encodeArgumentPairList(args, name);
            // TODO: provide proper values for the CALL, OP and RHO parameters
            return RFFIFactory.getRFFI().getCallRFFI().invokeCall(getAddressFromSymbolInfo(frame, symbol), name, new Object[]{CALL, OP, list, RHO});
        }

        @Specialization
        protected Object callNamedFunction(String name, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName) {
            return callNamedFunctionWithPackage(name, args, null);
        }

        @Specialization
        protected Object callNamedFunctionWithPackage(String name, RArgsValuesAndNames args, String packageName) {
            DLL.RegisteredNativeSymbol rns = new DLL.RegisteredNativeSymbol(DLL.NativeSymbolType.External, null, null);
            long func = DLL.findSymbol(name, packageName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.C_SYMBOL_NOT_IN_TABLE, name);
            }
            Object list = encodeArgumentPairList(args, name);
            // TODO: provide proper values for the CALL, OP and RHO parameters
            return RFFIFactory.getRFFI().getCallRFFI().invokeCall(func, name, new Object[]{CALL, OP, list, RHO});
        }

        @Fallback
        protected Object fallback(Object f, @SuppressWarnings("unused") Object args, @SuppressWarnings("unused") Object packageName) {
            throw fallback(f);
        }
    }

    @RBuiltin(name = ".External.graphics", kind = PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"}, behavior = COMPLEX)
    public abstract static class DotExternalGraphics extends LookupAdapter {

        private final BranchProfile errorProfile = BranchProfile.create();

        @Override
        @TruffleBoundary
        protected RExternalBuiltinNode lookupBuiltin(RList f) {
            if (FastROptions.UseInternalGraphics.getBooleanValue()) {
                switch (lookupName(f)) {
                    case "C_mtext":
                        return new GraphicsCCalls.C_mtext();
                    case "C_plotXY":
                        return new C_PlotXY();
                }
            }
            return null;
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == f", "builtin != null"})
        protected Object doExternal(VirtualFrame frame, RList f, RArgsValuesAndNames args, RMissing packageName, //
                        @Cached("f") RList cached, //
                        @Cached("lookupBuiltin(f)") RExternalBuiltinNode builtin) {
            return builtin.call(frame, args);
        }

        @Specialization
        protected Object callNamedFunction(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") Object packageName) {
            String name = getNameFromSymbolInfo(frame, symbol);
            Object list = encodeArgumentPairList(args, name);
            return RFFIFactory.getRFFI().getCallRFFI().invokeCall(getAddressFromSymbolInfo(frame, symbol), name, new Object[]{list});
        }

        @Specialization
        protected Object callNamedFunction(String name, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName) {
            return callNamedFunctionWithPackage(name, args, null);
        }

        @Specialization
        protected Object callNamedFunctionWithPackage(String name, RArgsValuesAndNames args, String packageName) {
            DLL.RegisteredNativeSymbol rns = new DLL.RegisteredNativeSymbol(DLL.NativeSymbolType.External, null, null);
            long func = DLL.findSymbol(name, packageName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.C_SYMBOL_NOT_IN_TABLE, name);
            }
            Object list = encodeArgumentPairList(args, name);
            return RFFIFactory.getRFFI().getCallRFFI().invokeCall(func, name, new Object[]{list});
        }

        @Fallback
        protected Object fallback(Object f, @SuppressWarnings("unused") Object args, @SuppressWarnings("unused") Object packageName) {
            throw fallback(f);
        }
    }

    @RBuiltin(name = ".Call.graphics", kind = PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"}, behavior = COMPLEX)
    public abstract static class DotCallGraphics extends LookupAdapter {

        private final BranchProfile errorProfile = BranchProfile.create();

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, RArgsValuesAndNames.EMPTY, RMissing.instance};
        }

        @Override
        @TruffleBoundary
        protected RExternalBuiltinNode lookupBuiltin(RList f) {
            switch (lookupName(f)) {
                default:
                    return null;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == f", "builtin != null"})
        protected Object doExternal(VirtualFrame frame, RList f, RArgsValuesAndNames args, RMissing packageName, //
                        @Cached("f") RList cached, //
                        @Cached("lookupBuiltin(f)") RExternalBuiltinNode builtin) {
            return builtin.call(frame, args);
        }

        @Specialization
        protected Object callNamedFunction(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") Object packageName) {
            return RFFIFactory.getRFFI().getCallRFFI().invokeCall(getAddressFromSymbolInfo(frame, symbol), getNameFromSymbolInfo(frame, symbol), args.getArguments());
        }

        @Specialization
        protected Object callNamedFunction(String name, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName) {
            return callNamedFunctionWithPackage(name, args, null);
        }

        @Specialization
        @TruffleBoundary
        protected Object callNamedFunctionWithPackage(String name, RArgsValuesAndNames args, String packageName) {
            DLL.RegisteredNativeSymbol rns = new DLL.RegisteredNativeSymbol(DLL.NativeSymbolType.Call, null, null);
            long func = DLL.findSymbol(name, packageName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.C_SYMBOL_NOT_IN_TABLE, name);
            }
            return RFFIFactory.getRFFI().getCallRFFI().invokeCall(func, name, args.getArguments());
        }

        @Fallback
        protected Object dotCallFallback(Object fobj, @SuppressWarnings("unused") Object args, @SuppressWarnings("unused") Object packageName) {
            throw fallback(fobj);
        }
    }
}
