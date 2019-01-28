/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.ForeignAccess.StandardFactory;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RObject;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.ffi.VectorRFFIWrapperFactory.AtomicVectorGetterNodeGen;
import com.oracle.truffle.r.runtime.ffi.VectorRFFIWrapperFactory.AtomicVectorSetterNodeGen;
import com.oracle.truffle.r.runtime.ffi.VectorRFFIWrapperFactory.NumberToIntNodeGen;
import com.oracle.truffle.r.runtime.ffi.VectorRFFIWrapperFactory.VectorRFFIWrapperNativePointerFactory.DispatchAllocateNodeGen;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public final class VectorRFFIWrapper implements TruffleObject {

    private final TruffleObject vector;

    public VectorRFFIWrapper(TruffleObject vector) {
        assert vector instanceof RObject;
        this.vector = vector;
        NativeDataAccess.setNativeWrapper((RObject) vector, this);
    }

    public static VectorRFFIWrapper get(TruffleObject x) {
        assert x instanceof RObject;
        Object wrapper = NativeDataAccess.getNativeWrapper((RObject) x);
        if (wrapper != null) {
            assert wrapper instanceof VectorRFFIWrapper;
            return (VectorRFFIWrapper) wrapper;
        } else {
            wrapper = new VectorRFFIWrapper(x);
            // Establish the 1-1 relationship between the object and its native wrapper
            NativeDataAccess.setNativeWrapper((RObject) x, wrapper);
            return (VectorRFFIWrapper) wrapper;
        }
    }

    public TruffleObject getVector() {
        return vector;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return VectorRFFIWrapperMRForeign.ACCESS;
    }

    @Override
    public int hashCode() {
        return vector.hashCode();
    }

    public static class VectorRFFIWrapperNativePointer implements TruffleObject {

        private final TruffleObject vector;

        VectorRFFIWrapperNativePointer(TruffleObject vector) {
            this.vector = vector;
            assert vector instanceof RObject;
            NativeDataAccess.asPointer(vector); // initialize the native mirror in the vector
        }

        abstract static class InteropRootNode extends RootNode {
            InteropRootNode() {
                super(/* TruffleRLanguageImpl.getCurrentLanguage() */null);
            }

            @Override
            public final SourceSection getSourceSection() {
                return RSyntaxNode.INTERNAL;
            }
        }

        // TODO: with separate version of this for the different types, it would be more efficient
        // and not need the dispatch
        public abstract static class DispatchAllocate extends Node {
            private static final long EMPTY_DATA_ADDRESS = 0x1BAD;

            public abstract long execute(Object vector);

            @Specialization
            @TruffleBoundary
            protected static long get(RList list) {
                return list.allocateNativeContents();
            }

            @Specialization
            @TruffleBoundary
            protected static long get(RIntVector vector) {
                return vector.allocateNativeContents();
            }

            @Specialization
            @TruffleBoundary
            protected static long get(RLogicalVector vector) {
                return vector.allocateNativeContents();
            }

            @Specialization
            @TruffleBoundary
            protected static long get(RRawVector vector) {
                return vector.allocateNativeContents();
            }

            @Specialization
            @TruffleBoundary
            protected static long get(RDoubleVector vector) {
                return vector.allocateNativeContents();
            }

            @Specialization
            @TruffleBoundary
            protected static long get(RComplexVector vector) {
                return vector.allocateNativeContents();
            }

            @Specialization
            @TruffleBoundary
            protected static long get(RStringVector vector) {
                return vector.allocateNativeContents();
            }

            @Specialization
            @TruffleBoundary
            protected static long get(CharSXPWrapper vector) {
                return vector.allocateNativeContents();
            }

            @Specialization
            protected static long get(@SuppressWarnings("unused") RNull nullValue) {
                // Note: GnuR is OK with, e.g., INTEGER(NULL), but it's illegal to read from or
                // write to the resulting address.
                return EMPTY_DATA_ADDRESS;
            }

            @Fallback
            protected static long get(Object vector) {
                throw RInternalError.shouldNotReachHere("invalid wrapped object " + vector.getClass().getSimpleName());
            }
        }

        @Override
        public ForeignAccess getForeignAccess() {
            return ForeignAccess.create(VectorRFFIWrapperNativePointer.class, new StandardFactory() {
                @Override
                public CallTarget accessIsNull() {
                    return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
                        @Override
                        public Object execute(VirtualFrame frame) {
                            return false;
                        }
                    });
                }

                @Override
                public CallTarget accessIsPointer() {
                    return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
                        @Override
                        public Object execute(VirtualFrame frame) {
                            return true;
                        }
                    });
                }

                @Override
                public CallTarget accessAsPointer() {
                    return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
                        @Child private DispatchAllocate dispatch = DispatchAllocateNodeGen.create();

                        @Override
                        public Object execute(VirtualFrame frame) {
                            VectorRFFIWrapperNativePointer receiver = (VectorRFFIWrapperNativePointer) ForeignAccess.getReceiver(frame);
                            return dispatch.execute(receiver.vector);
                        }
                    });
                }

                @Override
                public CallTarget accessToNative() {
                    return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
                        @Override
                        public Object execute(VirtualFrame frame) {
                            return ForeignAccess.getReceiver(frame);
                        }
                    });
                }
            });
        }
    }

    @MessageResolution(receiverType = VectorRFFIWrapper.class)
    public static class VectorRFFIWrapperMR {

        @Resolve(message = "IS_POINTER")
        public abstract static class IntVectorWrapperNativeIsPointerNode extends Node {
            protected Object access(@SuppressWarnings("unused") VectorRFFIWrapper receiver) {
                return false;
            }
        }

        @Resolve(message = "TO_NATIVE")
        public abstract static class IntVectorWrapperNativeAsPointerNode extends Node {
            protected Object access(VectorRFFIWrapper receiver) {
                return new VectorRFFIWrapperNativePointer(receiver.vector);
            }
        }

        @Resolve(message = "HAS_SIZE")
        public abstract static class VectorWrapperHasSizeNode extends Node {
            protected Object access(@SuppressWarnings("unused") VectorRFFIWrapper receiver) {
                return true;
            }
        }

        @Resolve(message = "GET_SIZE")
        public abstract static class VectorWrapperGetSizeNode extends Node {
            @Child private Node getSizeMsg = Message.GET_SIZE.createNode();

            protected Object access(VectorRFFIWrapper receiver) {
                try {
                    return ForeignAccess.sendGetSize(getSizeMsg, receiver.vector);
                } catch (UnsupportedMessageException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }
        }

        @Resolve(message = "READ")
        abstract static class VectorWrapperReadNode extends Node {
            @Child private Node readMsg = Message.READ.createNode();
            @Child private NumberToInt getIndexNode = NumberToIntNodeGen.create();
            @Child private AtomicVectorGetterNode getElemNode = AtomicVectorGetterNodeGen.create();

            public Object access(VectorRFFIWrapper receiver, Object index) {
                int i = getIndexNode.executeInteger(index);
                return getElemNode.execute(receiver.vector, i);
            }
        }

        @Resolve(message = "WRITE")
        abstract static class VectorWrapperWriteNode extends Node {
            @Child private Node writeMsg = Message.WRITE.createNode();
            @Child private NumberToInt getIndexNode = NumberToIntNodeGen.create();
            @Child private AtomicVectorSetterNode setElemNode = AtomicVectorSetterNodeGen.create();

            public Object access(VectorRFFIWrapper receiver, Object index, Object value) {
                int ind = getIndexNode.executeInteger(index);
                return setElemNode.execute(receiver.vector, ind, value);
            }
        }

        @Resolve(message = "IS_EXECUTABLE")
        abstract static class VectorWrapperIsExecutableNode extends Node {
            @Child private Node isExecMsg = Message.IS_EXECUTABLE.createNode();

            public Object access(VectorRFFIWrapper receiver) {
                return ForeignAccess.sendIsExecutable(isExecMsg, receiver.vector);
            }
        }

        @Resolve(message = "EXECUTE")
        abstract static class VectorWrapperExecuteNode extends Node {
            @Child private Node execMsg = Message.EXECUTE.createNode();

            protected Object access(VectorRFFIWrapper receiver, Object[] arguments) {
                try {
                    // Currently, there is only one "executable" object, which is
                    // CharSXPWrapper.
                    // See CharSXPWrapperMR for the EXECUTABLE message handler.
                    assert arguments.length == 0 && receiver.vector instanceof CharSXPWrapper;
                    return ForeignAccess.sendExecute(execMsg, receiver.vector);
                } catch (UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }
        }

        @CanResolve
        public abstract static class VectorWrapperCheck extends Node {
            protected static boolean test(TruffleObject receiver) {
                return receiver instanceof VectorRFFIWrapper;
            }
        }
    }

    public abstract static class NumberToInt extends Node {

        public abstract int executeInteger(Object value);

        @Specialization
        protected int doInt(int x) {
            return x;
        }

        @Specialization
        protected int doLong(long x) {
            return (int) x;
        }

        @Specialization
        protected int doDouble(double x) {
            return (int) x;
        }
    }

    public abstract static class AtomicVectorGetterNode extends Node {
        @Child private Node readMsgNode;

        public abstract Object execute(Object vector, int index);

        @Specialization
        protected Object doStringVector(RStringVector vector, int index) {
            vector.wrapStrings();
            // TODO: for now character vector shouldn't return plain java.lang.String,
            // otherwise we'd need to make sure that all the places that expect CharSXP
            // can also deal with java.lang.String
            return vector.getWrappedDataAt(index);
        }

        @Specialization
        protected Object doIntVector(RIntVector vector, int index) {
            return vector.getDataAt(index);
        }

        @Specialization
        protected Object doDoubleVector(RDoubleVector vector, int index) {
            return vector.getDataAt(index);
        }

        @Specialization
        protected Object doComplexVector(RComplexVector vector, int index) {
            return vector.getComplexPartAt(index);
        }

        @Specialization
        protected Object doRawVector(RRawVector vector, int index) {
            return vector.getRawDataAt(index);
        }

        @Specialization
        protected Object doLogicalVector(RLogicalVector vector, int index,
                        @Cached("create()") BranchProfile naProfile) {
            byte ret = vector.getDataAt(index);
            if (ret == RRuntime.LOGICAL_NA) {
                naProfile.enter();
                return RRuntime.INT_NA;
            }
            return ret;
        }

        @Specialization
        protected Object doList(RList vector, int index) {
            return vector.getDataAt(index);
        }

        @Fallback
        protected Object doOther(Object target, int index) {
            assert target instanceof TruffleObject;
            try {
                if (readMsgNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    readMsgNode = insert(Message.READ.createNode());
                }
                return ForeignAccess.sendRead(readMsgNode, (TruffleObject) target, index);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }

    }

    public abstract static class AtomicVectorSetterNode extends Node {
        @Child private Node writeMsgNode;

        public abstract Object execute(Object vector, int index, Object value);

        @Specialization
        protected Object doIntVector(RIntVector vector, int index, int value, @Cached("create()") BranchProfile naProfile) {
            if (RRuntime.isNA(value)) {
                naProfile.enter();
                vector.setComplete(false);
            }
            vector.setDataAt(vector.getInternalStore(), index, value);
            return vector;
        }

        @Specialization
        protected Object doDoubleVector(RDoubleVector vector, int index, double value, @Cached("create()") BranchProfile naProfile) {
            if (RRuntime.isNA(value)) {
                naProfile.enter();
                vector.setComplete(false);
            }
            vector.setDataAt(vector.getInternalStore(), index, value);
            return vector;
        }

        @Specialization
        protected Object doRawVector(RRawVector vector, int index, byte value) {
            vector.setRawDataAt(vector.getInternalStore(), index, value);
            return vector;
        }

        @Specialization
        protected Object doLogicalVector(RLogicalVector vector, int index, int value,
                        @Cached("createBinaryProfile()") ConditionProfile booleanProfile,
                        @Cached("create()") BranchProfile naProfile) {
            if (RRuntime.isNA(value)) {
                naProfile.enter();
                vector.setComplete(false);
                vector.setDataAt(vector.getInternalStore(), index, RRuntime.LOGICAL_NA);
                return vector;
            }
            vector.setDataAt(vector.getInternalStore(), index, booleanProfile.profile(value == 0) ? RRuntime.LOGICAL_FALSE : RRuntime.LOGICAL_TRUE);
            return vector;
        }

        @Specialization
        protected Object doList(RList vector, int index, Object value,
                        @Cached("createBinaryProfile()") ConditionProfile lookupProfile) {
            Object usedValue = value;
            if (lookupProfile.profile(value instanceof Long)) {
                usedValue = NativeDataAccess.lookup((long) value);
            }
            vector.setDataAt(index, usedValue);
            return vector;
        }

        @Specialization
        protected Object doStringVector(RStringVector vector, int index, long value, @Cached("create()") BranchProfile naProfile) {
            Object usedValue = value;
            usedValue = NativeDataAccess.lookup(value);
            assert usedValue instanceof CharSXPWrapper;
            if (RRuntime.isNA(((CharSXPWrapper) usedValue).getContents())) {
                naProfile.enter();
                vector.setComplete(false);
            }
            vector.setWrappedDataAt(index, (CharSXPWrapper) usedValue);
            return vector;
        }

        @Specialization
        protected Object doStringVector(RStringVector vector, int index, CharSXPWrapper value, @Cached("create()") BranchProfile naProfile) {
            if (RRuntime.isNA(value.getContents())) {
                naProfile.enter();
                vector.setComplete(false);
            }
            vector.setWrappedDataAt(index, value);
            return vector;
        }

        @Specialization
        protected Object doComplexVector(RComplexVector vector, int index, double value, @Cached("create()") BranchProfile naProfile) {
            if (RRuntime.isNA(value)) {
                naProfile.enter();
                vector.setComplete(false);
            }
            vector.setDataAt(vector.getInternalStore(), index, value);
            return vector;
        }

        @Fallback
        protected Object doOther(Object target, int index, Object value) {
            assert target instanceof TruffleObject;
            try {
                if (writeMsgNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    writeMsgNode = insert(Message.WRITE.createNode());
                }
                return ForeignAccess.sendWrite(writeMsgNode, (TruffleObject) target, index, value);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

}
