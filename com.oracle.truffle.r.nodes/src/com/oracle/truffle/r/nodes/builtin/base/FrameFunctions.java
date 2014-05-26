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

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;

/**
 * sys.R.
 */
public class FrameFunctions {

    /**
     * The environment of the caller of the function that called parent.frame(n = 1).
     */
    @RBuiltin(name = "parent.frame", kind = INTERNAL)
    public abstract static class ParentFrame extends RBuiltinNode {
        @Specialization(guards = "isOne")
        public REnvironment parentFrame(@SuppressWarnings("unused") double n) {
            controlVisibility();
            Frame callerFrame = Utils.getStackFrame(FrameAccess.READ_ONLY, 2);
            return EnvFunctions.frameToEnvironment(callerFrame);
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
