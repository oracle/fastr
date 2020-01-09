/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.foreign;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.function.call.RExplicitCallNode;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;

/**
 * Node that provides methods to check if given native symbol is overridden by R function and to
 * execute that R function.
 */
final class CallRegisteredROverride extends Node {
    @Child private RExplicitCallNode explicitCall;
    private final ConditionProfile isRegisteredFunProfile = ConditionProfile.createBinaryProfile();

    public static CallRegisteredROverride create() {
        return new CallRegisteredROverride();
    }

    public boolean isRegisteredRFunction(NativeCallInfo nativeCallInfo) {
        return isRegisteredRFunction(nativeCallInfo.address);
    }

    public boolean isRegisteredRFunction(SymbolHandle handle) {
        return isRegisteredFunProfile.profile(!handle.isLong() && handle.asTruffleObject() instanceof RFunction);
    }

    public boolean isRegisteredRFunction(RExternalPtr ptr) {
        return isRegisteredRFunction(ptr.getAddr());
    }

    public Object execute(VirtualFrame frame, NativeCallInfo nativeCallInfo, RArgsValuesAndNames args) {
        return execute(frame, nativeCallInfo.address, args);
    }

    public Object execute(VirtualFrame frame, RExternalPtr ptr, RArgsValuesAndNames args) {
        return execute(frame, ptr.getAddr(), args);
    }

    public Object execute(VirtualFrame frame, SymbolHandle func, RArgsValuesAndNames args) {
        if (explicitCall == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            explicitCall = insert(RExplicitCallNode.create());
        }
        RFunction function = (RFunction) func.asTruffleObject();
        return explicitCall.call(frame, function, args);
    }
}
