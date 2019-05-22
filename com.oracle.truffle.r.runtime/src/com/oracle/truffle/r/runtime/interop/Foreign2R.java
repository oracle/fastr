/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.interop;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RInteropScalar.RInteropNA;
import com.oracle.truffle.r.runtime.data.RNull;

/**
 * Converts 'primitive' values from the outside world to internal FastR representations. For
 * conversion of more complex cases see {@link ConvertForeignObjectNode}.
 */
@GenerateUncached
@ImportStatic({DSLConfig.class, RRuntime.class})
public abstract class Foreign2R extends Node {

    public static Foreign2R create() {
        return Foreign2RNodeGen.create();
    }

    public static Foreign2R getUncached() {
        return Foreign2RNodeGen.getUncached();
    }

    public Object convert(Object obj) {
        return execute(obj, false, true);
    }

    public Object convert(Object obj, boolean preserveByte, boolean printWarning) {
        return execute(obj, preserveByte, printWarning);
    }

    protected abstract Object execute(Object obj, boolean preserveByte, boolean printWarning);

    @Specialization
    public byte doBoolean(boolean obj, @SuppressWarnings("unused") boolean preserveByte, @SuppressWarnings("unused") boolean printWarning) {
        return RRuntime.asLogical(obj);
    }

    @Specialization(guards = "!preserveByte")
    public int doByte(byte obj, @SuppressWarnings("unused") boolean preserveByte, @SuppressWarnings("unused") boolean printWarning) {
        return ((Byte) obj).intValue();
    }

    @Specialization(guards = "preserveByte")
    public byte doBytePreserved(byte obj, @SuppressWarnings("unused") boolean preserveByte, @SuppressWarnings("unused") boolean printWarning) {
        return obj;
    }

    @Specialization
    public int doShort(short obj, @SuppressWarnings("unused") boolean preserveByte, @SuppressWarnings("unused") boolean printWarning) {
        return ((Short) obj).intValue();
    }

    @Specialization(guards = "interop.fitsInDouble(l)", limit = "getInteropLibraryCacheSize()")
    public double doLongDouble(long l, @SuppressWarnings("unused") boolean preserveByte, @SuppressWarnings("unused") boolean printWarning,
                    @SuppressWarnings("unused") @CachedLibrary("l") InteropLibrary interop) {
        try {
            return interop.asDouble(l);
        } catch (UnsupportedMessageException ex) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @Specialization(guards = {"!interop.fitsInDouble(l)", "printWarning"}, limit = "getInteropLibraryCacheSize()")
    public double doLongPrecissionLossWarning(long l, @SuppressWarnings("unused") boolean preserveByte, @SuppressWarnings("unused") boolean printWarning,
                    @SuppressWarnings("unused") @CachedLibrary("l") InteropLibrary interop) {
        double d = (((Long) l).doubleValue());
        RError.warning(RError.SHOW_CALLER, RError.Message.PRECISSION_LOSS_BY_CONVERSION, l, d);
        return d;
    }

    @Specialization(guards = {"!interop.fitsInDouble(l)", "!printWarning"}, limit = "getInteropLibraryCacheSize()")
    public double doLongPrecissionLoss(long l, @SuppressWarnings("unused") boolean preserveByte, @SuppressWarnings("unused") boolean printWarning,
                    @SuppressWarnings("unused") @CachedLibrary("l") InteropLibrary interop) {
        return (((Long) l).doubleValue());
    }

    @Specialization
    public double doFloat(float obj, @SuppressWarnings("unused") boolean preserveByte, @SuppressWarnings("unused") boolean printWarning) {
        return (((Float) obj).doubleValue());
    }

    @Specialization
    public String doChar(char obj, @SuppressWarnings("unused") boolean preserveByte, @SuppressWarnings("unused") boolean printWarning) {
        return ((Character) obj).toString();
    }

    @Specialization
    public Object doInteropNA(RInteropNA interopNA, @SuppressWarnings("unused") boolean preserveByte, @SuppressWarnings("unused") boolean printWarning) {
        return interopNA.getValue();
    }

    @Specialization(guards = "isNA(d)")
    public double doDoubleNA(@SuppressWarnings("unused") double d, @SuppressWarnings("unused") boolean preserveByte, @SuppressWarnings("unused") boolean printWarning) {
        return Double.NaN;
    }

    @Specialization(guards = "!isNA(d)")
    public double doDouble(double d, @SuppressWarnings("unused") boolean preserveByte, @SuppressWarnings("unused") boolean printWarning) {
        return d;
    }

    @Specialization(guards = "isNA(i)")
    public double doIntNA(int i, @SuppressWarnings("unused") boolean preserveByte, @SuppressWarnings("unused") boolean printWarning) {
        return i;
    }

    @Specialization(guards = "!isNA(i)")
    public int doInt(int i, @SuppressWarnings("unused") boolean preserveByte, @SuppressWarnings("unused") boolean printWarning) {
        return i;
    }

    @Specialization(guards = {"isForeignObject(obj)", "interop.isNull(obj)"}, limit = "getInteropLibraryCacheSize()")
    public RNull doNull(@SuppressWarnings("unused") Object obj, @SuppressWarnings("unused") boolean preserveByte, @SuppressWarnings("unused") boolean printWarning,
                    @SuppressWarnings("unused") @CachedLibrary("obj") InteropLibrary interop) {
        return RNull.instance;
    }

    @Specialization(guards = {"isForeignObject(obj)", "!interop.isNull(obj)"}, limit = "getInteropLibraryCacheSize()")
    public Object doForeignObject(TruffleObject obj, boolean preserveByte, boolean printWarning,
                    @Cached("create()") Foreign2R recursive,
                    @CachedLibrary("obj") InteropLibrary interop) {
        try {
            Object unboxed = unbox(obj, interop);
            if (unboxed != null) {
                return recursive.execute(unboxed, preserveByte, printWarning);
            }
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere();
        }
        return obj;
    }

    @Fallback
    public Object doObject(Object obj, @SuppressWarnings("unused") boolean preserveByte, @SuppressWarnings("unused") boolean printWarning) {
        return obj;
    }

    public static Object unbox(Object obj, InteropLibrary interop) throws UnsupportedMessageException {
        if (interop.isBoolean(obj)) {
            return interop.asBoolean(obj);
        } else if (interop.isString(obj)) {
            return interop.asString(obj);
        } else if (interop.isNumber(obj)) {
            if (interop.fitsInByte(obj)) {
                return interop.asByte(obj);
            } else if (interop.fitsInShort(obj)) {
                return interop.asShort(obj);
            } else if (interop.fitsInInt(obj)) {
                return interop.asInt(obj);
            } else if (interop.fitsInLong(obj)) {
                return interop.asLong(obj);
            } else if (interop.fitsInFloat(obj)) {
                return interop.asFloat(obj);
            } else if (interop.fitsInDouble(obj)) {
                return interop.asDouble(obj);
            }
        }
        return null;
    }

    protected boolean isNull(Object o) {
        return o == null;
    }

}
