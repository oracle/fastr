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

package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.frame.*;

public abstract class S3DispatchNode extends DispatchNode {

    @Child private ReadVariableNode lookup;
    @CompilationFinal private String lastFun;
    @Child private WriteVariableNode wvnCallEnv;
    @Child private WriteVariableNode wvnGeneric;
    @Child private WriteVariableNode wvnClass;
    @Child protected WriteVariableNode wvnMethod;
    @Child private WriteVariableNode wvnDefEnv;
    @Child protected IndirectCallNode funCallNode = Truffle.getRuntime().createIndirectCallNode();
    protected String targetFunctionName;
    protected RFunction targetFunction;
    protected RStringVector klass;
    protected FunctionCall funCall;
    protected Frame genCallEnv;
    protected Frame genDefEnv;
    protected boolean isFirst;

    protected void findFunction(final String functionName, Frame frame) {
        if (lookup == null || !functionName.equals(lastFun)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lastFun = functionName;
            ReadVariableNode rvn = ReadVariableNode.create(functionName, RType.Function, false, true);
            lookup = lookup == null ? insert(rvn) : lookup.replace(rvn);
        }
        Object func = targetFunction = null;
        targetFunctionName = null;
        boolean prevIgnoreError = RError.ignoreError(true);
        try {
            if (frame instanceof VirtualFrame) {
                func = lookup.execute((VirtualFrame) frame);
            } else {
                func = lookup.execute(null, (MaterializedFrame) frame);
            }
        } catch (RError r) {
            // in this context, this is not a reportable error
        } finally {
            RError.ignoreError(prevIgnoreError);
        }
        if (func instanceof RFunction) {
            targetFunctionName = functionName;
            targetFunction = (RFunction) func;
        }
    }

    protected void findFunction(final String generic, final String className, Frame frame) {
        checkLength(className, generic);
        findFunction(functionName(generic, className), frame);
    }

    @TruffleBoundary
    private static String functionName(String generic, String className) {
        return new StringBuilder(generic).append(RRuntime.RDOT).append(className).toString();
    }

    protected WriteVariableNode initWvn(WriteVariableNode wvn, final String name) {
        WriteVariableNode node = wvn;
        if (node == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            node = WriteVariableNode.create(name, null, false, false);
            insert(node);
        }
        return node;
    }

    protected void defineVars(VirtualFrame frame) {
        addVars(frame);
        wvnMethod = initWvn(wvnMethod, RRuntime.RDotMethod);
        wvnGeneric = initWvn(wvnGeneric, RRuntime.RDotGeneric);
        wvnGeneric.execute(frame, genericName);
        wvnClass = initWvn(wvnClass, RRuntime.RDotClass);
        wvnClass.execute(frame, klass);
        wvnCallEnv = initWvn(wvnCallEnv, RRuntime.RDotGenericCallEnv);
        wvnCallEnv.execute(frame, genCallEnv);
        wvnDefEnv = initWvn(wvnDefEnv, RRuntime.RDotGenericDefEnv);
        wvnDefEnv.execute(frame, genDefEnv);
    }

    protected void defineVarsNew(VirtualFrame frame) {
        RArguments.setS3Generic(frame, genericName);
        RArguments.setS3Class(frame, klass);
        RArguments.setS3CallEnv(frame, genCallEnv);
        RArguments.setS3DefEnv(frame, genDefEnv);
    }

    protected void addVars(VirtualFrame frame) {
        addVars0(frame.getFrameDescriptor());
    }

    @TruffleBoundary
    private static void addVars0(FrameDescriptor fDesc) {
        findOrAddFrameSlot(fDesc, RRuntime.RDotGeneric);
        findOrAddFrameSlot(fDesc, RRuntime.RDotMethod);
        findOrAddFrameSlot(fDesc, RRuntime.RDotClass);
        findOrAddFrameSlot(fDesc, RRuntime.RDotGenericCallEnv);
        findOrAddFrameSlot(fDesc, RRuntime.RDotGenericDefEnv);
    }

    // TODO FINDORADDFRAMESLOT
    private static FrameSlot findOrAddFrameSlot(FrameDescriptor fd, Object identifier) {
        return findOrAddFrameSlot(fd, identifier, FrameSlotKind.Illegal);
    }

    private static FrameSlot findOrAddFrameSlot(FrameDescriptor fd, Object identifier, FrameSlotKind kind) {
        FrameSlot slot = fd.findFrameSlot(identifier);
        if (slot != null) {
            return slot;
        }
        return fd.addFrameSlot(identifier, FrameSlotChangeMonitor.createMonitor(), kind);
    }

    protected void removeVars(VirtualFrame frame) {
        removeVars0(frame.getFrameDescriptor());
    }

    @TruffleBoundary
    private static void removeVars0(FrameDescriptor fDesc) {
        fDesc.removeFrameSlot(RRuntime.RDotGeneric);
        fDesc.removeFrameSlot(RRuntime.RDotMethod);
        fDesc.removeFrameSlot(RRuntime.RDotClass);
        fDesc.removeFrameSlot(RRuntime.RDotGenericCallEnv);
        fDesc.removeFrameSlot(RRuntime.RDotGenericDefEnv);
    }

    private void checkLength(final String className, final String generic) {
        // The magic number two taken from src/main/objects.c
        if (className.length() + generic.length() + 2 > RRuntime.LEN_METHOD_NAME) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.TOO_LONG_CLASS_NAME, generic);
        }
    }
}
