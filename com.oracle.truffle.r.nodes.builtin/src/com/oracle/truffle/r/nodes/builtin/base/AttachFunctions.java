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
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.REnvironment.DetachException;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * TODO The specialization signatures are weird owing to issues with named parameter handling.
 */
public class AttachFunctions {
    @RBuiltin(name = "attach", kind = INTERNAL, parameterNames = {"what", "pos", "name", "warn.conflicts"})
    public abstract static class Attach extends RInvisibleBuiltinNode {

        private static final String POS_WARNING = "*** 'pos=1' is not possible; setting 'pos=2' for now.\n" + "*** Note that 'pos=1' will give an error in the future";

        // TODO 3rd parameter "name" has default "deparse(substitute(what)"
        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(2), null, ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_FALSE)};
        }

        @Specialization
        public REnvironment doAttach(@SuppressWarnings("unused") RNull what, int pos, String name) {
            controlVisibility();
            REnvironment env = new REnvironment.NewEnv(name);
            doAttachEnv(pos, env);
            return env;
        }

        @Specialization
        public REnvironment doAttach(RNull what, double pos, RAbstractStringVector name) {
            return doAttach(what, (int) pos, name.getDataAt(0));
        }

        @Specialization
        public REnvironment doAttach(REnvironment what, String name, @SuppressWarnings("unused") String unused) {
            controlVisibility();
            return doAttachEnv(what, 2, name);
        }

        @Specialization
        public REnvironment doAttach(REnvironment what, int pos, String name) {
            controlVisibility();
            return doAttachEnv(what, pos, name);
        }

        @Specialization
        public REnvironment doAttach(REnvironment what, double pos, String name) {
            controlVisibility();
            return doAttachEnv(what, (int) pos, name);
        }

        REnvironment doAttachEnv(REnvironment what, int pos, String name) {
            REnvironment env = new REnvironment.NewEnv(name);
            RStringVector names = what.ls(true, null);
            for (int i = 0; i < names.getLength(); i++) {
                String key = names.getDataAt(i);
                Object value = what.get(key);
                // TODO copy?
                env.safePut(key, value);
            }
            doAttachEnv(pos, env);
            return env;

        }

        @Specialization
        public REnvironment doAttach(RList what, String name, @SuppressWarnings("unused") String unused) {
            controlVisibility();
            return doAttachList(what, 2, name);
        }

        @Specialization
        public REnvironment doAttach(RList what, int pos, String name) {
            controlVisibility();
            return doAttachList(what, pos, name);
        }

        @Specialization
        public REnvironment doAttach(RList what, double pos, String name) {
            controlVisibility();
            return doAttachList(what, (int) pos, name);
        }

        REnvironment doAttachList(RList what, int pos, String name) {
            REnvironment env = new REnvironment.NewEnv(name);
            RStringVector names = (RStringVector) what.getNames();
            for (int i = 0; i < names.getLength(); i++) {
                env.safePut(names.getDataAt(i), what.getDataAt(i));
            }
            doAttachEnv(pos, env);
            return env;
        }

        void doAttachEnv(int pos, REnvironment env) {
            // GnuR appears to allow any value of pos except 1.
            // Values < 1 are intepreted as 2
            int ipos = pos;
            if (ipos == 1) {
                RContext.getInstance().setEvalWarning(POS_WARNING);
                ipos = 2;
            }
            if (ipos < 1) {
                ipos = 2;
            }
            REnvironment.attach(ipos, env);
        }
    }

    @RBuiltin(name = "detach", kind = INTERNAL, parameterNames = {"name", "pos", "unload", "character.only", "force"})
    public abstract static class Detach extends RInvisibleBuiltinNode {

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(2), ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_FALSE),
                            ConstantNode.create(RRuntime.LOGICAL_FALSE)};
        }

        @SuppressWarnings("unused")
        @Specialization
        public Object doDetach1(VirtualFrame frame, int name, int pos, byte unload, byte characterOnly, byte force) {
            controlVisibility();
            return doDetach3(frame, name, unload == RRuntime.LOGICAL_TRUE, force == RRuntime.LOGICAL_TRUE);
        }

        @Specialization
        public Object doDetach(VirtualFrame frame, double name, int pos, byte unload, byte characterOnly, byte force) {
            return doDetach1(frame, (int) name, pos, unload, characterOnly, force);
        }

        @SuppressWarnings("unused")
        @Specialization
        public Object doDetach2(VirtualFrame frame, String name, int pos, byte unload, byte characterOnly, byte force) {
            controlVisibility();
            int ix = REnvironment.lookupIndexOnSearchPath(name);
            if (ix <= 0) {
                throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "name");
            }
            return doDetach3(frame, ix, unload == RRuntime.LOGICAL_TRUE, force == RRuntime.LOGICAL_TRUE);
        }

        REnvironment doDetach3(VirtualFrame frame, int pos, boolean unload, boolean force) {
            if (pos == 1) {
                throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "pos");
            }
            try {
                return REnvironment.detach(pos, unload, force);
            } catch (DetachException ex) {
                throw RError.error(frame, getEncapsulatingSourceSection(), ex);
            }
        }
    }
}
