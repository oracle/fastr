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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.ffi.impl.javaGD.JavaGDContext;
import com.oracle.truffle.r.ffi.impl.javaGD.SVGImageContainer;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode.Arg1;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.builtins.RBehavior;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RNull;
import org.rosuda.javaGD.GDContainer;
import org.rosuda.javaGD.GDInterface;

/**
 * Builtin function for setting {@code fileNameTemplate} for the current SVG device.
 */
@RBuiltin(name = ".fastr.svg.set.filename", parameterNames = {"fname"}, visibility = RVisibility.OFF, kind = RBuiltinKind.INTERNAL, behavior = RBehavior.COMPLEX)
public abstract class FastRSVGSetFileName extends Arg1 {
    static {
        Casts casts = new Casts(FastRSVGSetFileName.class);
        casts.arg("fname").asStringVector().findFirst();
    }

    @Specialization
    @TruffleBoundary
    public Object svgSetFname(String fname) {
        JavaGDContext javaGDContext = JavaGDContext.getContext(RContext.getInstance());
        GDInterface currGD = javaGDContext.getGD(javaGDContext.getCurrentGdId());
        GDContainer currContainer = currGD.c;
        if (!(currContainer instanceof SVGImageContainer)) {
            throw RInternalError.shouldNotReachHere(".fastr.svg.get.content() must be called when current device is an SVG device");
        }
        ((SVGImageContainer) currContainer).setFileNameTemplate(fname);
        return RNull.instance;
    }
}
