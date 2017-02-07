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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RList2EnvNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * The {@code expr} parameter is supposed to be R graphics code, e.g. {@code rect(4,2,...);}, which
 * is supposed to be run and recorded (probably in order to be re-run as is if the device size is
 * changed). The visible behavior is that a e.g. rectangle created via {@code recordGraphics}
 * maintains its size regardless of resizes of the device (e.g. window).
 *
 * TODO: the current implementation is a stub that only runs the command, but does not fiddle with
 * the recording like GnuR does.
 */
@RBuiltin(name = "recordGraphics", kind = INTERNAL, parameterNames = {"expr", "list", "env"}, behavior = COMPLEX)
public abstract class RecordGraphics extends RBuiltinNode {
    @Child private SetVisibilityNode visibility = SetVisibilityNode.create();
    @Child private RList2EnvNode list2EnvNode = new RList2EnvNode();

    static {
        Casts casts = new Casts(RecordGraphics.class);
        casts.arg("expr").mustBe(instanceOf(RLanguage.class).or(instanceOf(RExpression.class)));
        casts.arg("list").mustBe(instanceOf(RList.class));
        casts.arg("env").mustBe(instanceOf(REnvironment.class));
    }

    public static RecordGraphics create() {
        return RecordGraphicsNodeGen.create();
    }

    @Specialization
    protected Object doEval(VirtualFrame frame, RLanguage expr, RList list, REnvironment env) {
        RCaller rCaller = RCaller.create(frame, getOriginalCall());
        try {
            return RContext.getEngine().eval(expr, createEnv(list, env), rCaller);
        } finally {
            visibility.executeAfterCall(frame, rCaller);
        }
    }

    @Specialization
    protected Object doEval(VirtualFrame frame, RExpression expr, RList list, REnvironment env) {
        RCaller rCaller = RCaller.create(frame, getOriginalCall());
        try {
            return RContext.getEngine().eval(expr, createEnv(list, env), rCaller);
        } finally {
            visibility.executeAfterCall(frame, rCaller);
        }
    }

    private REnvironment createEnv(RList list, REnvironment parent) {
        REnvironment newEnv = RDataFactory.createNewEnv("<recordGraphics env>", true, list.getLength());
        newEnv.setParent(parent);
        return list2EnvNode.execute(list, newEnv);
    }
}
