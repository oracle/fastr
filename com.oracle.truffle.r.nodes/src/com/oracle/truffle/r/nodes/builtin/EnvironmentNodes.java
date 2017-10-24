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
package com.oracle.truffle.r.nodes.builtin;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.profiles.ConditionProfile;
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
        protected REnvironment doEmptyList(@SuppressWarnings("unused") RAbstractListVector list, REnvironment target, String envName, REnvironment parentEnv) {
            REnvironment createNewEnv;
            if (target == null) {
                createNewEnv = RDataFactory.createNewEnv(envName);
                RArguments.initializeEnclosingFrame(createNewEnv.getFrame(), parentEnv.getFrame());
                createNewEnv.setParent(parentEnv);
            } else {
                createNewEnv = target;
            }
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
                boolean hasEnclosingFD = !FrameSlotChangeMonitor.isEnclosingFrameDescriptor(cachedFd,
                                parentEnv.getFrame());
                if (hasEnclosingFD) {
                    cachedFd = cachedFd.copy();
                }

                result = RDataFactory.createNewEnv(cachedFd, envName);
                if (hasEnclosingFD) {
                    FrameSlotChangeMonitor.initializeNonFunctionFrameDescriptor(result.getName(), result.getFrame());
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
            for (int i = list.getLength() - 1; i >= 0; i--) {
                String name = names.getDataAt(i);
                if (!ignoreMissingNames && name.length() == 0) {
                    throw error(RError.Message.ZERO_LENGTH_VARIABLE);
                }
                // in case of duplicates, last element in list wins
                if (result.get(name) == null) {
                    result.safePut(name, UpdateShareableChildValue.update(list, list.getDataAt(i)));
                }
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

    public static final class GetFunctionEnvironmentNode extends RBaseNode {
        private final ConditionProfile noEnvProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile createProfile = ConditionProfile.createBinaryProfile();

        /**
         * Returns the environment that {@code func} was created in.
         */
        public Object getEnvironment(RFunction fun) {
            Frame enclosing = fun.getEnclosingFrame();
            if (noEnvProfile.profile(enclosing == null)) {
                return RNull.instance;
            }
            REnvironment env = RArguments.getEnvironment(enclosing);
            if (createProfile.profile(env == null)) {
                return REnvironment.createEnclosingEnvironments(enclosing.materialize());
            }
            return env;
        }

        public static GetFunctionEnvironmentNode create() {
            return new GetFunctionEnvironmentNode();
        }
    }
}
