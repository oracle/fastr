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

import com.oracle.truffle.api.CompilerDirectives;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.foreign.FortranAndCFunctionsFactory.FortranResultNamesSetterNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.foreign.LookupAdapter.ExtractNativeCallInfoNode;
import com.oracle.truffle.r.nodes.function.call.RExplicitCallNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.InvokeCNode;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

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

    /**
     * Interface to .Fortran native functions. Some functions have explicit implementations in
     * FastR, otherwise the .Fortran interface uses the machinery that implements the .C interface.
     */
    @RBuiltin(name = ".Fortran", kind = PRIMITIVE, parameterNames = {".NAME", "...", "NAOK", "DUP", "PACKAGE", "ENCODING"}, behavior = COMPLEX)
    public abstract static class Fortran extends CRFFIAdapter implements Lookup {
        // TODO: rename to DotFortran

        static {
            Casts.noCasts(Fortran.class);
        }

        @Child private FortranResultNamesSetter resNamesSetter = FortranResultNamesSetterNodeGen.create();

        @Child private RExplicitCallNode explicitCall;

        @Override
        @TruffleBoundary
        public RExternalBuiltinNode lookupBuiltin(RList symbol) {
            return null;
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == symbol", "builtin != null"})
        protected Object doFortran(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, byte naok, byte dup, Object rPackage, RMissing encoding, @Cached("symbol") RList cached,
                        @Cached("lookupBuiltin(symbol)") RExternalBuiltinNode builtin) {
            return resNamesSetter.execute(builtin.call(frame, args), args);
        }

        @Specialization(guards = "lookupBuiltin(symbol) == null")
        protected RList doFortran(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, byte naok, byte dup, @SuppressWarnings("unused") Object rPackage,
                        @SuppressWarnings("unused") RMissing encoding,
                        @Cached("new()") ExtractNativeCallInfoNode extractSymbolInfo,
                        @Cached("createBinaryProfile()") ConditionProfile registeredProfile) {
            NativeCallInfo nativeCallInfo = extractSymbolInfo.execute(symbol);
            if (registeredProfile.profile(isRegisteredRFunction(nativeCallInfo))) {
                if (explicitCall == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    explicitCall = insert(RExplicitCallNode.create());
                }
                RFunction function = (RFunction) nativeCallInfo.address.asTruffleObject();
                Object result = explicitCall.call(frame, function, args);
                return RDataFactory.createList(new Object[]{result});
            } else {
                return invokeCNode.dispatch(frame, nativeCallInfo, naok, dup, args);
            }
        }

        @Specialization
        protected RList doFortran(VirtualFrame frame, RAbstractStringVector symbol, RArgsValuesAndNames args, byte naok, byte dup, Object rPackage, @SuppressWarnings("unused") RMissing encoding,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode,
                        @Cached("createBinaryProfile()") ConditionProfile registeredProfile) {
            String libName = LookupAdapter.checkPackageArg(rPackage);
            DLL.RegisteredNativeSymbol rns = new DLL.RegisteredNativeSymbol(DLL.NativeSymbolType.Fortran, null, null);
            DLL.SymbolHandle func = findSymbolNode.execute(symbol.getDataAt(0), libName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                throw error(RError.Message.C_SYMBOL_NOT_IN_TABLE, symbol);
            }
            if (registeredProfile.profile(isRegisteredRFunction(func))) {
                if (explicitCall == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    explicitCall = insert(RExplicitCallNode.create());
                }
                RFunction function = (RFunction) func.asTruffleObject();
                Object result = explicitCall.call(frame, function, args);
                return RDataFactory.createList(new Object[]{result});
            } else {
                return invokeCNode.dispatch(frame, new NativeCallInfo(symbol.getDataAt(0), func, rns.getDllInfo()), naok, dup, args);
            }
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object fallback(Object symbol, Object args, Object naok, Object dup, Object rPackage, Object encoding) {
            throw LookupAdapter.fallback(this, symbol);
        }

        protected boolean isRegisteredRFunction(NativeCallInfo nativeCallInfo) {
            return isRegisteredRFunction(nativeCallInfo.address);
        }

        private static boolean isRegisteredRFunction(SymbolHandle handle) {
            return !handle.isLong() && handle.asTruffleObject() instanceof RFunction;
        }
    }

    public abstract static class FortranResultNamesSetter extends RBaseNode {

        public abstract Object execute(Object result, RArgsValuesAndNames argNames);

        @Specialization
        public Object handleArgNames(RAttributable result, RArgsValuesAndNames argValNames,
                        @Cached("create()") SetNamesAttributeNode namesSetter,
                        @Cached("create()") BranchProfile namesProfile) {
            ArgumentsSignature sig = argValNames.getSignature();
            if (sig.getNonNullCount() > 0) {
                namesProfile.enter();
                String[] argNames = sig.getNames();
                String[] names = new String[sig.getLength()];
                if (argNames == null) {
                    Arrays.fill(names, "");
                } else {
                    for (int i = 0; i < sig.getLength(); i++) {
                        String argName = argNames[i];
                        if (argName == null) {
                            names[i] = "";
                        } else {
                            names[i] = argName;
                        }
                    }
                }
                namesSetter.execute(result, RDataFactory.createStringVector(names, true));
            }

            return result;
        }

        @Fallback
        public Object handleOthers(Object result, @SuppressWarnings("unused") RArgsValuesAndNames argNames) {
            // do nothing
            return result;
        }
    }

    @RBuiltin(name = ".C", kind = PRIMITIVE, parameterNames = {".NAME", "...", "NAOK", "DUP", "PACKAGE", "ENCODING"}, behavior = COMPLEX)
    public abstract static class DotC extends CRFFIAdapter {

        static {
            Casts.noCasts(DotC.class);
        }

        @Child private RExplicitCallNode explicitCall;

        @Specialization
        protected RList c(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, byte naok, byte dup, @SuppressWarnings("unused") Object rPackage, @SuppressWarnings("unused") RMissing encoding,
                        @Cached("new()") ExtractNativeCallInfoNode extractSymbolInfo,
                        @Cached("createBinaryProfile()") ConditionProfile registeredProfile) {
            NativeCallInfo nativeCallInfo = extractSymbolInfo.execute(symbol);
            if (registeredProfile.profile(isRegisteredRFunction(nativeCallInfo))) {
                if (explicitCall == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    explicitCall = insert(RExplicitCallNode.create());
                }
                RFunction function = (RFunction) nativeCallInfo.address.asTruffleObject();
                Object result = explicitCall.call(frame, function, args);
                return RDataFactory.createList(new Object[]{result});
            } else {
                return invokeCNode.dispatch(frame, nativeCallInfo, naok, dup, args);
            }
        }

        @Specialization
        protected RList c(VirtualFrame frame, RAbstractStringVector symbol, RArgsValuesAndNames args, byte naok, byte dup, Object rPackage, @SuppressWarnings("unused") RMissing encoding,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode,
                        @Cached("createBinaryProfile()") ConditionProfile registeredProfile) {
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
            if (registeredProfile.profile(isRegisteredRFunction(func))) {
                if (explicitCall == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    explicitCall = insert(RExplicitCallNode.create());
                }

                RFunction function = (RFunction) func.asTruffleObject();
                Object result = explicitCall.call(frame, function, args);
                return RDataFactory.createList(new Object[]{result});
            } else {
                return invokeCNode.dispatch(frame, new NativeCallInfo(symbol.getDataAt(0), func, rns.getDllInfo()), naok, dup, args);
            }
        }

        protected boolean isRegisteredRFunction(NativeCallInfo nativeCallInfo) {
            return isRegisteredRFunction(nativeCallInfo.address);
        }

        private static boolean isRegisteredRFunction(SymbolHandle handle) {
            return !handle.isLong() && handle.asTruffleObject() instanceof RFunction;
        }
    }
}
