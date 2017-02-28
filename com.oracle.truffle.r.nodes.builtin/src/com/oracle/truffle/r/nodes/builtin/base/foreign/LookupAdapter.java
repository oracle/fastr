/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.foreign;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.library.stats.RandFunctionsNodes;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RInternalCodeBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.foreign.LookupAdapterFactory.ExtractNativeCallInfoNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalCode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

interface Lookup {
    RExternalBuiltinNode lookupBuiltin(RList symbol);
}

/**
 * Locator for "builtin" package function implementations. The "builtin" packages contain many
 * functions that are called from R code via the FFI, e.g. {@code .Call}, but implemented internally
 * in GnuR, and not necessarily following the FFI API. The value passed to {@code .Call} etc., is a
 * symbol, created when the package is loaded and stored in the namespace environment of the
 * package, that is a list-valued object. Evidently these "builtins" are somewhat similar to the
 * {@code .Primitive} and {@code .Internal} builtins and, similarly, most of these are
 * re-implemented in Java in FastR. The {@link Lookup#lookupBuiltin(RList)} method checks the name
 * in the list object and returns the {@link RExternalBuiltinNode} that implements the function, or
 * {@code null}. A {@code null} result implies that the builtin is not implemented in Java, but
 * called directly via the FFI interface, which is only possible for functions that use the FFI in a
 * way that FastR can handle.
 *
 * This class also handles the "lookup" of the {@link NativeCallInfo} data for builtins that are
 * still implemented by native code.
 */
abstract class LookupAdapter extends RBuiltinNode.Arg3 implements Lookup {
    @Child protected ExtractNativeCallInfoNode extractSymbolInfo = ExtractNativeCallInfoNodeGen.create();

    protected static class UnimplementedExternal extends RExternalBuiltinNode {
        private final String name;

        static {
            Casts.noCasts(UnimplementedExternal.class);
        }

        public UnimplementedExternal(String name) {
            this.name = name;
        }

        @Override
        public final Object call(RArgsValuesAndNames args) {
            throw RInternalError.unimplemented("unimplemented external builtin: " + name);
        }
    }

    private static final String UNKNOWN_EXTERNAL_BUILTIN = "UNKNOWN_EXTERNAL_BUILTIN";

    public static String lookupName(RList symbol) {
        CompilerAsserts.neverPartOfCompilation();
        if (symbol.getNames() != null) {
            RAbstractStringVector names = symbol.getNames();
            for (int i = 0; i < names.getLength(); i++) {
                if (names.getDataAt(i).equals("name")) {
                    String name = RRuntime.asString(symbol.getDataAt(i));
                    return name != null ? name : UNKNOWN_EXTERNAL_BUILTIN;
                }
            }
        }
        return UNKNOWN_EXTERNAL_BUILTIN;
    }

    @TruffleBoundary
    protected static RuntimeException fallback(Lookup lookup, Object symbol) {
        String name = null;
        if (symbol instanceof RList) {
            name = lookupName((RList) symbol);
            name = name == UNKNOWN_EXTERNAL_BUILTIN ? null : name;
            if (name != null && lookup.lookupBuiltin((RList) symbol) != null) {
                /*
                 * if we reach this point, then the cache saw a different value for f. the lists
                 * that contain the information about native calls are never expected to change.
                 */
                throw RInternalError.shouldNotReachHere("fallback reached for " + lookup + " " + name);
            }
        }
        throw RError.nyi((RBaseNode) lookup, lookup + " specialization failure: " + (name == null ? "<unknown>" : name));
    }

    public static String checkPackageArg(Object rPackage) {
        String libName = null;
        if (!(rPackage instanceof RMissing)) {
            libName = RRuntime.asString(rPackage);
            if (libName == null) {
                throw RError.error(RError.SHOW_CALLER, RError.Message.ARGUMENT_MUST_BE_STRING, "PACKAGE");
            }
        }
        return libName;
    }

    /**
     * Extracts the salient information needed for a native call from the {@link RList} value
     * provided from R.
     */
    public abstract static class ExtractNativeCallInfoNode extends Node {
        @Child private ExtractVectorNode nameExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
        @Child private ExtractVectorNode addressExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
        @Child private ExtractVectorNode packageExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
        @Child private ExtractVectorNode infoExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);

        protected abstract NativeCallInfo execute(VirtualFrame frame, RList symbol);

        @Specialization
        protected NativeCallInfo extractNativeCallInfo(VirtualFrame frame, RList symbol) {
            if (nameExtract == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                nameExtract = insert(ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true));
            }
            if (addressExtract == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                addressExtract = insert(ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true));
            }
            if (packageExtract == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                packageExtract = insert(ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true));
            }
            String name = RRuntime.asString(nameExtract.applyAccessField(frame, symbol, "name"));
            SymbolHandle address = ((RExternalPtr) addressExtract.applyAccessField(frame, symbol, "address")).getAddr();
            // field name may be "package" or "dll", but always at (R) index 3
            RList packageList = (RList) packageExtract.apply(frame, symbol, new Object[]{3}, RLogical.valueOf(false), RMissing.instance);
            DLLInfo dllInfo = (DLLInfo) ((RExternalPtr) addressExtract.applyAccessField(frame, packageList, "info")).getExternalObject();
            return new NativeCallInfo(name, address, dllInfo);

        }
    }

    protected static RExternalBuiltinNode getExternalModelBuiltinNode(String name) {
        return new RInternalCodeBuiltinNode(RContext.getInstance(), "stats", RInternalCode.loadSourceRelativeTo(RandFunctionsNodes.class, "model.R"), name);
    }

    protected static final int CallNST = DLL.NativeSymbolType.Call.ordinal();
    protected static final int ExternalNST = DLL.NativeSymbolType.External.ordinal();

    public static DLL.RegisteredNativeSymbol createRegisteredNativeSymbol(int nstOrd) {
        // DSL cannot resolve DLL.DLL.NativeSymbolType
        DLL.NativeSymbolType nst = DLL.NativeSymbolType.values()[nstOrd];
        return new DLL.RegisteredNativeSymbol(nst, null, null);
    }
}
