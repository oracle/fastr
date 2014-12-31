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
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.*;

public class AttachFunctions {
    @RBuiltin(name = "attach", kind = INTERNAL, parameterNames = {"what", "pos", "name"})
    public abstract static class Attach extends RInvisibleBuiltinNode {

        @CreateCast("arguments")
        public RNode[] castArguments(RNode[] arguments) {
            arguments[1] = CastIntegerNodeGen.create(arguments[1], false, false, false);
            return arguments;
        }

        @Specialization
        protected REnvironment doAttach(@SuppressWarnings("unused") RNull what, RAbstractIntVector pos, RAbstractStringVector name) {
            controlVisibility();
            REnvironment env = RDataFactory.createNewEnv(name.getDataAt(0));
            doAttachEnv(pos.getDataAt(0), env);
            return env;
        }

        @Specialization
        protected REnvironment doAttach(REnvironment what, RAbstractIntVector pos, RAbstractStringVector name) {
            controlVisibility();
            REnvironment env = RDataFactory.createNewEnv(name.getDataAt(0));
            RStringVector names = what.ls(true, null);
            for (int i = 0; i < names.getLength(); i++) {
                String key = names.getDataAt(i);
                Object value = what.get(key);
                // TODO copy?
                env.safePut(key, value);
            }
            doAttachEnv(pos.getDataAt(0), env);
            return env;
        }

        @Specialization
        protected REnvironment doAttach(RList what, RAbstractIntVector pos, RAbstractStringVector name) {
            controlVisibility();
            REnvironment env = RDataFactory.createNewEnv(name.getDataAt(0));
            RStringVector names = (RStringVector) what.getNames();
            for (int i = 0; i < names.getLength(); i++) {
                env.safePut(names.getDataAt(i), what.getDataAt(i));
            }
            doAttachEnv(pos.getDataAt(0), env);
            return env;
        }

        @SuppressWarnings("unused")
        @Fallback
        protected REnvironment doAttach(Object what, Object pos, Object name) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.ATTACH_BAD_TYPE);
        }

        protected void doAttachEnv(int pos, REnvironment env) {
            // GnuR appears to allow any value of pos except 1.
            // Values < 1 are interpreted as 2
            int ipos = pos;
            if (ipos < 1) {
                ipos = 2;
            }
            REnvironment.attach(ipos, env);
        }
    }

    @RBuiltin(name = "detach", kind = INTERNAL, parameterNames = {"pos"})
    public abstract static class Detach extends RInvisibleBuiltinNode {

        @CreateCast("arguments")
        public RNode[] castArguments(RNode[] arguments) {
            arguments[0] = CastIntegerNodeGen.create(arguments[0], false, false, false);
            return arguments;
        }

        @Specialization
        protected Object doDetach(RAbstractIntVector pos) {
            controlVisibility();
            try {
                return REnvironment.detach(pos.getDataAt(0));
            } catch (DetachException ex) {
                throw RError.error(getEncapsulatingSourceSection(), ex);
            }
        }
    }
}
