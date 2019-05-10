/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;

public class RBaseNodeWithWarnings extends RBaseNode {
    @CompilerDirectives.CompilationFinal boolean hasSeenWarning;

    /**
     * Raises the given warning with the error context determined by {@link #getErrorContext()}.
     * This function is profiled so that it will only use a real call if a warning was issued from
     * this node before.
     */
    public final void warning(Message msg) {
        warning((RBaseNode) null, msg);
    }

    public final void warning(RBaseNode ctx, RError.Message msg) {
        if (!hasSeenWarning) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            hasSeenWarning = true;
        }
        RError.warning(ctx != null ? ctx : getErrorContext(), msg);
    }

    /**
     * @see #warning(Message)
     */
    public final void warning(RError.Message msg, Object arg1) {
        if (!hasSeenWarning) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            hasSeenWarning = true;
        }
        RError.warning(getErrorContext(), msg, arg1);
    }

    /**
     * @see #warning(Message)
     */
    public final void warning(RError.Message msg, Object arg1, Object arg2) {
        if (!hasSeenWarning) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            hasSeenWarning = true;
        }
        RError.warning(getErrorContext(), msg, arg1, arg2);
    }

    /**
     * @see #warning(Message)
     */
    public final void warning(RError.Message msg, Object arg1, Object arg2, Object arg3) {
        if (!hasSeenWarning) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            hasSeenWarning = true;
        }
        RError.warning(getErrorContext(), msg, arg1, arg2, arg3);
    }
}
