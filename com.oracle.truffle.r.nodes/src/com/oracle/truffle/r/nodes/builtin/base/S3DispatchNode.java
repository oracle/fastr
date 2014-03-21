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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
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
    protected String targetFunctionName;
    protected RFunction targetFunction;
    protected RStringVector klass;
    protected RNode[] argNodes;
    protected FunctionCall funCall;
    protected Frame genCallEnv;
    protected Frame genDefEnv;

    protected boolean findFunction(final String functionName, Frame frame) {
        if (lookup == null || !functionName.equals(lastFun)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lastFun = functionName;
            ReadVariableNode rvn = ReadVariableNode.create(functionName, RRuntime.TYPE_FUNCTION, false);
            lookup = lookup == null ? adoptChild(rvn) : lookup.replace(rvn);
        }
        Object func = targetFunction = null;
        try {
            if (frame instanceof VirtualFrame) {
                func = lookup.execute((VirtualFrame) frame);
            } else {
                func = lookup.execute(null, (MaterializedFrame) frame);
            }
        } catch (RError r) {
        }
        if (func != null && func instanceof RFunction) {
            targetFunctionName = functionName;
            targetFunction = (RFunction) func;
            return true;
        }
        return false;
    }

    protected void findFunction(final String generic, final String className, Frame frame) {
        checkLength(className, generic);
        findFunction(RRuntime.toString(new StringBuilder(generic).append(RRuntime.RDOT).append(className)), frame);
    }

    protected WriteVariableNode initWvn(WriteVariableNode wvn, final String name) {
        WriteVariableNode node = wvn;
        if (node == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            node = WriteVariableNode.create(name, null, false, false);
            adoptChild(node);
        }
        return node;
    }

    protected void defineVars(VirtualFrame frame) {
        wvnGeneric = initWvn(wvnGeneric, RRuntime.RDotGeneric);
        wvnGeneric.execute(frame, genericName);
        wvnMethod = initWvn(wvnMethod, RRuntime.RDotMethod);
        wvnMethod.execute(frame, targetFunctionName);
        wvnClass = initWvn(wvnClass, RRuntime.RDotClass);
        wvnClass.execute(frame, klass);
        wvnCallEnv = initWvn(wvnCallEnv, RRuntime.RDotGenericCallEnv);
        wvnCallEnv.execute(frame, genCallEnv);
        wvnDefEnv = initWvn(wvnDefEnv, RRuntime.RDotGenericDefEnv);
        wvnDefEnv.execute(frame, genDefEnv);
    }

    private void checkLength(final String className, final String generic) {
        // The magic number two taken from src/main/objects.c
        if (className.length() + generic.length() + 2 > RRuntime.LEN_METHOD_NAME) {
            throw RError.getTooLongClassName(getEncapsulatingSourceSection(), generic);
        }
    }
}
