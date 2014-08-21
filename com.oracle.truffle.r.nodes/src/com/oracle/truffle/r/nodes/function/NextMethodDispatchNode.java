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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public class NextMethodDispatchNode extends S3DispatchNode {

    @Child protected ReadVariableNode rvnDefEnv;
    @Child protected ReadVariableNode rvnCallEnv;
    @Child protected ReadVariableNode rvnGroup;
    @Child protected ReadVariableNode rvnMethod;
    @Child protected WriteVariableNode wvnGroup;
    private String group;
    private String lastGroup;
    private String storedFunctionName;
    private String lastStoredFunctionName;
    private String baseName;
    private String[] prefix;
    private boolean hasGroup;
    private boolean lastHasGroup;
    private final Object[] args;

    NextMethodDispatchNode(final String genericName, final RStringVector type, final Object[] args) {
        this.genericName = genericName;
        this.type = type;
        this.args = args;
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

    private Object executeHelper(VirtualFrame frame) {
        // Merge arguments passed to current function with arguments passed to NextMethod call.
        final Object[] mergedArgs = new Object[RArguments.getArgumentsLength(frame) + args.length];
        RArguments.copyArgumentsInto(frame, mergedArgs);
        System.arraycopy(args, 0, mergedArgs, RArguments.getArgumentsLength(frame), args.length);
        Object[] argObject = RArguments.createS3Args(targetFunction, mergedArgs);
        final VirtualFrame newFrame = Truffle.getRuntime().createVirtualFrame(argObject, new FrameDescriptor());
        defineVarsNew(newFrame);
        if (storedFunctionName != null) {
            RArguments.setS3Method(newFrame, storedFunctionName);
        } else {
            RArguments.setS3Method(newFrame, targetFunctionName);
        }
        if (hasGroup) {
            RArguments.setS3Group(newFrame, this.group);
        }
        return funCallNode.call(frame, targetFunction.getTarget(), argObject);
    }

    private boolean isSame() {
        return lastHasGroup == hasGroup && isEqual(lastGroup, group) && isEqual(lastStoredFunctionName, storedFunctionName);
    }

    private static boolean isEqual(final String a, final String b) {
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
        // TODO if(genDefEnv == null) genDefEnv = globalenv
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
        storedFunctionName = RArguments.getS3Method(frame);
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
