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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.library.graphics.GraphicsCCalls.C_Par;
import com.oracle.truffle.r.library.graphics.GraphicsCCalls.C_PlotXY;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_M_setPrimitiveMethodsNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_getClassFromCacheNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_initMethodDispatchNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_methodsPackageMetaNameNodeGen;
import com.oracle.truffle.r.library.methods.MethodsListDispatchFactory.R_set_method_dispatchNodeGen;
import com.oracle.truffle.r.library.stats.*;
import com.oracle.truffle.r.library.stats.GammaFunctionsFactory.QgammaNodeGen;
import com.oracle.truffle.r.library.stats.SplineFunctionsFactory.SplineCoefNodeGen;
import com.oracle.truffle.r.library.stats.SplineFunctionsFactory.SplineEvalNodeGen;
import com.oracle.truffle.r.library.tools.*;
import com.oracle.truffle.r.library.tools.ToolsTextFactory.CodeFilesAppendNodeGen;
import com.oracle.truffle.r.library.tools.ToolsTextFactory.DoTabExpandNodeGen;
import com.oracle.truffle.r.library.utils.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ffi.*;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolInfo;

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
            throw RError.nyi(getEncapsulatingSourceSection(), getRBuiltin().name() + " specialization failure: " + (name == null ? "<unknown>" : name));
        }
    }

    /**
     * For now, just some special case functions that are built in to the implementation.
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
        protected Object doExternal(RList f, RArgsValuesAndNames args, byte naok, byte dup, RMissing rPackage, RMissing encoding, @Cached("f") RList cached,
                        @Cached("lookupBuiltin(f)") RExternalBuiltinNode builtin) {
            controlVisibility();
            return builtin.call(args);
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
                default:
                    return null;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == f", "builtin != null"})
        protected Object doExternal(RList f, RArgsValuesAndNames args, RMissing packageName, @Cached("f") RList cached, @Cached("lookupBuiltin(f)") RExternalBuiltinNode builtin) {
            controlVisibility();
            return builtin.call(args);
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
                throw RError.error(getEncapsulatingSourceSection(), Message.C_SYMBOL_NOT_IN_TABLE, name);
            }
            try {
                return RFFIFactory.getRFFI().getCallRFFI().invokeCall(symbolInfo, args.getArguments());
            } catch (Throwable t) {
                errorProfile.enter();
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.NATIVE_CALL_FAILED, t.getMessage());
            }
        }

        @Fallback
        protected Object dotCallFallback(Object fobj, @SuppressWarnings("unused") Object args, @SuppressWarnings("unused") Object packageName) {
            throw fallback(fobj);
        }
    }

    @RBuiltin(name = ".External", kind = RBuiltinKind.PRIMITIVE, parameterNames = {".NAME", "...", "PACKAGE"})
    public abstract static class DotExternal extends LookupAdapter {

        @Override
        protected RExternalBuiltinNode lookupBuiltin(RList f) {
            switch (lookupName(f)) {
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
        protected Object doExternal(RList f, RArgsValuesAndNames args, RMissing packageName, @Cached("f") RList cached, @Cached("lookupBuiltin(f)") RExternalBuiltinNode builtin) {
            controlVisibility();
            return builtin.call(args);
        }

        @Fallback
        protected Object fallback(Object f, @SuppressWarnings("unused") Object args, @SuppressWarnings("unused") Object packageName) {
            throw fallback(f);
        }
    }

    @RBuiltin(name = ".External2", kind = RBuiltinKind.PRIMITIVE, parameterNames = {".NAME", "..."})
    public abstract static class DotExternal2 extends LookupAdapter {

        @Override
        protected RExternalBuiltinNode lookupBuiltin(RList f) {
            switch (lookupName(f)) {
                case "writetable":
                    return new WriteTable();
                case "typeconvert":
                    return TypeConvertNodeGen.create();
                case "C_par":
                    return new C_Par();
                case "C_parseRd":
                    return C_ParseRdNodeGen.create();
                default:
                    return null;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == f", "builtin != null"})
        protected Object doExternal(RList f, RArgsValuesAndNames args, @Cached("f") RList cached, @Cached("lookupBuiltin(f)") RExternalBuiltinNode builtin) {
            controlVisibility();
            return builtin.call(args);
        }

        @Fallback
        protected Object fallback(Object f, @SuppressWarnings("unused") Object args) {
            throw fallback(f);
        }
    }

    @RBuiltin(name = ".External.graphics", kind = RBuiltinKind.PRIMITIVE, parameterNames = {".NAME", "..."})
    public abstract static class DotExternalGraphics extends LookupAdapter {

        @Override
        protected RExternalBuiltinNode lookupBuiltin(RList f) {
            switch (lookupName(f)) {
                case "C_plotXY":
                    return new C_PlotXY();
                default:
                    return null;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == f", "builtin != null"})
        protected Object doExternal(RList f, RArgsValuesAndNames args, @Cached("f") RList cached, @Cached("lookupBuiltin(f)") RExternalBuiltinNode builtin) {
            controlVisibility();
            return builtin.call(args);
        }

        @Fallback
        protected Object fallback(Object f, @SuppressWarnings("unused") Object args) {
            throw fallback(f);
        }
    }
}
