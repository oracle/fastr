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
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromStringAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromStringAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.interop.ForeignArrayToVectorNode;

public final class RForeignStringWrapper extends RAbstractStringVector implements RForeignVectorWrapper {

    protected final TruffleObject delegate;

    public RForeignStringWrapper(TruffleObject delegate) {
        super(RDataFactory.INCOMPLETE_VECTOR);
        this.delegate = delegate;
    }

    @Override
    public boolean isMaterialized() {
        return false;
    }

    @Override
    public int getLength() {
        return RRuntime.getForeignArraySize(delegate, getInterop());
    }

    @Override
    public Object getInternalStore() {
        return delegate;
    }

    @Override
    @TruffleBoundary
    public RAbstractVector internalCopy() {
        return ForeignArrayToVectorNode.getUncached().toVector(delegate, getRType());
    }

    @Override
    public RStringVector materialize() {
        return (RStringVector) copy();
    }

    @Override
    @TruffleBoundary
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    @Override
    @TruffleBoundary
    public String getDataAt(int index) {
        return getString(delegate, index, getInterop());
    }

    private static String getString(TruffleObject truffleObjectIn, int index, InteropLibrary delegateInterop) throws RuntimeException {
        try {
            Object result = delegateInterop.readArrayElement(truffleObjectIn, index);
            if (result instanceof TruffleObject) {
                try {
                    result = getInterop().asString(result);
                } catch (UnsupportedMessageException ex) {
                    if (getInterop().isNull(result)) {
                        return RRuntime.STRING_NA;
                    }
                    throw RInternalError.shouldNotReachHere(ex);
                }
            }
            return result.toString();
        } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    private static InteropLibrary getInterop() {
        return InteropLibrary.getFactory().getUncached();
    }

    private static final class FastPathAccess extends FastPathFromStringAccess {

        @Child private InteropLibrary delegateInterop;
        @Child private InteropLibrary elementInterop = insert(InteropLibrary.getFactory().createDispatched(DSLConfig.getInteropLibraryCacheSize()));
        private final ValueProfile resultProfile = ValueProfile.createClassProfile();

        FastPathAccess(RAbstractContainer value) {
            super(value);
            delegateInterop = InteropLibrary.getFactory().create(((RForeignStringWrapper) value).delegate);
        }

        @Override
        public boolean supports(Object value) {
            return super.supports(value) && delegateInterop.accepts(((RForeignStringWrapper) value).delegate);
        }

        @Override
        protected int getLength(RAbstractContainer vector) {
            return RRuntime.getForeignArraySize(((RForeignStringWrapper) vector).delegate, delegateInterop);
        }

        @Override
        protected String getStringImpl(AccessIterator accessIter, int index) {
            try {
                Object value = delegateInterop.readArrayElement(accessIter.getStore(), index);
                try {
                    return resultProfile.profile(elementInterop.asString(value));
                } catch (UnsupportedMessageException ume) {
                    if (elementInterop.isNull(value)) {
                        return RRuntime.STRING_NA;
                    }
                    throw RInternalError.shouldNotReachHere(ume);
                }
            } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromStringAccess SLOW_PATH_ACCESS = new SlowPathFromStringAccess() {
        @Override
        @TruffleBoundary
        protected int getLength(RAbstractContainer vector) {
            return RRuntime.getForeignArraySize(((RForeignStringWrapper) vector).delegate, getInterop());
        }

        @Override
        @TruffleBoundary
        protected String getStringImpl(AccessIterator accessIter, int index) {
            RForeignStringWrapper vector = (RForeignStringWrapper) accessIter.getStore();
            return RForeignStringWrapper.getString(vector.delegate, index, getInterop());
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }
}
