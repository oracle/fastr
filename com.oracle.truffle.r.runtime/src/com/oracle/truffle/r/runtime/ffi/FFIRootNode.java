/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RNull;

public final class FFIRootNode extends RootNode {
    @Child CallRFFI.CallRFFINode callRFFINode = RFFIFactory.getRFFI().getCallRFFI().createCallRFFINode();

    public FFIRootNode() {
        super(RContext.getRRuntimeASTAccess().getTruffleRLanguage(), null, new FrameDescriptor());

    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object[] args = frame.getArguments();
        NativeCallInfo nativeCallInfo = (NativeCallInfo) args[0];
        boolean isVoidCall = (boolean) args[1];
        Object[] callArgs = (Object[]) args[2];
        if (isVoidCall) {
            callRFFINode.invokeVoidCall(nativeCallInfo, callArgs);
            return RNull.instance;
        } else {
            return callRFFINode.invokeCall(nativeCallInfo, callArgs);
        }
    }

    public static RootCallTarget createCallTarget() {
        return Truffle.getRuntime().createCallTarget(new FFIRootNode());
    }

}
