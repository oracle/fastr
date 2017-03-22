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

import com.oracle.truffle.r.library.fastrGrid.GridContext;
import com.oracle.truffle.r.library.fastrGrid.GridState;
import com.oracle.truffle.r.library.fastrGrid.graphics.RGridGraphicsAdapter;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RNull;

/**
 * Node that handles the {@code C_X11} external calls. Those calls may be initiated from either the
 * {@code X11} function or FastR specific {@code awt} function. In either case the result is that
 * the AWT window is opened and ready for drawing.
 */
public final class InitWindowedDevice extends RExternalBuiltinNode {
    static {
        Casts.noCasts(InitWindowedDevice.class);
    }

    @Override
    protected Object call(RArgsValuesAndNames args) {
        GridState gridState = GridContext.getContext().getGridState();
        if (!gridState.isDeviceInitialized()) {
            GridContext.getContext().getCurrentDevice().openNewPage();
            gridState.setDeviceInitialized();
        }
        RGridGraphicsAdapter.setCurrentDevice(args.getLength() == 0 ? "awt" : "X11cairo");
        return RNull.instance;
    }
}
