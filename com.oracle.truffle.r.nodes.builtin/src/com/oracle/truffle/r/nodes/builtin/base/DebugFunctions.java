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

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * Placeholders for the debug functions.
 */
public class DebugFunctions {
    @RBuiltin(name = "debug", kind = RBuiltinKind.INTERNAL, parameterNames = {"fun", "text", "condition"})
    public abstract static class Debug extends RInvisibleBuiltinNode {
        @SuppressWarnings("unused")
        @Specialization
        @SlowPath
        protected RNull debug(RFunction fun, RAbstractStringVector text, RNull condition) {
            // TODO implement
            controlVisibility();
            return RNull.instance;
        }
    }

    @RBuiltin(name = "debugonce", kind = RBuiltinKind.INTERNAL, parameterNames = {"fun", "text", "condition"})
    public abstract static class DebugOnce extends RInvisibleBuiltinNode {
        @SuppressWarnings("unused")
        @Specialization
        @SlowPath
        protected RNull debugonce(RFunction fun, RAbstractStringVector text, RNull condition) {
            // TODO implement
            controlVisibility();
            return RNull.instance;
        }
    }

    @RBuiltin(name = "undebug", kind = RBuiltinKind.INTERNAL, parameterNames = {"fun"})
    public abstract static class UnDebug extends RInvisibleBuiltinNode {
        @SuppressWarnings("unused")
        @Specialization
        @SlowPath
        protected RNull undebug(RFunction fun) {
            // TODO implement
            controlVisibility();
            return RNull.instance;
        }
    }

    @RBuiltin(name = "isdebugged", kind = RBuiltinKind.INTERNAL, parameterNames = {"fun"})
    public abstract static class IsDebugged extends RBuiltinNode {
        @SuppressWarnings("unused")
        @Specialization
        @SlowPath
        protected byte isDebugged(RFunction fun) {
            // TODO implement
            controlVisibility();
            return RRuntime.LOGICAL_FALSE;
        }
    }

}
