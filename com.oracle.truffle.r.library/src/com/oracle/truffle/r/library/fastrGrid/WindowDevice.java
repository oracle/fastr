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
package com.oracle.truffle.r.library.fastrGrid;

import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.library.fastrGrid.device.awt.JFrameDevice;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.context.RContext;

/**
 * Contains code specific to FastR device that shows the graphical output interactively in a window.
 */
public final class WindowDevice {
    private WindowDevice() {
        // only static members
    }

    public static GridDevice createWindowDevice() {
        return createWindowDevice(GridDevice.DEFAULT_WIDTH, GridDevice.DEFAULT_HEIGHT);
    }

    public static GridDevice createWindowDevice(int width, int height) {
        JFrameDevice frameDevice = new JFrameDevice(width, height);
        if (RContext.getInstance().hasExecutor()) {
            frameDevice.setResizeListener(WindowDevice::redrawAll);
        } else {
            noSchedulingSupportWarning();
        }
        return frameDevice;
    }

    public static RError awtNotSupported() {
        throw RError.error(RError.NO_CALLER, Message.GENERIC, "AWT based grid devices are not supported.");
    }

    private static void redrawAll() {
        RContext ctx = RContext.getInstance();
        if (ctx.hasExecutor()) {
            // to be robust we re-check the executor availability
            ctx.schedule(() -> {
                GridContext.getContext().evalInternalRFunction("redrawAll");
            });
        }
    }

    private static void noSchedulingSupportWarning() {
        // Note: the PolyglotEngine was not built with an Executor
        RError.warning(RError.NO_CALLER, Message.GENERIC, "Grid cannot resize the drawings. If you resize the window, the content will be lost.");
    }
}
