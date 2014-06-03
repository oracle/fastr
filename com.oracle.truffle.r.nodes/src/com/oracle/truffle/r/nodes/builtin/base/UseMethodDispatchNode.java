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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public class UseMethodDispatchNode extends S3DispatchNode {

    private MaterializedFrame storedEnclosingFrame;

    UseMethodDispatchNode(final String generic, final RStringVector type) {
        this.genericName = generic;
        this.type = type;
    }

    @Override
    public DispatchNode.FunctionCall execute(VirtualFrame frame) {
        Frame callerFrame = Utils.getCallerFrame(FrameAccess.MATERIALIZE);
        initArgNodes(frame);
        if (targetFunction == null || !isFirst || !findFunction(targetFunctionName, callerFrame)) {
            findTargetFunction(callerFrame);
        }
        funCall = new DispatchNode.FunctionCall(targetFunction, CallArgumentsNode.create(argNodes, null));
        setEnvironment(frame);
        return funCall;
    }

    public Object executeNoCache(VirtualFrame frame) {
        Frame callerFrame = Utils.getCallerFrame(FrameAccess.MATERIALIZE);
        findTargetFunction(callerFrame);
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
        Object[] argObject = RArguments.createS3Args(targetFunction, argList.toArray());
        VirtualFrame newFrame = Truffle.getRuntime().createVirtualFrame(argObject, new FrameDescriptor());
        genCallEnv = callerFrame;
        defineVarsNew(newFrame);
        RArguments.setS3Method(newFrame, targetFunctionName);
        System.out.println("wrting targetFunctionName " + targetFunctionName);
        return funCallNode.call(newFrame, targetFunction.getTarget(), argObject);
    }

    @Override
    public DispatchNode.FunctionCall execute(VirtualFrame frame, RStringVector aType) {
        this.type = aType;
        Frame callerFrame = Utils.getCallerFrame(FrameAccess.MATERIALIZE);
        initArgNodes(frame);
        findTargetFunction(callerFrame);
        funCall = new DispatchNode.FunctionCall(targetFunction, CallArgumentsNode.create(argNodes, null));
        setEnvironment(frame);
        return funCall;
    }

    private void initArgNodes(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        RNode[] nodes = new RNode[RArguments.getArgumentsLength(frame)];
        for (int i = 0; i < nodes.length; ++i) {
            nodes[i] = ConstantNode.create(RArguments.getArgument(frame, i));
        }
        argNodes = insert(nodes);
    }

    private void findTargetFunction(Frame callerFrame) {
        for (int i = 0; i < this.type.getLength(); ++i) {
            findFunction(this.genericName, this.type.getDataAt(i), callerFrame);
            if (targetFunction != null) {
                RStringVector classVec = null;
                if (i > 0) {
                    isFirst = false;
                    classVec = RDataFactory.createStringVector(Arrays.copyOfRange(this.type.getDataCopy(), i, this.type.getLength()), true);
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
                CompilerDirectives.transferToInterpreter();
                throw RError.getUnknownFunctionUseMethod(getEncapsulatingSourceSection(), this.genericName, RRuntime.toString(this.type));
            }
        }
    }

    private void setEnvironment(VirtualFrame frame) {
        genCallEnv = Utils.getCallerFrame(FrameAccess.MATERIALIZE);
        defineVars(frame);
        wvnMethod.execute(frame, targetFunctionName);
        storedEnclosingFrame = RArguments.getEnclosingFrame(frame);
        RArguments.setEnclosingFrame(frame, targetFunction.getEnclosingFrame());
        targetFunction.setEnclosingFrame(frame.materialize());
    }

    @Override
    protected void unsetEnvironment(VirtualFrame frame) {
        // Remove all generic variables added by defineVars
        removeVars(frame);
        targetFunction.setEnclosingFrame(RArguments.getEnclosingFrame(frame));
        RArguments.setEnclosingFrame(frame, storedEnclosingFrame);
    }
}
