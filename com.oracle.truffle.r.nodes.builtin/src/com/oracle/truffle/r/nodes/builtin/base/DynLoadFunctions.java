/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RInvisibleBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLException;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.NativeSymbolType;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolInfo;

public class DynLoadFunctions {

    private static final String DLLINFOLIST_CLASS = "DLLInfoList";

    @RBuiltin(name = "dyn.load", visibility = RVisibility.OFF, kind = INTERNAL, parameterNames = {"lib", "local", "now", "unused"})
    public abstract static class DynLoad extends RInvisibleBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RList doDynLoad(RAbstractStringVector libVec, RAbstractLogicalVector localVec, byte now, @SuppressWarnings("unused") String unused) {
            controlVisibility();
            // Length checked by GnuR
            if (libVec.getLength() > 1) {
                throw RError.error(this, RError.Message.TYPE_EXPECTED, RType.Character.getName());
            }
            String lib = libVec.getDataAt(0);
            // Length not checked by GnuR
            byte local = localVec.getDataAt(0);
            try {
                DLLInfo dllInfo = DLL.loadPackageDLL(lib, asBoolean(local), asBoolean(now));
                return dllInfo.toRList();
            } catch (DLLException ex) {
                // This is not a recoverable error
                System.out.println("exception while loading " + lib + ":");
                ex.printStackTrace();
                throw RInternalError.shouldNotReachHere(ex);
            }
        }

        private static boolean asBoolean(byte b) {
            return b == RRuntime.LOGICAL_TRUE ? true : false;
        }
    }

    @RBuiltin(name = "dyn.unload", visibility = RVisibility.OFF, kind = INTERNAL, parameterNames = {"lib"})
    public abstract static class DynUnload extends RInvisibleBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RNull doDynunload(RAbstractStringVector lib) {
            controlVisibility();
            try {
                DLL.unload(lib.getDataAt(0));
            } catch (DLLException ex) {
                throw RError.error(this, ex);
            }
            return RNull.instance;
        }
    }

    @RBuiltin(name = "getLoadedDLLs", kind = INTERNAL, parameterNames = {})
    public abstract static class GetLoadedDLLs extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RList doGetLoadedDLLs() {
            controlVisibility();
            ArrayList<DLLInfo> dlls = DLL.getLoadedDLLs();
            String[] names = new String[dlls.size()];
            Object[] data = new Object[names.length];
            for (int i = 0; i < names.length; i++) {
                DLLInfo dllInfo = dlls.get(i);
                // name field is used a list element name
                names[i] = dllInfo.name;
                data[i] = dllInfo.toRList();
            }
            RList result = RDataFactory.createList(data, RDataFactory.createStringVector(names, RDataFactory.COMPLETE_VECTOR));
            result.setClassAttr(RDataFactory.createStringVectorFromScalar(DLLINFOLIST_CLASS));
            return result;
        }
    }

    @RBuiltin(name = "is.loaded", kind = INTERNAL, parameterNames = {"symbol", "package", "type"})
    public abstract static class IsLoaded extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected byte isLoaded(RAbstractStringVector symbol, RAbstractStringVector packageName, RAbstractStringVector typeVec) {
            controlVisibility();
            String type = typeVec.getDataAt(0);
            NativeSymbolType nst = null;
            switch (type) {
                case "":
                    break;
                case "Fortran":
                    nst = NativeSymbolType.Fortran;
                    break;
                case "Call":
                    nst = NativeSymbolType.Call;
                    break;
                case "External":
                    nst = NativeSymbolType.External;
                    break;
                default:
                    // Not an error in GnuR
            }
            DLL.RegisteredNativeSymbol rns = new DLL.RegisteredNativeSymbol(nst, null, null);
            boolean found = DLL.findSymbol(symbol.getDataAt(0), packageName.getDataAt(0), rns) != DLL.SYMBOL_NOT_FOUND;
            return RRuntime.asLogical(found);
        }
    }

    @RBuiltin(name = "getSymbolInfo", kind = INTERNAL, parameterNames = {"symbol", "package", "withReg"})
    public abstract static class GetSymbolInfo extends RBuiltinNode {

        @Specialization
        @TruffleBoundary
        protected Object getSymbolInfo(RAbstractStringVector symbolVec, String packageName, byte withReg) {
            controlVisibility();
            String symbol = symbolVec.getDataAt(0);
            DLL.RegisteredNativeSymbol rns = DLL.RegisteredNativeSymbol.any();
            long f = DLL.findSymbol(RRuntime.asString(symbol), packageName, rns);
            SymbolInfo symbolInfo = null;
            if (f != DLL.SYMBOL_NOT_FOUND) {
                symbolInfo = new SymbolInfo(rns.getDllInfo(), symbol, f);
            }
            return getResult(symbolInfo, withReg);
        }

        @Specialization(guards = "isDLLInfo(externalPtr)")
        @TruffleBoundary
        protected Object getSymbolInfo(RAbstractStringVector symbolVec, RExternalPtr externalPtr, byte withReg) {
            controlVisibility();
            DLL.DLLInfo dllInfo = DLL.getDLLInfoForId((int) externalPtr.getAddr());
            if (dllInfo == null) {
                throw RError.error(this, RError.Message.REQUIRES_NAME_DLLINFO);
            }

            DLL.RegisteredNativeSymbol rns = DLL.RegisteredNativeSymbol.any();
            String symbol = symbolVec.getDataAt(0);
            long f = DLL.dlsym(dllInfo, RRuntime.asString(symbol), rns);
            SymbolInfo symbolInfo = null;
            if (f != DLL.SYMBOL_NOT_FOUND) {
                symbolInfo = new SymbolInfo(dllInfo, symbol, f);
            }
            return getResult(symbolInfo, withReg);
        }

        private static Object getResult(DLL.SymbolInfo symbolInfo, byte withReg) {
            if (symbolInfo != null) {
                return symbolInfo.createRSymbolObject(new DLL.RegisteredNativeSymbol(DLL.NativeSymbolType.Any, null, null), RRuntime.fromLogical(withReg));
            } else {
                return RNull.instance;
            }
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object getSymbolInfo(Object symbol, Object packageName, Object withReg) {
            throw RError.error(this, RError.Message.REQUIRES_NAME_DLLINFO);
        }

        protected static boolean isDLLInfo(RExternalPtr externalPtr) {
            return DLL.isDLLInfo(externalPtr);
        }
    }
}
