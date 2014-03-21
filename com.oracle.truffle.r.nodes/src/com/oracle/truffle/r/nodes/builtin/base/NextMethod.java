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
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.RBuiltin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(value = "NextMethod", lastParameterKind = LastParameterKind.VAR_ARGS_SPECIALIZE)
public abstract class NextMethod extends S3MethodDispatch {

    @Child protected ReadVariableNode rvnDefEnv;
    @Child protected ReadVariableNode rvnCallEnv;
    @Child protected ReadVariableNode rvnGeneric;
    @Child protected ReadVariableNode rvnClass;
    @Child protected ReadVariableNode rvnGroup;
    @Child protected ReadVariableNode rvnMethod;
    @Child protected WriteVariableNode wvnGroup;
    protected String group;
    protected String storedFunctionName;
    private static final Object[] PARAMETER_NAMES = new Object[]{"generic", "object", "..."};
    private String baseName;
    private String[] prefix;
    private boolean hasGroup;

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getArguments() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    @Specialization(order = 1)
    public Object nextMethod(VirtualFrame frame, String genericMethod, @SuppressWarnings("unused") Object obj, Object[] args) {
        this.genericName = genericMethod;
        readGenericVars(frame);
        if (this.genericName == null) {
            throw RError.getUnspecifiedGenFunction(getEncapsulatingSourceSection());
        }
        int nextClassIndex = 0;
        String currentFunctionName = null;
        if (storedFunctionName == null) {
            currentFunctionName = frame.getArguments(RArguments.class).getFunction().getName();
        } else {
            currentFunctionName = storedFunctionName;
        }
        for (int i = 0; i < klass.getLength(); ++i) {
            if (RRuntime.toString(new StringBuffer(baseName).append(RRuntime.RDOT).append(klass.getDataAt(i))).equals(currentFunctionName)) {
                nextClassIndex = i + 1;
                break;
            }
        }
        // First try generic.class then group.class.
        for (; nextClassIndex < klass.getLength() && targetFunction == null; ++nextClassIndex) {
            for (int i = 0; i < prefix.length && targetFunction == null; findFunction(prefix[i++], klass.getDataAt(nextClassIndex), genCallEnv)) {
            }
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
        if (nextClassIndex == klass.getLength()) {
            classVec = RDataFactory.createStringVector("");
        } else {
            classVec = RDataFactory.createStringVector(Arrays.copyOfRange(klass.getDataCopy(), nextClassIndex, klass.getLength()), true);
        }
        LinkedHashMap<String, Object> attr = new LinkedHashMap<>();
        attr.put(RRuntime.PREVIOUS_ATTR_KEY, klass.copyResized(klass.getLength(), false));
        classVec.setAttributes(attr);
        klass = classVec;
        if (storedFunctionName != null) {
            targetFunctionName = storedFunctionName;
        }
        final RArguments currentArguments = frame.getArguments(RArguments.class);
        // Merge arguments passed to current function with arguments passed to NextMethod call.
        final Object[] mergedArgs = Arrays.copyOf(currentArguments.getArgumentsArray(), currentArguments.getLength() + args.length);
        System.arraycopy(args, 0, mergedArgs, currentArguments.getLength(), args.length);
        final RArguments newArguments = RArguments.create(targetFunction, targetFunction.getEnclosingFrame(), mergedArgs, currentArguments.getNames());
        final VirtualFrame newFrame = Truffle.getRuntime().createVirtualFrame(frame.getCaller(), newArguments, new FrameDescriptor());
        defineVars(newFrame);
        if (hasGroup) {
            wvnGroup = initWvn(wvnGroup, RRuntime.RDotGroup);
            wvnGroup.execute(newFrame, this.group);
        }
        return funcDefnNode.execute(newFrame);
    }

    @Specialization(order = 10)
    public Object nextMethod(VirtualFrame frame, @SuppressWarnings("unused") RMissing generic, @SuppressWarnings("unused") RMissing obj, @SuppressWarnings("unused") RMissing args) {
        return nextMethod(frame, null, null, new Object[0]);
    }

    @Specialization(order = 11)
    public Object nextMethod(VirtualFrame frame, String generic, Object obj, @SuppressWarnings("unused") RMissing args) {
        return nextMethod(frame, generic, obj, new Object[0]);
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

    private void readGenericVars(VirtualFrame frame) {
        rvnDefEnv = initRvn(RRuntime.RDotGenericDefEnv, rvnDefEnv);
        genDefEnv = readFrame(rvnDefEnv, frame);
        // TODO if(genDefEnv == null) genDefEnv = globalenv
        rvnCallEnv = initRvn(RRuntime.RDotGenericCallEnv, rvnCallEnv);
        genCallEnv = readFrame(rvnCallEnv, frame);
        if (genCallEnv == null) {
            genCallEnv = frame;
        }
        rvnGeneric = initRvn(RRuntime.RDotGeneric, rvnGeneric);
        try {
            this.genericName = rvnGeneric.executeString(frame);
        } catch (UnexpectedResultException e1) {
        } catch (RError r) {
        }
        rvnClass = initRvn(RRuntime.RDotClass, rvnClass);
        try {
            klass = rvnClass.executeRStringVector(frame);
        } catch (UnexpectedResultException e) {
            klass = getAlternateClassHr(frame);
        } catch (RError r) {
            klass = getAlternateClassHr(frame);
        }
        rvnGroup = initRvn(RRuntime.RDotGroup, rvnGroup);
        try {
            group = rvnGroup.executeString(frame);
            if (!group.isEmpty()) {
                handlePresentGroup();
            } else {
                handleMissingGroup();
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
    }

    private void handlePresentGroup() {
        baseName = group;
        prefix = new String[2];
        prefix[0] = genericName;
        prefix[1] = group;
        hasGroup = true;
    }

    private RStringVector getAlternateClassHr(VirtualFrame frame) {
        RArguments enclArgs = frame.getArguments(RArguments.class);
        if (enclArgs == null || enclArgs.getLength() == 0 || enclArgs.getArgument(0) == null || !(enclArgs.getArgument(0) instanceof RAbstractVector)) {
            throw RError.getObjectNotSpecified(getEncapsulatingSourceSection());
        }
        RAbstractVector enclosingArg = (RAbstractVector) enclArgs.getArgument(0);
        if (!enclosingArg.isObject()) {
            throw RError.getObjectNotSpecified(getEncapsulatingSourceSection());
        }
        return enclosingArg.getClassHierarchy();
    }

    private ReadVariableNode initRvn(final String name, ReadVariableNode node) {
        if (node == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            ReadVariableNode rvn = ReadVariableNode.create(name, false);
            return insert(rvn);
        }
        return node;
    }
}
