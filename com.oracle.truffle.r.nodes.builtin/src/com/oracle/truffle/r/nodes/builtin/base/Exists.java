/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.TypeFromModeNode;
import com.oracle.truffle.r.nodes.attributes.TypeFromModeNodeGen;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.env.REnvironment;

@RBuiltin(name = "exists", kind = INTERNAL, parameterNames = {"x", "envir", "mode", "inherits"}, behavior = PURE)
public abstract class Exists extends RBuiltinNode {

    @Child private TypeFromModeNode typeFromMode = TypeFromModeNodeGen.create();

    /**
     * Explicit execute declaration, because it is invoked by the fast-path version.
     */
    public abstract byte execute(String nameVec, REnvironment env, String mode, boolean inherits);

    @Override
    protected void createCasts(@SuppressWarnings("unused") CastBuilder casts) {
        casts.arg("x").mustBe(stringValue(), Message.INVALID_FIRST_ARGUMENT).asStringVector().findFirst();
        casts.arg("envir").mustBe(REnvironment.class);
        casts.arg("mode").mustBe(stringValue()).asStringVector().findFirst();
        casts.arg("inherits").mustBe(numericValue()).asLogicalVector().findFirst().map(toBoolean());
    }

    @Specialization
    @TruffleBoundary
    protected byte existsStringEnv(String name, REnvironment env, String mode, boolean inherits) {
        RType modeType = typeFromMode.execute(mode);
        if (!inherits) {
            Object obj = env.get(name);
            if (modeType != RType.Any && obj instanceof RPromise) {
                obj = PromiseHelperNode.evaluateSlowPath(null, (RPromise) obj);
            }
            return RRuntime.asLogical(obj != null && RRuntime.checkType(obj, modeType));
        }
        for (REnvironment e = env; e != REnvironment.emptyEnv(); e = e.getParent()) {
            Object obj = e.get(name);
            if (modeType != RType.Any && obj instanceof RPromise) {
                obj = PromiseHelperNode.evaluateSlowPath(null, (RPromise) obj);
            }
            if (obj != null && RRuntime.checkType(obj, modeType)) {
                return RRuntime.LOGICAL_TRUE;
            }
        }
        return RRuntime.LOGICAL_FALSE;
    }
}
