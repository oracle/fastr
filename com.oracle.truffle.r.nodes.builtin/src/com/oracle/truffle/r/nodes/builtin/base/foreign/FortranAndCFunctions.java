/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base.foreign;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.foreign.FortranAndCFunctionsFactory.FortranResultNamesSetterNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.foreign.LookupAdapter.ExtractNativeCallInfoNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ffi.CRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
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

        @Child protected CRFFI.InvokeCNode invokeCNode = RFFIFactory.getCRFFI().createInvokeCNode();

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

        static {
            Casts.noCasts(Fortran.class);
        }

        @Child private FortranResultNamesSetter resNamesSetter = FortranResultNamesSetterNodeGen.create();

        @Override
        @TruffleBoundary
        public RExternalBuiltinNode lookupBuiltin(RList symbol) {
            return null;
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "1", guards = {"cached == symbol", "builtin != null"})
        protected Object doExternal(VirtualFrame frame, RList symbol, RArgsValuesAndNames args, byte naok, byte dup, Object rPackage, RMissing encoding, @Cached("symbol") RList cached,
                        @Cached("lookupBuiltin(symbol)") RExternalBuiltinNode builtin) {
            return resNamesSetter.execute(builtin.call(frame, args), args);
        }

        @Specialization(guards = "lookupBuiltin(symbol) == null")
        protected RList c(RList symbol, RArgsValuesAndNames args, byte naok, byte dup, @SuppressWarnings("unused") Object rPackage, @SuppressWarnings("unused") RMissing encoding,
                        @Cached("new()") ExtractNativeCallInfoNode extractSymbolInfo) {
            NativeCallInfo nativeCallInfo = extractSymbolInfo.execute(symbol);
            return invokeCNode.dispatch(nativeCallInfo, naok, dup, args);
        }

        @Specialization
        protected RList c(RAbstractStringVector symbol, RArgsValuesAndNames args, byte naok, byte dup, Object rPackage, @SuppressWarnings("unused") RMissing encoding,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode) {
            String libName = LookupAdapter.checkPackageArg(rPackage);
            DLL.RegisteredNativeSymbol rns = new DLL.RegisteredNativeSymbol(DLL.NativeSymbolType.Fortran, null, null);
            DLL.SymbolHandle func = findSymbolNode.execute(symbol.getDataAt(0), libName, rns);
            if (func == DLL.SYMBOL_NOT_FOUND) {
                throw error(RError.Message.C_SYMBOL_NOT_IN_TABLE, symbol);
            }
            return invokeCNode.dispatch(new NativeCallInfo(symbol.getDataAt(0), func, rns.getDllInfo()), naok, dup, args);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object fallback(Object symbol, Object args, Object naok, Object dup, Object rPackage, Object encoding) {
            throw LookupAdapter.fallback(this, symbol);
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

        @Specialization
        protected RList c(RList symbol, RArgsValuesAndNames args, byte naok, byte dup, @SuppressWarnings("unused") Object rPackage, @SuppressWarnings("unused") RMissing encoding,
                        @Cached("new()") ExtractNativeCallInfoNode extractSymbolInfo) {
            NativeCallInfo nativeCallInfo = extractSymbolInfo.execute(symbol);
            return invokeCNode.dispatch(nativeCallInfo, naok, dup, args);
        }

        @Specialization
        protected RList c(RAbstractStringVector symbol, RArgsValuesAndNames args, byte naok, byte dup, Object rPackage, @SuppressWarnings("unused") RMissing encoding,
                        @Cached("create()") DLL.RFindSymbolNode findSymbolNode) {
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
            return invokeCNode.dispatch(new NativeCallInfo(symbol.getDataAt(0), func, rns.getDllInfo()), naok, dup, args);
        }
    }
}
