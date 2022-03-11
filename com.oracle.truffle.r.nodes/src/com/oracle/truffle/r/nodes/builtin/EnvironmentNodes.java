/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.EnvironmentNodesFactory.GetFunctionEnvironmentNodeGen;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.UpdateShareableChildValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.ContextStateImpl;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public final class EnvironmentNodes {

    /**
     * Convert an {@link RList} to an {@link REnvironment}, which is needed in several builtins,
     * e.g. {@code substitute}.
     */
    public abstract static class RList2EnvNode extends RBaseNode {
        private final boolean ignoreMissingNames;

        protected RList2EnvNode() {
            this(false);
        }

        protected RList2EnvNode(boolean ignoreMissingNames) {
            this.ignoreMissingNames = ignoreMissingNames;
        }

        public abstract REnvironment execute(Object listOrNull, REnvironment target, String envName, REnvironment parentEnv);

        @Specialization(guards = "isEmpty(list)")
        protected REnvironment doEmptyList(@SuppressWarnings("unused") RAbstractListVector list, REnvironment target, String envName, REnvironment parentEnv,
                        @Cached("createBinaryProfile()") ConditionProfile nullTargetProfile) {
            return nullTargetProfile.profile(target == null) ? createNewEnv(envName, parentEnv) : target;
        }

        @TruffleBoundary
        private static REnvironment createNewEnv(String envName, REnvironment parentEnv) {
            REnvironment createNewEnv = RDataFactory.createNewEnv(envName);
            RArguments.initializeEnclosingFrame(createNewEnv.getFrame(), parentEnv.getFrame());
            createNewEnv.setParent(parentEnv);
            return createNewEnv;
        }

        @Specialization(guards = "!isEmpty(list)")
        @TruffleBoundary
        protected REnvironment doList(RAbstractListVector list, REnvironment target, String envName, REnvironment parentEnv) {
            RStringVector names = list.getNames();
            if (names == null) {
                throw error(RError.Message.LIST_NAMES_SAME_LENGTH);
            }
            REnvironment result;
            if (target == null) {
                assert parentEnv != null;

                FrameDescriptor cachedFd = ContextStateImpl.getFrameDescriptorFromList(list);
                boolean hasEnclosingFD = !FrameSlotChangeMonitor.isEnclosingFrameDescriptor(cachedFd, parentEnv.getFrame());
                if (hasEnclosingFD) {
                    cachedFd = FrameSlotChangeMonitor.copyFrameDescriptorWithMetadata(cachedFd);
                }

                result = RDataFactory.createNewEnv(cachedFd, envName);
                if (hasEnclosingFD) {
                    FrameSlotChangeMonitor.initializeNonFunctionFrameDescriptor(result.getFrame().getFrameDescriptor(), result.getFrame());
                }
                RArguments.initializeEnclosingFrame(result.getFrame(), parentEnv.getFrame());
                RArguments.setEnclosingFrame(result.getFrame(), parentEnv.getFrame(), false);
                result.setParent(parentEnv);
            } else {
                result = target;
            }
            if (parentEnv != null) {
                result.setParent(parentEnv);
            }
            int len = list.getLength();
            for (int i = 0; i < len; i++) {
                String name = names.getDataAt(i);
                if (!ignoreMissingNames && name.length() == 0) {
                    throw error(RError.Message.ZERO_LENGTH_VARIABLE);
                }
                // in case of duplicates, last element in list wins
                result.safePut(name, UpdateShareableChildValue.update(list, list.getDataAt(i)));
            }
            return result;
        }

        protected boolean isEmpty(RAbstractListVector list) {
            return list.getLength() == 0;
        }

        public static RList2EnvNode create() {
            return EnvironmentNodesFactory.RList2EnvNodeGen.create();
        }

        public static RList2EnvNode create(boolean ignoreMissingNames) {
            return EnvironmentNodesFactory.RList2EnvNodeGen.create(ignoreMissingNames);
        }
    }

    @GenerateUncached
    public abstract static class GetFunctionEnvironmentNode extends RBaseNode {

        public abstract Object execute(RFunction fun);

        /**
         * Returns the environment that {@code func} was created in.
         */
        @Specialization
        protected Object getEnvironment(RFunction fun,
                        @Cached("createBinaryProfile()") ConditionProfile noEnvProfile,
                        @Cached("createBinaryProfile()") ConditionProfile createProfile) {
            Frame enclosing = fun.getEnclosingFrame();
            if (noEnvProfile.profile(enclosing == null)) {
                return RNull.instance;
            }
            REnvironment env = RArguments.getEnvironment(enclosing);
            if (createProfile.profile(env == null)) {
                return REnvironment.frameToEnvironment(enclosing.materialize());
            }
            return env;
        }

        public static GetFunctionEnvironmentNode create() {
            return GetFunctionEnvironmentNodeGen.create();
        }

        public static GetFunctionEnvironmentNode getUncached() {
            return GetFunctionEnvironmentNodeGen.getUncached();
        }
    }
}
