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
package com.oracle.truffle.r.runtime.interop;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributeStorage;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public final class TruffleObjectConverter {

    private Node hasSizeNode = com.oracle.truffle.api.interop.Message.HAS_SIZE.createNode();
    private Node getSizeNode = com.oracle.truffle.api.interop.Message.GET_SIZE.createNode();
    private Node readNode = com.oracle.truffle.api.interop.Message.READ.createNode();
    private Node isBoxedNode = com.oracle.truffle.api.interop.Message.IS_BOXED.createNode();
    private Node unboxNode = com.oracle.truffle.api.interop.Message.UNBOX.createNode();
    private Node keysNode = com.oracle.truffle.api.interop.Message.KEYS.createNode();
    private Foreign2R foreign2R = Foreign2R.create();

    public TruffleObjectConverter() {
    }

    public Node[] getSubNodes() {
        return new Node[]{
                        hasSizeNode, getSizeNode, readNode, isBoxedNode, unboxNode, keysNode, foreign2R
        };
    }

    @TruffleBoundary
    public Object convert(TruffleObject obj) {
        class RStringWrapper extends TruffleObjectWrapper implements RAbstractStringVector {
            final TruffleObject object;

            RStringWrapper(int length, TruffleObject object) {
                super(length);
                this.object = object;
            }

            @Override
            @TruffleBoundary
            public Object getDataAtAsObject(int index) {
                return getDataAt(index);
            }

            @Override
            @TruffleBoundary
            public String getDataAt(int index) {
                Object value;
                try {
                    value = ForeignAccess.sendRead(readNode, object, index);
                    return String.valueOf(foreign2R.execute(value));
                } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }

            @Override
            @TruffleBoundary
            public RStringVector materialize() {
                throw RInternalError.shouldNotReachHere();
            }
        }
        class RListWrapper extends TruffleObjectWrapper implements RAbstractListVector {

            private final RStringVector names;

            RListWrapper(int length, RStringVector names) {
                super(length);
                this.names = names;
                if (names != null) {
                    DynamicObject attrs = RAttributesLayout.createNames(names);
                    initAttributes(attrs);
                }
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
                    Object value = ForeignAccess.sendRead(readNode, obj, names != null ? names.getDataAt(index) : index);
                    return foreign2R.execute(value);
                } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }

            @Override
            @TruffleBoundary
            public RStringVector getNames() {
                return names;
            }

            @Override
            @TruffleBoundary
            public RList materialize() {
                throw RInternalError.shouldNotReachHere();
            }
        }
        try {
            if (ForeignAccess.sendHasSize(hasSizeNode, obj)) {
                int size = (Integer) ForeignAccess.sendGetSize(getSizeNode, obj);
                ForeignArray2R.InteropTypeCheck typeCheck = new ForeignArray2R.InteropTypeCheck();
                for (int i = 0; i < size; i++) {
                    Object value = ForeignAccess.sendRead(readNode, obj, i);
                    if (value instanceof TruffleObject && ForeignAccess.sendIsBoxed(isBoxedNode, (TruffleObject) value)) {
                        value = ForeignAccess.sendUnbox(unboxNode, (TruffleObject) value);
                    }
                    if (typeCheck.checkForeign(value) == ForeignArray2R.InteropTypeCheck.RType.NONE) {
                        break;
                    }
                }
                switch (typeCheck.getType()) {
                    case BOOLEAN:
                        class RLogicalWrapper extends TruffleObjectWrapper implements RAbstractLogicalVector {

                            RLogicalWrapper(int length) {
                                super(length);
                            }

                            @Override
                            @TruffleBoundary
                            public Object getDataAtAsObject(int index) {
                                return getDataAt(index);
                            }

                            @Override
                            @TruffleBoundary
                            public byte getDataAt(int index) {
                                try {
                                    Object value = ForeignAccess.sendRead(readNode, obj, index);
                                    return (byte) foreign2R.execute(value);
                                } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                                    throw RInternalError.shouldNotReachHere(e);
                                }
                            }

                            @Override
                            @TruffleBoundary
                            public RLogicalVector materialize() {
                                throw RInternalError.shouldNotReachHere();
                            }
                        }
                        return new RLogicalWrapper(size);
                    case INTEGER:
                        class RIntWrapper extends TruffleObjectWrapper implements RAbstractIntVector {

                            RIntWrapper(int length) {
                                super(length);
                            }

                            @Override
                            @TruffleBoundary
                            public Object getDataAtAsObject(int index) {
                                return getDataAt(index);
                            }

                            @Override
                            @TruffleBoundary
                            public int getDataAt(int index) {
                                try {
                                    Object value = ForeignAccess.sendRead(readNode, obj, index);
                                    return ((Number) foreign2R.execute(value)).intValue();
                                } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                                    throw RInternalError.shouldNotReachHere(e);
                                }
                            }

                            @Override
                            @TruffleBoundary
                            public RIntVector materialize() {
                                throw RInternalError.shouldNotReachHere();
                            }
                        }
                        return new RIntWrapper(size);
                    case DOUBLE:
                        class RDoubleWrapper extends TruffleObjectWrapper implements RAbstractDoubleVector {

                            RDoubleWrapper(int length) {
                                super(length);
                            }

                            @Override
                            @TruffleBoundary
                            public Object getDataAtAsObject(int index) {
                                return getDataAt(index);
                            }

                            @Override
                            @TruffleBoundary
                            public double getDataAt(int index) {
                                try {
                                    Object value = ForeignAccess.sendRead(readNode, obj, index);
                                    return ((Number) foreign2R.execute(value)).doubleValue();
                                } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                                    throw RInternalError.shouldNotReachHere(e);
                                }
                            }

                            @Override
                            @TruffleBoundary
                            public RDoubleVector materialize() {
                                throw RInternalError.shouldNotReachHere();
                            }
                        }
                        return new RDoubleWrapper(size);
                    case STRING:
                        return new RStringWrapper(size, obj);
                    case NONE:
                        return new RListWrapper(size, null);
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            }
            TruffleObject keys = (TruffleObject) ForeignAccess.send(keysNode, obj);
            if (keys != null) {
                int size = (Integer) ForeignAccess.sendGetSize(getSizeNode, keys);
                RAbstractStringVector abstractNames = new RStringWrapper(size, keys);
                String[] namesData = new String[size];
                boolean namesComplete = true;
                for (int i = 0; i < size; i++) {
                    namesData[i] = abstractNames.getDataAt(i);
                    namesComplete &= RRuntime.isNA(namesData[i]);
                }
                RStringVector names = RDataFactory.createStringVector(namesData, namesComplete);

                return new RListWrapper(size, names);
            }
        } catch (InteropException e) {
            // nothing to do
        }
        return obj;
    }

    private abstract static class TruffleObjectWrapper extends RAttributeStorage implements RAbstractVector {

        private final int length;

        TruffleObjectWrapper(int length) {
            this.length = length;
        }

        @Override
        public RAbstractVector copy() {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public RVector<?> copyResized(int size, boolean fillNA) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public RAbstractVector copyWithNewDimensions(int[] newDimensions) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public RVector<?> copyResizedWithDimensions(int[] newDimensions, boolean fillNA) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public RAbstractVector copyDropAttributes() {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public RVector<?> createEmptySameType(int newLength, boolean newIsComplete) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public boolean isMatrix() {
            return false;
        }

        @Override
        public boolean isArray() {
            return false;
        }

        @Override
        public boolean checkCompleteness() {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public void setComplete(boolean complete) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public boolean isComplete() {
            return false;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public RAbstractContainer resize(int size) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public boolean hasDimensions() {
            return false;
        }

        @Override
        public int[] getDimensions() {
            return null;
        }

        @Override
        public void setDimensions(int[] newDimensions) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public RTypedValue getNonShared() {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public RStringVector getNames() {
            return null;
        }

        @Override
        public final void setNames(RStringVector newNames) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public RList getDimNames() {
            return null;
        }

        @Override
        public void setDimNames(RList newDimNames) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public Object getRowNames() {
            return null;
        }

        @Override
        public void setRowNames(RAbstractVector rowNames) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public ForeignAccess getForeignAccess() {
            throw RInternalError.shouldNotReachHere();
        }
    }

}
