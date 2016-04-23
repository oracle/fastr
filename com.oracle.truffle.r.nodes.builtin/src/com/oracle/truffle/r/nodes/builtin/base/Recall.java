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

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.CallMatcherNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;

/**
 * The {@code Recall} {@code .Internal}.
 */
@RBuiltin(name = "Recall", kind = INTERNAL, parameterNames = {"..."}, nonEvalArgs = {0})
public abstract class Recall extends RBuiltinNode {

    private final BranchProfile errorProfile = BranchProfile.create();

    @Child private LocalReadVariableNode readArgs = LocalReadVariableNode.create(ArgumentsSignature.VARARG_NAME, false);
    @Child private CallMatcherNode call = CallMatcherNode.create(false, false);

    @Specialization
    protected Object recall(VirtualFrame frame, @SuppressWarnings("unused") RArgsValuesAndNames args) {
        /*
         * The args passed to the Recall internal are ignored, they are always "...". Instead, this
         * builtin looks at the arguments passed to the surrounding function.
         */
        Frame cframe = Utils.getCallerFrame(frame, FrameAccess.READ_ONLY);
        RFunction function = RArguments.getFunction(cframe);
        if (function == null) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.RECALL_CALLED_OUTSIDE_CLOSURE);
        }
        RArgsValuesAndNames actualArgs = (RArgsValuesAndNames) readArgs.execute(frame);
        return call.execute(frame, actualArgs.getSignature(), actualArgs.getArguments(), function, null, null);
    }
}
