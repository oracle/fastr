/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.function;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public class UseMethodDispatchNode extends S3DispatchNode {

    UseMethodDispatchNode(final String generic, final RStringVector type) {
        this.genericName = generic;
        this.type = type;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Frame funFrame = Utils.getCallerFrame(FrameAccess.MATERIALIZE);
        if (funFrame == null) {
            funFrame = frame;
        }
        if (targetFunction == null) {
            findTargetFunction(funFrame);
        }
        return executeHelper(frame, funFrame);
    }

    @Override
    public Object execute(VirtualFrame frame, RStringVector aType) {
        this.type = aType;
        Frame funFrame = Utils.getCallerFrame(FrameAccess.MATERIALIZE);
        if (funFrame == null) {
            funFrame = frame;
        }
        findTargetFunction(funFrame);
        return executeHelper(frame, funFrame);
    }

    @Override
    public Object executeInternal(VirtualFrame frame, Object[] args) {
        if (targetFunction == null) {
            findTargetFunction(frame);
        }
        return executeHelper(frame, args);
    }

    @Override
    public Object executeInternal(VirtualFrame frame, RStringVector aType, Object[] args) {
        this.type = aType;
        findTargetFunction(frame);
        return executeHelper(frame, args);
    }

    // TODO: the executeHelper methods share quite a bit of code, but is it better or worse from
    // having one method with a rather convoluted control flow structure?

    private Object executeHelper(VirtualFrame frame, Frame callerFrame) {
        // Extract arguments from current frame...
        int argCount = RArguments.getArgumentsLength(frame);
        assert RArguments.getNamesLength(frame) == 0 || RArguments.getNamesLength(frame) == argCount;
        boolean hasNames = RArguments.getNamesLength(frame) > 0;
        int argListSize = argCount;
        Object[] argValues = new Object[argListSize];
        String[] argNames = hasNames ? new String[argListSize] : null;
        int fi = 0;
        int index = 0;
        for (; fi < argCount; ++fi) {
            Object arg = RArguments.getArgument(frame, fi);
            if (arg instanceof RArgsValuesAndNames) {
                RArgsValuesAndNames varArgsContainer = (RArgsValuesAndNames) arg;
                argListSize += varArgsContainer.length() - 1;
                argValues = Utils.resizeArray(argValues, argListSize);
                argNames = Utils.resizeArray(argNames, argListSize);
                Object[] varArgsValues = varArgsContainer.getValues();
                String[] varArgsNames = varArgsContainer.getNames();
                for (int i = 0; i < varArgsContainer.length(); i++) {
                    addArg(argValues, varArgsValues[i], index);
                    String name = varArgsNames == null ? null : varArgsNames[i];
                    argNames[index] = name;
                    index++;
                }
            } else {
                addArg(argValues, arg, index);
                if (hasNames) {
                    argNames[index] = RArguments.getName(frame, fi);
                }
                index++;
            }
        }

        // ...and use them as 'supplied' arguments...
        EvaluatedArguments evaledArgs = EvaluatedArguments.create(argValues, argNames);
        // ...to match them against the chosen function's formal arguments
        EvaluatedArguments reorderedArgs = ArgumentMatcher.matchArgumentsEvaluated(frame, targetFunction, evaledArgs, getEncapsulatingSourceSection());
        return executeHelper2(callerFrame, reorderedArgs.getEvaluatedArgs(), reorderedArgs.getNames());
    }

    private Object executeHelper(VirtualFrame callerFrame, Object[] args) {
        // Extract arguments from current frame...
        int argCount = args.length;
        int argListSize = argCount;
        Object[] argValues = new Object[argListSize];
        int fi = 0;
        int index = 0;
        for (; fi < argCount; ++fi) {
            Object arg = args[fi];
            if (arg instanceof Object[]) {
                Object[] varArgs = (Object[]) arg;
                argListSize += varArgs.length - 1;
                argValues = Utils.resizeArray(argValues, argListSize);

                for (Object varArg : varArgs) {
                    addArg(argValues, varArg, index++);
                }
            } else {
                addArg(argValues, arg, index++);
            }
        }

        // ...and use them as 'supplied' arguments...
        EvaluatedArguments evaledArgs = EvaluatedArguments.create(argValues, null);
        // ...to match them against the chosen function's formal arguments
        EvaluatedArguments reorderedArgs = ArgumentMatcher.matchArgumentsEvaluated(callerFrame, targetFunction, evaledArgs, getEncapsulatingSourceSection());
        return executeHelper2(callerFrame, reorderedArgs.getEvaluatedArgs(), reorderedArgs.getNames());
    }

    private static void addArg(Object[] values, Object value, int index) {
        if (value == RMissing.instance || (value instanceof RPromise && RMissingHelper.isMissingSymbol((RPromise) value))) {
            values[index] = null;
        } else {
            values[index] = value;
        }
    }

    @SlowPath
    private Object executeHelper2(Frame callerFrame, Object[] arguments, String[] argNames) {
        Object[] argObject = RArguments.createS3Args(targetFunction, funCallNode.getSourceSection(), arguments, argNames);
        VirtualFrame newFrame = Truffle.getRuntime().createVirtualFrame(argObject, new FrameDescriptor());
        genCallEnv = callerFrame;
        defineVarsNew(newFrame);
        RArguments.setS3Method(newFrame, targetFunctionName);
        return funCallNode.call(newFrame, targetFunction.getTarget(), argObject);
    }

    private void findTargetFunction(Frame callerFrame) {
        findTargetFunctionLookup(callerFrame);
        if (targetFunction == null) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNKNOWN_FUNCTION_USE_METHOD, this.genericName, RRuntime.toString(this.type));
        }
    }

    @SlowPath
    private void findTargetFunctionLookup(Frame callerFrame) {
        for (int i = 0; i < this.type.getLength(); ++i) {
            findFunction(this.genericName, this.type.getDataAt(i), callerFrame);
            if (targetFunction != null) {
                RStringVector classVec = null;
                if (i > 0) {
                    isFirst = false;
                    classVec = RDataFactory.createStringVector(Arrays.copyOfRange(this.type.getDataWithoutCopying(), i, this.type.getLength()), true);
                    classVec.setAttr(RRuntime.PREVIOUS_ATTR_KEY, this.type.copyResized(this.type.getLength(), false));
                } else {
                    isFirst = true;
                    classVec = this.type.copyResized(this.type.getLength(), false);
                }
                klass = classVec;
                break;
            }
        }
        if (targetFunction != null) {
            return;
        }
        findFunction(this.genericName, RRuntime.DEFAULT, callerFrame);
    }
}
