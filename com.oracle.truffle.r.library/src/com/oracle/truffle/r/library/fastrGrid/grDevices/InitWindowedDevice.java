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
import com.oracle.truffle.r.library.fastrGrid.GridContext;
import com.oracle.truffle.r.library.fastrGrid.device.awt.BufferedImageDevice;
import com.oracle.truffle.r.library.fastrGrid.device.awt.BufferedImageDevice.NotSupportedImageFormatException;
import com.oracle.truffle.r.library.fastrGrid.device.awt.BufferedJFrameDevice;
import com.oracle.truffle.r.library.fastrGrid.device.awt.JFrameDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RNull;

/**
 * Node that handles the {@code C_X11} external calls. Those calls may be initiated from either the
 * {@code X11}, {@code jpeg}, {@code bmp}, {@code png} functions and from FastR specific {@code awt}
 * . The arguments determine which device should be opened.
 */
public final class InitWindowedDevice extends RExternalBuiltinNode {
    static {
        Casts.noCasts(InitWindowedDevice.class);
    }

    @Override
    @TruffleBoundary
    protected Object call(RArgsValuesAndNames args) {
        // if the first argument is a String, then it may describes the image format and filename to
        // use, the format is e.g. "jpeg::quality:filename"
        if (args.getLength() >= 1) {
            String name = RRuntime.asString(args.getArgument(0));
            if (!RRuntime.isNA(name) && name.contains("::")) {
                return openImageDevice(name);
            }
        }
        // otherwise the
        GridContext.getContext().setCurrentDevice(args.getLength() == 0 ? "awt" : "X11cairo", new BufferedJFrameDevice(JFrameDevice.create()));
        return RNull.instance;
    }

    private Object openImageDevice(String name) {
        String formatName = name.substring(0, name.indexOf("::"));
        String filename = name.substring(name.lastIndexOf(':') + 1);
        try {
            BufferedImageDevice device = BufferedImageDevice.open(filename, formatName, 700, 700);
            GridContext.getContext().setCurrentDevice(formatName, device);
        } catch (NotSupportedImageFormatException e) {
            throw error(Message.GENERIC, String.format("Format '%s' is not supported.", formatName));
        }
        return RNull.instance;
    }
}
