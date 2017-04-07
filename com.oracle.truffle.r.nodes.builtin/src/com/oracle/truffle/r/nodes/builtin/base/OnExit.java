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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;

@RBuiltin(name = "on.exit", visibility = OFF, kind = PRIMITIVE, parameterNames = {"expr", "add"}, nonEvalArgs = 0, behavior = COMPLEX)
public abstract class OnExit extends RBuiltinNode {

    @CompilationFinal private FrameSlot onExitSlot;

    private final ConditionProfile addProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile newProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile emptyPromiseProfile = ConditionProfile.createBinaryProfile();

    static {
        Casts casts = new Casts(OnExit.class);
        casts.arg("add").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).mustNotBeNA().map(toBoolean());
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RNull.instance, RRuntime.LOGICAL_FALSE};
    }

    @SuppressWarnings("unchecked")
    @Specialization
    protected Object onExit(VirtualFrame frame, RPromise expr, boolean add) {

        if (onExitSlot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            onExitSlot = frame.getFrameDescriptor().findOrAddFrameSlot(RFrameSlot.OnExit, FrameSlotKind.Object);
        }

        // the empty (RNull.instance) expression is used to clear on.exit
        if (emptyPromiseProfile.profile(expr.isDefaultArgument())) {
            assert expr.getRep() instanceof ConstantNode : "only ConstantNode expected for defaulted promise";
            frame.setObject(onExitSlot, new ArrayList<>());
        } else {
            assert !expr.isEvaluated() : "promise cannot already be evaluated";
            Object value;
            try {
                value = frame.getObject(onExitSlot);
            } catch (FrameSlotTypeException e) {
                throw RInternalError.shouldNotReachHere();
            }
            ArrayList<Object> list;
            if (newProfile.profile(value == null)) {
                // initialize the list of exit handlers
                frame.setObject(onExitSlot, list = new ArrayList<>());
            } else {
                list = (ArrayList<Object>) value;
                if (addProfile.profile(!add)) {
                    // add is false, so clear the existing list
                    list.clear();
                }
            }
            list.add(expr.getRep());
        }
        return RNull.instance;
    }
}
