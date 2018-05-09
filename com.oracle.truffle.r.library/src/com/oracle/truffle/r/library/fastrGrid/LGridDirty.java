/*
 * Copyright (C) 2001-3 Paul Murrell
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.library.fastrGrid;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.library.fastrGrid.ViewPort.InitViewPortNode;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.library.fastrGrid.grDevices.OpenDefaultDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RNull;

/**
 * {@code L_gridDirty} is external that gets called before any grid operation, this is ensured by
 * routing all external calls at R level through {@code grid.Call} function, which first invokes
 * {@code L_gridDirty} before invoking the requested external. LGridDirty is responsible for delayed
 * initialization of the grid state and device specific grid state if the device has changed since
 * the last time.
 */
public final class LGridDirty extends RExternalBuiltinNode {
    @Child private InitViewPortNode initViewPort = new InitViewPortNode();
    @Child private OpenDefaultDevice openDefaultDevice = new OpenDefaultDevice();

    static {
        Casts.noCasts(LGridDirty.class);
    }

    @Override
    public Object call(VirtualFrame frame, RArgsValuesAndNames args) {
        GridState gridState = GridContext.getContext().getGridState();
        if (gridState.isDeviceInitialized()) {
            return RNull.instance;
        }

        // the rest only takes place if the device has been changed since the last time
        openDefaultDevice.execute();

        CompilerDirectives.transferToInterpreter();
        // the current device has not been initialized yet...
        gridState = GridContext.getContext().getGridState();
        GridDevice device = GridContext.getContext().getCurrentDevice();
        device.openNewPage();
        gridState.initGPar(device);
        gridState.setViewPort(initViewPort.execute(frame));
        DisplayList.initDisplayList(gridState);
        gridState.setDeviceInitialized();
        return RNull.instance;
    }

    @Override
    protected Object call(RArgsValuesAndNames args) {
        assert false : "should be shadowed by the overload with frame";
        return null;
    }
}
