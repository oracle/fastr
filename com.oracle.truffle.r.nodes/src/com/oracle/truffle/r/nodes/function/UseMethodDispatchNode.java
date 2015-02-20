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

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.frame.*;

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
public class UseMethodDispatchNode extends S3DispatchNode {

    private final BranchProfile errorProfile = BranchProfile.create();
    private final ConditionProfile topLevelFrameProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile callerFrameSlotPath = ConditionProfile.createBinaryProfile();

    @CompilationFinal private final String[] suppliedArgNames;

    UseMethodDispatchNode(String genericName, RStringVector type, String[] evaledArgNames) {
        super(genericName);
        this.type = type;
        this.suppliedArgNames = evaledArgNames;
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
            findTargetFunction(RArguments.getEnclosingFrame(frame));
        }
        return executeHelper(frame, funFrame);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame, RStringVector aType) {
        this.type = aType;
        Frame funFrame = getCallerFrame(frame);
        findTargetFunction(RArguments.getEnclosingFrame(frame));
        return executeHelper(frame, funFrame);
    }

    @Override
    public Object executeInternal(VirtualFrame frame, Object[] args) {
        if (targetFunction == null) {
            // TBD getEnclosing?
            findTargetFunction(frame);
        }
        return executeHelper(frame, args);
    }

    @Override
    public Object executeInternalGeneric(VirtualFrame frame, RStringVector aType, Object[] args) {
        this.type = aType;
        // TBD getEnclosing?
        findTargetFunction(frame);
        return executeHelper(frame, args);
    }

    private Object executeHelper(VirtualFrame frame, Frame callerFrame) {
        // Extract arguments from current frame...
        int argCount = RArguments.getArgumentsLength(frame);
        assert RArguments.getNamesLength(frame) == 0 || RArguments.getNamesLength(frame) == argCount;
        boolean hasNames = RArguments.getNamesLength(frame) > 0;
        Object[] argValues = new Object[argCount];
        String[] argNames = hasNames ? new String[argCount] : null;
        int fi = 0;
        for (; fi < argCount; ++fi) {
            argValues[fi] = RArguments.getArgument(frame, fi);
            if (hasNames) {
                argNames[fi] = RArguments.getName(frame, fi);
            }
        }
        EvaluatedArguments reorderedArgs = reorderArgs(frame, targetFunction, argValues, argNames, false, getSourceSection());
        return executeHelper2(callerFrame, reorderedArgs.getEvaluatedArgs(), reorderedArgs.getNames());
    }

    private Object executeHelper(VirtualFrame callerFrame, Object[] args) {
        // Extract arguments from current frame...
        int argCount = args.length;
        int argListSize = argCount;
        Object[] argValues = new Object[argListSize];
        int fi = 0;
        int index = 0;
        String[] argNames = suppliedArgNames;
        for (; fi < argCount; ++fi) {
            Object arg = args[fi];
            if (arg instanceof RArgsValuesAndNames) {
                RArgsValuesAndNames varArgs = (RArgsValuesAndNames) arg;
                Object[] varArgValues = varArgs.getValues();
                String[] varArgNames = varArgs.getNames();
                argListSize += varArgs.length() - 1;
                if (varArgNames != null) {
                    if (argNames == null) {
                        argNames = new String[argListSize];
                    } else {
                        argNames = Utils.resizeArray(argNames, argListSize);
                        System.arraycopy(suppliedArgNames, fi, argNames, fi + varArgs.length() - 1, argCount - fi);
                    }
                }
                argValues = Utils.resizeArray(argValues, argListSize);

                for (int i = 0; i < varArgs.length(); i++) {
                    addArg(argValues, varArgValues[i], index);
                    if (varArgNames != null) {
                        argNames[index] = varArgNames[i];
                    }
                    index++;
                }

            } else {
                addArg(argValues, arg, index++);
            }
        }

        // ...and use them as 'supplied' arguments...
        EvaluatedArguments evaledArgs = EvaluatedArguments.create(argValues, argNames);
        // ...to match them against the chosen function's formal arguments
        EvaluatedArguments reorderedArgs = ArgumentMatcher.matchArgumentsEvaluated(callerFrame, targetFunction, evaledArgs, getEncapsulatingSourceSection(), promiseHelper, false);
        return executeHelper2(callerFrame, reorderedArgs.getEvaluatedArgs(), reorderedArgs.getNames());
    }

    private static void addArg(Object[] values, Object value, int index) {
        if (RMissingHelper.isMissing(value) || (value instanceof RPromise && RMissingHelper.isMissingName((RPromise) value))) {
            values[index] = null;
        } else {
            values[index] = value;
        }
    }

    @TruffleBoundary
    private Object executeHelper2(Frame callerFrame, Object[] arguments, String[] argNames) {
        Object[] argObject = RArguments.createS3Args(targetFunction, getSourceSection(), null, RArguments.getDepth(callerFrame) + 1, arguments, argNames);
        // todo: cannot create frame descriptors in compiled code
        FrameDescriptor frameDescriptor = new FrameDescriptor();
        FrameSlotChangeMonitor.initializeFrameDescriptor(frameDescriptor, true);
        VirtualFrame newFrame = Truffle.getRuntime().createVirtualFrame(argObject, frameDescriptor);
        genCallEnv = callerFrame;
        defineVarsAsArguments(newFrame);
        RArguments.setS3Method(newFrame, targetFunctionName);
        return indirectCallNode.call(newFrame, targetFunction.getTarget(), argObject);
    }

    private void findTargetFunction(Frame callerFrame) {
        findTargetFunctionLookup(callerFrame);
        if (targetFunction == null) {
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNKNOWN_FUNCTION_USE_METHOD, this.genericName, RRuntime.toString(this.type));
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
