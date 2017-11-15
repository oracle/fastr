/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromListAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromListAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.interop.Foreign2R;

public final class RForeignNamedListWrapper extends RForeignWrapper implements RAbstractListVector {

    private final RStringVector names;

    public RForeignNamedListWrapper(TruffleObject delegate, RStringVector names) {
        super(delegate);
        this.names = names;
    }

    @Override
    public Object getInternalStore() {
        return this;
    }

    @Override
    public int getLength() {
        return names.getLength();
    }

    @Override
    public RStringVector getNames() {
        return names;
    }

    @Override
    public RList materialize() {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    @TruffleBoundary
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    @Override
    @TruffleBoundary
    public Object getDataAt(int index) {
        try {
            return FOREIGN_TO_R.execute(ForeignAccess.sendRead(READ, delegate, names.getDataAt(index)));
        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    private static final class FastPathAccess extends FastPathFromListAccess {

        FastPathAccess(RAbstractContainer value) {
            super(value);
        }

        @Child private Node getSize = Message.GET_SIZE.createNode();
        @Child private Node read = Message.READ.createNode();
        @Child private Foreign2R foreign2r = Foreign2R.create();

        @Override
        public RType getType() {
            return RType.List;
        }

        @Override
        protected int getLength(RAbstractContainer vector) {
            return ((RForeignNamedListWrapper) vector).getLength();
        }

        @Override
        protected Object getListElement(Object internalStore, int index) {
            try {
                RForeignNamedListWrapper wrapper = (RForeignNamedListWrapper) internalStore;
                return foreign2r.execute(ForeignAccess.sendRead(read, wrapper.delegate, wrapper.names.getDataAt(index)));
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final Foreign2R FOREIGN_TO_R = Foreign2R.create();

    private static final SlowPathFromListAccess SLOW_PATH_ACCESS = new SlowPathFromListAccess() {
        @Override
        public RType getType() {
            return RType.List;
        }

        @Override
        @TruffleBoundary
        protected int getLength(RAbstractContainer vector) {
            return ((RForeignNamedListWrapper) vector).names.getLength();
        }

        @Override
        protected Object getListElement(Object store, int index) {
            RForeignNamedListWrapper vector = (RForeignNamedListWrapper) store;
            try {
                return FOREIGN_TO_R.execute(ForeignAccess.sendRead(READ, vector.delegate, vector.names.getDataAt(index)));
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }
}
