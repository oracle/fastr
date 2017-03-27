/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 2001-3 Paul Murrell
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.fastrGrid;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.library.fastrGrid.ViewPort.InitViewPortNode;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RNull;

final class LGridDirty extends RExternalBuiltinNode {
    @Child private InitViewPortNode initViewPort = new InitViewPortNode();

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
        CompilerDirectives.transferToInterpreter();

        // if no device has been opened yet, open the default one and make it current
        if (GridContext.getContext().getCurrentDevice() == null) {
            GridContext.getContext().openDefaultDevice();
        }

        // the current device has not been initialized yet...
        GridDevice device = GridContext.getContext().getCurrentDevice();
        device.openNewPage();
        gridState.setViewPort(initViewPort.execute(frame));
        gridState.setDeviceInitialized();
        if (gridState.getGpar() == null) {
            gridState.initGPar(device);
        }
        if (gridState.getViewPort() == null) {
            gridState.setViewPort(initViewPort.execute(frame));
        }

        return RNull.instance;
    }

    @Override
    protected Object call(RArgsValuesAndNames args) {
        // shadowed by the VirtualFrame overload
        return null;
    }
}
