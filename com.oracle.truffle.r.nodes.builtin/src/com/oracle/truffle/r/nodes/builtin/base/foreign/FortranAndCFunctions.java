/*
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates
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

import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.foreign.LookupAdapter.ExtractNativeCallInfoNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.InvokeCNode;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

/**
 * {@code .C} and {@code .Fortran} functions, which share a common signature.
 *
 * TODO Completeness (more types, more error checks), Performance (copying). Especially all the
 * subtleties around copying.
 *
 * See <a href="https://stat.ethz.ch/R-manual/R-devel/library/base/html/Foreign.html">here</a>.
 */
public class FortranAndCFunctions {

    protected abstract static class CRFFIAdapter extends RBuiltinNode.Arg6 {

        @Child protected InvokeCNode invokeCNode = RFFIFactory.getCRFFI().createInvokeCNode();

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, RArgsValuesAndNames.EMPTY, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_FALSE, RMissing.instance, RMissing.instance};
        }
    }

    @RBuiltin(name = ".Fortran", kind = PRIMITIVE, parameterNames = {".NAME", "...", "NAOK", "DUP", "PACKAGE", "ENCODING"}, behavior = COMPLEX)
    public abstract static class DotFortran extends CRFFIAdapter {
        static {
            Casts.noCasts(DotFortran.class);
        }

        @Specialization
        protected RList doFortranList(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, byte naok, byte dup, @SuppressWarnings("unused") Object rPackage,
                        @SuppressWarnings("unused") RMissing encoding,
                        @Cached ExtractNativeCallInfoNode extractSymbolInfo,
                        @Cached CallRegisteredROverride callRegisteredROverride,
                        @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef) {
            NativeCallInfo nativeCallInfo = extractSymbolInfo.execute(symbol);
            if (callRegisteredROverride.isRegisteredRFunction(nativeCallInfo)) {
                Object result = callRegisteredROverride.execute(frame, nativeCallInfo, args);
                return RDataFactory.createList(new Object[]{result});
            } else {
                return invokeCNode.dispatch(frame, nativeCallInfo, naok, dup, args, ctxRef.get());
            }
        }

        @Specialization
        protected RList doFortranName(VirtualFrame frame, RAbstractStringVector symbol, RArgsValuesAndNames args, byte naok, byte dup, Object rPackage, @SuppressWarnings("unused") RMissing encoding,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode,
                        @Cached CallRegisteredROverride callRegisteredROverride,
                        @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef) {
            String libName = LookupAdapter.checkPackageArg(rPackage);
            DLL.RegisteredNativeSymbol rns = new DLL.RegisteredNativeSymbol(DLL.NativeSymbolType.Fortran, null, null);
            DLL.SymbolHandle func = findSymbolNode.execute(symbol.getDataAt(0), libName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                throw error(RError.Message.C_SYMBOL_NOT_IN_TABLE, symbol);
            }
            if (callRegisteredROverride.isRegisteredRFunction(func)) {
                Object result = callRegisteredROverride.execute(frame, func, args);
                return RDataFactory.createList(new Object[]{result});
            } else {
                return invokeCNode.dispatch(frame, new NativeCallInfo(symbol.getDataAt(0), func, rns.getDllInfo()), naok, dup, args, ctxRef.get());
            }
        }

        @Specialization
        protected RList fortranPtr(VirtualFrame frame, RExternalPtr ptr, RArgsValuesAndNames args, byte naok, byte dup, @SuppressWarnings("unused") Object rPackage,
                        @SuppressWarnings("unused") RMissing encoding,
                        @Cached CallRegisteredROverride callRegisteredROverride,
                        @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef) {
            if (callRegisteredROverride.isRegisteredRFunction(ptr)) {
                Object result = callRegisteredROverride.execute(frame, ptr, args);
                return RDataFactory.createList(new Object[]{result});
            } else {
                return invokeCNode.dispatch(frame, new NativeCallInfo("", ptr.getAddr(), null), naok, dup, args, ctxRef.get());
            }
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object fallback(Object symbol, Object args, Object naok, Object dup, Object rPackage, Object encoding) {
            throw LookupAdapter.fallback(this, symbol);
        }
    }

    @RBuiltin(name = ".C", kind = PRIMITIVE, parameterNames = {".NAME", "...", "NAOK", "DUP", "PACKAGE", "ENCODING"}, behavior = COMPLEX)
    public abstract static class DotC extends CRFFIAdapter {
        static {
            Casts.noCasts(DotC.class);
        }

        @Specialization
        protected RList cList(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, byte naok, byte dup, @SuppressWarnings("unused") Object rPackage,
                        @SuppressWarnings("unused") RMissing encoding,
                        @Cached ExtractNativeCallInfoNode extractSymbolInfo,
                        @Cached CallRegisteredROverride callRegisteredROverride,
                        @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef) {
            NativeCallInfo nativeCallInfo = extractSymbolInfo.execute(symbol);
            if (callRegisteredROverride.isRegisteredRFunction(nativeCallInfo)) {
                Object result = callRegisteredROverride.execute(frame, nativeCallInfo, args);
                return RDataFactory.createList(new Object[]{result});
            } else {
                return invokeCNode.dispatch(frame, nativeCallInfo, naok, dup, args, ctxRef.get());
            }
        }

        @Specialization
        protected RList cName(VirtualFrame frame, RAbstractStringVector symbol, RArgsValuesAndNames args, byte naok, byte dup, Object rPackage, @SuppressWarnings("unused") RMissing encoding,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode,
                        @Cached CallRegisteredROverride callRegisteredROverride,
                        @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef) {
            String libName = null;
            if (!(rPackage instanceof RMissing)) {
                libName = RRuntime.asString(rPackage);
                if (libName == null) {
                    throw error(RError.Message.ARGUMENT_MUST_BE_STRING, "PACKAGE");
                }
            }
            DLL.RegisteredNativeSymbol rns = new DLL.RegisteredNativeSymbol(DLL.NativeSymbolType.C, null, null);
            DLL.SymbolHandle func = findSymbolNode.execute(symbol.getDataAt(0), libName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                throw error(RError.Message.C_SYMBOL_NOT_IN_TABLE, symbol);
            }
            if (callRegisteredROverride.isRegisteredRFunction(func)) {
                Object result = callRegisteredROverride.execute(frame, func, args);
                return RDataFactory.createList(new Object[]{result});
            } else {
                return invokeCNode.dispatch(frame, new NativeCallInfo(symbol.getDataAt(0), func, rns.getDllInfo()), naok, dup, args, ctxRef.get());
            }
        }

        @Specialization
        protected RList cPtr(VirtualFrame frame, RExternalPtr ptr, RArgsValuesAndNames args, byte naok, byte dup, @SuppressWarnings("unused") Object rPackage,
                        @SuppressWarnings("unused") RMissing encoding,
                        @Cached CallRegisteredROverride callRegisteredROverride,
                        @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef) {
            if (callRegisteredROverride.isRegisteredRFunction(ptr)) {
                Object result = callRegisteredROverride.execute(frame, ptr, args);
                return RDataFactory.createList(new Object[]{result});
            } else {
                return invokeCNode.dispatch(frame, new NativeCallInfo("", ptr.getAddr(), null), naok, dup, args, ctxRef.get());
            }
        }
    }
}
