/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.nodes.RNode;

final class ReturnSpecial extends RNode {

    @Child private RNode value;
    private final BranchProfile isPromiseEvalProfile = BranchProfile.create();

    protected ReturnSpecial(RNode value) {
        this.value = value;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return Return.doReturn(frame, value.visibleExecute(frame), isPromiseEvalProfile);
    }
}

/**
 * Return a value from the currently executing function, which is identified by the
 * {@link RArguments#getCall(com.oracle.truffle.api.frame.Frame) call}. The return value will be
 * delivered via a {@link ReturnException}, which is subsequently caught in the
 * {@link FunctionDefinitionNode}.
 */
@RBuiltin(name = "return", kind = PRIMITIVE, parameterNames = {"value"}, behavior = COMPLEX, nonEvalArgs = {0})
public abstract class Return extends RBuiltinNode.Arg1 {

    public static RNode createSpecial(@SuppressWarnings("unused") ArgumentsSignature signature, RNode[] arguments, @SuppressWarnings("unused") boolean inReplacement) {
        return arguments.length == 1 ? new ReturnSpecial(arguments[0]) : null;
    }

    static {
        Casts.noCasts(Return.class);
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RNull.instance};
    }

    static ReturnException doReturn(VirtualFrame frame, Object value, BranchProfile isPromiseEvalProfile) {
        RCaller call = RArguments.getCall(frame);
        while (call.isPromise()) {
            isPromiseEvalProfile.enter();
            call = call.getParent();
        }
        throw new ReturnException(value, call);
    }

    @Specialization
    protected Object returnFunction(VirtualFrame frame, RPromise x,
                    @Cached("new()") PromiseHelperNode promiseHelper,
                    @Cached("create()") BranchProfile isPromiseEvalProfile,
                    @Cached("create()") SetVisibilityNode visibility) {
        if (x.isEvaluated()) {
            visibility.execute(frame, true);
        }
        Object value = promiseHelper.evaluate(frame, x);
        throw doReturn(frame, value, isPromiseEvalProfile);
    }

    @Specialization
    protected RList returnFunction(VirtualFrame frame, @SuppressWarnings("unused") RMissing x,
                    @Cached("create()") BranchProfile isPromiseEvalProfile) {
        throw doReturn(frame, RNull.instance, isPromiseEvalProfile);
    }
}
