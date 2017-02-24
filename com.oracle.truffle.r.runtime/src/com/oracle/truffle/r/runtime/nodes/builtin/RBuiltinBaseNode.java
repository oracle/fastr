/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.nodes.builtin;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.RErrorException;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class RBuiltinBaseNode extends RBaseNode {

    protected abstract RBaseNode getErrorContext();

    public final RError error(RErrorException exception) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getErrorContext(), exception);
    }

    public final RError error(RError.Message message) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getErrorContext(), message);
    }

    public final RError error(RError.Message message, Object arg) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getErrorContext(), message, arg);
    }

    public final RError error(RError.Message message, Object arg1, Object arg2) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getErrorContext(), message, arg1, arg2);
    }

    public final RError error(RError.Message message, Object arg1, Object arg2, Object arg3) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getErrorContext(), message, arg1, arg2, arg3);
    }

    public final RError error(RError.Message message, Object arg1, Object arg2, Object arg3, Object arg4) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getErrorContext(), message, arg1, arg2, arg3, arg4);
    }

    public final RError error(RError.Message message, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getErrorContext(), message, arg1, arg2, arg3, arg4, arg5);
    }

    public final RError error(RError.Message message, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getErrorContext(), message, arg1, arg2, arg3, arg4, arg5, arg6);
    }
}
