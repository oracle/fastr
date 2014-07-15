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
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.function.*;
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
        FormalArguments formals = ((RRootNode) targetFunction.getTarget().getRootNode()).getFormalArguments();

        int defaultCount = formals.getArgsCount();
        int argCount = RArguments.getArgumentsLength(frame);
        int argListSize = Math.max(argCount, defaultCount);
        ArrayList<Object> argList = new ArrayList<>(argListSize);
        int fi = 0;
        for (; fi < argCount; ++fi) {
            Object arg = RArguments.getArgument(frame, fi);
            if (arg instanceof Object[]) {
                Object[] arrayArg = (Object[]) arg;
                argListSize += arrayArg.length;
                argList.ensureCapacity(argListSize);

                for (Object anArg : arrayArg) {
                    argList.add(anArg);
                }
            } else {
                // TODO Gero: Replace with proper missing handling when available!
                if (arg == RMissing.instance) {
                    argList.add(formals.getDefaultArgs()[fi].execute(frame));
                } else {
                    argList.add(arg);
                }
            }
        }
        // TODO Gero, move this to ArgumentsMatcher

        while (fi < defaultCount) {
            argList.add(formals.getDefaultArgs()[fi].execute(frame));
            fi++;
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
