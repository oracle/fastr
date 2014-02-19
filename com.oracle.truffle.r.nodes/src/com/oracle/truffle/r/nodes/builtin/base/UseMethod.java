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

        final Map<?, ?> attributes = arg.getAttributes();
        final Object classAttrb = attributes.get(RRuntime.CLASS_ATTR_KEY);
        RFunction targetFunction = null;
        if (classAttrb instanceof RStringVector) {
            RStringVector classNames = (RStringVector) classAttrb;
            for (int i = 0; i < classNames.getLength() && targetFunction == null; ++i) {
                targetFunction = findFunction(classNames.getDataAt(i), generic, frame);
            }
        }
        if (classAttrb instanceof String) {
            targetFunction = findFunction((String) classAttrb, generic, frame);
        }
        if (targetFunction == null) {
            targetFunction = findFunction(RRuntime.DEFAULT, generic, frame);
            if (targetFunction == null) {
                throw RError.getUnknownFunctionUseMethod(getEncapsulatingSourceSection(), generic, classAttrb.toString());
            }

        }
        FunctionDefinitionNode fDefn = (FunctionDefinitionNode) (((DefaultCallTarget) targetFunction.getTarget()).getRootNode());
        RArguments currentArguments = frame.getArguments(RArguments.class);
        RArguments newArguments = RArguments.create(targetFunction, targetFunction.getEnclosingFrame(), currentArguments.getArgumentsArray(), currentArguments.getNames());
        VirtualFrame newFrame = Truffle.getRuntime().createVirtualFrame(frame.getCaller(), newArguments, frame.getFrameDescriptor().copy());

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
        return fDefn.execute(newFrame);
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
    private RFunction findFunction(final String className, final String generic, VirtualFrame frame) {
        final String funcName = generic + "." + className;
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
            return (RFunction) func;
        }
        return null;
    }
}
