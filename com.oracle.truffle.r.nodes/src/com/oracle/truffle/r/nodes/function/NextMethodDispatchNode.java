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

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.frame.*;

public class NextMethodDispatchNode extends S3DispatchNode {

    @Child private ReadVariableNode rvnDefEnv;
    @Child private ReadVariableNode rvnCallEnv;
    @Child private ReadVariableNode rvnGroup;
    @Child private ReadVariableNode rvnMethod;
    @Child private WriteVariableNode wvnGroup;
    private String group;
    private String lastGroup;
    private String storedFunctionName;
    private String lastStoredFunctionName;
    private String baseName;
    private String[] prefix;
    private boolean hasGroup;
    private boolean lastHasGroup;
    @CompilationFinal private final Object[] args;
    @CompilationFinal private final String[] argNames;

    NextMethodDispatchNode(String genericName, RStringVector type, Object[] args, String[] argNames, String storedFunctionName) {
        this.genericName = genericName;
        this.type = type;
        this.args = args;
        this.argNames = argNames;
        this.storedFunctionName = storedFunctionName;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        readGenericVars(frame);
        if (!isSame() || !isFirst) {
            findTargetFunction(frame);
            storeValues();
        }
        return executeHelper(frame);
    }

    @Override
    public Object execute(VirtualFrame frame, final RStringVector aType) {
        readGenericVars(frame);
        findTargetFunction(frame);
        storeValues();
        return executeHelper(frame);
    }

    private EvaluatedArguments processArgs(VirtualFrame frame) {
        int argsLength = args == null ? 0 : args.length;
        // Extract arguments from current frame...
        int funArgsLength = RArguments.getArgumentsLength(frame);
        assert RArguments.getNamesLength(frame) == 0 || RArguments.getNamesLength(frame) == funArgsLength;
        boolean hasNames = RArguments.getNamesLength(frame) > 0;
        Object[] funArgValues = new Object[funArgsLength + argsLength];
        String[] funArgNames = hasNames ? new String[funArgsLength + argsLength] : null;
        int index = 0;
        for (int fi = 0; fi < funArgsLength; fi++) {
            Object argVal = RArguments.getArgument(frame, fi);
            if (argVal instanceof RArgsValuesAndNames) {
                RArgsValuesAndNames varArgs = (RArgsValuesAndNames) argVal;
                int varArgsLength = varArgs.length();
                if (varArgsLength > 1) {
                    funArgValues = Utils.resizeArray(funArgValues, funArgValues.length + varArgsLength - 1);
                }
                System.arraycopy(varArgs.getValues(), 0, funArgValues, index, varArgsLength);
                if (hasNames) {
                    if (varArgsLength > 1) {
                        funArgNames = Utils.resizeArray(funArgNames, funArgNames.length + varArgsLength - 1);
                    }
                    if (varArgs.getNames() != null) {
                        System.arraycopy(varArgs.getNames(), 0, funArgNames, index, varArgsLength);
                    }
                } else if (varArgs.getNames() != null) {
                    funArgNames = new String[funArgsLength + argsLength];
                    System.arraycopy(varArgs.getNames(), 0, funArgNames, index, varArgsLength);
                }
                index += varArgsLength;
            } else {
                funArgValues[index] = argVal;
                if (hasNames) {
                    funArgNames[index] = RArguments.getName(frame, fi);
                }
                index++;
            }
        }
        if (argsLength > 0) {
            if (funArgNames == null && argNames != null) {
                funArgNames = new String[funArgsLength + argsLength];
            }
            for (int i = 0; i < argsLength; i++) {
                funArgValues[index] = args[i];
                if (argNames != null) {
                    funArgNames[index] = argNames[i];
                }
                index++;
            }
        }
        EvaluatedArguments evaledArgs = EvaluatedArguments.create(funArgValues, funArgNames);
        // ...to match them against the chosen function's formal arguments
        EvaluatedArguments reorderedArgs = ArgumentMatcher.matchArgumentsEvaluated(frame, targetFunction, evaledArgs, getSourceSection(), promiseHelper, true);
        return reorderedArgs;
    }

    private Object executeHelper(VirtualFrame frame) {
        EvaluatedArguments evaledArgs = processArgs(frame);
        Object[] argObject = RArguments.createS3Args(targetFunction, getSourceSection(), RArguments.getDepth(frame) + 1, evaledArgs.getEvaluatedArgs(), evaledArgs.getNames());
        // todo: cannot create frame descriptors in compiled code
        FrameDescriptor frameDescriptor = new FrameDescriptor();
        FrameSlotChangeMonitor.initializeFrameDescriptor(frameDescriptor, true);
        final VirtualFrame newFrame = Truffle.getRuntime().createVirtualFrame(argObject, frameDescriptor);
        defineVarsAsArguments(newFrame);
        if (storedFunctionName != null) {
            RArguments.setS3Method(newFrame, storedFunctionName);
        } else {
            RArguments.setS3Method(newFrame, targetFunctionName);
        }
        if (hasGroup) {
            RArguments.setS3Group(newFrame, this.group);
        }
        return indirectCallNode.call(frame, targetFunction.getTarget(), argObject);
    }

    private boolean isSame() {
        return lastHasGroup == hasGroup && isEqual(lastGroup, group) && isEqual(lastStoredFunctionName, storedFunctionName);
    }

    private static boolean isEqual(String a, String b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        return a.equals(b);
    }

    private void findTargetFunction(VirtualFrame frame) {
        int nextClassIndex = 0;
        String currentFunctionName = storedFunctionName == null ? RArguments.getFunction(frame).getName() : storedFunctionName;
        for (int i = 0; i < type.getLength(); ++i) {
            if (RRuntime.toString(new StringBuffer(baseName).append(RRuntime.RDOT).append(type.getDataAt(i))).equals(currentFunctionName)) {
                nextClassIndex = i + 1;
                break;
            }
        }
        final int firstClassIndex = nextClassIndex;
        int index = 0;
        // First try generic.class then group.class.
        for (; nextClassIndex < type.getLength() && targetFunction == null; ++nextClassIndex) {
            for (; index < prefix.length && targetFunction == null; findFunction(prefix[index++], type.getDataAt(nextClassIndex), genCallEnv)) {
            }
        }
        if (firstClassIndex == nextClassIndex && index == 1) {
            isFirst = true;
        } else {
            isFirst = false;
        }
        if (targetFunction == null) {
            findFunction(this.genericName, RRuntime.DEFAULT, genCallEnv);
        }
        if (targetFunction == null) {
            findFunction(this.genericName, frame);
            if (targetFunction == null || !targetFunction.isBuiltin()) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.NO_METHOD_FOUND);
            }
        }
        RStringVector classVec = null;
        if (nextClassIndex == type.getLength()) {
            classVec = RDataFactory.createStringVector("");
        } else {
            classVec = RDataFactory.createStringVector(Arrays.copyOfRange(type.getDataWithoutCopying(), nextClassIndex, type.getLength()), true);
        }
        classVec.setAttr(RRuntime.PREVIOUS_ATTR_KEY, type.copyResized(type.getLength(), false));
        klass = classVec;
    }

    private void storeValues() {
        lastHasGroup = hasGroup;
        lastGroup = group;
        lastStoredFunctionName = storedFunctionName;
    }

    private void readGenericVars(VirtualFrame frame) {
        genDefEnv = RArguments.getS3DefEnv(frame);
        if (genDefEnv == null) {
            genDefEnv = RArguments.getEnclosingFrame(frame);
        }
        genCallEnv = RArguments.getS3CallEnv(frame);
        if (genCallEnv == null) {
            genCallEnv = frame.materialize();
        }
        group = RArguments.getS3Group(frame);
        if (group == null || group.isEmpty()) {
            handleMissingGroup();
        } else {
            handlePresentGroup();
        }
        String functionName = RArguments.getS3Method(frame);
        if (functionName != null) {
            storedFunctionName = functionName;
        }
    }

    private void handleMissingGroup() {
        baseName = genericName;
        prefix = new String[1];
        prefix[0] = genericName;
        hasGroup = false;
    }

    private void handlePresentGroup() {
        baseName = group;
        prefix = new String[2];
        prefix[0] = genericName;
        prefix[1] = group;
        hasGroup = true;
    }
}
