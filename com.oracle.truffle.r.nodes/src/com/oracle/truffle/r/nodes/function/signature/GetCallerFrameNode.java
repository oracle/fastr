/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.profiles.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.nodes.*;

public final class GetCallerFrameNode extends RBaseNode {

    @CompilationFinal private boolean slowPathSeen;
    private final BranchProfile topLevelProfile = BranchProfile.create();

    @Override
    public NodeCost getCost() {
        return slowPathSeen ? NodeCost.MONOMORPHIC : NodeCost.NONE;
    }

    public MaterializedFrame execute(VirtualFrame frame) {
        MaterializedFrame funFrame = RArguments.getCallerFrame(frame);
        if (funFrame == null) {
            if (!slowPathSeen) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                slowPathSeen = true;
            }
            RError.performanceWarning("slow caller frame access in UseMethod dispatch");
            Frame callerFrame = Utils.getCallerFrame(frame, FrameAccess.MATERIALIZE);
            if (callerFrame != null) {
                return callerFrame.materialize();
            } else {
                topLevelProfile.enter();
                // S3 method can be dispatched from top-level where there is no caller frame
                return frame.materialize();
            }
        }
        return funFrame;
    }
}
