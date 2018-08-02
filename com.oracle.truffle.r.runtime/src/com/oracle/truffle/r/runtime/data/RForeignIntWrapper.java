/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromIntAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromIntAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

public final class RForeignIntWrapper extends RForeignWrapper implements RAbstractIntVector {

    public RForeignIntWrapper(TruffleObject delegate) {
        super(delegate);
    }

    @Override
    public RIntVector materialize() {
        return (RIntVector) copy();
    }

    @Override
    @TruffleBoundary
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    @Override
    @TruffleBoundary
    public int getDataAt(int index) {
        Object value = null;
        try {
            value = ForeignAccess.sendRead(READ, delegate, index);
            return ((Number) value).intValue();
        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
            throw RInternalError.shouldNotReachHere(e);
        } catch (ClassCastException e) {
            if (value instanceof TruffleObject) {
                return unbox((TruffleObject) value, e);
            }
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    private static int unbox(TruffleObject value, ClassCastException e) throws RuntimeException {
        if (ForeignAccess.sendIsNull(IS_NULL, value)) {
            return RRuntime.INT_NA;
        }
        if (ForeignAccess.sendIsBoxed(IS_BOXED, value)) {
            try {
                return ((Number) ForeignAccess.sendUnbox(UNBOX, value)).intValue();
            } catch (UnsupportedMessageException | ClassCastException ex) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
        throw RInternalError.shouldNotReachHere(e);
    }

    private static final class FastPathAccess extends FastPathFromIntAccess {

        FastPathAccess(RAbstractContainer value) {
            super(value);
        }

        private final ValueProfile resultProfile = ValueProfile.createClassProfile();
        private final ConditionProfile isTruffleObjectProfile = ConditionProfile.createBinaryProfile();
        @Child private Node getSize = Message.GET_SIZE.createNode();
        @Child private Node read = Message.READ.createNode();
        @Child private Node isNull;
        @Child private Node isBoxed;
        @Child private Node unbox;

        @Override
        protected int getLength(RAbstractContainer vector) {
            try {
                return (int) ForeignAccess.sendGetSize(getSize, ((RForeignWrapper) vector).delegate);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }

        @Override
        public int getIntImpl(AccessIterator accessIter, int index) {
            Object value;
            try {
                value = ForeignAccess.sendRead(read, (TruffleObject) accessIter.getStore(), index);
                if (isTruffleObjectProfile.profile(value instanceof TruffleObject)) {
                    if (isNull == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        isNull = insert(Message.IS_NULL.createNode());
                    }
                    if (ForeignAccess.sendIsNull(isNull, (TruffleObject) value)) {
                        return RRuntime.INT_NA;
                    }
                    if (isBoxed == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        isBoxed = insert(Message.IS_BOXED.createNode());
                    }
                    if (ForeignAccess.sendIsBoxed(isBoxed, (TruffleObject) value)) {
                        if (unbox == null) {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            unbox = insert(Message.UNBOX.createNode());
                        }
                        value = ForeignAccess.sendUnbox(unbox, (TruffleObject) value);
                    }
                }
                return ((Number) resultProfile.profile(value)).intValue();
            } catch (UnsupportedMessageException | UnknownIdentifierException | ClassCastException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromIntAccess SLOW_PATH_ACCESS = new SlowPathFromIntAccess() {
        @Override
        @TruffleBoundary
        protected int getLength(RAbstractContainer vector) {
            try {
                return (int) ForeignAccess.sendGetSize(GET_SIZE, ((RForeignIntWrapper) vector).delegate);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }

        @Override
        public int getIntImpl(AccessIterator accessIter, int index) {
            RForeignIntWrapper vector = (RForeignIntWrapper) accessIter.getStore();
            Object value = null;
            try {
                value = ForeignAccess.sendRead(READ, vector.delegate, index);
                return ((Number) value).intValue();
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
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
