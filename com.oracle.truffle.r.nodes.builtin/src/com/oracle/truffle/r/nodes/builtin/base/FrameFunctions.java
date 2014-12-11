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

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.EvalPolicy;
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

        private final ConditionProfile currentFrameProfile = ConditionProfile.createBinaryProfile();
        protected final BranchProfile errorProfile = BranchProfile.create();

        /**
         * Determine the frame access mode of a subclass. The rule of thumb is that subclasses that
         * only use the frame internally should not materialize it, i.e., they should use
         * {@link FrameAccess#READ_ONLY} or {@link FrameAccess#READ_WRITE}.
         */
        protected abstract FrameAccess frameAccess();

        /**
         * Handles n > 0 and n < 0 and errors relating to stack depth.
         */
        protected Frame getFrame(VirtualFrame frame, int n) {
            int actualFrame;
            int depth = RArguments.getDepth(frame);
            if (n > 0) {
                if (n > depth) {
                    errorProfile.enter();
                    throw RError.error(RError.Message.NOT_THAT_MANY_FRAMES);
                }
                actualFrame = n;
            } else {
                actualFrame = depth + n - 1;
            }
            if (currentFrameProfile.profile(actualFrame == depth)) {
                return frame;
            } else {
                Frame callerFrame = Utils.getStackFrame(frameAccess(), actualFrame);
                if (callerFrame == null) {
                    errorProfile.enter();
                    throw RError.error(RError.Message.NOT_THAT_MANY_FRAMES);
                }
                return callerFrame;
            }
        }
    }

    private abstract static class CallHelper extends FrameHelper {
        @TruffleBoundary
        protected RLanguage createCall(Frame cframe, @SuppressWarnings("unused") boolean sysCall, boolean expandDots) {
            SourceSection callSource = RArguments.getCallSourceSection(cframe);
            String functionName = extractFunctionName(callSource.getCode());
            RLanguage call = null;
            RArgsValuesAndNames argsValuesAndNames = null;
            int argsLength = RArguments.getArgumentsLength(cframe);

            if (argsLength > 0) {
                Object[] values;
                String[] names;
                Object arg1 = RArguments.getArgument(cframe, 0);
                if (arg1 instanceof RArgsValuesAndNames) {
                    // ...
                    RArgsValuesAndNames temp = ((RArgsValuesAndNames) arg1);
                    if (expandDots || temp.length() == 0) {
                        values = temp.getValues();
                        names = temp.getNames();
                    } else {
                        names = new String[]{"..."};
                        RNode[] listArgs = new RNode[temp.getValues().length];
                        String[] listNames = new String[listArgs.length];
                        for (int i = 0; i < listArgs.length; i++) {
                            listArgs[i] = RASTUtils.createNodeForValue(temp.getValues()[i]);
                            String listName;
                            if ((listName = temp.getNames()[i]) != null) {
                                listNames[i] = listName;
                            }
                        }
                        RNode varArgs = PromiseNode.createVarArgs(null, EvalPolicy.PROMISED, listArgs, listNames, null, null);
                        CallArgumentsNode callArgsNode = CallArgumentsNode.create(false, false, new RNode[]{varArgs}, names);
                        values = new Object[]{RASTUtils.createCall("list", callArgsNode)};
                        call = RDataFactory.createLanguage(RASTUtils.createCall(functionName, callArgsNode));
                    }
                } else {
                    values = new Object[argsLength];
                    /*
                     * There is a bug in that RArguments names are filled (from the formals)
                     * regardless of whether they were used in the call. I.e. can't distinguish g(x)
                     * and g(a=x)
                     */
                    names = new String[argsLength];
                    int argc = 0;
                    for (int i = 0; i < argsLength; i++) {
                        Object arg = RArguments.getArgument(cframe, i);
                        if (!(arg instanceof RMissing)) {
                            values[argc] = arg;
                            names[argc] = RArguments.getName(cframe, i);
                            argc++;
                        }
                    }
                    if (argc != argsLength) {
                        if (argc == 0) {
                            values = new Object[0];
                            names = new String[0];
                        } else {
                            values = Arrays.copyOfRange(values, 0, argc);
                            names = Arrays.copyOfRange(names, 0, argc);
                        }
                    }
                }
                argsValuesAndNames = new RArgsValuesAndNames(values, names);
            } else {
                // Call.makeCall treats argsValuesAndNames == null as zero
            }

            if (call == null) {
                call = Call.makeCall(functionName, argsValuesAndNames);
            }
            /*
             * the "names" are set as the "names" attribute, unless they were all unset. N.B. The
             * names are attributing the AST (as a list) and the function name counts and has a null
             * "name"!
             */
            if (argsValuesAndNames != null && !argsValuesAndNames.isAllNamesEmpty()) {
                String[] argNames = argsValuesAndNames.getNames();
                String[] attrNames = new String[1 + argNames.length];
                attrNames[0] = "";
                System.arraycopy(argNames, 0, attrNames, 1, argNames.length);
                call.setAttr(RRuntime.NAMES_ATTR_KEY, RDataFactory.createStringVector(attrNames, RDataFactory.COMPLETE_VECTOR));
            }
            return call;
        }

        @TruffleBoundary
        private static String extractFunctionName(String callSource) {
            // TODO in the general case we need to parse this into an AST
            int index = callSource.indexOf('(');
            return callSource.substring(0, index);
        }
    }

    @RBuiltin(name = "sys.call", kind = INTERNAL, parameterNames = {"which"})
    public abstract static class SysCall extends CallHelper {

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.READ_ONLY;
        }

        @Specialization
        protected RLanguage sysCall(VirtualFrame frame, int which) {
            /*
             * sys.call preserves provided names but does not create them, unlike match.call The
             * generated call has the same number of arguments as provided, modulo ... processing.
             * ... is always expanded.
             */
            controlVisibility();
            Frame cframe = getFrame(frame, which);
            return createCall(cframe, true, true);
        }

        @Specialization
        protected RLanguage sysCall(VirtualFrame frame, double which) {
            return sysCall(frame, (int) which);
        }

    }

    /**
     * Generate a call object in which all of the arguments are fully qualified.
     */
    @RBuiltin(name = "match.call", kind = INTERNAL, parameterNames = {"definition", "call", "expand.dots"})
    public abstract static class MatchCall extends CallHelper {

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.READ_ONLY;
        }

        @Specialization
        protected RLanguage matchCall(VirtualFrame frame, @SuppressWarnings("unused") RNull definition, @SuppressWarnings("unused") RLanguage call, byte expandDots) {
            // TODO handle an explicitly provided call (default from R closure is
            // sys.call(sys.parent())
            controlVisibility();
            RPromise callArg = (RPromise) RArguments.getArgument(frame, 1);
            if (callArg.isDefault()) {
                Frame cframe = Utils.getCallerFrame(frame, FrameAccess.READ_ONLY);
                return createCall(cframe, false, RRuntime.fromLogical(expandDots));
            } else {
                throw RError.nyi(getEncapsulatingSourceSection(), " explicit call argument not implemented");
            }
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
        protected int sysNFrame(VirtualFrame frame) {
            controlVisibility();
            return RArguments.getDepth(frame) - 1;
        }
    }

    @RBuiltin(name = "sys.frame", kind = INTERNAL, parameterNames = {"which"})
    public abstract static class SysFrame extends FrameHelper {

        private final ConditionProfile zeroProfile = ConditionProfile.createBinaryProfile();

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.MATERIALIZE;
        }

        @Specialization
        protected REnvironment sysFrame(VirtualFrame frame, int which) {
            controlVisibility();
            if (zeroProfile.profile(which == 0)) {
                // TODO Strictly this should be the value of .GlobalEnv
                // (which may differ from globalenv() during startup)
                return REnvironment.globalEnv();
            } else {
                Frame callerFrame = getFrame(frame, which);
                return REnvironment.frameToEnvironment(callerFrame.materialize());
            }
        }

        @Specialization
        protected REnvironment sysFrame(VirtualFrame frame, double which) {
            return sysFrame(frame, (int) which);
        }
    }

    @RBuiltin(name = "sys.parent", kind = INTERNAL, parameterNames = {"n"})
    public abstract static class SysParent extends RBuiltinNode {

        @Specialization
        protected int sysParent(VirtualFrame frame, int n) {
            controlVisibility();
            int p = RArguments.getDepth(frame) - n - 1;
            return p < 0 ? 0 : p;
        }

        @Specialization
        protected int sysParent(VirtualFrame frame, double n) {
            return sysParent(frame, (int) n);
        }

    }

    @RBuiltin(name = "sys.function", kind = INTERNAL, parameterNames = {"which"})
    public abstract static class SysFunction extends FrameHelper {

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.READ_ONLY;
        }

        @Specialization
        protected Object sysFunction(VirtualFrame frame, int which) {
            controlVisibility();
            // N.B. Despite the spec, n==0 is treated as the current function
            Frame callerFrame = getFrame(frame, which);
            RFunction func = RArguments.getFunction(callerFrame);
            if (func == null) {
                return RNull.instance;
            } else {
                return func;
            }
        }

        @Specialization
        protected Object sysFunction(VirtualFrame frame, double which) {
            return sysFunction(frame, (int) which);
        }
    }

    @RBuiltin(name = "sys.parents", kind = INTERNAL, parameterNames = {})
    public abstract static class SysParents extends FrameHelper {

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.READ_ONLY;
        }

        @Specialization
        protected RIntVector sysParents(VirtualFrame frame) {
            controlVisibility();
            int d = RArguments.getDepth(frame) - 1;
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
            errorProfile.enter();
            throw RError.nyi(null, "sys.frames is not implemented");
        }
    }

    /**
     * The environment of the caller of the function that called parent.frame.
     */
    @RBuiltin(name = "parent.frame", kind = INTERNAL, parameterNames = {"n"})
    public abstract static class ParentFrame extends FrameHelper {

        private final ConditionProfile nullProfile = ConditionProfile.createBinaryProfile();

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.MATERIALIZE;
        }

        @Specialization
        protected REnvironment parentFrame(VirtualFrame frame, int n) {
            controlVisibility();
            if (n == 0) {
                errorProfile.enter();
                throw RError.error(RError.Message.INVALID_ARGUMENT, RRuntime.toString(n));
            }
            int p = RArguments.getDepth(frame) - n - 1;
            Frame callerFrame = Utils.getStackFrame(FrameAccess.MATERIALIZE, p);
            if (nullProfile.profile(callerFrame == null)) {
                return REnvironment.globalEnv();
            } else {
                return REnvironment.frameToEnvironment(callerFrame.materialize());
            }
        }

        @Specialization
        protected REnvironment parentFrame(VirtualFrame frame, double n) {
            return parentFrame(frame, (int) n);
        }
    }

}
