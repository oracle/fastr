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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromIntAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromIntAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

public final class RForeignIntWrapper extends RForeignVectorWrapper implements RAbstractIntVector {

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
            value = getInterop().readArrayElement(delegate, index);
            return ((Number) value).intValue();
        } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
            throw RInternalError.shouldNotReachHere(e);
        } catch (ClassCastException e) {
            if (value instanceof TruffleObject) {
                return unbox((TruffleObject) value, e);
            }
            if (value instanceof Character) {
                return (Character) value;
            }
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @TruffleBoundary
    private static int unbox(TruffleObject value, ClassCastException e) throws RuntimeException {
        if (getInterop().isNull(value)) {
            return RRuntime.INT_NA;
        }
        try {
            if (getInterop().isNumber(value)) {
                return getInterop().asInt(value);
            } else {
                return getInterop().asString(value).charAt(0);
            }
        } catch (UnsupportedMessageException | ClassCastException ex) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    @Override
    public RVector<?> createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createIntVector(new int[newLength], newIsComplete);
    }

    private static final class FastPathAccess extends FastPathFromIntAccess {

        @Child private InteropLibrary delegateInterop;
        @Child private InteropLibrary elementInterop;

        private final ValueProfile resultProfile = ValueProfile.createClassProfile();
        private final ConditionProfile isTruffleObjectProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isIntProfile = ConditionProfile.createBinaryProfile();

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
            return RRuntime.getForeignArraySize(((RForeignVectorWrapper) vector).delegate, delegateInterop);
        }

        @Override
        public int getIntImpl(AccessIterator accessIter, int index) {
            Object value;
            try {
                value = delegateInterop.readArrayElement(accessIter.getStore(), index);

                if (isTruffleObjectProfile.profile(value instanceof TruffleObject)) {
                    if (elementInterop == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        elementInterop = insert(InteropLibrary.getFactory().createDispatched(DSLConfig.getInteropLibraryCacheSize()));
                    }
                    if (elementInterop.isNumber(value)) {
                        value = elementInterop.asInt(value);
                    } else if (elementInterop.isNull(value)) {
                        return RRuntime.INT_NA;
                    } else {
                        value = elementInterop.asString(value).charAt(0);
                    }
                }

                if (isIntProfile.profile(value instanceof Number)) {
                    return ((Number) resultProfile.profile(value)).intValue();
                } else {
                    return (Character) resultProfile.profile(value);
                }
            } catch (UnsupportedMessageException | InvalidArrayIndexException |

                            ClassCastException e) {
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
            return RRuntime.getForeignArraySize(((RForeignVectorWrapper) vector).delegate, getInterop());
        }

        @Override
        @TruffleBoundary
        public int getIntImpl(AccessIterator accessIter, int index) {
            RForeignIntWrapper vector = (RForeignIntWrapper) accessIter.getStore();
            Object value = null;
            try {
                value = getInterop().readArrayElement(vector.delegate, index);
                return ((Number) value).intValue();
            } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                throw RInternalError.shouldNotReachHere(e);
            } catch (ClassCastException e) {
                if (value instanceof TruffleObject) {
                    return unbox((TruffleObject) value, e);
                }
                if (value instanceof Character) {
                    return (Character) value;
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
