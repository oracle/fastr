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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * sys.R.
 */
public class FrameFunctions {

    /**
     * The environment of the caller of the function that called parent.frame(n = 1).
     */
    @RBuiltin(".Internal.parent.frame")
    public abstract static class ParentFrame extends RBuiltinNode {
        @Specialization(guards = "isOne")
        public REnvironment parentFrame(VirtualFrame frame, @SuppressWarnings("unused") double n) {
            controlVisibility();
            VirtualFrame callerFrame = (VirtualFrame) frame.getCaller().unpack();
            RFunction func = EnvFunctions.frameToFunction(callerFrame);
            if (func == null) {
                // called from shell
                return REnvironment.globalEnv();
            } else {
                // need the caller of func
                VirtualFrame funcCallerFrame = (VirtualFrame) callerFrame.getCaller().unpack();
                return EnvFunctions.callerEnvironment(funcCallerFrame);
            }
        }

        @Specialization
        public REnvironment parentFrame(@SuppressWarnings("unused") VirtualFrame frame, @SuppressWarnings("unused") Object n) {
            throw RError.getGenericError(getEncapsulatingSourceSection(), "invalid argument");
        }

        public boolean isOne(@SuppressWarnings("unused") VirtualFrame frame, double n) {
            return (int) n == 1;
        }
    }
}
