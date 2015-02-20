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
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public final class NextMethodDispatchNode extends S3DispatchLegacyNode {

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

    NextMethodDispatchNode(String genericName, RStringVector type, Object[] args, ArgumentsSignature suppliedSignature, String storedFunctionName) {
        super(genericName, suppliedSignature);
        this.type = type;
        this.args = args;
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
    public Object executeGeneric(VirtualFrame frame, RStringVector aType) {
        readGenericVars(frame);
        findTargetFunction(frame);
        storeValues();
        return executeHelper(frame);
    }

    private EvaluatedArguments processArgs(VirtualFrame frame) {
        int argsLength = args == null ? 0 : args.length;
        // Extract arguments from current frame...
        int funArgsLength = RArguments.getArgumentsLength(frame);
        ArgumentsSignature signature = RArguments.getSignature(frame);
        assert signature.getLength() == funArgsLength;
        Object[] funArgValues = new Object[funArgsLength + argsLength];
        String[] funArgNames = new String[funArgsLength + argsLength];
        int index = 0;
        for (int fi = 0; fi < funArgsLength; fi++) {
            Object argVal = RArguments.getArgument(frame, fi);
            if (argVal instanceof RArgsValuesAndNames) {
                RArgsValuesAndNames varArgs = (RArgsValuesAndNames) argVal;
                int varArgsLength = varArgs.length();
                if (varArgsLength != 1) {
                    funArgValues = Utils.resizeArray(funArgValues, funArgValues.length + varArgsLength - 1);
                }
                System.arraycopy(varArgs.getValues(), 0, funArgValues, index, varArgsLength);
                if (varArgsLength != 1) {
                    funArgNames = Utils.resizeArray(funArgNames, funArgNames.length + varArgsLength - 1);
                }
                for (int i = 0; i < varArgsLength; i++) {
                    funArgNames[index++] = varArgs.getSignature().getName(i);
                }
            } else {
                funArgValues[index] = argVal;
                funArgNames[index] = signature.getName(fi);
                index++;
            }
        }
        if (argsLength > 0) {
            for (int i = 0; i < argsLength; i++) {
                funArgValues[index] = args[i];
                if (suppliedSignature != null) {
                    funArgNames[index] = suppliedSignature.getName(i);
                }
                index++;
            }
        }


        ArgumentsSignature evaluatedSignature = ArgumentsSignature.get(funArgNames);

        EvaluatedArguments evaledArgs = EvaluatedArguments.create(funArgValues, evaluatedSignature);
        // ...to match them against the chosen function's formal arguments
        EvaluatedArguments reorderedArgs = ArgumentMatcher.matchArgumentsEvaluated(frame, targetFunction, evaledArgs, getSourceSection(), promiseHelper, true);
        return reorderedArgs;
    }

    private Object executeHelper(VirtualFrame frame) {
        EvaluatedArguments evaledArgs = processArgs(frame);
        Object[] argObject = RArguments.createS3Args(targetFunction, getSourceSection(), null, RArguments.getDepth(frame) + 1, evaledArgs.getEvaluatedArgs(), evaledArgs.getSignature());
        defineVarsAsArguments(argObject, genericName, klass, genCallEnv, genDefEnv);
        if (storedFunctionName != null) {
            RArguments.setS3Method(argObject, storedFunctionName);
        } else {
            RArguments.setS3Method(argObject, targetFunctionName);
        }
        if (hasGroup) {
            RArguments.setS3Group(argObject, this.group);
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

        Object method = RArguments.getS3Method(frame);
        String functionName;
        if (method == null) {
            functionName = null;
        } else if (method instanceof String) {
            functionName = (String) method;
        } else {
            functionName = ((RStringVector) method).getDataAt(0);
        }
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
