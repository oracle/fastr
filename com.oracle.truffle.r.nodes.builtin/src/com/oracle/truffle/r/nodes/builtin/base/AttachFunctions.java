/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.DetachException;

public class AttachFunctions {
    @RBuiltin(name = "attach", visibility = OFF, kind = INTERNAL, parameterNames = {"what", "pos", "name"}, behavior = COMPLEX)
    public abstract static class Attach extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("what").allowNull().mustBe(instanceOf(REnvironment.class).or(instanceOf(RAbstractListVector.class)), RError.Message.ATTACH_BAD_TYPE);
            casts.arg("pos").mustBe(numericValue(), Message.MUST_BE_INTEGER, "pos").asIntegerVector();
            casts.arg("name").mustBe(stringValue());
        }

        @Specialization
        @TruffleBoundary
        protected REnvironment doAttach(@SuppressWarnings("unused") RNull what, RAbstractIntVector pos, RAbstractStringVector name) {
            REnvironment env = RDataFactory.createNewEnv(name.getDataAt(0));
            doAttachEnv(pos.getDataAt(0), env);
            return env;
        }

        @Specialization
        @TruffleBoundary
        protected REnvironment doAttach(REnvironment what, RAbstractIntVector pos, RAbstractStringVector name) {
            REnvironment env = RDataFactory.createNewEnv(name.getDataAt(0));
            RStringVector names = what.ls(true, null, false);
            for (int i = 0; i < names.getLength(); i++) {
                String key = names.getDataAt(i);
                Object value = what.get(key);
                // TODO: copy/sharing?
                env.safePut(key, value);
            }
            doAttachEnv(pos.getDataAt(0), env);
            return env;
        }

        @Specialization
        @TruffleBoundary
        protected REnvironment doAttach(RAbstractListVector what, RAbstractIntVector pos, RAbstractStringVector name) {
            REnvironment env = RDataFactory.createNewEnv(name.getDataAt(0));
            RStringVector names = what.getNames();
            for (int i = 0; i < names.getLength(); i++) {
                // TODO: copy/sharing?
                env.safePut(names.getDataAt(i), what.getDataAt(i));
            }
            doAttachEnv(pos.getDataAt(0), env);
            return env;
        }

        private static void doAttachEnv(int pos, REnvironment env) {
            // GnuR appears to allow any value of pos except 1.
            // Values < 1 are interpreted as 2
            int ipos = pos;
            if (ipos < 1) {
                ipos = 2;
            }
            REnvironment.attach(ipos, env);
        }
    }

    @RBuiltin(name = "detach", visibility = OFF, kind = INTERNAL, parameterNames = {"pos"}, behavior = COMPLEX)
    public abstract static class Detach extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("pos").mustBe(numericValue(), Message.MUST_BE_INTEGER, "pos").asIntegerVector();
        }

        @Specialization
        @TruffleBoundary
        protected Object doDetach(RAbstractIntVector pos) {
            try {
                return REnvironment.detach(pos.getDataAt(0));
            } catch (DetachException ex) {
                throw RError.error(this, ex);
            }
        }
    }
}
