/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.util.ArrayList;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.FrameSlotNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;
import com.oracle.truffle.r.runtime.ops.na.NAProfile;

/**
 * Placeholder. {@code on.exit} is special (cf {@code .Internal} in that {@code expr} is not
 * evaluated, but {@code add} is. TODO arrange for the {@code expr} be stored with the currently
 * evaluating function using a new slot in {@link RArguments} and run it on function exit.
 */
@RBuiltin(name = "on.exit", visibility = OFF, kind = PRIMITIVE, parameterNames = {"expr", "add"}, nonEvalArgs = 0, behavior = COMPLEX)
public abstract class OnExit extends RBuiltinNode {

    @Child private FrameSlotNode onExitSlot = FrameSlotNode.create(RFrameSlot.OnExit, true);

    private final ConditionProfile addProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile existingProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile emptyPromiseProfile = ConditionProfile.createBinaryProfile();
    private final NAProfile na = NAProfile.create();

    private final BranchProfile invalidateProfile = BranchProfile.create();

    static {
        Casts casts = new Casts(OnExit.class);
        casts.arg("add").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE);
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RNull.instance, RRuntime.LOGICAL_FALSE};
    }

    @Specialization
    protected Object onExit(VirtualFrame frame, RPromise expr, byte add) {

        if (na.isNA(add)) {
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "add");
        }

        // the empty (RNull.instance) expression is used to clear on.exit
        boolean empty = emptyPromiseProfile.profile(expr.isDefaultArgument());

        assert !empty || expr.getRep() instanceof ConstantNode : "only ConstantNode expected for defaulted promise";
        assert empty || !expr.isEvaluated() : "promise cannot already be evaluated";

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
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, slot, current = new ArrayList<>(), false, invalidateProfile);
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
