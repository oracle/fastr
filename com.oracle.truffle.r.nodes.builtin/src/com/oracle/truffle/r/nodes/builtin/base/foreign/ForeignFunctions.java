/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base.foreign;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.r.library.grDevices.DevicesCCalls;
import com.oracle.truffle.r.library.graphics.GraphicsCCalls;
import com.oracle.truffle.r.library.graphics.GraphicsCCalls.C_Par;
import com.oracle.truffle.r.library.graphics.GraphicsCCalls.C_PlotXY;
import com.oracle.truffle.r.library.grid.GridFunctionsFactory.InitGridNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_M_setPrimitiveMethodsNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_getClassFromCacheNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_initMethodDispatchNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_methodsPackageMetaNameNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_set_method_dispatchNodeGen;
import com.oracle.truffle.r.library.stats.Covcor;
import com.oracle.truffle.r.library.stats.GammaFunctionsFactory.QgammaNodeGen;
import com.oracle.truffle.r.library.stats.RnormNodeGen;
import com.oracle.truffle.r.library.stats.RunifNodeGen;
import com.oracle.truffle.r.library.stats.SplineFunctionsFactory.SplineCoefNodeGen;
import com.oracle.truffle.r.library.stats.SplineFunctionsFactory.SplineEvalNodeGen;
import com.oracle.truffle.r.library.tools.C_ParseRdNodeGen;
import com.oracle.truffle.r.library.tools.DirChmodNodeGen;
import com.oracle.truffle.r.library.tools.Rmd5NodeGen;
import com.oracle.truffle.r.library.tools.ToolsTextFactory.CodeFilesAppendNodeGen;
import com.oracle.truffle.r.library.tools.ToolsTextFactory.DoTabExpandNodeGen;
import com.oracle.truffle.r.library.utils.CountFields;
import com.oracle.truffle.r.library.utils.Crc64NodeGen;
import com.oracle.truffle.r.library.utils.Download;
import com.oracle.truffle.r.library.utils.MenuNodeGen;
import com.oracle.truffle.r.library.utils.TypeConvertNodeGen;
import com.oracle.truffle.r.library.utils.WriteTable;
import com.oracle.truffle.r.nodes.access.AccessFieldNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolInfo;
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
            list = RDataFactory.createPairList(args.getArgument(i), list, name == null ? RNull.instance : RDataFactory.createSymbol(name));
        }
        list = RDataFactory.createPairList(symbolName, list);
        return list;
    }

    @TruffleBoundary
    protected static String getNameFromSymbolInfo(RList symbol) {
        return RRuntime.asString(symbol.getDataAt(AccessFieldNode.getElementIndexByName(symbol.getNames(), "name")));
    }

    @TruffleBoundary
    protected static long getAddressFromSymbolInfo(RList symbol) {
        return ((RExternalPtr) symbol.getDataAt(AccessFieldNode.getElementIndexByName(symbol.getNames(), "address"))).getAddr();
    }

    protected abstract static class LookupAdapter extends RBuiltinNode {

        protected abstract RExternalBuiltinNode lookupBuiltin(RList f);

        private static final String UNKNOWN_EXTERNAL_BUILTIN = "UNKNOWN_EXTERNAL_BUILTIN";

        protected String lookupName(RList f) {
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
    }

    /**
     * Interface to .Fortran native functions. Some functions have explicit implementations in
     * FastR, otherwise the .Fortran interface uses the machinery that implements the .C interface.
     */
    @RBuiltin(name = ".Fortran", kind = RBuiltinKind.PRIMITIVE, parameterNames = {".NAME", "...", "NAOK", "DUP", "PACKAGE", "ENCODING"})
    public abstract static class Fortran extends LookupAdapter {

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, RArgsValuesAndNames.EMPTY, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_FALSE, RMissing.instance, RMissing.instance};
        }

        @Override
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
        protected Object doExternal(RList f, RArgsValuesAndNames args, byte naok, byte dup, RMissing rPackage, RMissing encoding, //
                        @Cached("f") RList cached, //
                        @Cached("lookupBuiltin(f)") RExternalBuiltinNode builtin) {
            controlVisibility();
            return builtin.call(args);
        }

        @Specialization(guards = "lookupBuiltin(symbol) == null")
        protected RList c(RList symbol, RArgsValuesAndNames args, byte naok, byte dup, @SuppressWarnings("unused") RMissing rPackage, @SuppressWarnings("unused") RMissing encoding) {
            controlVisibility();
            return DotC.dispatch(this, getAddressFromSymbolInfo(symbol), getNameFromSymbolInfo(symbol), naok, dup, args.getArguments());
        }

        @Specialization
        protected RList c(String f, RArgsValuesAndNames args, byte naok, byte dup, @SuppressWarnings("unused") RMissing rPackage, @SuppressWarnings("unused") RMissing encoding, //
                        @Cached("create()") BranchProfile errorProfile) {
            controlVisibility();
            SymbolInfo symbolInfo = DLL.findRegisteredSymbolinInDLL(f, null, "");
            if (symbolInfo == null) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.C_SYMBOL_NOT_IN_TABLE, f);
            }
            return DotC.dispatch(this, symbolInfo.address, symbolInfo.symbol, naok, dup, args.getArguments());
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
    @RBuiltin(name = ".Call", kind = RBuiltinKind.PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"})
    public abstract static class DotCall extends LookupAdapter {

        private final BranchProfile errorProfile = BranchProfile.create();

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, RArgsValuesAndNames.EMPTY, RMissing.instance};
        }

        @Override
        protected RExternalBuiltinNode lookupBuiltin(RList f) {
            switch (lookupName(f)) {
                case "fft":
                    return new Fft();
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
                case "crc64":
                    return Crc64NodeGen.create();
                case "cov":
                    return new Covcor(false);
                case "cor":
                    return new Covcor(true);
                case "SplineCoef":
                    return SplineCoefNodeGen.create();
                case "SplineEval":
                    return SplineEvalNodeGen.create();
                case "doTabExpand":
                    return DoTabExpandNodeGen.create();
                case "codeFilesAppend":
                    return CodeFilesAppendNodeGen.create();
                case "Rmd5":
                    return Rmd5NodeGen.create();
                case "flushconsole":
                    return new Flushconsole();
                case "dirchmod":
                    return DirChmodNodeGen.create();
                case "cairoProps":
                    return CairoPropsNodeGen.create();
                case "makeQuartzDefault":
                    return new MakeQuartzDefault();
                case "menu":
                    return MenuNodeGen.create();
                case "L_initGrid":
                    return InitGridNodeGen.create();
                default:
                    return null;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == f", "builtin != null"})
        protected Object doExternal(RList f, RArgsValuesAndNames args, RMissing packageName, //
                        @Cached("f") RList cached, //
                        @Cached("lookupBuiltin(f)") RExternalBuiltinNode builtin) {
            controlVisibility();
            return builtin.call(args);
        }

        @Specialization
        public Object callNamedFunction(RList symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") Object packageName) {
            controlVisibility();
            return RFFIFactory.getRFFI().getCallRFFI().invokeCall(getAddressFromSymbolInfo(symbol), getNameFromSymbolInfo(symbol), args.getArguments());
        }

        @Specialization
        public Object callNamedFunction(String name, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName) {
            return callNamedFunctionWithPackage(name, args, null);
        }

        @Specialization
        public Object callNamedFunctionWithPackage(String name, RArgsValuesAndNames args, String packageName) {
            controlVisibility();
            SymbolInfo symbolInfo = DLL.findSymbolInfo(name, packageName);
            if (symbolInfo == null) {
                errorProfile.enter();
                throw RError.error(this, Message.C_SYMBOL_NOT_IN_TABLE, name);
            }
            return RFFIFactory.getRFFI().getCallRFFI().invokeCall(symbolInfo.address, symbolInfo.symbol, args.getArguments());
        }

        @Fallback
        protected Object dotCallFallback(Object fobj, @SuppressWarnings("unused") Object args, @SuppressWarnings("unused") Object packageName) {
            throw fallback(fobj);
        }
    }

    @RBuiltin(name = ".External", kind = RBuiltinKind.PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"})
    public abstract static class DotExternal extends LookupAdapter {

        private final BranchProfile errorProfile = BranchProfile.create();

        @Override
        protected RExternalBuiltinNode lookupBuiltin(RList f) {
            String name = lookupName(f);
            if (FastROptions.UseInternalGraphics) {
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
                case "countfields":
                    return new CountFields();
                case "readtablehead":
                    return new ReadTableHead();
                case "rnorm":
                    return RnormNodeGen.create();
                case "runif":
                    return RunifNodeGen.create();
                case "qgamma":
                    return QgammaNodeGen.create();
                case "download":
                    return new Download();
                default:
                    return null;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == f", "builtin != null"})
        protected Object doExternal(RList f, RArgsValuesAndNames args, RMissing packageName, //
                        @Cached("f") RList cached, //
                        @Cached("lookupBuiltin(f)") RExternalBuiltinNode builtin) {
            controlVisibility();
            return builtin.call(args);
        }

        @Specialization
        public Object callNamedFunction(RList symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") Object packageName) {
            String name = getNameFromSymbolInfo(symbol);
            Object list = encodeArgumentPairList(args, name);
            return RFFIFactory.getRFFI().getCallRFFI().invokeCall(getAddressFromSymbolInfo(symbol), name, new Object[]{list});
        }

        @Specialization
        public Object callNamedFunction(String name, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName) {
            return callNamedFunctionWithPackage(name, args, null);
        }

        @Specialization
        public Object callNamedFunctionWithPackage(String name, RArgsValuesAndNames args, String packageName) {
            controlVisibility();
            SymbolInfo symbolInfo = DLL.findSymbolInfo(name, packageName);
            if (symbolInfo == null) {
                errorProfile.enter();
                throw RError.error(this, Message.C_SYMBOL_NOT_IN_TABLE, name);
            }
            Object list = encodeArgumentPairList(args, symbolInfo.symbol);
            return RFFIFactory.getRFFI().getCallRFFI().invokeCall(symbolInfo.address, symbolInfo.symbol, new Object[]{list});
        }

        @Fallback
        protected Object fallback(Object f, @SuppressWarnings("unused") Object args, @SuppressWarnings("unused") Object packageName) {
            throw fallback(f);
        }
    }

    @RBuiltin(name = ".External2", kind = RBuiltinKind.PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"})
    public abstract static class DotExternal2 extends LookupAdapter {
        private static final Object CALL = "call";
        private static final Object OP = "op";
        private static final Object RHO = "rho";

        private final BranchProfile errorProfile = BranchProfile.create();

        @Override
        protected RExternalBuiltinNode lookupBuiltin(RList f) {
            if (FastROptions.UseInternalGraphics) {
                switch (lookupName(f)) {
                    case "C_par":
                        return new C_Par();
                }
            }
            switch (lookupName(f)) {
                case "writetable":
                    return new WriteTable();
                case "typeconvert":
                    return TypeConvertNodeGen.create();
                case "C_parseRd":
                    return C_ParseRdNodeGen.create();
                default:
                    return null;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == f", "builtin != null"})
        protected Object doExternal(RList f, RArgsValuesAndNames args, RMissing packageName, //
                        @Cached("f") RList cached, //
                        @Cached("lookupBuiltin(f)") RExternalBuiltinNode builtin) {
            controlVisibility();
            return builtin.call(args);
        }

        @Specialization
        public Object callNamedFunction(RList symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") Object packageName) {
            String name = getNameFromSymbolInfo(symbol);
            Object list = encodeArgumentPairList(args, name);
            // TODO: provide proper values for the CALL, OP and RHO parameters
            return RFFIFactory.getRFFI().getCallRFFI().invokeCall(getAddressFromSymbolInfo(symbol), name, new Object[]{CALL, OP, list, RHO});
        }

        @Specialization
        public Object callNamedFunction(String name, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName) {
            return callNamedFunctionWithPackage(name, args, null);
        }

        @Specialization
        public Object callNamedFunctionWithPackage(String name, RArgsValuesAndNames args, String packageName) {
            controlVisibility();
            SymbolInfo symbolInfo = DLL.findSymbolInfo(name, packageName);
            if (symbolInfo == null) {
                errorProfile.enter();
                throw RError.error(this, Message.C_SYMBOL_NOT_IN_TABLE, name);
            }
            Object list = encodeArgumentPairList(args, symbolInfo.symbol);
            // TODO: provide proper values for the CALL, OP and RHO parameters
            return RFFIFactory.getRFFI().getCallRFFI().invokeCall(symbolInfo.address, symbolInfo.symbol, new Object[]{CALL, OP, list, RHO});
        }

        @Fallback
        protected Object fallback(Object f, @SuppressWarnings("unused") Object args, @SuppressWarnings("unused") Object packageName) {
            throw fallback(f);
        }
    }

    @RBuiltin(name = ".External.graphics", kind = RBuiltinKind.PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"})
    public abstract static class DotExternalGraphics extends LookupAdapter {

        private final BranchProfile errorProfile = BranchProfile.create();

        @Override
        protected RExternalBuiltinNode lookupBuiltin(RList f) {
            if (FastROptions.UseInternalGraphics) {
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
        protected Object doExternal(RList f, RArgsValuesAndNames args, RMissing packageName, //
                        @Cached("f") RList cached, //
                        @Cached("lookupBuiltin(f)") RExternalBuiltinNode builtin) {
            controlVisibility();
            return builtin.call(args);
        }

        @Specialization
        public Object callNamedFunction(RList symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") Object packageName) {
            String name = getNameFromSymbolInfo(symbol);
            Object list = encodeArgumentPairList(args, name);
            return RFFIFactory.getRFFI().getCallRFFI().invokeCall(getAddressFromSymbolInfo(symbol), name, new Object[]{list});
        }

        @Specialization
        public Object callNamedFunction(String name, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName) {
            return callNamedFunctionWithPackage(name, args, null);
        }

        @Specialization
        public Object callNamedFunctionWithPackage(String name, RArgsValuesAndNames args, String packageName) {
            controlVisibility();
            SymbolInfo symbolInfo = DLL.findSymbolInfo(name, packageName);
            if (symbolInfo == null) {
                errorProfile.enter();
                throw RError.error(this, Message.C_SYMBOL_NOT_IN_TABLE, name);
            }
            Object list = encodeArgumentPairList(args, symbolInfo.symbol);
            return RFFIFactory.getRFFI().getCallRFFI().invokeCall(symbolInfo.address, symbolInfo.symbol, new Object[]{list});
        }

        @Fallback
        protected Object fallback(Object f, @SuppressWarnings("unused") Object args, @SuppressWarnings("unused") Object packageName) {
            throw fallback(f);
        }
    }

    @RBuiltin(name = ".Call.graphics", kind = RBuiltinKind.PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"})
    public abstract static class DotCallGraphics extends LookupAdapter {

        private final BranchProfile errorProfile = BranchProfile.create();

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, RArgsValuesAndNames.EMPTY, RMissing.instance};
        }

        @Override
        protected RExternalBuiltinNode lookupBuiltin(RList f) {
            switch (lookupName(f)) {
                default:
                    return null;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == f", "builtin != null"})
        protected Object doExternal(RList f, RArgsValuesAndNames args, RMissing packageName, //
                        @Cached("f") RList cached, //
                        @Cached("lookupBuiltin(f)") RExternalBuiltinNode builtin) {
            controlVisibility();
            return builtin.call(args);
        }

        @Specialization
        public Object callNamedFunction(RList symbol, RArgsValuesAndNames args, @SuppressWarnings("unused") Object packageName) {
            return RFFIFactory.getRFFI().getCallRFFI().invokeCall(getAddressFromSymbolInfo(symbol), getNameFromSymbolInfo(symbol), args.getArguments());
        }

        @Specialization
        public Object callNamedFunction(String name, RArgsValuesAndNames args, @SuppressWarnings("unused") RMissing packageName) {
            return callNamedFunctionWithPackage(name, args, null);
        }

        @Specialization
        public Object callNamedFunctionWithPackage(String name, RArgsValuesAndNames args, String packageName) {
            controlVisibility();
            SymbolInfo symbolInfo = DLL.findSymbolInfo(name, packageName);
            if (symbolInfo == null) {
                errorProfile.enter();
                throw RError.error(this, Message.C_SYMBOL_NOT_IN_TABLE, name);
            }
            return RFFIFactory.getRFFI().getCallRFFI().invokeCall(symbolInfo.address, symbolInfo.symbol, args.getArguments());
        }

        @Fallback
        protected Object dotCallFallback(Object fobj, @SuppressWarnings("unused") Object args, @SuppressWarnings("unused") Object packageName) {
            throw fallback(fobj);
        }
    }
}
