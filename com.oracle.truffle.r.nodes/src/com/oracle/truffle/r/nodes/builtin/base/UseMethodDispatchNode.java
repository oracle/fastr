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

import com.oracle.truffle.api.frame.*;
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
        VirtualFrame callerFrame = (VirtualFrame) frame.getCaller().unpack();
        if (targetFunction != null && findFunction(targetFunctionName, callerFrame)) {
            assert (funCall != null);
        } else {
            findTargetFunction(callerFrame);
            initArgNodes(frame);
            funCall = new DispatchNode.FunctionCall(targetFunction, CallArgumentsNode.create(argNodes, null));
        }
        setEnvironment(frame);
        return funCall;
    }

    private void findTargetFunction(VirtualFrame callerFrame) {
        for (int i = 0; i < this.type.getLength(); ++i) {
            findFunction(this.genericName, this.type.getDataAt(i), callerFrame);
            if (targetFunction != null) {
                RStringVector classVec = null;
                if (i > 0) {
                    classVec = RDataFactory.createStringVector(Arrays.copyOfRange(this.type.getDataCopy(), i, this.type.getLength()), true);
                    LinkedHashMap<String, Object> attr = new LinkedHashMap<>();
                    attr.put(RRuntime.PREVIOUS_ATTR_KEY, this.type.copyResized(this.type.getLength(), false));
                    classVec.setAttributes(attr);
                } else {
                    classVec = this.type.copyResized(this.type.getLength(), false);
                }
                klass = classVec;
                break;
            }
        }
        if (targetFunction == null) {
            findFunction(this.genericName, RRuntime.DEFAULT, callerFrame);
            if (targetFunction == null) {
                throw RError.getUnknownFunctionUseMethod(getEncapsulatingSourceSection(), this.genericName, RRuntime.toString(this.type));
            }
        }
    }

    private void initArgNodes(VirtualFrame frame) {
        if (argNodes != null) {
            return;
        }
        final RArguments currentArguments = frame.getArguments(RArguments.class);
        argNodes = new RNode[currentArguments.getLength()];
        for (int i = 0; i < currentArguments.getLength(); ++i) {
            argNodes[i] = ConstantNode.create(currentArguments.getArgument(i));
        }
    }

    private void setEnvironment(VirtualFrame frame) {
        genCallEnv = frame.getCaller().unpack().materialize();
        defineVars(frame);
        storedEnclosingFrame = frame.getArguments(RArguments.class).getEnclosingFrame();
        frame.getArguments(RArguments.class).setEnclosingFrame(targetFunction.getEnclosingFrame());
        targetFunction.setEnclosingFrame(frame.materialize());
    }

    @Override
    protected void unsetEnvironment(VirtualFrame frame) {
        // TODO:Remove all generic variables added by defineVars
        targetFunction.setEnclosingFrame(frame.getArguments(RArguments.class).getEnclosingFrame());
        frame.getArguments(RArguments.class).setEnclosingFrame(storedEnclosingFrame);
    }
}
