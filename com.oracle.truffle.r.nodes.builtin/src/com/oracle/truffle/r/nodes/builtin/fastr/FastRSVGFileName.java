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
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.builtins.RBehavior;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import org.rosuda.javaGD.GDContainer;
import org.rosuda.javaGD.GDInterface;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RBuiltin(name = ".fastr.svg.filename", parameterNames = {}, visibility = RVisibility.OFF, kind = RBuiltinKind.INTERNAL, behavior = RBehavior.COMPLEX)
public abstract class FastRSVGFileName extends RBuiltinNode.Arg0 {
    /**
     * Returns filename (filename template) from the current SVG device.
     * It is expected that the current device is an SVG device and has created only 1 page.
     */
    @Specialization
    @TruffleBoundary
    public Object svgGetFileName() {
        JavaGDContext javaGDCtx = JavaGDContext.getContext(RContext.getInstance());
        GDInterface currGD = javaGDCtx.getGD(javaGDCtx.getCurrentGdId());
        if (currGD.getCreatedPagesCount() > 1) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "SVG device opened more than one page");
        }
        GDContainer currContainer = currGD.c;
        if (!(currContainer instanceof SVGImageContainer)) {
            throw RInternalError.shouldNotReachHere(".fastr.svg.filename() must be called when current device is an SVG device");
        }
        String fnameTemplate = ((SVGImageContainer) currContainer).getFileNameTemplate();
        String fname;
        if (fileNameTemplateSupportsMorePages(fnameTemplate)) {
            fname = String.format(fnameTemplate, 1);
        } else {
            fname = fnameTemplate;
        }
        return RDataFactory.createStringVectorFromScalar(fname);
    }

    private static boolean fileNameTemplateSupportsMorePages(String fileNameTemplate) {
        Pattern multiPagePattern = Pattern.compile(".*%\\d+d.*");
        Matcher matcher = multiPagePattern.matcher(fileNameTemplate);
        return matcher.find();
    }
}
