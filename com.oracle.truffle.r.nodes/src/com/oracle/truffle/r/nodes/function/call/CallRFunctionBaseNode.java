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

    public boolean setNeedsCallerFrame() {
        boolean value = !needsNoCallerFrame.isValid();
        needsNoCallerFrame.invalidate();
        return value;
    }

    private Object getCallerFrameClosure(VirtualFrame callerFrame) {
        if (CompilerDirectives.inInterpreter()) {
            return new InvalidateNoCallerFrame(needsNoCallerFrame, callerFrame.materialize());
        }
        return invalidateNoCallerFrame;
    }

    protected final Object getCallerFrameObject(VirtualFrame callerFrame) {
        return needsNoCallerFrame.isValid() ? getCallerFrameClosure(callerFrame) : callerFrame.materialize();
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
