/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.function;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode.ReadKind;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.nodes.*;

public abstract class S3FunctionLookupNode extends RBaseNode {
    protected static final int MAX_CACHE_DEPTH = 3;

    protected final boolean throwsError;
    protected final boolean nextMethod;

    protected S3FunctionLookupNode(boolean throwsError, boolean nextMethod) {
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

        public Result(String generic, RFunction function, Object clazz, String targetFunctionName, boolean groupMatch) {
            this.generic = generic.intern();
            this.function = function;
            this.signature = function == null ? null : ArgumentMatcher.getFunctionSignature(function);
            this.clazz = clazz;
            this.targetFunctionName = targetFunctionName;
            this.groupMatch = groupMatch;
        }
    }

    public static S3FunctionLookupNode create(boolean throwsError, boolean nextMethod) {
        return new UseMethodFunctionLookupUninitializedNode(throwsError, nextMethod);
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
        // look for a generic function reachable from the caller frame
        if ((result = lookupClassGenerics(callerFrame, genericName, groupName, type, nextMethod, false, op)) != null) {
            return result;
        }
        Object methodsTable = getTable.get();
        if (methodsTable instanceof RPromise) {
            methodsTable = PromiseHelperNode.evaluateSlowPath(null, (RPromise) methodsTable);
        }
        MaterializedFrame methodsTableFrame = methodsTable == null ? null : ((REnvironment) methodsTable).getFrame();

        if (methodsTableFrame != null) {
            // look for a generic function in the methods table
            if ((result = lookupClassGenerics(methodsTableFrame, genericName, groupName, type, nextMethod, true, op)) != null) {
                return result;
            }
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

    private static Result lookupClassGenerics(MaterializedFrame callerFrame, String genericName, String groupName, RStringVector type, boolean nextMethod, boolean inMethodsTable, LookupOperation op) {
        Result result = null;
        for (int i = nextMethod ? 1 : 0; i < type.getLength(); i++) {
            String clazzName = type.getDataAt(i);
            boolean groupMatch = false;

            String functionName = genericName + RRuntime.RDOT + type.getDataAt(i);
            RFunction function = checkPromise(op.read(callerFrame, functionName, inMethodsTable));

            if (function == null && groupName != null) {
                groupMatch = true;
                functionName = groupName + RRuntime.RDOT + clazzName;
                function = checkPromise(op.read(callerFrame, functionName, inMethodsTable));
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

    public abstract S3FunctionLookupNode.Result execute(VirtualFrame frame, String genericName, RStringVector type, String group, MaterializedFrame callerFrame, MaterializedFrame genericDefFrame);

    private static UseMethodFunctionLookupCachedNode specialize(String genericName, RStringVector type, String group, MaterializedFrame callerFrame, MaterializedFrame genericDefFrame,
                    S3FunctionLookupNode next) {
        ArrayList<ReadVariableNode> unsuccessfulReadsCaller = new ArrayList<>();
        ArrayList<ReadVariableNode> unsuccessfulReadsTable = new ArrayList<>();
        class SuccessfulReads {
            ReadVariableNode successfulRead;
            boolean successfulReadIsTable;
            ReadVariableNode methodsTableRead;
        }
        SuccessfulReads reads = new SuccessfulReads();

        LookupOperation op = (lookupFrame, name, inMethodsTable) -> {
            ReadVariableNode read = ReadVariableNode.create(name, RType.Function, inMethodsTable ? ReadKind.SilentLocal : ReadKind.Silent);
            Object result = read.execute(null, lookupFrame);
            if (result == null) {
                (inMethodsTable ? unsuccessfulReadsTable : unsuccessfulReadsCaller).add(read);
            } else {
                reads.successfulRead = read;
                if (inMethodsTable) {
                    reads.successfulReadIsTable = true;
                }
            }
            return result;
        };

        GetMethodsTable getTable = () -> {
            if (genericDefFrame != null) {
                reads.methodsTableRead = ReadVariableNode.create(RRuntime.RS3MethodsTable, RType.Any, ReadKind.SilentLocal);
                return reads.methodsTableRead.execute(null, genericDefFrame);
            } else {
                return null;
            }
        };

        Result result = performLookup(callerFrame, genericName, group, type, next.nextMethod, op, getTable);

        UseMethodFunctionLookupCachedNode cachedNode;

        if (result != null) {
            cachedNode = new UseMethodFunctionLookupCachedNode(next.throwsError, next.nextMethod, genericName, type, group, null, unsuccessfulReadsCaller, unsuccessfulReadsTable,
                            reads.methodsTableRead, reads.successfulRead, reads.successfulReadIsTable, result.function, result.clazz, result.targetFunctionName, result.groupMatch, next);
        } else {
            RFunction builtin = next.throwsError ? builtin = RContext.lookupBuiltin(genericName) : null;
            cachedNode = new UseMethodFunctionLookupCachedNode(next.throwsError, next.nextMethod, genericName, type, group, builtin, unsuccessfulReadsCaller, unsuccessfulReadsTable,
                            reads.methodsTableRead, null, false, null, null, null, false, next);
        }
        return cachedNode;
    }

    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    private static final class UseMethodFunctionLookupUninitializedNode extends S3FunctionLookupNode {
        private int depth;

        public UseMethodFunctionLookupUninitializedNode(boolean throwsError, boolean nextMethod) {
            super(throwsError, nextMethod);
        }

        @Override
        public Result execute(VirtualFrame frame, String genericName, RStringVector type, String group, MaterializedFrame callerFrame, MaterializedFrame genericDefFrame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (++depth > MAX_CACHE_DEPTH) {
                return replace(new UseMethodFunctionLookupGenericNode(throwsError, nextMethod)).execute(frame, genericName, type, group, callerFrame, genericDefFrame);
            } else {
                UseMethodFunctionLookupCachedNode cachedNode = replace(specialize(genericName, type, group, callerFrame, genericDefFrame, this));
                return cachedNode.execute(frame, genericName, type, group, callerFrame, genericDefFrame);
            }
        }
    }

    private static final class UseMethodFunctionLookupCachedNode extends S3FunctionLookupNode {

        @Child private S3FunctionLookupNode next;

        @CompilationFinal private final String[] cachedTypeContents;
        @Children private final ReadVariableNode[] unsuccessfulReadsCallerFrame;
        @Child private ReadVariableNode readS3MethodsTable;
        @Children private final ReadVariableNode[] unsuccessfulReadsDefFrame;
        // if unsuccessfulReadsDefFrame != null, then this read will go to the def frame
        @Child private ReadVariableNode successfulRead;

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
        private final boolean successfulReadIsTable;

        public UseMethodFunctionLookupCachedNode(boolean throwsError, boolean nextMethod, String genericName, RStringVector type, String group, RFunction builtin,
                        List<ReadVariableNode> unsuccessfulReadsCaller, List<ReadVariableNode> unsuccessfulReadsDef, ReadVariableNode readS3MethodsTable, ReadVariableNode successfulRead,
                        boolean successfulReadIsTable, RFunction function, Object clazz, String targetFunctionName, boolean groupMatch, S3FunctionLookupNode next) {
            super(throwsError, nextMethod);
            this.cachedGenericName = genericName;
            this.cachedGroup = group;
            this.builtin = builtin;
            this.readS3MethodsTable = readS3MethodsTable;
            this.next = next;
            this.cachedType = type;
            this.cachedTypeContents = type.getDataCopy();
            this.unsuccessfulReadsCallerFrame = unsuccessfulReadsCaller.toArray(new ReadVariableNode[unsuccessfulReadsCaller.size()]);
            this.unsuccessfulReadsDefFrame = unsuccessfulReadsDef.toArray(new ReadVariableNode[unsuccessfulReadsDef.size()]);
            this.successfulRead = successfulRead;
            this.successfulReadIsTable = successfulReadIsTable;
            this.result = new Result(genericName, function != null ? function : builtin, clazz, targetFunctionName, groupMatch);
        }

        @Override
        public Result execute(VirtualFrame frame, String genericName, RStringVector type, String group, MaterializedFrame callerFrame, MaterializedFrame genericDefFrame) {
            do {
                if ((genericIdentityProfile.profile(genericName != cachedGenericName) && !cachedGenericName.equals(genericName)) || !isEqualType(type) || group != cachedGroup) {
                    return next.execute(frame, genericName, type, group, callerFrame, genericDefFrame);
                }
                if (!executeReads(unsuccessfulReadsCallerFrame, callerFrame)) {
                    break;
                }
                REnvironment methodsTable;
                if (readS3MethodsTable == null) {
                    methodsTable = null;
                } else {
                    methodsTable = (REnvironment) methodsTableProfile.profile(readS3MethodsTable.execute(frame, genericDefFrame));
                    if (methodsTable != null && !executeReads(unsuccessfulReadsDefFrame, methodsTable.getFrame())) {
                        break;
                    }
                }
                if (successfulRead != null) {
                    Object actualFunction = successfulRead.execute(null, successfulReadIsTable ? methodsTable.getFrame() : callerFrame);
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
                    throw S3FunctionLookupNode.NoGenericMethodException.instance;
                }
            } while (true);
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return replace(specialize(genericName, type, group, callerFrame, genericDefFrame, next)).execute(frame, genericName, type, group, callerFrame, genericDefFrame);
        }

        @ExplodeLoop
        private static boolean executeReads(ReadVariableNode[] reads, Frame callerFrame) {
            for (ReadVariableNode read : reads) {
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
                return ReadVariableNode.lookupFunction(name, lookupFrame, inMethodsTable);
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
                    throw S3FunctionLookupNode.NoGenericMethodException.instance;
                }
            }
            return result;
        }
    }

    @SuppressWarnings("serial")
    public static final class NoGenericMethodException extends ControlFlowException {
        public static final NoGenericMethodException instance = new NoGenericMethodException();

        private NoGenericMethodException() {
            // empty
        }
    }
}
