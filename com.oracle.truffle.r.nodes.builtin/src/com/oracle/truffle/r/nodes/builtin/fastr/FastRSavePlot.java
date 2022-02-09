/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.ffi.impl.javaGD.BufferedImageContainer;
import com.oracle.truffle.r.ffi.impl.javaGD.JavaGDContext;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RNull;
import org.rosuda.javaGD.GDContainer;
import org.rosuda.javaGD.GDInterface;
import org.rosuda.javaGD.GDObject;

import java.awt.*;
import java.io.IOException;
import java.util.Collection;

import static com.oracle.truffle.r.ffi.impl.javaGD.FileOutputContainer.NotSupportedImageFormatException;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

/**
 * A FastR-specific `savePlot` builtin function, defined in `grDevices` package. We expect that this
 * node is instantiated iff UseInternalGridGraphics option is false, therefore, JavaGD package is
 * used instead of internal fastr-grid.
 *
 * We do not support other device than `dev.cur()`, because it is currently not possible to map
 * grid-specific device IDs (where `dev.cur()` is a grid-specific ID) to JavaGD-specific IDs.
 * grid-specific IDs are tracked in {@code devices.c:R_CurrentDevice} and JavaGD-specific IDs are
 * tracked in {@code jGDtalk.c:javaGDDeviceController}.
 */
@RBuiltin(name = ".fastr.savePlot", parameterNames = {"filename", "type", "device"}, visibility = OFF, kind = INTERNAL, behavior = COMPLEX)
public abstract class FastRSavePlot extends RBuiltinNode.Arg3 {
    static {
        Casts casts = new Casts(FastRSavePlot.class);
        casts.arg("filename").asStringVector().findFirst();
        casts.arg("type").asStringVector().findFirst();
        casts.arg("device").asIntegerVector().findFirst();
    }

    @Specialization
    @CompilerDirectives.TruffleBoundary
    protected Object doSavePlot(String filePath, String fileType, @SuppressWarnings("unused") int deviceId) {
        RContext ctx = RContext.getInstance();
        if (!(ctx.gridContext instanceof JavaGDContext)) {
            throw RInternalError.shouldNotReachHere(FastRSavePlot.class.getSimpleName() + " should be used only with JavaGD package, not with internal grid graphics");
        }
        JavaGDContext javaGDContext = (JavaGDContext) ctx.gridContext;
        int gdId = javaGDContext.getCurrentGdId();

        GDInterface device = javaGDContext.getGD(gdId);
        GDContainer deviceContainer = device.c;
        Dimension deviceDimension = deviceContainer.getSize();
        BufferedImageContainer imageOutputContainer = null;
        try {
            imageOutputContainer = new BufferedImageContainer((int) deviceDimension.getWidth(), (int) deviceDimension.getHeight(), fileType, filePath, 70);
        } catch (NotSupportedImageFormatException e) {
            throw error(RError.Message.GENERIC, "Unsupported image format: " + fileType);
        }
        // imageOutputContainer is now empty, we have to copy all the GDObjects from the `device`
        // into `imageOutputContainer`.
        Collection<GDObject> gdObjects = deviceContainer.getGDObjects();
        for (GDObject gdObject : gdObjects) {
            imageOutputContainer.add(gdObject);
        }
        TruffleFile truffleFile = ctx.getSafeTruffleFile(filePath);
        try {
            imageOutputContainer.saveImage(truffleFile);
        } catch (IOException e) {
            throw error(RError.Message.GENERIC, "Cannot write to file " + truffleFile.getPath());
        }
        return RNull.instance;
    }
}
