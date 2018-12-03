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
package com.oracle.truffle.r.runtime.interop;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RForeignBooleanWrapper;
import com.oracle.truffle.r.runtime.data.RForeignDoubleWrapper;
import com.oracle.truffle.r.runtime.data.RForeignIntWrapper;
import com.oracle.truffle.r.runtime.data.RForeignListWrapper;
import com.oracle.truffle.r.runtime.data.RForeignStringWrapper;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromListAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromListAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

public final class TruffleObjectConverter {

    private Node hasSizeNode = Message.HAS_SIZE.createNode();
    private Node getSizeNode = Message.GET_SIZE.createNode();
    private Node executeNode = Message.EXECUTE.createNode();
    private Node readNode = Message.READ.createNode();
    private Node keysNode = Message.KEYS.createNode();
    private Foreign2R f2r = Foreign2R.create();

    public Node[] getSubNodes() {
        return new Node[]{hasSizeNode, getSizeNode, executeNode, readNode, keysNode, f2r};
    }

    @TruffleBoundary
    public Object convert(TruffleObject obj) {
        try {
            if (ForeignAccess.sendHasSize(hasSizeNode, obj)) {
                int size = (Integer) ForeignAccess.sendGetSize(getSizeNode, obj);
                ForeignTypeCheck typeCheck = new ForeignTypeCheck();
                for (int i = 0; i < size; i++) {
                    Object value = ForeignAccess.sendRead(readNode, obj, i);
                    if (typeCheck.check(f2r.execute(value)) == RType.List) {
                        break;
                    }
                }
                switch (typeCheck.getType()) {
                    case Logical:
                        return new RForeignBooleanWrapper(obj);
                    case Integer:
                        return new RForeignIntWrapper(obj);
                    case Double:
                        return new RForeignDoubleWrapper(obj);
                    case Character:
                        return new RForeignStringWrapper(obj);
                    case List:
                    case Null:
                        return new RForeignListWrapper(obj);
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            }

            try {
                RContext context = RContext.getInstance();
                TruffleLanguage.Env env = context.getEnv();
                TruffleObject classStatic = null;
                String[] staticNames = null;
                if (env.isHostObject(obj) && !(env.asHostObject(obj) instanceof Class)) {
                    if (!ConvertForeignObjectNode.isForeignArray(obj, hasSizeNode)) {
                        try {
                            classStatic = context.toJavaStatic(obj, readNode, executeNode);
                            staticNames = readKeys(classStatic);
                        } catch (UnknownIdentifierException | NoSuchFieldError | UnsupportedMessageException e) {
                        }
                    }
                }

                String[] names;
                try {
                    names = readKeys(obj);
                } catch (UnsupportedMessageException e) {
                    names = null;
                }

                int staticNamesLen;
                String[] compoundNames;
                if (staticNames == null) {
                    staticNamesLen = 0;
                    compoundNames = (names != null) ? names : new String[0];
                } else if (names == null) {
                    staticNamesLen = staticNames.length;
                    compoundNames = staticNames;
                } else {
                    staticNamesLen = staticNames.length;
                    compoundNames = new String[staticNamesLen + names.length];
                    System.arraycopy(staticNames, 0, compoundNames, 0, staticNamesLen);
                    System.arraycopy(names, 0, compoundNames, staticNamesLen, names.length);
                }
                RStringVector compoundNamesVec = RDataFactory.createStringVector(compoundNames, true);
                return new CompoundNamedListWrapper(classStatic, obj, staticNamesLen, compoundNamesVec);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }

        } catch (InteropException e) {
            // nothing to do
        }
        return obj;
    }

    private String[] readKeys(TruffleObject obj)
                    throws UnknownIdentifierException, InteropException, UnsupportedMessageException {
        TruffleObject keys = ForeignAccess.sendKeys(keysNode, obj);
        int size = (Integer) ForeignAccess.sendGetSize(getSizeNode, keys);
        String[] names = new String[size];
        for (int i = 0; i < size; i++) {
            Object value = ForeignAccess.sendRead(readNode, keys, i);
            names[i] = (String) value;
        }
        return names;
    }

    private static final class CompoundNamedListWrapper implements RAbstractListVector {

        private static final Node READ = Message.READ.createNode();

        private TruffleObject classDelegate;

        private TruffleObject delegate;

        private int staticNamesLen;

        private RStringVector names;

        CompoundNamedListWrapper(TruffleObject classDelegate, TruffleObject delegate, int staticNamesLen, RStringVector names) {
            this.classDelegate = classDelegate;
            this.delegate = delegate;
            this.staticNamesLen = staticNamesLen;
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
        public void setNames(RStringVector newNames) {
            // should only be used on materialized sequence
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public RList getDimNames() {
            return null;
        }

        @Override
        public void setDimNames(RList newDimNames) {
            // should only be used on materialized sequence
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public Object getRowNames() {
            return RNull.instance;
        }

        @Override
        public void setRowNames(RAbstractVector rowNames) {
            // should only be used on materialized sequence
            throw RInternalError.shouldNotReachHere();
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
                return (index < staticNamesLen)
                                ? FOREIGN_TO_R.execute(ForeignAccess.sendRead(READ, classDelegate, names.getDataAt(index)))
                                : FOREIGN_TO_R.execute(ForeignAccess.sendRead(READ, delegate, names.getDataAt(index)));
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }

        @Override
        public RAbstractContainer resize(int size) {
            return materialize().resize(size);
        }

        @Override
        public boolean isComplete() {
            return false;
        }

        @Override
        public void setComplete(boolean complete) {
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
        public RAbstractVector copy() {
            return new CompoundNamedListWrapper(classDelegate, delegate, staticNamesLen, names);
        }

        @Override
        public RAbstractVector copyDropAttributes() {
            return copy();
        }

        @Override
        public RAbstractVector copyWithNewDimensions(int[] newDimensions) {
            RAbstractVector res = copy();
            res.setDimensions(newDimensions);
            return res;
        }

        @Override
        public DynamicObject initAttributes() {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public void initAttributes(DynamicObject newAttributes) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public DynamicObject getAttributes() {
            return null;
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
        public boolean isObject() {
            return false;
        }

        @Override
        public RTypedValue getNonShared() {
            return materialize().getNonShared();
        }

        @Override
        public int getTypedValueInfo() {
            return 0;
        }

        @Override
        public void setTypedValueInfo(int value) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public boolean isS4() {
            return false;
        }

        @Override
        public RVector<?> copyResized(int size, boolean fillNA) {
            RAbstractVector v = copy();
            return v.copyResized(size, fillNA);
        }

        @Override
        public RVector<?> copyResizedWithDimensions(int[] newDimensions, boolean fillNA) {
            RAbstractVector v = copy();
            return v.copyResizedWithDimensions(newDimensions, fillNA);
        }

        @Override
        public RVector<?> createEmptySameType(int newLength, boolean newIsComplete) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public void setLength(int l) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public int getTrueLength() {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public void setTrueLength(int l) {
            throw RInternalError.shouldNotReachHere();
        }

        private static final class FastPathAccess extends FastPathFromListAccess {

            FastPathAccess(RAbstractContainer value) {
                super(value);
            }

            @Child private Node read = Message.READ.createNode();
            @Child private Foreign2R foreign2r = Foreign2R.create();

            @Override
            public RType getType() {
                return RType.List;
            }

            @Override
            protected int getLength(RAbstractContainer vector) {
                return ((CompoundNamedListWrapper) vector).getLength();
            }

            @Override
            protected Object getListElementImpl(AccessIterator accessIter, int index) {
                try {
                    CompoundNamedListWrapper wrapper = (CompoundNamedListWrapper) accessIter.getStore();
                    return (index < wrapper.staticNamesLen)
                                    ? foreign2r.execute(ForeignAccess.sendRead(read, wrapper.classDelegate, wrapper.names.getDataAt(index)))
                                    : foreign2r.execute(ForeignAccess.sendRead(read, wrapper.delegate, wrapper.names.getDataAt(index)));
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
            protected int getLength(RAbstractContainer vector) {
                return ((CompoundNamedListWrapper) vector).getLength();
            }

            @Override
            protected Object getListElementImpl(AccessIterator accessIter, int index) {
                CompoundNamedListWrapper vector = (CompoundNamedListWrapper) accessIter.getStore();
                try {
                    return (index < vector.staticNamesLen)
                                    ? FOREIGN_TO_R.execute(ForeignAccess.sendRead(READ, vector.classDelegate, vector.names.getDataAt(index)))
                                    : FOREIGN_TO_R.execute(ForeignAccess.sendRead(READ, vector.delegate, vector.names.getDataAt(index)));
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

}
