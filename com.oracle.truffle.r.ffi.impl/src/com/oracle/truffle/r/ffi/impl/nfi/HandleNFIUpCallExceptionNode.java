/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nfi;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.ffi.CallRFFI.HandleUpCallExceptionNode;
import com.oracle.truffle.r.runtime.ffi.DownCallNodeFactory.DownCallNode;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;

public class HandleNFIUpCallExceptionNode extends Node implements HandleUpCallExceptionNode {
    @Child private DownCallNode setFlagNode = TruffleNFI_DownCallNodeFactory.INSTANCE.createDownCallNode(NativeFunction.set_exception_flag);
    private final ConditionProfile isEmbeddedTopLevel = ConditionProfile.createBinaryProfile();

    @Override
    @TruffleBoundary
    public void execute(Throwable originalEx) {
        if (isEmbeddedTopLevel.profile(RContext.isEmbedded() && isTopLevel())) {
            return;
        }
        setFlagNode.call();
        RuntimeException ex;
        if (originalEx instanceof RuntimeException) {
            ex = (RuntimeException) originalEx;
        } else {
            ex = new RuntimeException(originalEx);
        }
        TruffleNFI_Context.getInstance().setLastUpCallException(ex);
    }

    private static boolean isTopLevel() {
        return ((TruffleNFI_Context) RContext.getInstance().getRFFI()).getCallDepth() == 0;
    }
}
