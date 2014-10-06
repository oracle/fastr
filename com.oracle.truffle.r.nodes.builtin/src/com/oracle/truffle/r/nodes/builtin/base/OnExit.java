/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.FrameSlotNode.InternalFrameSlot;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.*;
import com.oracle.truffle.r.runtime.env.frame.*;
import com.oracle.truffle.r.runtime.ops.na.*;

/**
 * Placeholder. {@code on.exit} is special (cf {@code .Internal} in that {@code expr} is not
 * evaluated, but {@code add} is. TODO arrange for the {@code expr} be stored with the currently
 * evaluating function using a new slot in {@link RArguments} and run it on function exit.
 */
@RBuiltin(name = "on.exit", kind = PRIMITIVE, parameterNames = {"expr", "add"}, nonEvalArgs = {0})
public abstract class OnExit extends RInvisibleBuiltinNode {

    @Child private FrameSlotNode onExitSlot = FrameSlotNode.create(InternalFrameSlot.OnExit, true);

    private final ConditionProfile addProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile existingProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile emptyPromiseProfile = ConditionProfile.createBinaryProfile();
    private final NAProfile na = NAProfile.create();

    private final PromiseProfile promiseProfile = new PromiseProfile();

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RNull.instance), ConstantNode.create(false)};
    }

    @Specialization
    protected Object onExit(VirtualFrame frame, RPromise expr, byte add) {
        controlVisibility();

        if (na.isNA(add)) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "add");
        }

        // the empty (RNull.instance) expression is used to clear on.exit
        boolean empty = emptyPromiseProfile.profile(expr.isDefault(promiseProfile));

        assert !empty || expr.getRep() instanceof ConstantNode : "only ConstantNode expected for defaulted promise";
        assert empty || !expr.isEvaluated(promiseProfile) : "promise cannot already be evaluated";

        ArrayList<Object> current;
        FrameSlot slot = onExitSlot.executeFrameSlot(frame);
        if (existingProfile.profile(onExitSlot.hasValue(frame))) {
            current = getCurrentList(frame, slot);
            if (addProfile.profile(!RRuntime.fromLogical(add))) {
                // add is false, so clear the existing
                current.clear();
            }
        } else {
            // initialize the list of exit handlers
            frame.setObject(slot, current = new ArrayList<>());
            FrameSlotChangeMonitor.checkAndUpdate(slot);
        }
        if (!empty) {
            current.add(expr.getRep());
        }
        return RNull.instance;
    }

    @SuppressWarnings("unchecked")
    private static ArrayList<Object> getCurrentList(VirtualFrame frame, FrameSlot slot) {
        try {
            return (ArrayList<Object>) frame.getObject(slot);
        } catch (FrameSlotTypeException e) {
            throw RInternalError.shouldNotReachHere();
        }
    }
}
