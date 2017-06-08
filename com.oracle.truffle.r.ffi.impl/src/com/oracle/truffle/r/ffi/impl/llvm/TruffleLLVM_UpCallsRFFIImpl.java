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
package com.oracle.truffle.r.ffi.impl.llvm;

import static com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_Utils.checkNativeAddress;

import java.nio.charset.StandardCharsets;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.ffi.impl.common.JavaUpCallsRFFIImpl;
import com.oracle.truffle.r.ffi.impl.common.RFFIUtils;
import com.oracle.truffle.r.ffi.impl.interop.NativeCharArray;
import com.oracle.truffle.r.ffi.impl.interop.NativeDoubleArray;
import com.oracle.truffle.r.ffi.impl.interop.NativeIntegerArray;
import com.oracle.truffle.r.ffi.impl.interop.NativeLogicalArray;
import com.oracle.truffle.r.ffi.impl.interop.NativeRawArray;
import com.oracle.truffle.r.ffi.impl.upcalls.Callbacks;
import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RScalar;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.ffi.CharSXPWrapper;

/**
 * (Incomplete) Variant of {@link JavaUpCallsRFFIImpl} for Truffle LLVM.
 *
 */
public class TruffleLLVM_UpCallsRFFIImpl extends JavaUpCallsRFFIImpl {

    public Object charSXPToNativeCharArray(Object x) {
        CharSXPWrapper chars = RFFIUtils.guaranteeInstanceOf(x, CharSXPWrapper.class);
        return new NativeCharArray(chars.getContents().getBytes());
    }

    // Checkstyle: stop method name check

    @Override
    public Object Rf_mkCharLenCE(Object obj, int len, int encoding) {
        if (obj instanceof NativeCharArray) {
            byte[] bytes = ((NativeCharArray) obj).getValue();
            return super.Rf_mkCharLenCE(bytes, bytes.length, encoding);
        } else {
            throw RInternalError.unimplemented();
        }
    }

    @Override
    public int Rf_error(Object msg) {
        if (msg instanceof NativeCharArray) {
            String smsg = new String(((NativeCharArray) msg).getValue(), StandardCharsets.UTF_8);
            super.Rf_error(smsg);
        } else {
            throw RInternalError.unimplemented();
        }
        return 0;
    }

    @Override
    public Object Rf_install(Object name) {
        if (name instanceof NativeCharArray) {
            return RDataFactory.createSymbolInterned(new String(((NativeCharArray) name).getValue(), StandardCharsets.UTF_8));
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
        int[] value = (int[]) super.INTEGER(checkNativeAddress(x));
        return new NativeIntegerArray(x, value);
    }

    @Override
    public Object REAL(Object x) {
        // Special handling in Truffle variant
        double[] value = (double[]) super.REAL(x);
        return new NativeDoubleArray(x, value);
    }

    @Override
    public Object R_Home() {
        byte[] sbytes = REnvVars.rHome().getBytes();
        return new NativeCharArray(sbytes);
    }

    @Override
    public Object Rf_findVar(Object symbolArg, Object envArg) {
        Object v = super.Rf_findVar(symbolArg, checkNativeAddress(envArg));
        if (v instanceof RTypedValue) {
            return v;
        } else {
            return wrapPrimitive(v);
        }
    }

    @Override
    public int Rf_defineVar(Object symbolArg, Object value, Object envArg) {
        super.Rf_defineVar(symbolArg, value, checkNativeAddress(envArg));
        return 0;
    }

    @Override
    @TruffleBoundary
    public int Rf_setAttrib(Object obj, Object name, Object val) {
        super.Rf_setAttrib(checkNativeAddress(obj), name, checkNativeAddress(val));
        return 0;
    }

    @Override
    public Object Rf_getAttrib(Object obj, Object name) {
        Object checkedObj = checkNativeAddress(obj);
        return super.Rf_getAttrib(checkedObj, name);
    }

    @Override
    public Object Rf_cons(Object car, Object cdr) {
        return super.Rf_cons(checkNativeAddress(car), checkNativeAddress(cdr));
    }

    @Override
    public Object CAR(Object e) {
        return super.CAR(checkNativeAddress(e));
    }

    @Override
    public Object CDR(Object e) {
        return super.CDR(checkNativeAddress(e));
    }

    @Override
    public Object CADR(Object e) {
        Object ne = checkNativeAddress(e);
        return super.CADR(ne);
    }

    @Override
    public Object SETCAR(Object x, Object y) {
        return super.SETCAR(checkNativeAddress(x), y);
    }

    public Object bytesToNativeCharArray(byte[] bytes) {
        Object result = new NativeCharArray(bytes);
        return result;
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
    public Object R_CHAR(Object x) {
        throw RInternalError.unimplemented();
    }

    public Object getCallback(int index) {
        return Callbacks.values()[index].call;
    }
}
