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
package com.oracle.truffle.r.nodes.function.signature;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.PromiseEvalFrameDebug;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.runtime.PromiseEvalFrame;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.nodes.IdenticalVisitor;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Support for returning the correct frame depth. If no promise evaluation is in progress, then we
 * simply return the current frame depth. Otherwise, and this is determined by
 * {@code RArguments.getPromiseFrame(frame) != null}, we need to check if the caller matches the
 * promise being evaluated. If the effective frame depth is exactly 1 higher than the promise frame
 * depth, then it must match. Otherwise, we have to check the caller for a match to the promise
 * (slow path).
 *
 */
public final class FrameDepthNode extends RBaseNode {
    private final ConditionProfile isPromiseEvalProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isFastPath = ConditionProfile.createBinaryProfile();

    public int execute(VirtualFrame frame) {
        Frame pfFrame = RArguments.getPromiseFrame(frame);
        int depth = RArguments.getDepth(frame);
        if (isPromiseEvalProfile.profile(pfFrame != null)) {
            PromiseEvalFrame promiseEvalFrame = (PromiseEvalFrame) pfFrame;
            int effectiveDepth = RArguments.getEffectiveDepth(frame);
            int pdepth = promiseEvalFrame.getPromiseFrameDepth();
            boolean match = false;
            if (isFastPath.profile(effectiveDepth == pdepth + 1)) {
                depth = effectiveDepth;
                match = true;
            } else {
                /* slow path, as must check if caller matches the promise */
                Frame eFrame = Utils.getStackFrame(FrameAccess.READ_ONLY, depth - 1);
                if (matchPromise(RArguments.getCall(eFrame), RContext.getRRuntimeASTAccess().unwrapPromiseRep(promiseEvalFrame.getPromise()))) {
                    depth = effectiveDepth;
                    match = true;
                }
            }
            if (PromiseEvalFrameDebug.enabled) {
                PromiseEvalFrameDebug.dumpStack("ged" + (match ? "(true)" : "(false)"));
                PromiseEvalFrameDebug.match(match, promiseEvalFrame, depth);
            }
        } else {
            if (PromiseEvalFrameDebug.enabled) {
                PromiseEvalFrameDebug.noPromise(depth);
            }
        }
        return depth;
    }

    @TruffleBoundary
    private static boolean matchPromise(RCaller call, RSyntaxNode promiseNode) {
        if (call == null) {
            return false;
        }
        RSyntaxNode callNode = RASTUtils.unwrap(call.getSyntaxNode()).asRSyntaxNode();
        return new IdenticalVisitor().accept(promiseNode, callNode);

    }

}
