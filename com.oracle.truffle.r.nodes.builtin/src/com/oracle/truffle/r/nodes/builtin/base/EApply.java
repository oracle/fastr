/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.ExtractNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.EnvFunctions.EnvToList;
import com.oracle.truffle.r.nodes.builtin.base.Lapply.LapplyInternalNode;
import com.oracle.truffle.r.nodes.builtin.base.LapplyNodeGen.LapplyInternalNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RDataFactory.VectorFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

@RBuiltin(name = "eapply", kind = INTERNAL, parameterNames = {"env", "FUN", "all.names", "USE.NAMES"}, splitCaller = true, behavior = COMPLEX)
public abstract class EApply extends RBuiltinNode.Arg4 {

    @Child private LapplyInternalNode eapply = LapplyInternalNodeGen.create();

    static {
        Casts casts = new Casts(EApply.class);
        casts.arg("FUN").mustBe(instanceOf(RFunction.class), RError.Message.APPLY_NON_FUNCTION);
        casts.arg("env").mustNotBeNull(Message.USE_NULL_ENV_DEFUNCT).mustBe(REnvironment.class, Message.ARG_MUST_BE_ENV);
        casts.arg("all.names").asLogicalVector().findFirst().replaceNA(RRuntime.LOGICAL_FALSE).map(Predef.toBoolean());
        casts.arg("USE.NAMES").asLogicalVector().findFirst().replaceNA(RRuntime.LOGICAL_FALSE).map(Predef.toBoolean());
    }

    @Specialization
    protected Object lapply(VirtualFrame frame, REnvironment env, RFunction fun, boolean allNames, boolean useNames,
                    @Cached("create()") EnvToList envToList,
                    @Cached("create()") ExtractNamesAttributeNode extractNamesNode,
                    @Cached("create()") VectorFactory factory) {
        Object l = envToList.execute(frame, env, allNames, true);
        Object[] result = eapply.execute(frame, l, fun);

        RStringVector names = null;
        if (useNames) {
            names = result.length == 0 ? factory.createEmptyStringVector() : extractNamesNode.execute((RAttributable) l);
        }
        return factory.createList(result, names);
    }

}
