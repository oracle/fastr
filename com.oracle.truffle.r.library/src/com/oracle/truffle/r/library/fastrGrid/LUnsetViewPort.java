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

import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asList;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asListOrNull;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;

public abstract class LUnsetViewPort extends RExternalBuiltinNode.Arg1 {
    static {
        Casts casts = new Casts(LUnsetViewPort.class);
        casts.arg(0).mustBe(numericValue()).asIntegerVector().findFirst();
    }

    public static LUnsetViewPort create() {
        return LUnsetViewPortNodeGen.create();
    }

    @Specialization
    @TruffleBoundary
    Object unsetViewPort(int n) {
        GridContext ctx = GridContext.getContext();
        GridState gridState = ctx.getGridState();

        // go n-steps up the view-port tree
        RList gvp = gridState.getViewPort();
        RList newVp = gvp;
        for (int i = 0; i < n; i++) {
            gvp = newVp;
            newVp = asListOrNull(gvp.getDataAt(ViewPort.PVP_PARENT));
            if (newVp == null) {
                throw error(Message.GENERIC, "cannot pop the top-level viewport ('grid' and 'graphics' output mixed?)");
            }
        }

        // gvp will be removed, newVp will be the new view-port
        // first update children of newVp -> remove gvp
        REnvironment children = (REnvironment) newVp.getDataAt(ViewPort.PVP_CHILDREN);
        String gvpName = RRuntime.asString(gvp.getDataAt(ViewPort.VP_NAME));
        safeRemoveFromEnv(children, gvpName);

        // update newVp transform etc. because it will be the current vp, it has to be up to date
        GridDevice device = ctx.getCurrentDevice();
        if (ViewPort.updateDeviceSizeInVP(newVp, device)) {
            // Note: like in other places calling this, why incremental == true, given that the
            // device has changed? Don't we want to recalculate the whole tree?
            DoSetViewPort.calcViewportTransform(newVp, newVp.getDataAt(ViewPort.PVP_PARENT), true, device, GridState.getInitialGPar(device));
        }

        gridState.setGpar(asList(newVp.getDataAt(ViewPort.PVP_GPAR)));

        // TODO: clipping
        gridState.setViewPort(newVp);

        // remove the parent link from the old viewport
        gvp.setDataAt(ViewPort.PVP_PARENT, RNull.instance);
        return RNull.instance;
    }

    private static void safeRemoveFromEnv(REnvironment children, String gvpName) {
        try {
            children.rm(gvpName);
        } catch (PutException e) {
            throw RInternalError.shouldNotReachHere("Cannot update view-port children environment");
        }
    }
}
