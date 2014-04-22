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
        if (isSame() && isFirst && findFunction(targetFunctionName, genCallEnv)) {
            assert (funCall != null);
        } else {
            findTargetFunction(frame);
            storeValues();
            initArgNodes(frame);
            funCall = new DispatchNode.FunctionCall(targetFunction, CallArgumentsNode.create(argNodes, null));
        }
        setEnvironment();
        return funCall;
    }

    @Override
    protected void unsetEnvironment(VirtualFrame frame) {
        targetFunction.setEnclosingFrame(RArguments.getEnclosingFrame(targetFunction.getEnclosingFrame()));
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
                throw RError.getNoMethodFound(getEncapsulatingSourceSection());
            }
        }
        RStringVector classVec = null;
        if (nextClassIndex == type.getLength()) {
            classVec = RDataFactory.createStringVector("");
        } else {
            classVec = RDataFactory.createStringVector(Arrays.copyOfRange(type.getDataCopy(), nextClassIndex, type.getLength()), true);
        }
        LinkedHashMap<String, Object> attr = new LinkedHashMap<>();
        attr.put(RRuntime.PREVIOUS_ATTR_KEY, type.copyResized(type.getLength(), false));
        classVec.setAttributes(attr);
        klass = classVec;
    }

    private void initArgNodes(VirtualFrame frame) {
        if (argNodes != null) {
            return;
        }
        // Merge arguments passed to current function with arguments passed to NextMethod call.
        final Object[] mergedArgs = Arrays.copyOf(RArguments.getArgumentsArray(frame), RArguments.getArgumentsLength(frame) + args.length);
        System.arraycopy(args, 0, mergedArgs, RArguments.getArgumentsLength(frame), args.length);
        argNodes = new RNode[mergedArgs.length];
        for (int i = 0; i < mergedArgs.length; ++i) {
            argNodes[i] = ConstantNode.create(mergedArgs[i]);
        }
    }

    private void setEnvironment() {
        final VirtualFrame newFrame = Truffle.getRuntime().createVirtualFrame(RArguments.create(), new FrameDescriptor());
        RArguments.setEnclosingFrame(newFrame, targetFunction.getEnclosingFrame());
        wvnMethod = initWvn(wvnMethod, RRuntime.RDotMethod);
        if (storedFunctionName != null) {
            wvnMethod.execute(newFrame, storedFunctionName);
        } else {
            wvnMethod.execute(newFrame, targetFunctionName);
        }
        defineVars(newFrame);
        if (hasGroup) {
            wvnGroup = initWvn(wvnGroup, RRuntime.RDotGroup);
            wvnGroup.execute(newFrame, this.group);
        }
        targetFunction.setEnclosingFrame(newFrame.materialize());
    }

    private void storeValues() {
        lastHasGroup = hasGroup;
        lastGroup = group;
        lastStoredFunctionName = storedFunctionName;
    }

    private void readGenericVars(VirtualFrame frame) {
        rvnDefEnv = initRvn(RRuntime.RDotGenericDefEnv, rvnDefEnv);
        genDefEnv = readFrame(rvnDefEnv, frame);
        // TODO if(genDefEnv == null) genDefEnv = globalenv
        rvnCallEnv = initRvn(RRuntime.RDotGenericCallEnv, rvnCallEnv);
        genCallEnv = readFrame(rvnCallEnv, frame);
        if (genCallEnv == null) {
            genCallEnv = frame.materialize();
        }
        rvnGroup = initRvn(RRuntime.RDotGroup, rvnGroup);
        try {
            group = rvnGroup.executeString(frame);
            if (group.isEmpty()) {
                handleMissingGroup();
            } else {
                handlePresentGroup();
            }
        } catch (UnexpectedResultException e) {
            handleMissingGroup();
        } catch (RError r) {
            handleMissingGroup();
        }
        rvnMethod = initRvn(RRuntime.RDotMethod, rvnMethod);
        try {
            storedFunctionName = rvnMethod.executeString(frame);
        } catch (UnexpectedResultException e) {
        } catch (RError r) {
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

    private ReadVariableNode initRvn(final String name, ReadVariableNode node) {
        if (node == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            ReadVariableNode rvn = ReadVariableNode.create(name, false);
            return insert(rvn);
        }
        return node;
    }

    private static MaterializedFrame readFrame(ReadVariableNode rvn, VirtualFrame frame) {
        try {
            Object temp = rvn.execute(frame);
            if (temp instanceof MaterializedFrame) {
                return (MaterializedFrame) temp;
            }
        } catch (RError r) {
        }
        return null;
    }
}
