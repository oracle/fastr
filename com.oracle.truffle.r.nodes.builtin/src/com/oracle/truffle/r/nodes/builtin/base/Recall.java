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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * The {@code Recall} {@code .Internal}.
 */
@RBuiltin(name = "Recall", kind = INTERNAL, parameterNames = {"..."})
public abstract class Recall extends RBuiltinNode {
    @Child private CallInlineCacheNode callCache = CallInlineCacheNode.create(3);

    @Specialization
    public Object execute(VirtualFrame frame, RArgsValuesAndNames args) {
        controlVisibility();
        Frame cframe = Utils.getCallerFrame(frame, FrameAccess.READ_ONLY);
        RFunction function = RArguments.getFunction(cframe);
        if (function == null) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.RECALL_CALLED_OUTSIDE_CLOSURE);
        }

        // Use arguments in "..." as arguments for RECALL call
        Object[] argsObject = RArguments.create(function, callCache.getSourceSection(), RArguments.getDepth(frame) + 1, args.getValues());
        return callCache.execute(frame, function.getTarget(), argsObject);
    }

}
