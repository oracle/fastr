/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.ForeignAccess.StandardFactory;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RObject;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
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

            public Object access(VectorRFFIWrapper receiver, Object index) {
                try {
                    return ForeignAccess.sendRead(readMsg, receiver.vector, index);
                } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }
        }

        @Resolve(message = "WRITE")
        abstract static class VectorWrapperWriteNode extends Node {
            @Child private Node writeMsg = Message.WRITE.createNode();

            public Object access(VectorRFFIWrapper receiver, Object index, Object value) {
                try {
                    return ForeignAccess.sendWrite(writeMsg, receiver.vector, index, value);
                } catch (UnsupportedMessageException | UnknownIdentifierException | UnsupportedTypeException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
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
            @Child private Node execMsg = Message.createExecute(0).createNode();

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

}
