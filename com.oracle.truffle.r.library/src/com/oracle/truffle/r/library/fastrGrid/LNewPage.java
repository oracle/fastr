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
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RNull;

final class LNewPage extends RExternalBuiltinNode {
    static {
        Casts.noCasts(LNewPage.class);
    }

    @Child private LGridDirty gridDirty = new LGridDirty();

    @Override
    public Object call(VirtualFrame frame, RArgsValuesAndNames args) {
        GridDevice device = GridContext.getContext().getCurrentDevice();
        if (GridContext.getContext().getGridState().isDeviceInitialized()) {
            device.openNewPage();
            return RNull.instance;
        }
        // There are some exceptions to the rule that any external call from grid R code is
        // preceeded by L_gridDirty call, L_newpage is one of them.
        CompilerDirectives.transferToInterpreter();
        return gridDirty.call(frame, RArgsValuesAndNames.EMPTY);
    }

    @Override
    protected Object call(RArgsValuesAndNames args) {
        assert false : "should be shadowed by the overload with frame";
        return null;
    }
}
