/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * sys.R. See <a
 * href="https://stat.ethz.ch/R-manual/R-devel/library/base/html/sys.parent.html">here</a>. N.B. The
 * frame for the sys functions themselves is not counted in the R spec. Frames are numbered 0, 1, ..
 * starting from .GlobalEnv. Non-negative arguments are frame numbers, negative arguments are
 * relative to the current frame.
 */
public class FrameFunctions {

    public abstract static class FrameHelper extends RBuiltinNode {

        /**
         * Handles n > 0 and n < 0 and errors relating to stack depth.
         */
        protected Frame getFrame(int nn) {
            int n = nn;
            if (n > 0) {
                int d = Utils.stackDepth();
                if (n > d) {
                    RError.error(RError.Message.NOT_THAT_MANY_FRAMES);
                }
                n = d - n;
            } else {
                n = -n + 1;
            }
            Frame callerFrame = Utils.getStackFrame(FrameAccess.MATERIALIZE, n);
            if (callerFrame == null) {
                RError.error(RError.Message.NOT_THAT_MANY_FRAMES);
            }
            return callerFrame;
        }
    }

    @RBuiltin(name = "sys.nframe", kind = INTERNAL, parameterNames = {})
    public abstract static class SysNFrame extends RBuiltinNode {
        @Specialization
        protected int sysNFrame() {
            controlVisibility();
            return Utils.stackDepth();
        }
    }

    @RBuiltin(name = "sys.frame", kind = INTERNAL, parameterNames = {"which"})
    public abstract static class SysFrame extends FrameHelper {

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(0)};
        }

        @Specialization
        protected REnvironment sysFrame(int nd) {
            controlVisibility();
            int n = nd;
            if (n == 0) {
                // TODO Strictly this should be the value of .GlobalEnv
                // (which may differ from globalenv() during startup)
                return REnvironment.globalEnv();
            } else {
                Frame callerFrame = getFrame(n);
                return REnvironment.frameToEnvironment(callerFrame.materialize());
            }
        }

        @Specialization
        protected REnvironment sysFrame(VirtualFrame frame, double nd) {
            return sysFrame(frame, (int) nd);
        }
    }

    @RBuiltin(name = "sys.parent", kind = INTERNAL, parameterNames = {"n"})
    public abstract static class SysParent extends RBuiltinNode {

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(1)};
        }

        @Specialization
        protected int sysParent(int nd) {
            controlVisibility();
            int n = nd;
            int d = Utils.stackDepth();
            if (n > d) {
                return 0;
            } else {
                return d - n;
            }
        }

        @Specialization
        protected int sysParent(double nd) {
            return sysParent((int) nd);
        }
    }

    @RBuiltin(name = "sys.function", kind = INTERNAL, parameterNames = {"which"})
    public abstract static class SysFunction extends FrameHelper {

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(0)};
        }

        @Specialization
        protected Object sysFunction(int nd) {
            controlVisibility();
            int n = nd;
            // N.B. Despite the spec, n==0 is treated as the current function
            Frame callerFrame = getFrame(n);
            RFunction func = RArguments.getFunction(callerFrame);
            if (func == null) {
                return RNull.instance;
            } else {
                return func;
            }
        }

        @Specialization
        protected Object sysFunction(VirtualFrame frame, double nd) {
            return sysFunction(frame, (int) nd);
        }
    }

    @RBuiltin(name = "sys.parents", kind = INTERNAL, parameterNames = {})
    public abstract static class SysParents extends FrameHelper {
        @Specialization
        protected RIntVector sysParents() {
            controlVisibility();
            int d = Utils.stackDepth();
            int[] data = new int[d];
            for (int i = 0; i < d; i++) {
                data[i] = i;
            }
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = "sys.frames", kind = INTERNAL, parameterNames = {})
    public abstract static class SysFrames extends FrameHelper {
        @Specialization
        protected Object sysFrames() {
            throw RError.nyi(null, "sys.frames is not implemented");
        }
    }

    /**
     * The environment of the caller of the function that called parent.frame.
     */
    @RBuiltin(name = "parent.frame", kind = INTERNAL, parameterNames = {"n"})
    public abstract static class ParentFrame extends FrameHelper {

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(1)};
        }

        @Specialization
        protected REnvironment parentFrame(double nd) {
            controlVisibility();
            int n = (int) nd;
            if (n == 0) {
                RError.error(RError.Message.INVALID_ARGUMENT, RRuntime.toString(n));
            }
            Frame callerFrame = Utils.getStackFrame(FrameAccess.MATERIALIZE, n + 1);
            if (callerFrame == null) {
                return REnvironment.globalEnv();
            } else {
                return REnvironment.frameToEnvironment(callerFrame.materialize());
            }
        }
    }
}
