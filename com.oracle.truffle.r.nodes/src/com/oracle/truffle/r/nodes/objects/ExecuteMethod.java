/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.nodes.function.RMissingHelper;
import com.oracle.truffle.r.nodes.function.signature.RArgumentsNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class ExecuteMethod extends RBaseNode {

    public abstract Object executeObject(VirtualFrame frame, RFunction fdef);

    @Child private LocalReadVariableNode readDefined = LocalReadVariableNode.create(RRuntime.R_DOT_DEFINED, true);
    @Child private LocalReadVariableNode readMethod = LocalReadVariableNode.create(RRuntime.RDotMethod, true);
    @Child private LocalReadVariableNode readTarget = LocalReadVariableNode.create(RRuntime.R_DOT_TARGET, true);
    @Child private LocalReadVariableNode readGeneric = LocalReadVariableNode.create(RRuntime.RDotGeneric, true);
    @Child private LocalReadVariableNode readMethods = LocalReadVariableNode.create(RRuntime.R_DOT_METHODS, true);
    @Child private RArgumentsNode argsNode = RArgumentsNode.create();

    @Specialization
    protected Object executeMethod(VirtualFrame frame, RFunction fdef) {

        Object[] args = argsNode.execute(RArguments.getFunction(frame), RArguments.getCall(frame), null, RArguments.getDepth(frame) + 1, RArguments.getArguments(frame),
                        RArguments.getSignature(frame), null);
        MaterializedFrame newFrame = Truffle.getRuntime().createMaterializedFrame(args);
        FrameDescriptor desc = newFrame.getFrameDescriptor();
        FrameSlotChangeMonitor.initializeFunctionFrameDescriptor("<executeMethod>", desc);
        FrameSlotChangeMonitor.initializeEnclosingFrame(newFrame, RArguments.getFunction(frame).getEnclosingFrame());
        FormalArguments formals = ((RRootNode) fdef.getRootNode()).getFormalArguments();
        if (formals != null) {
            ArgumentsSignature signature = formals.getSignature();
            MaterializedFrame currentFrame = frame.materialize();
            FrameDescriptor currentFrameDesc = currentFrame.getFrameDescriptor();
            for (int i = 0; i < signature.getLength(); i++) {
                String argName = signature.getName(i);
                boolean missing = RMissingHelper.isMissingArgument(frame, argName);
                Object val = slotRead(currentFrame, currentFrameDesc, argName);
                slotInit(newFrame, desc, argName, val);
                if (missing && !(val instanceof RArgsValuesAndNames || val == RMissing.instance)) {
                    throw RInternalError.unimplemented();
                }
            }
        }

        slotInit(newFrame, desc, RRuntime.R_DOT_DEFINED, readDefined.execute(frame));
        slotInit(newFrame, desc, RRuntime.RDotMethod, readMethod.execute(frame));
        slotInit(newFrame, desc, RRuntime.R_DOT_TARGET, readTarget.execute(frame));
        slotInit(newFrame, desc, RRuntime.RDotGeneric, readGeneric.execute(frame));
        slotInit(newFrame, desc, RRuntime.R_DOT_METHODS, readMethods.execute(frame));

        Object ret = callMethod(fdef, newFrame);
        return ret;
    }

    @TruffleBoundary
    static Object callMethod(RFunction fdef, MaterializedFrame newFrame) {
        return RContext.getEngine().evalGeneric(fdef, newFrame);
    }

    @TruffleBoundary
    public static Object slotRead(MaterializedFrame currentFrame, FrameDescriptor desc, String name) {
        FrameSlot slot = desc.findFrameSlot(name);
        if (slot != null) {
            return currentFrame.getValue(slot);
        } else {
            return null;
        }
    }

    @TruffleBoundary
    static void slotInit(MaterializedFrame newFrame, FrameDescriptor desc, String name, Object value) {
        if (value instanceof Byte) {
            FrameSlot frameSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(desc, name, FrameSlotKind.Byte);
            newFrame.setByte(frameSlot, (byte) value);
        } else if (value instanceof Integer) {
            FrameSlot frameSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(desc, name, FrameSlotKind.Int);
            newFrame.setInt(frameSlot, (int) value);
        } else if (value instanceof Double) {
            FrameSlot frameSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(desc, name, FrameSlotKind.Double);
            newFrame.setDouble(frameSlot, (double) value);
        } else {
            FrameSlot frameSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(desc, name, FrameSlotKind.Object);
            newFrame.setObject(frameSlot, value);

        }
    }
}
