/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.nodes.builtin.fastr;

import java.awt.*;

import org.rosuda.javaGD.GDInterface;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.ffi.impl.javaGD.AWTGraphicsGD;
import com.oracle.truffle.r.ffi.impl.javaGD.JavaGDContext;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.builtins.RBehavior;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RNull;

/**
 * A fastr-specific builtin that is called within {@code grDevices:::awt}.
 */
@RBuiltin(name = ".fastr.awtSetGraphics", parameterNames = {"graphicsObject"}, visibility = RVisibility.OFF, kind = RBuiltinKind.INTERNAL, behavior = RBehavior.COMPLEX)
public abstract class FastRGDSetGraphics extends RBuiltinNode.Arg1 {
    static {
        Casts casts = new Casts(FastRGDSetGraphics.class);
        casts.arg("graphicsObject").mustNotBeNull();
    }

    @Specialization
    @TruffleBoundary
    public Object setGraphics(Object hostGraphicsObject) {
        RContext rContext = RContext.getInstance();
        assert rContext.getEnv().isHostObject(hostGraphicsObject);
        Object graphicsObject = rContext.getEnv().asHostObject(hostGraphicsObject);
        assert graphicsObject instanceof Graphics;
        Graphics graphics = (Graphics) graphicsObject;
        JavaGDContext javaGDContext = JavaGDContext.getContext(rContext);
        GDInterface currentGDInterface = javaGDContext.getGD(javaGDContext.getCurrentGdId());
        assert currentGDInterface instanceof AWTGraphicsGD;
        ((AWTGraphicsGD) currentGDInterface).setGraphics(graphics);
        return RNull.instance;
    }
}
