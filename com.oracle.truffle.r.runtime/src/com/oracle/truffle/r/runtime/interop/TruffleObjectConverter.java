/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RForeignBooleanWrapper;
import com.oracle.truffle.r.runtime.data.RForeignDoubleWrapper;
import com.oracle.truffle.r.runtime.data.RForeignIntWrapper;
import com.oracle.truffle.r.runtime.data.RForeignListWrapper;
import com.oracle.truffle.r.runtime.data.RForeignStringWrapper;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromListAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromListAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

public final class TruffleObjectConverter {

    public static final Object UNREADABLE = new Object();

    public static Node[] getSubNodes() {
        return new Node[]{getInterop(), Foreign2R.getUncached()};
    }

    @TruffleBoundary
    public static Object convert(TruffleObject obj) {
        try {
            if (getInterop().hasArrayElements(obj)) {
                int size = RRuntime.getForeignArraySize(obj, getInterop());
                ForeignTypeCheck typeCheck = new ForeignTypeCheck();
                for (int i = 0; i < size; i++) {
                    Object value = getInterop().readArrayElement(obj, i);
                    if (typeCheck.check(Foreign2R.getUncached().convert(value)) == RType.List) {
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
                TruffleObject classStatic = null;
                String[] staticNames = null;
                if (!ConvertForeignObjectNode.isForeignArray(obj, getInterop())) {
                    try {
                        classStatic = ToJavaStaticNode.getUncached().execute(obj);
                        staticNames = classStatic != null ? readMembers(classStatic) : null;
                    } catch (UnknownIdentifierException | NoSuchFieldError | UnsupportedMessageException e) {
                    }
                }

                String[] names;
                try {
                    names = readMembers(obj);
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

    private static String[] readMembers(TruffleObject obj)
                    throws InteropException, UnsupportedMessageException {
        InteropLibrary interop = getInterop();
        Object members = interop.getMembers(obj);
        int size = RRuntime.getForeignArraySize(members, getInterop());
        String[] names = new String[size];
        for (int i = 0; i < size; i++) {
            Object value = interop.readArrayElement(members, i);
            names[i] = (String) value;
        }
        return names;
    }

    private static final class CompoundNamedListWrapper extends RAbstractListVector {

        private TruffleObject classDelegate;

        private TruffleObject delegate;

        private int staticNamesLen;

        private RStringVector names;

        CompoundNamedListWrapper(TruffleObject classDelegate, TruffleObject delegate, int staticNamesLen, RStringVector names) {
            super(RDataFactory.INCOMPLETE_VECTOR);
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
                return (index < staticNamesLen) ? read(getInterop(), classDelegate, names, index) : read(getInterop(), delegate, names, index);
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }

        private static final class FastPathAccess extends FastPathFromListAccess {

            @Child private InteropLibrary delegateInterop;
            @Child private InteropLibrary classDelegateInterop;
            @Child private Foreign2R foreign2r = Foreign2R.create();

            FastPathAccess(RAbstractContainer value) {
                super(value);
                CompoundNamedListWrapper wrapper = (CompoundNamedListWrapper) value;
                delegateInterop = InteropLibrary.getFactory().create(wrapper.delegate);
                if (wrapper.classDelegate != null) {
                    classDelegateInterop = InteropLibrary.getFactory().create(wrapper.classDelegate);
                }
            }

            @Override
            public boolean supports(Object value) {
                if (!(value instanceof CompoundNamedListWrapper)) {
                    return false;
                }
                CompoundNamedListWrapper wrapper = (CompoundNamedListWrapper) value;
                if (wrapper.classDelegate == null) {
                    return super.supports(value) && delegateInterop.accepts(wrapper.delegate);
                } else {
                    return super.supports(value) && delegateInterop.accepts(wrapper.delegate) && classDelegateInterop.accepts(wrapper.classDelegate);
                }

            }

            @Override
            public RType getType() {
                return RType.List;
            }

            @Override
            protected int getLength(RAbstractContainer vector) {
                return vector.getLength();
            }

            @Override
            protected Object getListElementImpl(AccessIterator accessIter, int index) {
                try {
                    CompoundNamedListWrapper wrapper = (CompoundNamedListWrapper) accessIter.getStore();
                    return (index < wrapper.staticNamesLen)
                                    ? foreign2r.convert(read(classDelegateInterop, wrapper.classDelegate, wrapper.names, index))
                                    : foreign2r.convert(read(delegateInterop, wrapper.delegate, wrapper.names, index));
                } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }
        }

        @Override
        public VectorAccess access() {
            return new FastPathAccess(this);
        }

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
                                    ? read(getInterop(), vector.classDelegate, vector.names, index)
                                    : read(getInterop(), vector.delegate, vector.names, index);
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

    private static Object read(InteropLibrary interop, TruffleObject obj, RStringVector names, int index) throws UnsupportedMessageException, UnknownIdentifierException {
        String memberName = names.getDataAt(index);
        if (interop.isMemberReadable(obj, memberName)) {
            return Foreign2R.getUncached().convert(interop.readMember(obj, memberName));
        } else {
            return UNREADABLE;
        }
    }

    private static InteropLibrary getInterop() {
        return InteropLibrary.getFactory().getUncached();
    }

}
