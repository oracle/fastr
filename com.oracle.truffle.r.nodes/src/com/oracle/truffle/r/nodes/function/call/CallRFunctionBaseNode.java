/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function.call;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.CallerFrameClosure;

public abstract class CallRFunctionBaseNode extends Node {

    protected final Assumption needsNoCallerFrame = Truffle.getRuntime().createAssumption("no caller frame");
    protected final CallerFrameClosure invalidateNoCallerFrame = new InvalidateNoCallerFrame(needsNoCallerFrame);
    private static final CallerFrameClosure DUMMY = new DummyCallerFrameClosure();

    public boolean setNeedsCallerFrame() {
        boolean value = !needsNoCallerFrame.isValid();
        needsNoCallerFrame.invalidate();
        return value;
    }

    private Object getCallerFrameClosure(MaterializedFrame callerFrame) {
        if (CompilerDirectives.inInterpreter()) {
            return new InvalidateNoCallerFrame(needsNoCallerFrame, callerFrame);
        }
        return invalidateNoCallerFrame;
    }

    private Object getCallerFrameClosure(VirtualFrame callerFrame) {
        if (CompilerDirectives.inInterpreter()) {
            return new InvalidateNoCallerFrame(needsNoCallerFrame, callerFrame != null ? callerFrame.materialize() : null);
        }
        return invalidateNoCallerFrame;
    }

    protected final Object getCallerFrameObject(VirtualFrame curFrame, MaterializedFrame callerFrame, boolean topLevel) {
        if (needsNoCallerFrame.isValid()) {
            return getCallerFrameClosure(callerFrame);
        } else {
            if (callerFrame != null) {
                return callerFrame;
            } else if (topLevel) {
                return DUMMY;
            }
            return curFrame.materialize();
        }
    }

    protected final Object getCallerFrameObject(VirtualFrame callerFrame) {
        return needsNoCallerFrame.isValid() ? getCallerFrameClosure(callerFrame) : callerFrame.materialize();
    }

    private static final class DummyCallerFrameClosure extends CallerFrameClosure {

        @Override
        public void setNeedsCallerFrame() {
        }

        @Override
        public MaterializedFrame getMaterializedCallerFrame() {
            return null;
        }

    }

    public static final class InvalidateNoCallerFrame extends CallerFrameClosure {

        private final Assumption needsNoCallerFrame;
        private final MaterializedFrame frame;

        protected InvalidateNoCallerFrame(Assumption needsNoCallerFrame) {
            this.needsNoCallerFrame = needsNoCallerFrame;
            this.frame = null;
        }

        protected InvalidateNoCallerFrame(Assumption needsNoCallerFrame, MaterializedFrame frame) {
            this.needsNoCallerFrame = needsNoCallerFrame;
            this.frame = frame;
        }

        @Override
        public void setNeedsCallerFrame() {
            needsNoCallerFrame.invalidate();
        }

        @Override
        public MaterializedFrame getMaterializedCallerFrame() {
            return frame;
        }

    }

}
