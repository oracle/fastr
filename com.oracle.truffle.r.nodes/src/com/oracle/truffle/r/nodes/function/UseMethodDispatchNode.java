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
    private static final class CheckReadsNode extends Node {
        @Children private final ReadVariableNode[] unsuccessfulReadsCallerFrame;
        @Children private final ReadVariableNode[] unsuccessfulReadsDefFrame;
        // if readsDefFrame != null, then this read will go to the def frame
        @Child private ReadVariableNode successfulRead;

        public final RFunction function;
        private final RStringVector clazz;
        private final String functionName;
        private final ArgumentsSignature signature;
        @CompilationFinal private final ArgumentsSignature[] varArgSignature;

        public CheckReadsNode(ReadVariableNode[] unsuccessfulReadsCallerFrame, ReadVariableNode[] unsuccessfulReadsDefFrame, ReadVariableNode successfulRead, RFunction function, RStringVector clazz,
                        String functionName, ArgumentsSignature signature, ArgumentsSignature[] varArgSignature) {
            this.unsuccessfulReadsCallerFrame = unsuccessfulReadsCallerFrame;
            this.unsuccessfulReadsDefFrame = unsuccessfulReadsDefFrame;
            this.successfulRead = successfulRead;
            this.function = function;
            this.clazz = clazz;
            this.functionName = functionName;
            this.signature = signature;
            this.varArgSignature = varArgSignature;
        }

        public boolean executeReads(Frame callerFrame, MaterializedFrame defFrame) {
            if (!executeReads(unsuccessfulReadsCallerFrame, callerFrame)) {
                return false;
            }
            Object actualFunction;
            if (unsuccessfulReadsDefFrame != null) {
                if (!executeReads(unsuccessfulReadsDefFrame, defFrame)) {
                    return false;
                }
                actualFunction = successfulRead.execute(null, defFrame);
            } else {
                actualFunction = successfulRead.execute(null, callerFrame);
            }
            return actualFunction == function;
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

        @ExplodeLoop
        private static boolean checkLastArgSignature(Object[] args) {
            for (int fi = 0; fi < args.length; ++fi) {
                Object arg = args[fi];
                if (arg instanceof RArgsValuesAndNames) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    return false;
                }
            }
            return true;
        }
    }

    @Child private CheckReadsNode cached;
    @Child private DirectCallNode call;

    public UseMethodDispatchCachedNode(String genericName, RStringVector type, ArgumentsSignature suppliedSignature) {
        super(genericName, type, suppliedSignature);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object[] arguments = extractArguments(frame);
        ArgumentsSignature signature = RArguments.getSignature(frame);
        MaterializedFrame genericDefFrame = RArguments.getEnclosingFrame(frame);
        MaterializedFrame callerFrame = getCallerFrame(frame);

        if (cached == null || !cached.executeReads(callerFrame, genericDefFrame)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            specialize(frame, callerFrame, signature, arguments, true);
        }

        EvaluatedArguments reorderedArgs = doReorderArguments(arguments, cached.function, signature, getSourceSection());
        if (cached.function.isBuiltin()) {
            ArgumentMatcher.evaluatePromises(frame, promiseHelper, reorderedArgs);
        }
        Object[] argObject = prepareArguments(callerFrame, genericDefFrame, reorderedArgs, cached.function, cached.clazz, cached.functionName);
        return call.call(frame, argObject);
    }

    private void specialize(Frame callerFrame, MaterializedFrame genericDefFrame, ArgumentsSignature signature, Object[] arguments, boolean throwsRError) {
        CompilerAsserts.neverPartOfCompilation();
        // look for a match in the caller frame hierarchy
        TargetLookupResult result = findTargetFunctionLookup(callerFrame, type, genericName);
        ReadVariableNode[] unsuccessfulReadsCaller = result.unsuccessfulReads;
        ReadVariableNode[] unsuccessfulReadsDef = null;
        if (result.successfulRead == null) {
            if (genericDefFrame != null) {
                // look for a match in the generic def frame hierarchy
                result = findTargetFunctionLookup(genericDefFrame, type, genericName);
                unsuccessfulReadsDef = result.unsuccessfulReads;
            }
            if (result.successfulRead == null) {
                if (throwsRError) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNKNOWN_FUNCTION_USE_METHOD, genericName, type);
                } else {
                    throw new NoGenericMethodException();
                }
            }
        }

        ArgumentsSignature[] varArgSignatures = new ArgumentsSignature[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] instanceof RArgsValuesAndNames) {
                varArgSignatures[i] = ((RArgsValuesAndNames) arguments[i]).getSignature();
            }
        }

        CheckReadsNode newCheckedReads = new CheckReadsNode(unsuccessfulReadsCaller, unsuccessfulReadsDef, result.successfulRead, result.targetFunction, result.clazz, result.targetFunctionName,
                        signature, varArgSignatures);
        DirectCallNode newCall = Truffle.getRuntime().createDirectCallNode(result.targetFunction.getTarget());
        if (call == null) {
            cached = insert(newCheckedReads);
            call = insert(newCall);
        } else {
            RError.performanceWarning("re-specializing UseMethodDispatchCachedNode");
            cached.replace(newCheckedReads);
            call.replace(newCall);
        }
    }

    @Override
    public Object executeGeneric(VirtualFrame frame, RStringVector aType) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public Object executeInternal(VirtualFrame frame, Object[] args) throws NoGenericMethodException {
        Object[] arguments = extractArguments(frame);
        ArgumentsSignature signature = RArguments.getSignature(frame);
        if (cached == null || !cached.executeReads(frame, null)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            specialize(frame, null, signature, arguments, false);
        }
        EvaluatedArguments reorderedArgs = reorderArguments(args, cached.function, suppliedSignature, getEncapsulatingSourceSection());
        if (cached.function.isBuiltin()) {
            ArgumentMatcher.evaluatePromises(frame, promiseHelper, reorderedArgs);
        }
        Object[] argObject = prepareArguments(frame, null, reorderedArgs, cached.function, cached.clazz, cached.functionName);
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
        Object[] argObject = prepareArguments(callerFrame, RArguments.getEnclosingFrame(frame), reorderedArgs, targetFunction, klass, targetFunctionName);
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
