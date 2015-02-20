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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.utilities.*;
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

    private final ConditionProfile topLevelFrameProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile callerFrameSlotPath = ConditionProfile.createBinaryProfile();

    private final ConditionProfile hasVarArgsProfile = ConditionProfile.createBinaryProfile();

    public UseMethodDispatchCachedNode(String genericName, RStringVector type, ArgumentsSignature suppliedSignature) {
        super(genericName, type, suppliedSignature);
    }

    private Frame getCallerFrame(VirtualFrame frame) {
        Frame funFrame = RArguments.getCallerFrame(frame);
        if (callerFrameSlotPath.profile(funFrame == null)) {
            funFrame = Utils.getCallerFrame(frame, FrameAccess.MATERIALIZE);
            RError.performanceWarning("slow caller frame access in UseMethod dispatch");
        }
        // S3 method can be dispatched from top-level where there is no caller frame
        return topLevelFrameProfile.profile(funFrame == null) ? frame : funFrame;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Frame funFrame = getCallerFrame(frame);
        if (targetFunction == null) {
            findTargetFunction(RArguments.getEnclosingFrame(frame), true);
        }
        return executeHelper(frame, funFrame);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame, RStringVector aType) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public Object executeInternal(VirtualFrame frame, Object[] args) {
        if (targetFunction == null) {
            // TBD getEnclosing?
            findTargetFunction(frame, false);
        }
        return executeHelper(frame, args);
    }

    @Override
    public Object executeInternalGeneric(VirtualFrame frame, RStringVector aType, Object[] args) {
        throw RInternalError.shouldNotReachHere();
    }

    private Object executeHelper(VirtualFrame frame, Frame callerFrame) {
        // Extract arguments from current frame...
        int argCount = RArguments.getArgumentsLength(frame);
        assert RArguments.getSignature(frame).getLength() == argCount;
        Object[] argValues = new Object[argCount];
        int fi = 0;
        for (; fi < argCount; ++fi) {
            argValues[fi] = RArguments.getArgument(frame, fi);
        }
        EvaluatedArguments reorderedArgs = reorderArgs(frame, targetFunction, argValues, RArguments.getSignature(frame), false, getSourceSection());
        return executeHelper2(frame, callerFrame.materialize(), reorderedArgs.getEvaluatedArgs(), reorderedArgs.getSignature());
    }

    private Object executeHelper(VirtualFrame callerFrame, Object[] args) {
        // Extract arguments from current frame...
        int argCount = args.length;
        int argListSize = argCount;

        boolean hasVarArgs = false;
        for (int fi = 0; fi < argCount; ++fi) {
            Object arg = args[fi];
            if (arg instanceof RArgsValuesAndNames) {
                hasVarArgs = true;
                argListSize += ((RArgsValuesAndNames) arg).length() - 1;
            }
        }
        Object[] argValues;
        ArgumentsSignature signature;
        if (hasVarArgsProfile.profile(hasVarArgs)) {
            argValues = new Object[argListSize];
            String[] argNames = new String[argListSize];
            int index = 0;
            for (int fi = 0; fi < argCount; ++fi) {
                Object arg = args[fi];
                if (arg instanceof RArgsValuesAndNames) {
                    RArgsValuesAndNames varArgs = (RArgsValuesAndNames) arg;
                    Object[] varArgValues = varArgs.getValues();
                    ArgumentsSignature varArgSignature = varArgs.getSignature();
                    for (int i = 0; i < varArgs.length(); i++) {
                        argNames[index] = varArgSignature.getName(i);
                        addArg(argValues, varArgValues[i], index++);
                    }
                } else {
                    argNames[index] = suppliedSignature.getName(fi);
                    addArg(argValues, arg, index++);
                }
            }
            signature = ArgumentsSignature.get(argNames);
        } else {
            argValues = new Object[argCount];
            for (int i = 0; i < argCount; i++) {
                addArg(argValues, args[i], i);
            }
            signature = suppliedSignature;
        }

        // ...and use them as 'supplied' arguments...
        EvaluatedArguments evaledArgs = EvaluatedArguments.create(argValues, signature);

        // ...to match them against the chosen function's formal arguments
        EvaluatedArguments reorderedArgs = ArgumentMatcher.matchArgumentsEvaluated(callerFrame, targetFunction, evaledArgs, getEncapsulatingSourceSection(), promiseHelper, false);
        return executeHelper2(callerFrame, callerFrame.materialize(), reorderedArgs.getEvaluatedArgs(), reorderedArgs.getSignature());
    }

    private static void addArg(Object[] values, Object value, int index) {
        if (RMissingHelper.isMissing(value) || (value instanceof RPromise && RMissingHelper.isMissingName((RPromise) value))) {
            values[index] = null;
        } else {
            values[index] = value;
        }
    }

    private Object executeHelper2(VirtualFrame frame, MaterializedFrame callerFrame, Object[] arguments, ArgumentsSignature signature) {
        Object[] argObject = RArguments.createS3Args(targetFunction, getSourceSection(), null, RArguments.getDepth(callerFrame) + 1, arguments, signature);
        // todo: cannot create frame descriptors in compiled code
        genCallEnv = callerFrame;
        defineVarsAsArguments(argObject, genericName, klass, genCallEnv, genDefEnv);
        RArguments.setS3Method(argObject, targetFunctionName);
        return indirectCallNode.call(frame, targetFunction.getTarget(), argObject);
    }

    private void findTargetFunction(Frame callerFrame, boolean throwsRError) {
        findTargetFunctionLookup(callerFrame);
        if (targetFunction == null) {
            errorProfile.enter();
            if (throwsRError) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNKNOWN_FUNCTION_USE_METHOD, this.genericName, RRuntime.toString(this.type));
            } else {
                throw new NoGenericMethodException();
            }
        }
    }

    @TruffleBoundary
    private void findTargetFunctionLookup(Frame callerFrame) {
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

final class UseMethodDispatchGenericNode extends S3DispatchGenericNode {

    private final ConditionProfile topLevelFrameProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile callerFrameSlotPath = ConditionProfile.createBinaryProfile();

    private final ConditionProfile hasVarArgsProfile = ConditionProfile.createBinaryProfile();

    public UseMethodDispatchGenericNode(String genericName, ArgumentsSignature suppliedSignature) {
        super(genericName, suppliedSignature);
    }

    private Frame getCallerFrame(VirtualFrame frame) {
        Frame funFrame = RArguments.getCallerFrame(frame);
        if (callerFrameSlotPath.profile(funFrame == null)) {
            funFrame = Utils.getCallerFrame(frame, FrameAccess.MATERIALIZE);
            RError.performanceWarning("slow caller frame access in UseMethod dispatch");
        }
        // S3 method can be dispatched from top-level where there is no caller frame
        return topLevelFrameProfile.profile(funFrame == null) ? frame : funFrame;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame, RStringVector type) {
        Frame funFrame = getCallerFrame(frame);
        findTargetFunction(RArguments.getEnclosingFrame(frame), type, true);
        return executeHelper(frame, funFrame);
    }

    @Override
    public Object executeInternal(VirtualFrame frame, Object[] args) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public Object executeInternalGeneric(VirtualFrame frame, RStringVector type, Object[] args) {
        // TBD getEnclosing?
        findTargetFunction(frame, type, false);
        return executeHelper(frame, args);
    }

    private Object executeHelper(VirtualFrame frame, Frame callerFrame) {
        // Extract arguments from current frame...
        int argCount = RArguments.getArgumentsLength(frame);
        assert RArguments.getSignature(frame).getLength() == argCount;
        Object[] argValues = new Object[argCount];
        int fi = 0;
        for (; fi < argCount; ++fi) {
            argValues[fi] = RArguments.getArgument(frame, fi);
        }
        EvaluatedArguments reorderedArgs = reorderArgs(frame, targetFunction, argValues, RArguments.getSignature(frame), false, getSourceSection());
        return executeHelper2(frame, callerFrame.materialize(), reorderedArgs.getEvaluatedArgs(), reorderedArgs.getSignature());
    }

    private Object executeHelper(VirtualFrame callerFrame, Object[] args) {
        // Extract arguments from current frame...
        int argCount = args.length;
        int argListSize = argCount;

        boolean hasVarArgs = false;
        for (int fi = 0; fi < argCount; ++fi) {
            Object arg = args[fi];
            if (arg instanceof RArgsValuesAndNames) {
                hasVarArgs = true;
                argListSize += ((RArgsValuesAndNames) arg).length() - 1;
            }
        }
        Object[] argValues;
        ArgumentsSignature signature;
        if (hasVarArgsProfile.profile(hasVarArgs)) {
            argValues = new Object[argListSize];
            String[] argNames = new String[argListSize];
            int index = 0;
            for (int fi = 0; fi < argCount; ++fi) {
                Object arg = args[fi];
                if (arg instanceof RArgsValuesAndNames) {
                    RArgsValuesAndNames varArgs = (RArgsValuesAndNames) arg;
                    Object[] varArgValues = varArgs.getValues();
                    ArgumentsSignature varArgSignature = varArgs.getSignature();
                    for (int i = 0; i < varArgs.length(); i++) {
                        argNames[index] = varArgSignature.getName(i);
                        addArg(argValues, varArgValues[i], index++);
                    }
                } else {
                    argNames[index] = suppliedSignature.getName(fi);
                    addArg(argValues, arg, index++);
                }
            }
            signature = ArgumentsSignature.get(argNames);
        } else {
            argValues = new Object[argCount];
            for (int i = 0; i < argCount; i++) {
                addArg(argValues, args[i], i);
            }
            signature = suppliedSignature;
        }

        // ...and use them as 'supplied' arguments...
        EvaluatedArguments evaledArgs = EvaluatedArguments.create(argValues, signature);

        // ...to match them against the chosen function's formal arguments
        EvaluatedArguments reorderedArgs = ArgumentMatcher.matchArgumentsEvaluated(callerFrame, targetFunction, evaledArgs, getEncapsulatingSourceSection(), promiseHelper, false);
        return executeHelper2(callerFrame, callerFrame.materialize(), reorderedArgs.getEvaluatedArgs(), reorderedArgs.getSignature());
    }

    private static void addArg(Object[] values, Object value, int index) {
        if (RMissingHelper.isMissing(value) || (value instanceof RPromise && RMissingHelper.isMissingName((RPromise) value))) {
            values[index] = null;
        } else {
            values[index] = value;
        }
    }

    private Object executeHelper2(VirtualFrame frame, MaterializedFrame callerFrame, Object[] arguments, ArgumentsSignature signature) {
        Object[] argObject = RArguments.createS3Args(targetFunction, getSourceSection(), null, RArguments.getDepth(callerFrame) + 1, arguments, signature);
        // todo: cannot create frame descriptors in compiled code
        genCallEnv = callerFrame;
        defineVarsAsArguments(argObject, genericName, klass, genCallEnv, genDefEnv);
        RArguments.setS3Method(argObject, targetFunctionName);
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
