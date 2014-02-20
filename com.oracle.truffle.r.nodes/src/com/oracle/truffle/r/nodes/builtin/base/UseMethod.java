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
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.impl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(value = "UseMethod")
public abstract class UseMethod extends RBuiltinNode {

    /*
     * TODO: If more than two parameters are passed to UseMethod the extra parameters are ignored
     * and a warning is generated.
     */
    private static final Object[] PARAMETER_NAMES = new Object[]{"generic", "object"};

    @Child protected ReadVariableNode lookup;
    @CompilationFinal protected String lastFun;

    private FunctionDefinitionNode funcDefnNode;

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getArguments() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    // TODO: Matrix, arrays, integers, strings etc have default class types so there should be a
// specialization for each one of them.

    @Specialization
    public Object useMethod(VirtualFrame frame, String generic, RAbstractVector arg) {

        final Map<String, Object> attributes = arg.getAttributes();
        final Object classAttrb = attributes.get(RRuntime.CLASS_ATTR_KEY);
        VirtualFrame newFrame = null;
        if (classAttrb instanceof RStringVector) {
            RStringVector classNames = (RStringVector) classAttrb;
            for (int i = 0; i < classNames.getLength() && newFrame == null; ++i) {
                newFrame = findFunction(classNames.getDataAt(i), generic, frame);
            }
        }
        if (classAttrb instanceof String) {
            newFrame = findFunction((String) classAttrb, generic, frame);
        }
        if (newFrame == null) {
            newFrame = findFunction(RRuntime.DEFAULT, generic, frame);
            if (newFrame == null) {
                throw RError.getUnknownFunctionUseMethod(getEncapsulatingSourceSection(), generic, classAttrb.toString());
            }
        }
        // Copy the variables defined(prior to call to UseMethod) in the current frame to the new
        // frame
        for (FrameSlot fs : frame.getFrameDescriptor().getSlots()) {
            switch (fs.getKind()) {
                case Object:
                    newFrame.setObject(fs, FrameUtil.getObjectSafe(frame, fs));
                    break;
                case Int:
                    newFrame.setInt(fs, FrameUtil.getIntSafe(frame, fs));
                    break;
                case Byte:
                    newFrame.setByte(fs, FrameUtil.getByteSafe(frame, fs));
                    break;
                case Long:
                    newFrame.setLong(fs, FrameUtil.getLongSafe(frame, fs));
                    break;
                case Double:
                    newFrame.setDouble(fs, FrameUtil.getDoubleSafe(frame, fs));
                    break;
                case Float:
                    newFrame.setFloat(fs, FrameUtil.getFloatSafe(frame, fs));
                    break;
                case Boolean:
                    newFrame.setBoolean(fs, FrameUtil.getBooleanSafe(frame, fs));
                    break;
            }
        }
        return funcDefnNode.execute(newFrame);
    }

    /*
     * If only one argument is passed to UseMethod, the first argument of enclosing function is used
     * to resolve the generic.
     */
    @Specialization
    public Object useMethod(VirtualFrame frame, String generic, RMissing arg) {
        RAbstractVector enclosingArg = (RAbstractVector) frame.getArguments(RArguments.class).getArgument(0);
        return useMethod(frame, generic, enclosingArg);
    }

    /*
     * @Specialization public Object useMethod(VirtualFrame frame, String generic, RDouble arg) { }
     */
    private VirtualFrame findFunction(final String className, final String generic, VirtualFrame frame) {
        StringBuilder sbFuncName = new StringBuilder(generic);
        sbFuncName.append(".");
        sbFuncName.append(className);
        final String funcName = RRuntime.toString(sbFuncName);
        if (lookup == null || !funcName.equals(lastFun)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lastFun = funcName;
            ReadVariableNode rvn = ReadVariableNode.create(funcName, true, false);
            lookup = lookup == null ? adoptChild(rvn) : lookup.replace(rvn);
        }
        Object func = null;
        try {
            func = lookup.execute((VirtualFrame) frame.getCaller().unpack());
        } catch (RError r) {
            return null;
        }
        if (func != null && func instanceof RFunction) {
            final RFunction targetFunction = (RFunction) func;
            funcDefnNode = (FunctionDefinitionNode) (((DefaultCallTarget) targetFunction.getTarget()).getRootNode());
            final RArguments currentArguments = frame.getArguments(RArguments.class);
            final RArguments newArguments = RArguments.create(targetFunction, targetFunction.getEnclosingFrame(), currentArguments.getArgumentsArray(), currentArguments.getNames());
            return Truffle.getRuntime().createVirtualFrame(frame.getCaller(), newArguments, frame.getFrameDescriptor().copy());
        }
        return null;
    }
}
