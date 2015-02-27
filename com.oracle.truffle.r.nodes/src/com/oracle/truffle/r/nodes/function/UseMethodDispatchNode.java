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
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.nodes.function.DispatchedCallNode.NoGenericMethodException;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * {@code UseMethod} is typically called like this:
 *
 * <pre>
 * f <- function(x, ...) UseMethod("f")
 * </pre>
 *
 * Locating the correct call depends on the class of {@code x}, and the search starts in the
 * enclosing (parent) environment of {@code f}, which, for packages, which is where most of these
 * definitions occur, will be the package {@code namepace} enviromnent.
 */
public abstract class UseMethodDispatchNode {

    public static DispatchNode createCached(String genericName, RStringVector type, ArgumentsSignature suppliedSignature) {
        return new UseMethodDispatchCachedNode(genericName, type, suppliedSignature);
    }

    public static DispatchNode createGeneric(String genericName, ArgumentsSignature suppliedSignature) {
        return new UseMethodDispatchGenericNode(genericName, suppliedSignature);
    }
}

final class UseMethodDispatchCachedNode extends S3DispatchCachedNode {

    @NodeInfo(cost = NodeCost.NONE)
    private static final class UnsuccessfulReadsNode extends Node {
        @Children private final ReadVariableNode[] reads;

        public UnsuccessfulReadsNode(ReadVariableNode[] reads) {
            this.reads = reads;
        }

        @ExplodeLoop
        public boolean executeReads(Frame callerFrame) {
            for (ReadVariableNode read : reads) {
                if (read.execute(null, callerFrame) != null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    return false;
                }
            }
            return true;
        }
    }

    @Child private UnsuccessfulReadsNode unsuccessfulReads;
    @Child private ReadVariableNode successfulRead;
    @Child private DirectCallNode call;
    @CompilationFinal private RFunction cachedFunction;
    @CompilationFinal private RStringVector cachedClass;
    @CompilationFinal private String cachedFunctionName;

    public UseMethodDispatchCachedNode(String genericName, RStringVector type, ArgumentsSignature suppliedSignature) {
        super(genericName, type, suppliedSignature);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        MaterializedFrame callerFrame = getCallerFrame(frame);
        if (unsuccessfulReads == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            specialize(frame, callerFrame, true);
        } else {
            executeReads(frame, callerFrame, true);
        }
        EvaluatedArguments reorderedArgs = reorderArguments(extractArguments(frame), cachedFunction, RArguments.getSignature(frame), getSourceSection());
        if (cachedFunction.isBuiltin()) {
            ArgumentMatcher.evaluatePromises(frame, promiseHelper, reorderedArgs);
        }
        Object[] argObject = prepareArguments(callerFrame, reorderedArgs, cachedFunction, cachedClass, cachedFunctionName);
        return call.call(frame, argObject);
    }

    @ExplodeLoop
    private void executeReads(Frame callerFrame, MaterializedFrame genericDefEnv, boolean throwsRError) {
        if (!unsuccessfulReads.executeReads(callerFrame)) {
            specialize(callerFrame, genericDefEnv, throwsRError);
        }
        if (successfulRead.execute(null, callerFrame) != cachedFunction) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            specialize(callerFrame, genericDefEnv, throwsRError);
        }
    }

    private void specialize(Frame callerFrame, MaterializedFrame genericDefFrame, boolean throwsRError) {
        CompilerAsserts.neverPartOfCompilation();
        TargetLookupResult result = findTargetFunctionLookup(callerFrame, type, genericName);
        ReadVariableNode[] unsuccessfulReadNodes = result.unsuccessfulReads;
        if (result.successfulRead == null) {
            if (genericDefFrame != null) {
                result = findTargetFunctionLookup(genericDefFrame, type, genericName);
            }
            if (result.successfulRead == null) {
                if (throwsRError) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNKNOWN_FUNCTION_USE_METHOD, genericName, type);
                } else {
                    throw new NoGenericMethodException();
                }
            }
            int newResultLength = result.unsuccessfulReads.length;
            unsuccessfulReadNodes = Arrays.copyOf(unsuccessfulReadNodes, unsuccessfulReadNodes.length + newResultLength);
            System.arraycopy(result.unsuccessfulReads, 0, unsuccessfulReadNodes, unsuccessfulReadNodes.length - newResultLength, newResultLength);
        }

        cachedFunction = result.targetFunction;
        cachedFunctionName = result.targetFunctionName;
        cachedClass = result.clazz;

        DirectCallNode newCall = Truffle.getRuntime().createDirectCallNode(cachedFunction.getTarget());
        UnsuccessfulReadsNode newUnsuccessfulReads = new UnsuccessfulReadsNode(unsuccessfulReadNodes);
        if (call == null) {
            call = insert(newCall);
            unsuccessfulReads = insert(newUnsuccessfulReads);
            successfulRead = insert(result.successfulRead);
        } else {
            RError.performanceWarning("re-specializing UseMethodDispatchCachedNode");
            call.replace(newCall);
            unsuccessfulReads.replace(newUnsuccessfulReads);
            successfulRead.replace(result.successfulRead);
        }
    }

    @Override
    public Object executeGeneric(VirtualFrame frame, RStringVector aType) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public Object executeInternal(VirtualFrame frame, Object[] args) throws NoGenericMethodException {
        if (unsuccessfulReads == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            specialize(frame, null, false);
        } else {
            executeReads(frame, null, false);
        }
        EvaluatedArguments reorderedArgs = reorderArguments(args, cachedFunction, suppliedSignature, getEncapsulatingSourceSection());
        if (cachedFunction.isBuiltin()) {
            ArgumentMatcher.evaluatePromises(frame, promiseHelper, reorderedArgs);
        }
        Object[] argObject = prepareArguments(frame, reorderedArgs, cachedFunction, cachedClass, cachedFunctionName);
        return call.call(frame, argObject);
    }

    @Override
    public Object executeInternalGeneric(VirtualFrame frame, RStringVector aType, Object[] args) throws NoGenericMethodException {
        throw RInternalError.shouldNotReachHere();
    }

    private static final class TargetLookupResult {
        private final ReadVariableNode[] unsuccessfulReads;
        private final ReadVariableNode successfulRead;
        private final RFunction targetFunction;
        private final String targetFunctionName;
        private final RStringVector clazz;

        public TargetLookupResult(ReadVariableNode[] unsuccessfulReads, ReadVariableNode successfulRead, RFunction targetFunction, String targetFunctionName, RStringVector clazz) {
            this.unsuccessfulReads = unsuccessfulReads;
            this.successfulRead = successfulRead;
            this.targetFunction = targetFunction;
            this.targetFunctionName = targetFunctionName;
            this.clazz = clazz;
        }
    }

    private static TargetLookupResult findTargetFunctionLookup(Frame callerFrame, RStringVector type, String genericName) {
        CompilerAsserts.neverPartOfCompilation();
        RFunction targetFunction = null;
        String targetFunctionName = null;
        RStringVector clazz = null;
        ArrayList<ReadVariableNode> unsuccessfulReads = new ArrayList<>();

        for (int i = 0; i <= type.getLength(); ++i) {
            String clazzName = i == type.getLength() ? RRuntime.DEFAULT : type.getDataAt(i);
            String functionName = genericName + RRuntime.RDOT + clazzName;
            ReadVariableNode rvn = ReadVariableNode.createFunctionLookup(functionName, false);
            Object func = rvn.execute(null, callerFrame);
            if (func != null) {
                assert func instanceof RFunction;
                targetFunctionName = functionName;
                targetFunction = (RFunction) func;

                if (i == 0) {
                    clazz = type.copyResized(type.getLength(), false);
                } else if (i == type.getLength()) {
                    clazz = null;
                } else {
                    clazz = RDataFactory.createStringVector(Arrays.copyOfRange(type.getDataWithoutCopying(), i, type.getLength()), true);
                    clazz.setAttr(RRuntime.PREVIOUS_ATTR_KEY, type.copyResized(type.getLength(), false));
                }
                return new TargetLookupResult(unsuccessfulReads.toArray(new ReadVariableNode[unsuccessfulReads.size()]), rvn, targetFunction, targetFunctionName, clazz);
            } else {
                unsuccessfulReads.add(rvn);
            }
        }
        return new TargetLookupResult(unsuccessfulReads.toArray(new ReadVariableNode[unsuccessfulReads.size()]), null, null, null, null);
    }
}

final class UseMethodDispatchGenericNode extends S3DispatchGenericNode {

    public UseMethodDispatchGenericNode(String genericName, ArgumentsSignature suppliedSignature) {
        super(genericName, suppliedSignature);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame, RStringVector type) {
        Frame callerFrame = getCallerFrame(frame);
        findTargetFunction(RArguments.getEnclosingFrame(frame), type, true);
        return executeHelper(frame, callerFrame, extractArguments(frame), RArguments.getSignature(frame), getSourceSection());
    }

    @Override
    public Object executeInternal(VirtualFrame frame, Object[] args) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public Object executeInternalGeneric(VirtualFrame frame, RStringVector type, Object[] args) {
        // TBD getEnclosing?
        findTargetFunction(frame, type, false);
        return executeHelper(frame, frame, args, suppliedSignature, getEncapsulatingSourceSection());
    }

    private Object executeHelper(VirtualFrame frame, Frame callerFrame, Object[] args, ArgumentsSignature paramSignature, SourceSection errorSourceSection) {
        EvaluatedArguments reorderedArgs = reorderArguments(args, targetFunction, paramSignature, errorSourceSection);
        if (targetFunction.isBuiltin()) {
            ArgumentMatcher.evaluatePromises(frame, promiseHelper, reorderedArgs);
        }
        Object[] argObject = prepareArguments(callerFrame, reorderedArgs, targetFunction, klass, targetFunctionName);
        return indirectCallNode.call(frame, targetFunction.getTarget(), argObject);
    }

    private void findTargetFunction(Frame callerFrame, RStringVector type, boolean throwsRError) {
        findTargetFunctionLookup(callerFrame, type);
        if (targetFunction == null) {
            errorProfile.enter();
            if (throwsRError) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNKNOWN_FUNCTION_USE_METHOD, genericName, RRuntime.toString(type));
            } else {
                throw new NoGenericMethodException();
            }
        }
    }

    @TruffleBoundary
    private void findTargetFunctionLookup(Frame callerFrame, RStringVector type) {
        for (int i = 0; i < type.getLength(); ++i) {
            findFunction(genericName, type.getDataAt(i), callerFrame);
            if (targetFunction != null) {
                RStringVector classVec = null;
                if (i > 0) {
                    isFirst = false;
                    classVec = RDataFactory.createStringVector(Arrays.copyOfRange(type.getDataWithoutCopying(), i, type.getLength()), true);
                    classVec.setAttr(RRuntime.PREVIOUS_ATTR_KEY, type.copyResized(type.getLength(), false));
                } else {
                    isFirst = true;
                    classVec = type.copyResized(type.getLength(), false);
                }
                klass = classVec;
                break;
            }
        }
        if (targetFunction != null) {
            return;
        }
        findFunction(genericName, RRuntime.DEFAULT, callerFrame);
    }
}
