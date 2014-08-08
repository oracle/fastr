/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode.RCustomBuiltinNode;
import com.oracle.truffle.r.nodes.function.ArgumentMatcher.VarArgsNode;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "Recall", kind = SUBSTITUTE)
// TODO INTERNAL
public class Recall extends RCustomBuiltinNode {
    private static final Object[] PARAMETER_NAMES = new Object[]{"..."};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Child private DirectCallNode callNode;
    @CompilationFinal private RFunction function;

    public Recall(RBuiltinNode prev) {
        super(prev);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        controlVisibility();
        if (function == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            function = RArguments.getFunction(frame);
            if (function == null) {
                throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.RECALL_CALLED_OUTSIDE_CLOSURE);
            }
            callNode = insert(Truffle.getRuntime().createDirectCallNode(function.getTarget()));
        }

        // TODO mjj This is almost certainly incorrect
        // Match arguments for function
        Object[] argsObject = RArguments.create(function, createArgs(frame, arguments[0]));
        return callNode.call(frame, argsObject);
    }

    @ExplodeLoop
    private static void executeArgs(VirtualFrame frame, RNode[] args, Object[] argValues) {
        for (int i = 0; i < args.length; i++) {
            argValues[i] = args[i].execute(frame);
        }
    }

    private static Object[] createArgs(VirtualFrame frame, RNode argNode) {
        RNode actualArgNode = argNode instanceof WrapArgumentNode ? ((WrapArgumentNode) argNode).getOperand() : argNode;
        if (actualArgNode instanceof VarArgsNode) {
            RNode[] args = ((VarArgsNode) actualArgNode).getArgumentNodes();
            Object[] argValues = new Object[args.length];
            executeArgs(frame, args, argValues);
            return argValues;
        } else {
            return new Object[]{argNode.execute(frame)};
        }
    }
}
