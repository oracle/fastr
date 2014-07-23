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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

import edu.umd.cs.findbugs.annotations.*;

public class GroupDispatchNode extends S3DispatchNode {

    @Child protected WriteVariableNode wvnGroup;
    @Child protected CallArgumentsNode callArgsNode;
    @Child protected ReadVariableNode builtInNode;
    @CompilationFinal private final String groupName;
    protected boolean writeGroup;
    private boolean isEnvSet;
    protected RStringVector typeLast;
    protected Object[] evaluatedArgs;
    protected RStringVector dotMethod;

    protected GroupDispatchNode(final String aGenericName, final String groupName, final CallArgumentsNode callArgNode) {
        this.genericName = aGenericName;
        this.groupName = groupName;
        this.callArgsNode = callArgNode;
    }

    @SuppressFBWarnings(value = "ES_COMPARING_STRINGS_WITH_EQ", justification = "GROUP_OPS is intended to be used as an identity")
    public static GroupDispatchNode create(final String aGenericName, final CallArgumentsNode callArgNode) {
        final String grpName = RGroupGenerics.getGroup(aGenericName);
        if (grpName == RGroupGenerics.GROUP_OPS) {
            return new OpsGroupDispatchNode(aGenericName, grpName, callArgNode);
        }
        return new GroupDispatchNode(aGenericName, grpName, callArgNode);
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
                        classVec = RDataFactory.createStringVector(Arrays.copyOfRange(this.type.getDataWithoutCopying(), i, this.type.getLength()), true);
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

    protected void unsetEnvironment() {
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
            wvnGroup = initWvn(wvnGroup, RGroupGenerics.RDotGroup);
            wvnGroup.execute(newFrame, this.groupName);
        }
        targetFunction.setEnclosingFrame(newFrame.materialize());
        isEnvSet = true;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        RNode[] args = callArgsNode.getArguments();
        if (args == null || args.length < 1) {
            return callBuiltin(frame);
        }
        evaluatedArgs = new Object[]{args[0].execute(frame)};
        if ((this.type = getArgClass(evaluatedArgs[0])) != null) {
            if (targetFunction != null && isEqualType(this.type, this.typeLast) && isFirst) {
                return executeHelper();
            }
            findTargetFunction(frame);
            if (targetFunction != null) {
                dotMethod = RDataFactory.createStringVector(new String[]{targetFunctionName}, true);
                return executeHelper();
            }
        }
        return callBuiltin(frame);
    }

    @Override
    public Object execute(VirtualFrame frame, final RStringVector aType) {
        throw new UnsupportedOperationException();
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

    protected Object callBuiltin(VirtualFrame frame) {
        this.typeLast = null;
        if (builtInNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            builtInNode = insert(ReadVariableNode.create(this.genericName, RRuntime.TYPE_FUNCTION, false));
        }
        RFunction builtinFunc = null;
        try {
            builtinFunc = builtInNode.executeFunction(frame);
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Builtin " + this.genericName + " not found");
        }
        initFunCall(builtinFunc);
        return this.funCall;
    }

    protected Object executeHelper() {
        setEnvironment();
        this.typeLast = this.type;
        initFunCall(targetFunction);
        return this.funCall;
    }

    protected RStringVector getArgClass(Object arg) {
        if (arg instanceof RAbstractVector && ((RAbstractVector) arg).isObject()) {
            return ((RAbstractVector) arg).getClassHierarchy();
        }
        return null;
    }

    @SlowPath
    private void initFunCall(RFunction func) {
        // avoid re-evaluating arguments.
        if (evaluatedArgs != null) {
            RNode[] argArray = new RNode[callArgsNode.getArguments().length];
            System.arraycopy(callArgsNode.getArguments(), evaluatedArgs.length, argArray, evaluatedArgs.length, argArray.length - evaluatedArgs.length);
            for (int i = 0; i < evaluatedArgs.length; ++i) {
                if (evaluatedArgs[i] != null) {
                    argArray[i] = ConstantNode.create(evaluatedArgs[i]);
                }
            }
            this.funCall = new DispatchNode.FunctionCall(func, CallArgumentsNode.create(callArgsNode.modeChange(), callArgsNode.modeChangeForAll(), argArray, callArgsNode.getNames()));
        }
        if (this.funCall == null) {
            this.funCall = new DispatchNode.FunctionCall(func, callArgsNode);
        }
        this.funCall.function = func;
    }
}
