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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class FunctionExpressionNode extends RNode {

    @Override
    public final RFunction execute(VirtualFrame frame) {
        return executeFunction(frame);
    }

    @Override
    public abstract RFunction executeFunction(VirtualFrame frame);

    public static FunctionExpressionNode create(RFunction function) {
        return new StaticFunctionExpressionNode(function);
    }

    public static FunctionExpressionNode create(RootCallTarget callTarget) {
        return new DynamicFunctionExpressionNode(callTarget);
    }

    public static final class StaticFunctionExpressionNode extends FunctionExpressionNode {

        private final RFunction function;

        public StaticFunctionExpressionNode(RFunction function) {
            this.function = function;
        }

        @Override
        public RFunction executeFunction(VirtualFrame frame) {
            return function;
        }

        public RFunction getFunction() {
            return function;
        }
    }

    public static final class DynamicFunctionExpressionNode extends FunctionExpressionNode {

        private final RootCallTarget callTarget;

        public DynamicFunctionExpressionNode(RootCallTarget callTarget) {
            this.callTarget = callTarget;
        }

        @Override
        public RFunction executeFunction(VirtualFrame frame) {
            return new RFunction("", callTarget, frame.materialize());
        }

        public RootCallTarget getCallTarget() {
            return callTarget;
        }
    }
}
