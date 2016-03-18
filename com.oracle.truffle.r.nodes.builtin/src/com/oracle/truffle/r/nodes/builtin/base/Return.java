/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;

@RBuiltin(name = "return", kind = PRIMITIVE, parameterNames = {"value"}, nonEvalArgs = {0})
public abstract class Return extends RBuiltinNode {

    @Child private PromiseHelperNode promiseHelper;

    private PromiseHelperNode initPromiseHelper() {
        if (promiseHelper == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            promiseHelper = insert(new PromiseHelperNode());
        }
        return promiseHelper;
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RNull.instance};
    }

    @Specialization
    protected Object returnFunction(@SuppressWarnings("unused") RMissing arg) {
        throw new ReturnException(RNull.instance);
    }

    @Specialization
    protected Object returnFunction(RNull arg) {
        throw new ReturnException(arg);
    }

    @Specialization
    protected Object returnFunction(VirtualFrame frame, RPromise expr) {
        controlVisibility();
        Object value = initPromiseHelper().evaluate(frame, expr);
        /*
         * The function we want to return from may not be the currently executing function, if
         * "return(expr)" was passed as an argument; see the implementation of tryCatch. In that
         * case "frame" identifies the function to return from, so we pass it in the
         * ReturnException, to be checked in FunctionDefinitionNode.execute.
         */
        throw new ReturnException(value, RArguments.getDepth(frame));
    }
}
