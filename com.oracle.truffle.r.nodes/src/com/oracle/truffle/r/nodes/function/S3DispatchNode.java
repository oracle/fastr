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
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class S3DispatchNode extends DispatchNode {

    @Child protected ReadVariableNode lookup;
    @CompilationFinal protected String lastFun;
    @Child protected WriteVariableNode wvnCallEnv;
    @Child protected WriteVariableNode wvnGeneric;
    @Child protected WriteVariableNode wvnClass;
    @Child protected WriteVariableNode wvnMethod;
    @Child protected WriteVariableNode wvnDefEnv;
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
            ReadVariableNode rvn = ReadVariableNode.create(functionName, RRuntime.TYPE_FUNCTION, false);
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
        if (func != null && func instanceof RFunction) {
            targetFunctionName = functionName;
            targetFunction = (RFunction) func;
        }
    }

    protected void findFunction(final String generic, final String className, Frame frame) {
        checkLength(className, generic);
        findFunction(functionName(generic, className), frame);
    }

    @SlowPath
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

    @SlowPath
    private static void addVars0(FrameDescriptor fDesc) {
        fDesc.findOrAddFrameSlot(RRuntime.RDotGeneric);
        fDesc.findOrAddFrameSlot(RRuntime.RDotMethod);
        fDesc.findOrAddFrameSlot(RRuntime.RDotClass);
        fDesc.findOrAddFrameSlot(RRuntime.RDotGenericCallEnv);
        fDesc.findOrAddFrameSlot(RRuntime.RDotGenericDefEnv);
    }

    protected void removeVars(VirtualFrame frame) {
        removeVars0(frame.getFrameDescriptor());
    }

    @SlowPath
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
            // TODO pass frame to make error catchable
            throw RError.uncatchableError(getEncapsulatingSourceSection(), RError.Message.TOO_LONG_CLASS_NAME, generic);
        }
    }
}
