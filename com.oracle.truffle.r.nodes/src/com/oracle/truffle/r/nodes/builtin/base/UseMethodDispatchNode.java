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

package com.oracle.truffle.r.nodes.builtin.base;

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
        if (targetFunction == null || !isFirst) {
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

    private Object executeHelper(VirtualFrame frame, Frame callerFrame) {
        List<Object> argList = new ArrayList<>();
        for (int i = 0; i < RArguments.getArgumentsLength(frame); ++i) {
            Object arg = RArguments.getArgument(frame, i);
            if (arg instanceof Object[]) {
                for (Object anArg : (Object[]) arg) {
                    argList.add(anArg);
                }
            } else {
                argList.add(arg);
            }
        }

        return executeHelper2(callerFrame, argList);
    }

    @SlowPath
    private Object executeHelper2(Frame callerFrame, List<Object> argList) {
        Object[] argObject = RArguments.createS3Args(targetFunction, argList.toArray());
        VirtualFrame newFrame = Truffle.getRuntime().createVirtualFrame(argObject, new FrameDescriptor());
        genCallEnv = callerFrame;
        defineVarsNew(newFrame);
        RArguments.setS3Method(newFrame, targetFunctionName);
        return funCallNode.call(newFrame, targetFunction.getTarget(), argObject);
    }

    @SlowPath
    private void findTargetFunction(Frame callerFrame) {
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
        if (targetFunction == null) {
            findFunction(this.genericName, RRuntime.DEFAULT, callerFrame);
            if (targetFunction == null) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNKNOWN_FUNCTION_USE_METHOD, this.genericName, RRuntime.toString(this.type));
            }
        }
    }
}
