/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine.interop.ffi.llvm;

import java.nio.charset.StandardCharsets;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.r.engine.interop.NativeCharArray;
import com.oracle.truffle.r.engine.interop.NativeDoubleArray;
import com.oracle.truffle.r.engine.interop.NativeIntegerArray;
import com.oracle.truffle.r.engine.interop.NativeLogicalArray;
import com.oracle.truffle.r.engine.interop.NativeRawArray;
import com.oracle.truffle.r.nodes.ffi.RFFIUtils;
import com.oracle.truffle.r.nodes.ffi.UpCallsRFFIImpl;
import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RScalar;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.RUnboundValue;
import com.oracle.truffle.r.runtime.ffi.CharSXPWrapper;
import com.oracle.truffle.r.runtime.ffi.RFFIVariables;

/**
 * (Incomplete) Variant of {@link UpCallsRFFIImpl} for Truffle LLVM.
 *
 */
public class TruffleLLVM_UpCallsRFFIImpl extends UpCallsRFFIImpl implements VariableUpCallsRFFI {
    private static TruffleLLVM_UpCallsRFFIImpl singleton;
    private static TruffleObject singletonTruffleObject;

    public static TruffleObject initialize() {
        if (singleton == null) {
            singleton = new TruffleLLVM_UpCallsRFFIImpl();
            singletonTruffleObject = JavaInterop.asTruffleObject(singleton);
        }
        return singletonTruffleObject;
    }

    public Object charSXPToNativeCharArray(Object x) {
        CharSXPWrapper chars = RFFIUtils.guaranteeInstanceOf(x, CharSXPWrapper.class);
        return new NativeCharArray(chars.getContents().getBytes());
    }

    // Checkstyle: stop method name check

    @Override
    public Object Rf_mkCharLenCE(Object bytes, int len, int encoding) {
        if (bytes instanceof NativeCharArray) {
            return super.Rf_mkCharLenCE(((NativeCharArray) bytes).getBytes(), len, encoding);
        } else {
            throw RInternalError.unimplemented();
        }
    }

    @Override
    public Object Rf_install(Object name) {
        if (name instanceof NativeCharArray) {
            return RDataFactory.createSymbolInterned(new String(((NativeCharArray) name).getBytes(), StandardCharsets.UTF_8));
        } else {
            throw RInternalError.unimplemented();
        }
    }

    @Override
    public Object RAW(Object x) {
        byte[] value = (byte[]) super.RAW(x);
        return new NativeRawArray(value);
    }

    @Override
    public Object LOGICAL(Object x) {
        byte[] value = (byte[]) super.LOGICAL(x);
        return new NativeLogicalArray(x, value);
    }

    @Override
    public Object INTEGER(Object x) {
        int[] value = (int[]) super.INTEGER(x);
        return new NativeIntegerArray(x, value);
    }

    @Override
    public Object REAL(Object x) {
        // Special handling in Truffle variant
        double[] value = (double[]) super.REAL(x);
        return new NativeDoubleArray(x, value);
    }

    public Object R_Home() {
        byte[] sbytes = REnvVars.rHome().getBytes();
        return new NativeCharArray(sbytes);
    }

    @Override
    public Object Rf_findVar(Object symbolArg, Object envArg) {
        Object v = super.Rf_findVar(symbolArg, envArg);
        if (v instanceof RTypedValue) {
            return v;
        } else {
            return wrapPrimitive(v);
        }
    }

    public Object bytesToNativeCharArray(byte[] bytes) {
        return new NativeCharArray(bytes);
    }

    private static RScalar wrapPrimitive(Object x) {
        if (x instanceof Double) {
            return RDouble.valueOf((double) x);
        } else if (x instanceof Integer) {
            return RInteger.valueOf((int) x);
        } else if (x instanceof Byte) {
            return RLogical.valueOf((byte) x);
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @Override
    public Object R_NilValue() {
        return RNull.instance;
    }

    @Override
    public Object R_UnboundValue() {
        return RUnboundValue.instance;
    }

    @Override
    public Object R_Srcref() {
        return RFFIVariables.R_Srcref.getValue();
    }

    @Override
    public Object R_MissingArg() {
        return RFFIVariables.R_MissingArg.getValue();
    }

    @Override
    public Object R_BaseSymbol() {
        return RFFIVariables.R_BaseSymbol.getValue();
    }

    @Override
    public Object R_BraceSymbol() {
        return RFFIVariables.R_BraceSymbol.getValue();
    }

    @Override
    public Object R_Bracket2Symbol() {
        return RFFIVariables.R_Bracket2Symbol.getValue();
    }

    @Override
    public Object R_BracketSymbol() {
        return RFFIVariables.R_BracketSymbol.getValue();
    }

    @Override
    public Object R_ClassSymbol() {
        return RFFIVariables.R_ClassSymbol.getValue();
    }

    @Override
    public Object R_DeviceSymbol() {
        return RFFIVariables.R_DeviceSymbol.getValue();
    }

    @Override
    public Object R_DimNamesSymbol() {
        return RFFIVariables.R_DimNamesSymbol.getValue();
    }

    @Override
    public Object R_DimSymbol() {
        return RFFIVariables.R_DimSymbol.getValue();
    }

    @Override
    public Object R_DollarSymbol() {
        return RFFIVariables.R_DollarSymbol.getValue();
    }

    @Override
    public Object R_DotsSymbol() {
        return RFFIVariables.R_DotsSymbol.getValue();
    }

    @Override
    public Object R_DropSymbol() {
        return RFFIVariables.R_DropSymbol.getValue();
    }

    @Override
    public Object R_LastvalueSymbol() {
        return RFFIVariables.R_LastvalueSymbol.getValue();
    }

    @Override
    public Object R_LevelsSymbol() {
        return RFFIVariables.R_LevelsSymbol.getValue();
    }

    @Override
    public Object R_ModeSymbol() {
        return RFFIVariables.R_ModeSymbol.getValue();
    }

    @Override
    public Object R_NaRmSymbol() {
        return RFFIVariables.R_NaRmSymbol.getValue();
    }

    @Override
    public Object R_NameSymbol() {
        return RFFIVariables.R_NameSymbol.getValue();
    }

    @Override
    public Object R_NamesSymbol() {
        return RFFIVariables.R_NamesSymbol.getValue();
    }

    @Override
    public Object R_NamespaceEnvSymbol() {
        return RFFIVariables.R_NamespaceEnvSymbol.getValue();
    }

    @Override
    public Object R_PackageSymbol() {
        return RFFIVariables.R_PackageSymbol.getValue();
    }

    @Override
    public Object R_QuoteSymbol() {
        return RFFIVariables.R_QuoteSymbol.getValue();
    }

    @Override
    public Object R_RowNamesSymbol() {
        return RFFIVariables.R_RowNamesSymbol.getValue();
    }

    @Override
    public Object R_SeedsSymbol() {
        return RFFIVariables.R_SeedsSymbol.getValue();
    }

    @Override
    public Object R_SourceSymbol() {
        return RFFIVariables.R_SourceSymbol.getValue();
    }

    @Override
    public Object R_TspSymbol() {
        return RFFIVariables.R_TspSymbol.getValue();
    }

    @Override
    public Object R_dot_defined() {
        return RFFIVariables.R_dot_defined.getValue();
    }

    @Override
    public Object R_dot_Method() {
        return RFFIVariables.R_dot_Method.getValue();
    }

    @Override
    public Object R_dot_target() {
        return RFFIVariables.R_dot_target.getValue();
    }

    @Override
    public Object R_NaString() {
        return RFFIVariables.R_NaString.getValue();
    }

    @Override
    public Object R_BlankString() {
        return RFFIVariables.R_BlankString.getValue();
    }

    @Override
    public Object R_BlankScalarString() {
        return RFFIVariables.R_BlankScalarString.getValue();
    }

    @Override
    public Object R_EmptyEnv() {
        return RFFIVariables.R_EmptyEnv.getValue();
    }

}
