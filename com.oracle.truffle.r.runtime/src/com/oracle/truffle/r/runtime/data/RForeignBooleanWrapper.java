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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromLogicalAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromLogicalAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

public final class RForeignBooleanWrapper extends RForeignVectorWrapper implements RAbstractLogicalVector {

    public RForeignBooleanWrapper(TruffleObject delegate) {
        super(delegate);
    }

    @Override
    public RLogicalVector materialize() {
        return (RLogicalVector) copy();
    }

    @Override
    @TruffleBoundary
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    @Override
    @TruffleBoundary
    public byte getDataAt(int index) {
        Object value = null;
        try {
            value = getInterop().readArrayElement(delegate, index);
            return RRuntime.asLogical((boolean) value);
        } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
            throw RInternalError.shouldNotReachHere(e);
        } catch (ClassCastException e) {
            if (value instanceof TruffleObject) {
                return unbox((TruffleObject) value, e);
            }
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    private static byte unbox(TruffleObject value, ClassCastException e) throws RuntimeException {
        if (getInterop().isNull(value)) {
            return RRuntime.LOGICAL_NA;
        }
        try {
            return RRuntime.asLogical(getInterop().asBoolean(value));
        } catch (UnsupportedMessageException | ClassCastException ex) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @Override
    public RVector<?> createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createLogicalVector(new byte[newLength], newIsComplete);
    }

    private static final class FastPathAccess extends FastPathFromLogicalAccess {

        @Child private InteropLibrary delegateInterop;
        @Child private InteropLibrary elementInterop = insert(InteropLibrary.getFactory().createDispatched(DSLConfig.getInteropLibraryCacheSize()));

        FastPathAccess(RAbstractContainer value) {
            super(value);
            delegateInterop = InteropLibrary.getFactory().create(((RForeignVectorWrapper) value).delegate);
        }

        @Override
        public boolean supports(Object value) {
            return super.supports(value) && delegateInterop.accepts(((RForeignVectorWrapper) value).delegate);
        }

        @Override
        protected int getLength(RAbstractContainer vector) {
            return RRuntime.getForeignArraySize(((RForeignBooleanWrapper) vector).delegate, delegateInterop);
        }

        @Override
        protected byte getLogicalImpl(AccessIterator accessIter, int index) {
            try {
                Object value = delegateInterop.readArrayElement(accessIter.getStore(), index);
                try {
                    return RRuntime.asLogical(elementInterop.asBoolean(value));
                } catch (UnsupportedMessageException ume) {
                    if (elementInterop.isNull(value)) {
                        return RRuntime.LOGICAL_NA;
                    }
                    throw RInternalError.shouldNotReachHere(ume);
                }
            } catch (UnsupportedMessageException | InvalidArrayIndexException | ClassCastException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromLogicalAccess SLOW_PATH_ACCESS = new SlowPathFromLogicalAccess() {

        @Override
        @TruffleBoundary
        protected int getLength(RAbstractContainer vector) {
            return RRuntime.getForeignArraySize(((RForeignVectorWrapper) vector).delegate, getInterop());
        }

        @Override
        protected byte getLogicalImpl(AccessIterator accessIter, int index) {
            RForeignBooleanWrapper vector = (RForeignBooleanWrapper) accessIter.getStore();
            Object value = null;
            try {
                value = getInterop().readArrayElement(vector.delegate, index);
                return RRuntime.asLogical((boolean) value);
            } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                throw RInternalError.shouldNotReachHere(e);
            } catch (ClassCastException e) {
                if (value instanceof TruffleObject) {
                    return unbox((TruffleObject) value, e);
                }
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }
}
