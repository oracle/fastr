/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments.S3Args;
import com.oracle.truffle.r.runtime.RDispatch;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class S3FunctionLookupNode extends RBaseNode {
    protected static final int MAX_CACHE_DEPTH = 3;

    protected final boolean throwsError;
    protected final boolean nextMethod;

    private S3FunctionLookupNode(boolean throwsError, boolean nextMethod) {
        this.throwsError = throwsError;
        this.nextMethod = nextMethod;
    }

    @ValueType
    public static final class Result {
        public final String generic;
        public final RFunction function;
        public final ArgumentsSignature signature;
        public final Object clazz;
        public final String targetFunctionName;
        public final boolean groupMatch;

        private Result(String generic, RFunction function, Object clazz, String targetFunctionName, boolean groupMatch) {
            this.generic = generic.intern();
            this.function = function;
            this.signature = function == null ? null : ArgumentMatcher.getFunctionSignature(function);
            this.clazz = clazz;
            this.targetFunctionName = targetFunctionName;
            this.groupMatch = groupMatch;
        }

        public S3Args createS3Args(Frame frame) {
            return new S3Args(generic, clazz, targetFunctionName, frame.materialize(), null, null);
        }
    }

    public static S3FunctionLookupNode create(boolean throwsError, boolean nextMethod) {
        return new UseMethodFunctionLookupUninitializedNode(throwsError, nextMethod, 0);
    }

    public static S3FunctionLookupNode createWithError() {
        return new UseMethodFunctionLookupUninitializedNode(true, false, 0);
    }

    public static S3FunctionLookupNode createWithException() {
        return new UseMethodFunctionLookupUninitializedNode(false, false, 0);
    }

    @FunctionalInterface
    private interface LookupOperation {
        Object read(MaterializedFrame frame, String name, boolean inMethodsTable);
    }

    @FunctionalInterface
    private interface GetMethodsTable {
        Object get();
    }

    @TruffleBoundary
    private static Result performLookup(MaterializedFrame callerFrame, String genericName, String groupName, RStringVector type, boolean nextMethod, LookupOperation op, GetMethodsTable getTable) {
        Result result;
        Object methodsTable = getTable.get();
        if (methodsTable instanceof RPromise) {
            methodsTable = PromiseHelperNode.evaluateSlowPath(null, (RPromise) methodsTable);
        }
        MaterializedFrame methodsTableFrame = methodsTable == null ? null : ((REnvironment) methodsTable).getFrame();

        // look for a generic function reachable from the caller frame
        if ((result = lookupClassGenerics(callerFrame, methodsTableFrame, genericName, groupName, type, nextMethod, op)) != null) {
            return result;
        }

        // look for the default method
        String functionName = genericName + RRuntime.RDOT + RRuntime.DEFAULT;
        RFunction function = checkPromise(op.read(callerFrame, functionName, false));
        if (function == null && methodsTableFrame != null) {
            function = checkPromise(op.read(methodsTableFrame, functionName, true));
        }
        if (function != null) {
            return new Result(genericName, function, RNull.instance, functionName, false);
        }
        return null;
    }

    private static Result lookupClassGenerics(MaterializedFrame callerFrame, MaterializedFrame methodsTableFrame, String genericName, String groupName, RStringVector type, boolean nextMethod,
                    LookupOperation op) {
        Result result = null;
        for (int i = nextMethod ? 1 : 0; i < type.getLength(); i++) {
            String clazzName = type.getDataAt(i);
            boolean groupMatch = false;

            String functionName = genericName + RRuntime.RDOT + type.getDataAt(i);
            RFunction function = checkPromise(op.read(callerFrame, functionName, false));

            if (function == null) {
                if (methodsTableFrame != null) {
                    function = checkPromise(op.read(methodsTableFrame, functionName, true));
                }

                if (function == null && groupName != null) {
                    groupMatch = true;
                    functionName = groupName + RRuntime.RDOT + clazzName;
                    function = checkPromise(op.read(callerFrame, functionName, false));
                    if (function == null && methodsTableFrame != null) {
                        function = checkPromise(op.read(methodsTableFrame, functionName, true));
                    }
                }
            }

            if (function != null) {
                Object dispatchType;
                if (i == 0) {
                    dispatchType = type.copyResized(type.getLength(), false);
                } else {
                    RStringVector clazz = RDataFactory.createStringVector(Arrays.copyOfRange(type.getDataWithoutCopying(), i, type.getLength()), true);
                    clazz.setAttr(RRuntime.PREVIOUS_ATTR_KEY, type.copyResized(type.getLength(), false));
                    dispatchType = clazz;
                }
                result = new Result(genericName, function, dispatchType, functionName, groupMatch);
                break;
            }
        }
        return result;
    }

    private static RFunction checkPromise(Object value) {
        if (value instanceof RPromise) {
            return (RFunction) PromiseHelperNode.evaluateSlowPath(null, (RPromise) value);
        } else {
            return (RFunction) value;
        }
    }

    /**
     * Searches for the correct S3 method for given function name and vector of class names.
     *
     * @param frame
     * @param genericName The name of the generic function to look for, e.g. 'length'.
     * @param type Vector of classes, if it is e.g. 'myclass', then this will search for
     *            'length.myclass'.
     * @param group See {@link RDispatch} and R documentation on "group" dispatch.
     * @param callerFrame The frame of the caller will be starting point of the search.
     * @param genericDefFrame This frame will be searched for special variables influencing the
     *            lookup, e.g. .__S3MethodsTable__. Any other caller than {@code UseMethod} or
     *            {@code NextMethod}, should supply base environment's frame using
     *            {@link GetBaseEnvFrameNode}.
     * @return Information about the lookup result.
     */
    public abstract S3FunctionLookupNode.Result execute(VirtualFrame frame, String genericName, RStringVector type, String group, MaterializedFrame callerFrame, MaterializedFrame genericDefFrame);

    private static UseMethodFunctionLookupCachedNode specialize(VirtualFrame frame, String genericName, RStringVector type, String group, MaterializedFrame callerFrame,
                    MaterializedFrame genericDefFrame,
                    S3FunctionLookupNode next) {
        ArrayList<ReadVariableNode> unsuccessfulReadsCaller = new ArrayList<>();
        ArrayList<LocalReadVariableNode> unsuccessfulReadsTable = new ArrayList<>();
        class SuccessfulReads {
            ReadVariableNode successfulRead;
            LocalReadVariableNode successfulReadTable;
            LocalReadVariableNode methodsTableRead;
        }
        SuccessfulReads reads = new SuccessfulReads();

        LookupOperation op = (lookupFrame, name, inMethodsTable) -> {
            Object result;
            if (inMethodsTable) {
                LocalReadVariableNode read = LocalReadVariableNode.create(name, true);
                result = read.execute(frame, lookupFrame);
                if (result == null) {
                    unsuccessfulReadsTable.add(read);
                } else {
                    reads.successfulReadTable = read;
                }
            } else {
                ReadVariableNode read = ReadVariableNode.createSilent(name, RType.Function);
                result = read.execute(frame, lookupFrame);
                if (result == null) {
                    unsuccessfulReadsCaller.add(read);
                } else {
                    reads.successfulRead = read;
                }
            }
            return result;
        };

        GetMethodsTable getTable = () -> {
            if (genericDefFrame != null) {
                reads.methodsTableRead = LocalReadVariableNode.create(RRuntime.RS3MethodsTable, true);
                return reads.methodsTableRead.execute(frame, genericDefFrame);
            } else {
                return null;
            }
        };

        Result result = performLookup(callerFrame, genericName, group, type, next.nextMethod, op, getTable);

        UseMethodFunctionLookupCachedNode cachedNode;

        if (result != null) {
            cachedNode = new UseMethodFunctionLookupCachedNode(next.throwsError, next.nextMethod, genericName, type, group, null, unsuccessfulReadsCaller, unsuccessfulReadsTable,
                            reads.methodsTableRead, reads.successfulRead, reads.successfulReadTable, result.function, result.clazz, result.targetFunctionName, result.groupMatch, next);
        } else {
            RFunction builtin = next.throwsError ? builtin = RContext.lookupBuiltin(genericName) : null;
            cachedNode = new UseMethodFunctionLookupCachedNode(next.throwsError, next.nextMethod, genericName, type, group, builtin, unsuccessfulReadsCaller, unsuccessfulReadsTable,
                            reads.methodsTableRead, null, null, null, null, null, false, next);
        }
        return cachedNode;
    }

    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    private static final class UseMethodFunctionLookupUninitializedNode extends S3FunctionLookupNode {
        private final int depth;

        UseMethodFunctionLookupUninitializedNode(boolean throwsError, boolean nextMethod, int depth) {
            super(throwsError, nextMethod);
            this.depth = depth;
        }

        @Override
        public Result execute(VirtualFrame frame, String genericName, RStringVector type, String group, MaterializedFrame callerFrame, MaterializedFrame genericDefFrame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (depth > MAX_CACHE_DEPTH) {
                return replace(new UseMethodFunctionLookupGenericNode(throwsError, nextMethod)).execute(frame, genericName, type, group, callerFrame, genericDefFrame);
            } else {
                UseMethodFunctionLookupCachedNode cachedNode = replace(
                                specialize(frame, genericName, type, group, callerFrame, genericDefFrame, new UseMethodFunctionLookupUninitializedNode(throwsError, nextMethod, depth + 1)));
                return cachedNode.execute(frame, genericName, type, group, callerFrame, genericDefFrame);
            }
        }
    }

    private static final class UseMethodFunctionLookupCachedNode extends S3FunctionLookupNode {

        @Child private S3FunctionLookupNode next;

        @CompilationFinal private final String[] cachedTypeContents;
        @Children private final ReadVariableNode[] unsuccessfulReadsCallerFrame;
        @Child private LocalReadVariableNode readS3MethodsTable;
        @Children private final LocalReadVariableNode[] unsuccessfulReadsTable;
        // if unsuccessfulReadsDefFrame != null, then this read will go to the def frame
        @Child private ReadVariableNode successfulRead;
        @Child private LocalReadVariableNode successfulReadTable;

        private final String cachedGenericName;
        private final RStringVector cachedType;

        private final Result result;

        private final ConditionProfile sameIdentityProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile genericIdentityProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile nullTypeProfile = BranchProfile.create();
        private final BranchProfile lengthMismatch = BranchProfile.create();
        private final BranchProfile notIdentityEqualElements = BranchProfile.create();
        private final ValueProfile methodsTableProfile = ValueProfile.createIdentityProfile();
        private final String cachedGroup;
        private final RFunction builtin;

        UseMethodFunctionLookupCachedNode(boolean throwsError, boolean nextMethod, String genericName, RStringVector type, String group, RFunction builtin,
                        List<ReadVariableNode> unsuccessfulReadsCaller, List<LocalReadVariableNode> unsuccessfulReadsTable, LocalReadVariableNode readS3MethodsTable, ReadVariableNode successfulRead,
                        LocalReadVariableNode successfulReadTable, RFunction function, Object clazz, String targetFunctionName, boolean groupMatch, S3FunctionLookupNode next) {
            super(throwsError, nextMethod);
            this.cachedGenericName = genericName;
            this.cachedGroup = group;
            this.builtin = builtin;
            this.readS3MethodsTable = readS3MethodsTable;
            this.next = next;
            this.cachedType = type;
            this.cachedTypeContents = type.getDataCopy();
            this.unsuccessfulReadsCallerFrame = unsuccessfulReadsCaller.toArray(new ReadVariableNode[unsuccessfulReadsCaller.size()]);
            this.unsuccessfulReadsTable = unsuccessfulReadsTable.toArray(new LocalReadVariableNode[unsuccessfulReadsTable.size()]);
            this.successfulRead = successfulRead;
            this.successfulReadTable = successfulReadTable;
            this.result = new Result(genericName, function != null ? function : builtin, clazz, targetFunctionName, groupMatch);
        }

        @Override
        public Result execute(VirtualFrame frame, String genericName, RStringVector type, String group, MaterializedFrame callerFrame, MaterializedFrame genericDefFrame) {
            do {
                if ((genericIdentityProfile.profile(genericName != cachedGenericName) && !cachedGenericName.equals(genericName)) || !isEqualType(type) || group != cachedGroup) {
                    return next.execute(frame, genericName, type, group, callerFrame, genericDefFrame);
                }
                if (!executeReads(frame, unsuccessfulReadsCallerFrame, callerFrame)) {
                    break;
                }
                REnvironment methodsTable;
                if (readS3MethodsTable == null) {
                    methodsTable = null;
                } else {
                    methodsTable = (REnvironment) methodsTableProfile.profile(readS3MethodsTable.execute(frame, genericDefFrame));
                    if (methodsTable != null && !executeReads(unsuccessfulReadsTable, methodsTable.getFrame())) {
                        break;
                    }
                }
                if (successfulRead != null || successfulReadTable != null) {

                    Object actualFunction = successfulRead != null ? successfulRead.execute(frame, callerFrame) : successfulReadTable.execute(frame, methodsTable.getFrame());
                    if (actualFunction != result.function) {
                        break;
                    }
                    return result;
                }
                if (throwsError) {
                    if (builtin != null) {
                        return result;
                    }
                    throw RError.error(this, RError.Message.UNKNOWN_FUNCTION_USE_METHOD, genericName, type);
                } else {
                    return null;
                }
            } while (true);
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return replace(specialize(frame, genericName, type, group, callerFrame, genericDefFrame, next)).execute(frame, genericName, type, group, callerFrame, genericDefFrame);
        }

        @ExplodeLoop
        private static boolean executeReads(VirtualFrame frame, ReadVariableNode[] reads, Frame callerFrame) {
            for (ReadVariableNode read : reads) {
                if (read.execute(frame, callerFrame) != null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    return false;
                }
            }
            return true;
        }

        @ExplodeLoop
        private static boolean executeReads(LocalReadVariableNode[] reads, Frame callerFrame) {
            for (LocalReadVariableNode read : reads) {
                if (read.execute(null, callerFrame) != null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    return false;
                }
            }
            return true;
        }

        private boolean isEqualType(RStringVector type) {
            if (sameIdentityProfile.profile(type == cachedType)) {
                return true;
            }
            if (cachedType == null) {
                return false;
            }
            if (type == null) {
                nullTypeProfile.enter();
                return false;
            }
            if (type.getLength() != cachedTypeContents.length) {
                lengthMismatch.enter();
                return false;
            }
            return compareLoop(type);
        }

        @ExplodeLoop
        private boolean compareLoop(RStringVector type) {
            for (int i = 0; i < cachedTypeContents.length; i++) {
                String elementOne = cachedTypeContents[i];
                String elementTwo = type.getDataAt(i);
                if (elementOne != elementTwo) {
                    notIdentityEqualElements.enter();
                    if (!elementOne.equals(elementTwo)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private static final class UseMethodFunctionLookupGenericNode extends S3FunctionLookupNode {

        protected UseMethodFunctionLookupGenericNode(boolean throwsError, boolean nextMethod) {
            super(throwsError, nextMethod);
        }

        @Override
        public Result execute(VirtualFrame frame, String genericName, RStringVector type, String group, MaterializedFrame callerFrame, MaterializedFrame genericDefFrame) {
            return executeInternal(genericName, type, group, callerFrame, genericDefFrame);
        }

        @TruffleBoundary
        private Result executeInternal(String genericName, RStringVector type, String group, MaterializedFrame callerFrame, MaterializedFrame genericDefFrame) {
            LookupOperation op = (lookupFrame, name, inMethodsTable) -> {
                return ReadVariableNode.lookupFunction(name, lookupFrame, inMethodsTable, true);
            };

            GetMethodsTable getTable = () -> {
                FrameSlot slot = genericDefFrame == null ? null : genericDefFrame.getFrameDescriptor().findFrameSlot(RRuntime.RS3MethodsTable);
                if (slot == null) {
                    return null;
                }
                try {
                    return genericDefFrame.getObject(slot);
                } catch (FrameSlotTypeException e) {
                    throw RInternalError.shouldNotReachHere();
                }
            };

            Result result = performLookup(callerFrame, genericName, group, type, nextMethod, op, getTable);

            if (result == null) {
                if (throwsError) {
                    RFunction function = RContext.lookupBuiltin(genericName);
                    if (function != null) {
                        return new Result(genericName, function, RNull.instance, genericName, false);
                    }
                    throw RError.error(this, RError.Message.UNKNOWN_FUNCTION_USE_METHOD, genericName, RRuntime.toString(type));
                } else {
                    return null;
                }
            }
            return result;
        }
    }
}
