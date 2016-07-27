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

import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;

/**
 * In the normal case, we are returning from the currently executing function, but in the case where
 * "return" was passed as an argument (promise) (e.g. in tryCatch) to a function, we will be
 * evaluating that in the context of a PromiseEvalFrame and the frame we need to return to is that
 * given by the PromiseEvalFrame.
 */
@RBuiltin(name = "return", kind = PRIMITIVE, parameterNames = {"value"}, nonEvalArgs = {0}, behavior = COMPLEX)
public abstract class Return extends RBuiltinNode {

    private final BranchProfile isPromiseEvalProfile = BranchProfile.create();

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
    protected Object returnFunction(VirtualFrame frame, @SuppressWarnings("unused") RMissing arg) {
        throw new ReturnException(RNull.instance, RArguments.getCall(frame));
    }

    @Specialization
    protected Object returnFunction(VirtualFrame frame, RNull arg) {
        throw new ReturnException(arg, RArguments.getCall(frame));
    }

    @Specialization
    protected Object returnFunction(VirtualFrame frame, RPromise expr) {
        // Evaluate the result
        Object value = initPromiseHelper().evaluate(frame, expr);
        RCaller call = RArguments.getCall(frame);
        while (call.isPromise()) {
            isPromiseEvalProfile.enter();
            call = call.getParent();
        }
        throw new ReturnException(value, call);
    }
}
