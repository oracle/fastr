/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.CallerFrameClosure;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public final class GetCallerFrameNode extends RBaseNode {

    private final BranchProfile frameAvailableProfile = BranchProfile.create();
    private final BranchProfile closureProfile = BranchProfile.create();
    @CompilationFinal private boolean slowPathInitialized;

    @Override
    public NodeCost getCost() {
        return NodeCost.NONE;
    }

    public MaterializedFrame execute(Frame frame) {
        Object callerFrameObject = RArguments.getCallerFrame(frame);
        if (callerFrameObject instanceof MaterializedFrame) {
            frameAvailableProfile.enter();
            return (MaterializedFrame) callerFrameObject;
        }
        if (callerFrameObject instanceof CallerFrameClosure) {
            closureProfile.enter();
            CallerFrameClosure closure = (CallerFrameClosure) callerFrameObject;
            RCaller parent = RArguments.getCall(frame);
            MaterializedFrame slowPathFrame = notifyCallers(closure, parent);
            if (slowPathFrame != null) {
                return slowPathFrame;
            }
        }
        assert callerFrameObject == null;

        // S3 method can be dispatched from top-level where there is no caller frame
        // Since RArguments does not allow to create arguments with a 'null' caller frame, this
        // must be the top level case.
        return frame.materialize();
    }

    @TruffleBoundary
    private static MaterializedFrame notifyCallers(CallerFrameClosure closure, RCaller parent) {

        // inform the responsible call node to create a caller frame
        closure.setNeedsCallerFrame();

        // if interpreted, we will have a materialized frame in the closure
        MaterializedFrame materializedCallerFrame = closure.getMaterializedCallerFrame();
        if (materializedCallerFrame != null) {
            return materializedCallerFrame;
        }
        RError.performanceWarning("slow caller frame access");
        // for now, get it on the very slow path
        Frame callerFrame = Utils.getCallerFrame(parent, FrameAccess.MATERIALIZE);
        if (callerFrame != null) {
            return callerFrame.materialize();
        }
        return null;
    }

}
