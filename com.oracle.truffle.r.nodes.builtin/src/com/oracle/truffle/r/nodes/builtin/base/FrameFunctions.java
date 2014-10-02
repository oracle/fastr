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

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

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
         * Determine the frame access mode of a subclass. The rule of thumb is that subclasses that
         * only use the frame internally should not materialise it, i.e., they should use
         * {@link FrameAccess#READ_ONLY} or {@link FrameAccess#READ_WRITE}.
         */
        protected abstract FrameAccess frameAccess();

        /**
         * Handles n > 0 and n < 0 and errors relating to stack depth.
         */
        protected Frame getFrame(int nn) {
            int n = nn;
            if (n > 0) {
                int d = Utils.stackDepth();
                if (n > d) {
                    throw RError.error(RError.Message.NOT_THAT_MANY_FRAMES);
                }
                n = d - n + 1; // add one to skip internal evaluation frame
            } else {
                n = -n + 1;
            }
            Frame callerFrame = Utils.getStackFrame(frameAccess(), n);
            if (callerFrame == null) {
                throw RError.error(RError.Message.NOT_THAT_MANY_FRAMES);
            }
            return callerFrame;
        }
    }

    @RBuiltin(name = "sys.call", kind = INTERNAL, parameterNames = {"which"})
    public abstract static class SysCall extends FrameHelper {

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.READ_ONLY;
        }

        @Specialization
        protected RLanguage sysCall(int which) {
            controlVisibility();
            Frame cframe = getFrame(which);
            Object[] values = new Object[RArguments.getArgumentsLength(cframe)];
            RArguments.copyArgumentsInto(cframe, values);
            int namesLength = RArguments.getNamesLength(cframe);
            String[] names = new String[namesLength];
            for (int i = 0; i < namesLength; ++i) {
                names[i] = RArguments.getName(cframe, i);
            }
            SourceSection callSource = RArguments.getCallSourceSection(cframe);
            String functionName = extractFunctionName(callSource.getCode());
            return Call.makeCall(functionName, new RArgsValuesAndNames(values, names));
        }

        @Specialization
        protected RLanguage sysCall(double which) {
            return sysCall((int) which);
        }

        @SlowPath
        private static String extractFunctionName(String callSource) {
            // TODO remove the need for this by assembling a proper RLanguage object for the call
            return callSource.substring(0, callSource.indexOf('('));
        }

    }

    /**
     * Generate a call object in which all of the arguments are fully qualified.
     */
    @RBuiltin(name = "match.call", kind = INTERNAL, parameterNames = {"definition", "call", "expand.dots"})
    public abstract static class MatchCall extends FrameHelper {

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.READ_ONLY;
        }

        @Specialization
        // TODO support expand.dots argument
        protected RLanguage matchCall(@SuppressWarnings("unused") RNull definition, @SuppressWarnings("unused") RLanguage call, @SuppressWarnings("unused") byte expandDots) {
            controlVisibility();
            Frame cframe = Utils.getCallerFrame(FrameAccess.READ_ONLY);
            Object[] values = new Object[RArguments.getArgumentsLength(cframe)];
            RArguments.copyArgumentsInto(cframe, values);
            int namesLength = RArguments.getNamesLength(cframe);
            String[] names = new String[namesLength];
            for (int i = 0; i < namesLength; ++i) {
                names[i] = RArguments.getName(cframe, i);
            }

            // extract the name of the function that was called
            // TODO find a better solution for this
            String callSource = RArguments.getCallSourceSection(cframe).getCode();
            String functionName = callSource.substring(0, callSource.indexOf('('));

            return Call.makeCall(functionName, new RArgsValuesAndNames(values, names));
        }

        @Specialization
        @SuppressWarnings("unused")
        protected RLanguage matchCall(RFunction definition, RLanguage call, byte expandDots) {
            controlVisibility();
            throw RInternalError.unimplemented();
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
        protected final FrameAccess frameAccess() {
            return FrameAccess.MATERIALIZE;
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
        protected REnvironment sysFrame(double nd) {
            return sysFrame((int) nd);
        }
    }

    @RBuiltin(name = "sys.parent", kind = INTERNAL, parameterNames = {"n"})
    public abstract static class SysParent extends RBuiltinNode {

        @Specialization
        protected int sysParent(int nd) {
            controlVisibility();
            int p = Utils.stackDepth() - nd;
            return p < 0 ? 0 : p;
        }

        @Specialization
        protected int sysParent(double nd) {
            return sysParent((int) nd);
        }

    }

    @RBuiltin(name = "sys.function", kind = INTERNAL, parameterNames = {"which"})
    public abstract static class SysFunction extends FrameHelper {

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.READ_ONLY;
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
        protected Object sysFunction(double nd) {
            return sysFunction((int) nd);
        }
    }

    @RBuiltin(name = "sys.parents", kind = INTERNAL, parameterNames = {})
    public abstract static class SysParents extends FrameHelper {

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.READ_ONLY;
        }

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

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.READ_ONLY;
        }

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
        protected final FrameAccess frameAccess() {
            return FrameAccess.MATERIALIZE;
        }

        @Specialization
        protected REnvironment parentFrame(double nd) {
            controlVisibility();
            int n = (int) nd;
            if (n == 0) {
                throw RError.error(RError.Message.INVALID_ARGUMENT, RRuntime.toString(n));
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
