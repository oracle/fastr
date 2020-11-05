/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.fastrGrid;

import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.library.fastrGrid.device.awt.JFrameDevice;
import com.oracle.truffle.r.library.fastrGrid.device.remote.RemoteDevice;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.FastRConfig;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Contains code specific to FastR device that shows the graphical output interactively in a window.
 */
public final class WindowDevice {
    private WindowDevice() {
        // only static members
    }

    public static GridDevice createWindowDevice(boolean byGridServer, int width, int height) {
        return createWindowDevice(RContext.getInstance(), byGridServer, width, height);
    }

    public static GridDevice createWindowDevice(RContext ctx, boolean byGridServer, int width, int height) {
        if (FastRConfig.UseRemoteGridAwtDevice) {
            noSchedulingSupportWarning();
            return RemoteDevice.createWindowDevice(ctx, width, height);
        } else {
            JFrameDevice frameDevice = new JFrameDevice(width, height);
            if (!byGridServer && ctx != null && ctx.hasExecutor() && !FastRConfig.UseRemoteGridAwtDevice) {
                frameDevice.setResizeListener(() -> redrawAll(ctx));
                frameDevice.setCloseListener(() -> devOff(ctx));
            } else {
                if (!byGridServer) {
                    noSchedulingSupportWarning();
                }
            }
            return frameDevice;
        }
    }

    public static RError awtNotSupported() {
        throw RError.error(RError.NO_CALLER, Message.GENERIC, "AWT based grid devices are not supported.");
    }

    private static void redrawAll(RContext ctx) {
        if (!ctx.hasExecutor()) {
            // to be robust we re-check the executor availability
            return;
        }
        ctx.schedule(() -> {
            Object prev = ctx.getEnv().getContext().enter(null);
            GridContext.getContext(ctx).evalInternalRFunction("redrawAll");
            ctx.getEnv().getContext().leave(null, prev);
        });
    }

    private static void devOff(RContext ctx) {
        if (!ctx.hasExecutor()) {
            // to be robust we re-check the executor availability
            return;
        }
        ctx.schedule(() -> {
            Object prev = ctx.getEnv().getContext().enter(null);
            RFunction devOffFun = getDevOffFunction(ctx);
            if (devOffFun != null) {
                RContext.getEngine().evalFunction(devOffFun, REnvironment.baseEnv(ctx).getFrame(), RCaller.topLevel, true, null);
            } else {
                RError.warning(RError.NO_CALLER, Message.GENERIC, "Could not locate grDevices::dev.off to close the window device.");
            }
            ctx.getEnv().getContext().leave(null, prev);
        });
    }

    private static RFunction getDevOffFunction(RContext ctx) {
        Object grDevices = ctx.stateREnvironment.getNamespaceRegistry().get("grDevices");
        if (!(grDevices instanceof REnvironment)) {
            return null;
        }
        Object devOff = ((REnvironment) grDevices).get("dev.off");
        if (devOff instanceof RPromise) {
            devOff = PromiseHelperNode.evaluateSlowPath((RPromise) devOff);
        }
        return devOff instanceof RFunction ? (RFunction) devOff : null;
    }

    private static void noSchedulingSupportWarning() {
        // Note: the PolyglotEngine was not built with an Executor or we use remote grid device
        RError.warning(RError.NO_CALLER, Message.GENERIC, "Grid cannot resize the drawings. If you resize the window, the content will be lost. " +
                        "You can redraw the contents using: 'popViewport(0, recording = FALSE); grid:::draw.all()'.");
    }
}
