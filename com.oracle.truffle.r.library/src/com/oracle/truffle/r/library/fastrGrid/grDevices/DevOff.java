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
package com.oracle.truffle.r.library.fastrGrid.grDevices;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.library.fastrGrid.GridContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice.DeviceCloseException;
import com.oracle.truffle.r.library.fastrGrid.device.SVGDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RNull;

public abstract class DevOff extends RExternalBuiltinNode.Arg1 {
    static {
        Casts casts = new Casts(DevOff.class);
        casts.arg(0).asIntegerVector().findFirst();
    }

    public static DevOff create() {
        return DevOffNodeGen.create();
    }

    @Specialization
    @TruffleBoundary
    public Object devOff(int whichR) {
        GridContext ctx = GridContext.getContext();
        int which = Math.abs(whichR) - 1; // convert to Java index
        if (which < 0 || which >= ctx.getDevicesSize()) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "Wrong device number.");
        }

        // FastR specific special handling for SVG device, when the index is negative, return the
        // SVG code
        if (whichR < 0) {
            return closeSvgDevice(ctx, which);
        }

        try {
            ctx.closeDevice(which);
        } catch (DeviceCloseException e) {
            throw error(Message.GENERIC, "Cannot close the device. Details: " + e.getMessage());
        }
        return RNull.instance;
    }

    private String closeSvgDevice(GridContext ctx, int which) {
        GridDevice dev = ctx.getDevice(which);
        ctx.removeDevice(which);
        if ((dev instanceof SVGDevice)) {
            return ((SVGDevice) dev).getContents();
        } else {
            warning(Message.GENERIC, "The device was not SVG device.");
            return "";
        }
    }
}
