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
        Frame callerFrame = Utils.getCallerFrame(FrameAccess.MATERIALIZE);
        if (targetFunction == null) {
            findTargetFunction(callerFrame);
        }
        return executeHelper(frame, callerFrame);
    }

    @Override
    public Object execute(VirtualFrame frame, RStringVector aType) {
        this.type = aType;
        Frame callerFrame = Utils.getCallerFrame(FrameAccess.MATERIALIZE);
        findTargetFunction(callerFrame);
        return executeHelper(frame, callerFrame);
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

    private static String[] resizeNamesArray(String[] oldNames, int newSize) {
        String[] newNames = new String[newSize];
        if (oldNames != null) {
            for (int i = 0; i < newSize; i++) {
                newNames[i] = oldNames[i];
            }
        }
        return newNames;
    }

    private static Object[] resizeValuesArray(Object[] oldValues, int newSize) {
        Object[] newValues = new Object[newSize];
        if (oldValues != null) {
            for (int i = 0; i < newSize; i++) {
                newValues[i] = oldValues[i];
            }
        }
        return newValues;
    }

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
            // conditional vs. allocation in extractArgs() - is it worth it?
            Object arg = RArguments.getArgument(frame, fi);
            if (arg instanceof ArgsValuesAndNames) {
                argValues = resizeValuesArray(argValues, argListSize);
                argNames = resizeNamesArray(argNames, argListSize);
                ArgsValuesAndNames varArgsContainer = (ArgsValuesAndNames) arg;
                Object[] varArgsValues = varArgsContainer.getValues();
                String[] varArgsNames = varArgsContainer.getNames();
                boolean allNamesNull = true;
                for (int i = 0; i < varArgsContainer.length(); i++) {
                    addArg(argValues, varArgsValues[i], index);
                    String name = varArgsNames[i];
                    allNamesNull |= name != null;
                    argNames[index] = name;
                    index++;
                }
                if (allNamesNull && !hasNames) {
                    argNames = null;
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
        // TODO Need rearrange here! suppliedArgsNames are in supplied order, argList in formal!!!
        EvaluatedArguments evaledArgs = EvaluatedArguments.create(argValues, argNames);
        // ...to match them against the chosen function's formal arguments
        EvaluatedArguments reorderedArgs = ArgumentMatcher.matchArgumentsEvaluated(targetFunction, evaledArgs, getEncapsulatingSourceSection());
        return executeHelper2(callerFrame, reorderedArgs.getEvaluatedArgs(), reorderedArgs.getNames());
    }

    private Object executeHelper(Frame callerFrame, Object[] args) {
        // Extract arguments from current frame...
        int argCount = args.length;
        int argListSize = argCount;
        Object[] argValues = new Object[argListSize];
        int fi = 0;
        int index = 0;
        for (; fi < argCount; ++fi) {
            // conditional vs. allocation in extractArgs() - is it worth it?
            Object arg = args[fi];
            if (arg instanceof Object[]) {
                Object[] varArgs = (Object[]) arg;
                argListSize += varArgs.length;
                argValues = resizeValuesArray(argValues, argListSize);

                for (Object varArg : varArgs) {
                    addArg(argValues, varArg, index++);
                }
            } else {
                addArg(argValues, arg, index++);
            }
        }

        // ...and use them as 'supplied' arguments...
        // TODO Need rearrange here! suppliedArgsNames are in supplied order, argList in formal!!!
        EvaluatedArguments evaledArgs = EvaluatedArguments.create(argValues, null);
        // ...to match them against the chosen function's formal arguments
        EvaluatedArguments reorderedArgs = ArgumentMatcher.matchArgumentsEvaluated(targetFunction, evaledArgs, getEncapsulatingSourceSection());
        return executeHelper2(callerFrame, reorderedArgs.getEvaluatedArgs(), reorderedArgs.getNames());
    }

    private static void addArg(Object[] values, Object value, int index) {
        if (RMissingHelper.isMissing(value)) {
            values[index] = null;
        } else {
            values[index] = value;
        }
    }

    @SlowPath
    private Object executeHelper2(Frame callerFrame, Object[] arguments, String[] argNames) {
        Object[] argObject = RArguments.createS3Args(targetFunction, arguments, argNames);
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
