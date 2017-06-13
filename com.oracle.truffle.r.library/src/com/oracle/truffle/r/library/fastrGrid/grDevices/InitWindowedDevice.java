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

import static com.oracle.truffle.r.library.fastrGrid.WindowDevice.awtNotSupported;

import java.awt.Graphics2D;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.r.library.fastrGrid.GridContext;
import com.oracle.truffle.r.library.fastrGrid.WindowDevice;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.library.fastrGrid.device.awt.BufferedImageDevice;
import com.oracle.truffle.r.library.fastrGrid.device.awt.BufferedImageDevice.NotSupportedImageFormatException;
import com.oracle.truffle.r.library.fastrGrid.device.awt.Graphics2DDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.FastRConfig;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RTypedValue;

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
        if (!FastRConfig.InternalGridAwtSupport) {
            throw awtNotSupported();
        }

        int width = getIntOrDefault(args, 1, GridDevice.DEFAULT_WIDTH);
        int height = getIntOrDefault(args, 2, GridDevice.DEFAULT_HEIGHT);

        // if the first argument is a String, then it may describes the image format and filename to
        // use, the format is e.g. "jpeg::quality:filename"
        if (args.getLength() >= 1) {
            String name = RRuntime.asString(args.getArgument(0));
            if (!RRuntime.isNA(name) && name.contains("::")) {
                return openImageDevice(name, width, height);
            }
        }

        // otherwise the windowed device
        // check if we got custom Graphics2D object as 3rd parameter
        boolean isFastRDevice = args.getArgument(0).equals(".FASTR.AWT");
        if (isFastRDevice && args.getLength() > 3) {
            Object arg3 = args.getArgument(3);
            boolean isTruffleObj = arg3 instanceof TruffleObject && !(arg3 instanceof RTypedValue);
            if (isTruffleObj && JavaInterop.isJavaObject(Graphics2D.class, (TruffleObject) arg3)) {
                Graphics2D graphics = JavaInterop.asJavaObject(Graphics2D.class, (TruffleObject) arg3);
                Graphics2DDevice device = new Graphics2DDevice(graphics, width, height, false);
                GridContext.getContext().setCurrentDevice("awt", device);
                return RNull.instance;
            } else if (isTruffleObj) {
                warning(Message.GENERIC, "3rd argument is foreign object, but not of type Graphics2D.");
            }
        }

        // otherwise create the window ourselves
        GridDevice device = WindowDevice.createWindowDevice(width, height);
        String name = isFastRDevice ? "awt" : "X11cairo";
        GridContext.getContext().setCurrentDevice(name, device);
        return RNull.instance;
    }

    private Object openImageDevice(String name, int width, int height) {
        String formatName = name.substring(0, name.indexOf("::"));
        String filename = name.substring(name.lastIndexOf(':') + 1);
        try {
            BufferedImageDevice device = BufferedImageDevice.open(filename, formatName, width, height);
            GridContext.getContext().setCurrentDevice(formatName.toUpperCase(), device);
        } catch (NotSupportedImageFormatException e) {
            throw error(Message.GENERIC, String.format("Format '%s' is not supported.", formatName));
        }
        return RNull.instance;
    }

    private static int getIntOrDefault(RArgsValuesAndNames args, int index, int defaultValue) {
        if (index >= args.getLength()) {
            return defaultValue;
        }
        int value = RRuntime.asInteger(args.getArgument(index));
        if (RRuntime.isNA(value)) {
            return defaultValue;
        }
        return value;
    }
}
