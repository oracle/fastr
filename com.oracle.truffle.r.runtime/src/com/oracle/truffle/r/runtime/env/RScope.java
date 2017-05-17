/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.env;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.metadata.ScopeProvider.AbstractScope;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.VirtualEvalFrame;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;

/**
 * Represents a variable scope for external tools like a debugger.<br>
 * This is basically a view on R environments.
 */
public final class RScope extends AbstractScope {

    private final Node current;
    private REnvironment env;

    /**
     * Intended to be used when creating a parent scope where we do not know any associated node.
     */
    private RScope(REnvironment env) {
        this.env = env;
        this.current = null;
    }

    private RScope(Node current, REnvironment env) {
        this.current = current;
        this.env = env;
    }

    @Override
    protected String getName() {
        // TODO
        return "function";
    }

    @Override
    protected Node getNode() {
        return current;
    }

    @Override
    protected Object getVariables(Frame frame) {
        return new VariablesMapObject(env, false);
    }

    private static MaterializedFrame getCallerFrame(Frame frame) {

        MaterializedFrame funFrame = RArguments.getCallerFrame(frame);
        if (funFrame == null) {
            Frame callerFrame = Utils.getCallerFrame(frame, FrameInstance.FrameAccess.MATERIALIZE);
            if (callerFrame != null) {
                return callerFrame.materialize();
            } else {
                // S3 method can be dispatched from top-level where there is no caller frame
                return frame.materialize();
            }
        }
        return funFrame;
    }

    private static REnvironment getEnv(Frame frame) {
        // TODO deopt frame
// PromiseDeoptimizeFrameNode deoptFrameNode = new PromiseDeoptimizeFrameNode();

        MaterializedFrame matFrame = getCallerFrame(frame);
        matFrame = matFrame instanceof VirtualEvalFrame ? ((VirtualEvalFrame) matFrame).getOriginalFrame() : matFrame;
// deoptFrameNode.deoptimizeFrame(matFrame);
        return REnvironment.frameToEnvironment(matFrame);
    }

    @Override
    protected Object getArguments(Frame frame) {
        return new VariablesMapObject(env, true);
    }

    @Override
    protected AbstractScope findParent() {
        if (this.env == REnvironment.emptyEnv()) {
            return null;
        }

        return new RScope(env.getParent());
    }

    private static String[] ls(REnvironment env) {
        RStringVector ls = env.ls(true, null, false);
        return ls.getDataWithoutCopying();
    }

    private static String[] collectArgs(REnvironment env) {
        ArgumentsSignature signature = RArguments.getSignature(env.getFrame());
        return signature.getNames();
    }

    public static RScope createScope(Node node, Frame frame) {
        return new RScope(node.getRootNode(), getEnv(frame));
    }

    private static Object getInteropValue(Object value) {
        return value;
    }

    static final class VariablesMapObject implements TruffleObject {

        private final REnvironment env;
        private final boolean argumentsOnly;

        private VariablesMapObject(REnvironment env, boolean argumentsOnly) {
            this.env = env;
            this.argumentsOnly = argumentsOnly;
        }

        @Override
        public ForeignAccess getForeignAccess() {
            return VariablesMapMessageResolutionForeign.ACCESS;
        }

        public static boolean isInstance(TruffleObject obj) {
            return obj instanceof VariablesMapObject;
        }

        @MessageResolution(receiverType = VariablesMapObject.class)
        static final class VariablesMapMessageResolution {

            @Resolve(message = "KEYS")
            abstract static class VarsMapKeysNode extends Node {

                @TruffleBoundary
                public Object access(VariablesMapObject varMap) {
                    if (varMap.argumentsOnly) {
                        return new ArgumentNamesObject(collectArgs(varMap.env));
                    } else {
                        return new VariableNamesObject(varMap.env);
                    }
                }
            }

            @Resolve(message = "KEY_INFO")
            public abstract static class VarMapsKeyInfoNode extends Node {

                private static final int READABLE = 1 << 1;
                private static final int WRITABLE = 1 << 2;
                private static final int INVOCABLE = 1 << 3;

                @SuppressWarnings("try")
                protected Object access(VariablesMapObject receiver, String identifier) {
                    int info = READABLE;

                    if (!receiver.env.bindingIsLocked(identifier)) {
                        info += WRITABLE;
                    }
                    if (receiver.env.get(identifier) instanceof RFunction) {
                        info += INVOCABLE;
                    }
                    return info;
                }
            }

            @Resolve(message = "READ")
            abstract static class VarsMapReadNode extends Node {

                @TruffleBoundary
                public Object access(VariablesMapObject varMap, String name) {
                    if (varMap.env == null) {
                        throw UnsupportedMessageException.raise(Message.READ);
                    }
                    Object value = varMap.env.get(name);

                    // If Java-null is returned, the identifier does not exist !
                    if (value == null) {
                        throw UnknownIdentifierException.raise(name);
                    } else {
                        return getInteropValue(value);
                    }
                }
            }

            @Resolve(message = "WRITE")
            abstract static class VarsMapWriteNode extends Node {

                @TruffleBoundary
                public Object access(VariablesMapObject varMap, String name, Object value) {
                    if (varMap.env == null) {
                        throw UnsupportedMessageException.raise(Message.WRITE);
                    }
                    try {
                        varMap.env.put(name, value);
                        return value;
                    } catch (PutException e) {
                        throw RInternalError.shouldNotReachHere(e);
                    }
                }
            }

        }
    }

    static final class VariableNamesObject implements TruffleObject {

        private final REnvironment env;

        private VariableNamesObject(REnvironment env) {
            this.env = env;
        }

        @Override
        public ForeignAccess getForeignAccess() {
            return VariableNamesMessageResolutionForeign.ACCESS;
        }

        public static boolean isInstance(TruffleObject obj) {
            return obj instanceof VariableNamesObject;
        }

        @MessageResolution(receiverType = VariableNamesObject.class)
        static final class VariableNamesMessageResolution {

            @Resolve(message = "HAS_SIZE")
            abstract static class VarNamesHasSizeNode extends Node {

                @SuppressWarnings("unused")
                public Object access(VariableNamesObject varNames) {
                    return true;
                }
            }

            @Resolve(message = "GET_SIZE")
            abstract static class VarNamesGetSizeNode extends Node {

                public Object access(VariableNamesObject varNames) {
                    return ls(varNames.env).length;
                }
            }

            @Resolve(message = "READ")
            abstract static class VarNamesReadNode extends Node {

                @TruffleBoundary
                public Object access(VariableNamesObject varNames, int index) {
                    String[] names = ls(varNames.env);
                    if (index >= 0 && index < names.length) {
                        return names[index];
                    } else {
                        throw UnknownIdentifierException.raise(Integer.toString(index));
                    }
                }
            }

        }
    }

    static final class ArgumentNamesObject implements TruffleObject {

        private final String[] names;

        private ArgumentNamesObject(String[] names) {
            this.names = names;
        }

        @Override
        public ForeignAccess getForeignAccess() {
            return VariableNamesMessageResolutionForeign.ACCESS;
        }

        public static boolean isInstance(TruffleObject obj) {
            return obj instanceof ArgumentNamesObject;
        }

        @MessageResolution(receiverType = ArgumentNamesObject.class)
        static final class ArgumentNamesMessageResolution {

            @Resolve(message = "HAS_SIZE")
            abstract static class ArgNamesHasSizeNode extends Node {

                @SuppressWarnings("unused")
                public Object access(ArgumentNamesObject varNames) {
                    return true;
                }
            }

            @Resolve(message = "GET_SIZE")
            abstract static class ArgNamesGetSizeNode extends Node {

                public Object access(ArgumentNamesObject varNames) {
                    return varNames.names.length;
                }
            }

            @Resolve(message = "READ")
            abstract static class ArgNamesReadNode extends Node {

                @TruffleBoundary
                public Object access(ArgumentNamesObject varNames, int index) {
                    if (index >= 0 && index < varNames.names.length) {
                        return varNames.names[index];
                    } else {
                        throw UnknownIdentifierException.raise(Integer.toString(index));
                    }
                }
            }

        }
    }

}
