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
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;

/**
 * Represents a variable scope for external tools like a debugger.<br>
 * This is basically a view on R environments.
 */
public class RScope extends AbstractScope {

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
        return new VariablesMapObject(collectVars(), collectArgs(), env);
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
        return new VariablesMapObject(collectVars(), collectArgs(), env);
    }

    @Override
    protected AbstractScope findParent() {
        if (this.env == REnvironment.emptyEnv()) {
            return null;
        }

        return new RScope(env.getParent());
    }

    private String[] collectVars() {
        RStringVector ls = env.ls(true, null, false);
        return ls.getDataWithoutCopying();
    }

    private String[] collectArgs() {
        ArgumentsSignature signature = RArguments.getSignature(env.getFrame());
        return signature.getNames();
    }

    public static RScope createScope(Node node, Frame frame) {
        return new RScope(node, getEnv(frame));
    }

    private static Object getInteropValue(Object value) {
        return value;
    }

    static final class VariablesMapObject implements TruffleObject {

        final String[] identifiers;
        final String[] args;
        final REnvironment frame;

        private VariablesMapObject(String[] identifiers, String[] args, REnvironment frame) {
            this.identifiers = identifiers;
            this.args = args;
            this.frame = frame;
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
                    return new VariableNamesObject(varMap.identifiers);
                }
            }

            @Resolve(message = "READ")
            abstract static class VarsMapReadNode extends Node {

                @TruffleBoundary
                public Object access(VariablesMapObject varMap, String name) {
                    if (varMap.frame == null) {
                        throw UnsupportedMessageException.raise(Message.READ);
                    }
                    Object value = varMap.frame.get(name);

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
                    if (varMap.frame == null) {
                        throw UnsupportedMessageException.raise(Message.WRITE);
                    }
                    try {
                        varMap.frame.put(name, value);
                        return value;
                    } catch (PutException e) {
                        throw RInternalError.shouldNotReachHere(e);
                    }
                }
            }

        }
    }

    static final class VariableNamesObject implements TruffleObject {

        final String[] names;

        private VariableNamesObject(String[] names) {
            this.names = names;
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
                    return varNames.names.length;
                }
            }

            @Resolve(message = "READ")
            abstract static class VarNamesReadNode extends Node {

                @TruffleBoundary
                public Object access(VariableNamesObject varNames, int index) {
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
