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
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public class GroupDispatchNode extends S3DispatchNode {

    @Child protected WriteVariableNode wvnGroup;
    @Child protected CallArgumentsNode callArgNodes;
    private final String groupName;
    protected boolean writeGroup;
    private boolean isEnvSet;
    protected RStringVector typeLast;

    protected GroupDispatchNode(final String aGenericName, final String groupName, final RNode[] args) {
        this.genericName = aGenericName;
        this.groupName = groupName;
        this.argNodes = insert(args);
    }

    public static GroupDispatchNode create(final String aGenericName, final RNode[] args) {
        final String grpName = RRuntime.getGroup(aGenericName);
        if (grpName == RRuntime.GROUP_OPS) {
            return new OpsGroupDispatchNode(aGenericName, grpName, args);
        }
        return new GroupDispatchNode(aGenericName, grpName, args);
    }

    protected void findTargetFunction(VirtualFrame frame) {
        final String[] prefix = {genericName, groupName};
        for (int i = 0; i < this.type.getLength(); ++i) {
            for (int j = 0; j < prefix.length; ++j) {
                findFunction(prefix[j], this.type.getDataAt(i), frame);
                if (targetFunction != null) {
                    RStringVector classVec = null;
                    if (i > 0) {
                        isFirst = false;
                        classVec = RDataFactory.createStringVector(Arrays.copyOfRange(this.type.getDataCopy(), i, this.type.getLength()), true);
                    } else {
                        isFirst = true;
                        classVec = this.type.copyResized(this.type.getLength(), false);
                    }
                    klass = classVec;
                    if (j == 1) {
                        writeGroup = true;
                    } else {
                        writeGroup = false;
                    }
                    break;
                }
            }
        }
    }

    @Override
    protected void unsetEnvironment(VirtualFrame frame) {
        if (isEnvSet) {
            targetFunction.setEnclosingFrame(RArguments.getEnclosingFrame(targetFunction.getEnclosingFrame()));
            isEnvSet = false;
        }
    }

    private void setEnvironment() {
        final VirtualFrame newFrame = Truffle.getRuntime().createVirtualFrame(RArguments.create(), new FrameDescriptor());
        RArguments.setEnclosingFrame(newFrame, targetFunction.getEnclosingFrame());
        defineVars(newFrame);
        wvnMethod.execute(newFrame, dotMethod);
        if (writeGroup) {
            wvnGroup = initWvn(wvnGroup, RRuntime.RDotGroup);
            wvnGroup.execute(newFrame, this.groupName);
        }
        targetFunction.setEnclosingFrame(newFrame.materialize());
        isEnvSet = true;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (callArgNodes == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callArgNodes = insert(CallArgumentsNode.create(argNodes, null));
        }
        if ((this.type = getArgClass(frame, 0)) != null) {
            if (targetFunction != null && isEqualType(this.type, this.typeLast) && findFunction(targetFunctionName, frame) && isFirst) {
                return executeHelper();
            }
            findTargetFunction(frame);
            if (targetFunction != null) {
                dotMethod = RDataFactory.createStringVector(new String[]{targetFunctionName}, true);
                return executeHelper();
            }
        }
        return callBuiltin();
    }

    protected static boolean isEqualType(final RStringVector currentType, final RStringVector lastType) {
        if (currentType == null && lastType == null) {
            return true;
        }
        if (currentType == null || lastType == null) {
            return false;
        }
        if (currentType.getLength() != lastType.getLength()) {
            return false;
        }
        for (int i = 0; i < currentType.getLength(); ++i) {
            if (!currentType.getDataAt(i).equals(lastType.getDataAt(i))) {
                return false;
            }
        }
        return true;
    }

    protected Object callBuiltin() {
        this.typeLast = null;
        return new DispatchNode.FunctionCall(RContext.getLookup().lookup(this.genericName), callArgNodes);
    }

    protected Object executeHelper() {
        setEnvironment();
        this.typeLast = this.type;
        return new DispatchNode.FunctionCall(targetFunction, callArgNodes);
    }

    protected RStringVector getArgClass(VirtualFrame frame, int index) {
        RAbstractVector arg1;
        if (index >= argNodes.length) {
            return null;
        }
        try {
            arg1 = argNodes[index].executeRAbstractVector(frame);
        } catch (UnexpectedResultException e) {
            return null;
        }
        if (!arg1.isObject()) {
            return null;
        }
        return arg1.getClassHierarchy();
    }
}
