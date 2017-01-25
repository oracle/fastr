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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.READS_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetClassAttributeNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLException;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.NativeSymbolType;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolInfo;

public class DynLoadFunctions {

    private static final String DLLINFOLIST_CLASS = "DLLInfoList";

    @RBuiltin(name = "dyn.load", visibility = OFF, kind = INTERNAL, parameterNames = {"lib", "local", "now", "unused"}, behavior = COMPLEX)
    public abstract static class DynLoad extends RBuiltinNode {
        @Child private DLL.LoadPackageDLLNode loadPackageDLLNode = DLL.LoadPackageDLLNode.create();

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("lib").mustBe(stringValue()).asStringVector().mustBe(size(1), RError.Message.CHAR_ARGUMENT).findFirst();
            casts.arg("local").asLogicalVector().findFirst().map(toBoolean());
            casts.arg("now").asLogicalVector().findFirst().map(toBoolean());
            casts.arg("unused").mustBe(stringValue()).asStringVector().findFirst();
        }

        @Specialization
        @TruffleBoundary
        protected RList doDynLoad(String lib, boolean local, boolean now, @SuppressWarnings("unused") String unused) {
            try {
                DLLInfo dllInfo = loadPackageDLLNode.execute(lib, local, now);
                return dllInfo.toRList();
            } catch (DLLException ex) {
                // This is not a recoverable error
                System.out.println("exception while loading " + lib + ":");
                ex.printStackTrace();
                throw RInternalError.shouldNotReachHere(ex);
            }
        }
    }

    @RBuiltin(name = "dyn.unload", visibility = OFF, kind = INTERNAL, parameterNames = {"lib"}, behavior = COMPLEX)
    public abstract static class DynUnload extends RBuiltinNode {
        @Child DLL.UnloadNode dllUnloadNode = DLL.UnloadNode.create();

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("lib").mustBe(stringValue()).asStringVector().mustBe(size(1), RError.Message.CHAR_ARGUMENT).findFirst();
        }

        @Specialization
        @TruffleBoundary
        protected RNull doDynunload(RAbstractStringVector lib) {
            try {
                dllUnloadNode.execute(lib.getDataAt(0));
            } catch (DLLException ex) {
                throw RError.error(this, ex);
            }
            return RNull.instance;
        }
    }

    @RBuiltin(name = "getLoadedDLLs", kind = INTERNAL, parameterNames = {}, behavior = READS_STATE)
    public abstract static class GetLoadedDLLs extends RBuiltinNode {

        @Child private SetClassAttributeNode setClassAttrNode = SetClassAttributeNode.create();

        @Specialization
        @TruffleBoundary
        protected RList doGetLoadedDLLs() {
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
            setClassAttrNode.execute(result, RDataFactory.createStringVectorFromScalar(DLLINFOLIST_CLASS));
            return result;
        }
    }

    @RBuiltin(name = "is.loaded", kind = INTERNAL, parameterNames = {"symbol", "PACKAGE", "type"}, behavior = READS_STATE)
    public abstract static class IsLoaded extends RBuiltinNode {
        @Child DLL.FindSymbolNode findSymbolNode = DLL.FindSymbolNode.create();

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("symbol").mustBe(stringValue()).asStringVector().mustBe(notEmpty()).findFirst();
            casts.arg("PACKAGE").mustBe(stringValue()).asStringVector().mustBe(notEmpty()).findFirst();
            casts.arg("type").mustBe(stringValue()).asStringVector().mustBe(notEmpty()).findFirst();
        }

        @Specialization
        @TruffleBoundary
        protected byte isLoaded(String symbol, String packageName, String type) {
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
            boolean found = findSymbolNode.execute(symbol, packageName, rns) != DLL.SYMBOL_NOT_FOUND;
            return RRuntime.asLogical(found);
        }
    }

    @RBuiltin(name = "getSymbolInfo", kind = INTERNAL, parameterNames = {"symbol", "package", "withRegistrationInfo"}, behavior = READS_STATE)
    public abstract static class GetSymbolInfo extends RBuiltinNode {
        @Child DLL.FindSymbolNode findSymbolNode = DLL.FindSymbolNode.create();

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("symbol").mustBe(stringValue()).asStringVector().mustBe(notEmpty()).findFirst();
            casts.arg("withRegistrationInfo").mustBe(logicalValue()).asLogicalVector().findFirst().map(toBoolean());
        }

        @Specialization
        @TruffleBoundary
        protected Object getSymbolInfo(String symbol, String packageName, boolean withReg) {
            DLL.RegisteredNativeSymbol rns = DLL.RegisteredNativeSymbol.any();
            DLL.SymbolHandle f = findSymbolNode.execute(RRuntime.asString(symbol), packageName, rns);
            SymbolInfo symbolInfo = null;
            if (f != DLL.SYMBOL_NOT_FOUND) {
                symbolInfo = new SymbolInfo(rns.getDllInfo(), symbol, f);
            }
            return getResult(symbolInfo, withReg);
        }

        @Specialization(guards = "isDLLInfo(externalPtr)")
        @TruffleBoundary
        protected Object getSymbolInfo(RAbstractStringVector symbolVec, RExternalPtr externalPtr, boolean withReg, //
                        @Cached("create()") DLL.DlsymNode dlsymNode) {
            DLL.DLLInfo dllInfo = (DLLInfo) externalPtr.getExternalObject();
            if (dllInfo == null) {
                throw RError.error(this, RError.Message.REQUIRES_NAME_DLLINFO);
            }

            DLL.RegisteredNativeSymbol rns = DLL.RegisteredNativeSymbol.any();
            String symbol = symbolVec.getDataAt(0);
            DLL.SymbolHandle f = dlsymNode.execute(dllInfo, RRuntime.asString(symbol), rns);
            SymbolInfo symbolInfo = null;
            if (f != DLL.SYMBOL_NOT_FOUND) {
                symbolInfo = new SymbolInfo(dllInfo, symbol, f);
            }
            return getResult(symbolInfo, withReg);
        }

        private static Object getResult(DLL.SymbolInfo symbolInfo, boolean withReg) {
            if (symbolInfo != null) {
                return symbolInfo.createRSymbolObject(new DLL.RegisteredNativeSymbol(DLL.NativeSymbolType.Any, null, null), withReg);
            } else {
                return RNull.instance;
            }
        }

        protected static boolean isDLLInfo(RExternalPtr externalPtr) {
            return DLL.isDLLInfo(externalPtr);
        }
    }
}
