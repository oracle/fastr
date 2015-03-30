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
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class S3FunctionLookupNode extends Node {
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

    public abstract S3FunctionLookupNode.Result execute(VirtualFrame frame, String genericName, RStringVector type, String group, MaterializedFrame callerFrame, MaterializedFrame genericDefFrame);

    private static UseMethodFunctionLookupCachedNode specialize(String genericName, RStringVector type, String group, MaterializedFrame callerFrame, MaterializedFrame genericDefFrame,
                    S3FunctionLookupNode next) {
        // look for a match in the caller frame hierarchy
        TargetLookupResult result = findTargetFunctionLookup(callerFrame, type, genericName, group, true, next.nextMethod);
        ReadVariableNode[] unsuccessfulReadsCaller = result.unsuccessfulReads;
        ReadVariableNode[] unsuccessfulReadsDef = null;
        if (result.successfulRead == null) {
            if (genericDefFrame != null) {
                // look for a match in the generic def frame hierarchy
                result = findTargetFunctionLookup(genericDefFrame, type, genericName, group, true, next.nextMethod);
                unsuccessfulReadsDef = result.unsuccessfulReads;
            }
        }
        RFunction builtin = null;
        if (next.throwsError && result.successfulRead == null) {
            builtin = RContext.getEngine().lookupBuiltin(genericName);
        }

        UseMethodFunctionLookupCachedNode cachedNode = new UseMethodFunctionLookupCachedNode(next.throwsError, next.nextMethod, genericName, type, group, builtin, unsuccessfulReadsCaller,
                        unsuccessfulReadsDef, result.successfulRead, result.targetFunction, result.clazz, result.targetFunctionName, result.groupMatch, next);
        return cachedNode;
    }

    protected static final class TargetLookupResult {
        public final ReadVariableNode[] unsuccessfulReads;
        public final ReadVariableNode successfulRead;
        public final RFunction targetFunction;
        public final String targetFunctionName;
        public final Object clazz;
        public final boolean groupMatch;

        public TargetLookupResult(ReadVariableNode[] unsuccessfulReads, ReadVariableNode successfulRead, RFunction targetFunction, String targetFunctionName, Object clazz, boolean groupMatch) {
            this.unsuccessfulReads = unsuccessfulReads;
            this.successfulRead = successfulRead;
            this.targetFunction = targetFunction;
            this.targetFunctionName = targetFunctionName;
            this.clazz = clazz;
            this.groupMatch = groupMatch;
        }
    }

    protected static TargetLookupResult findTargetFunctionLookup(Frame lookupFrame, RStringVector type, String genericName, String groupName, boolean createReadVariableNodes, boolean nextMethod) {
        CompilerAsserts.neverPartOfCompilation();
        ArrayList<ReadVariableNode> unsuccessfulReads = createReadVariableNodes ? new ArrayList<>() : null;

        for (int i = nextMethod ? 1 : 0; i <= type.getLength(); i++) {
            String clazzName = i == type.getLength() ? RRuntime.DEFAULT : type.getDataAt(i);
            String functionName = genericName + RRuntime.RDOT + clazzName;
            TargetLookupResult lookupResult = lookupFunction(lookupFrame, type, createReadVariableNodes, unsuccessfulReads, i, functionName, false);
            if (lookupResult == null && groupName != null) {
                functionName = groupName + RRuntime.RDOT + clazzName;
                lookupResult = lookupFunction(lookupFrame, type, createReadVariableNodes, unsuccessfulReads, i, functionName, true);
            }
            if (lookupResult != null) {
                return lookupResult;
            }
        }
        if (createReadVariableNodes) {
            return new TargetLookupResult(unsuccessfulReads.toArray(new ReadVariableNode[unsuccessfulReads.size()]), null, null, null, null, false);
        } else {
            return null;
        }
    }

    private static TargetLookupResult lookupFunction(Frame lookupFrame, RStringVector type, boolean createReadVariableNodes, ArrayList<ReadVariableNode> unsuccessfulReads, int i, String functionName,
                    boolean groupMatch) {
        ReadVariableNode rvn;
        RFunction function;
        if (createReadVariableNodes) {
            rvn = ReadVariableNode.createFunctionLookup(functionName, false);
            function = (RFunction) rvn.execute(null, lookupFrame);
        } else {
            rvn = null;
            function = ReadVariableNode.lookupFunction(functionName, lookupFrame);
        }
        if (function != null) {
            ReadVariableNode[] array = createReadVariableNodes ? unsuccessfulReads.toArray(new ReadVariableNode[unsuccessfulReads.size()]) : null;
            if (i == 0) {
                return new TargetLookupResult(array, rvn, function, functionName, type.copyResized(type.getLength(), false), groupMatch);
            } else if (i == type.getLength()) {
                return new TargetLookupResult(array, rvn, function, functionName, RNull.instance, groupMatch);
            } else {
                RStringVector clazz = RDataFactory.createStringVector(Arrays.copyOfRange(type.getDataWithoutCopying(), i, type.getLength()), true);
                clazz.setAttr(RRuntime.PREVIOUS_ATTR_KEY, type.copyResized(type.getLength(), false));
                return new TargetLookupResult(array, rvn, function, functionName, clazz, groupMatch);
            }
        } else {
            if (createReadVariableNodes) {
                unsuccessfulReads.add(rvn);
            }
            return null;
        }
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
        private final String cachedGroup;
        private final RFunction builtin;

        public UseMethodFunctionLookupCachedNode(boolean throwsError, boolean nextMethod, String genericName, RStringVector type, String group, RFunction builtin,
                        ReadVariableNode[] unsuccessfulReadsCaller, ReadVariableNode[] unsuccessfulReadsDef, ReadVariableNode successfulRead, RFunction function, Object clazz,
                        String targetFunctionName, boolean groupMatch, S3FunctionLookupNode next) {
            super(throwsError, nextMethod);
            this.cachedGenericName = genericName;
            this.cachedGroup = group;
            this.builtin = builtin;
            this.next = next;
            this.cachedType = type;
            this.cachedTypeContents = type.getDataCopy();
            this.unsuccessfulReadsCallerFrame = unsuccessfulReadsCaller;
            this.unsuccessfulReadsDefFrame = unsuccessfulReadsDef;
            this.successfulRead = successfulRead;
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
                if (unsuccessfulReadsDefFrame != null && !executeReads(unsuccessfulReadsDefFrame, genericDefFrame)) {
                    break;
                }
                if (successfulRead != null) {
                    Object actualFunction = successfulRead.execute(null, unsuccessfulReadsDefFrame == null ? callerFrame : genericDefFrame);
                    if (actualFunction != result.function) {
                        break;
                    }
                    return result;
                }
                if (throwsError) {
                    if (builtin != null) {
                        return result;
                    }
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNKNOWN_FUNCTION_USE_METHOD, genericName, type);
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
            TargetLookupResult lookupResult = findTargetFunctionLookup(callerFrame, type, genericName, group, false, nextMethod);
            if (lookupResult == null) {
                lookupResult = findTargetFunctionLookup(genericDefFrame, type, genericName, group, false, nextMethod);
                if (lookupResult == null) {
                    if (throwsError) {
                        RFunction function = RContext.getEngine().lookupBuiltin(genericName);
                        if (function != null) {
                            return new Result(genericName, function, RNull.instance, genericName, false);
                        }
                        throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNKNOWN_FUNCTION_USE_METHOD, genericName, RRuntime.toString(type));
                    } else {
                        throw S3FunctionLookupNode.NoGenericMethodException.instance;
                    }
                }
            }
            return new Result(genericName, lookupResult.targetFunction, lookupResult.clazz, lookupResult.targetFunctionName, lookupResult.groupMatch);
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
