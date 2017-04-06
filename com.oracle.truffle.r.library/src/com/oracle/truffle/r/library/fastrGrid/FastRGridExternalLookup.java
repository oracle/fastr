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

import com.oracle.truffle.r.library.fastrGrid.DisplayList.LGetDisplayListElement;
import com.oracle.truffle.r.library.fastrGrid.DisplayList.LInitDisplayList;
import com.oracle.truffle.r.library.fastrGrid.DisplayList.LSetDisplayListOn;
import com.oracle.truffle.r.library.fastrGrid.grDevices.DevCurr;
import com.oracle.truffle.r.library.fastrGrid.grDevices.DevHoldFlush;
import com.oracle.truffle.r.library.fastrGrid.grDevices.DevOff;
import com.oracle.truffle.r.library.fastrGrid.grDevices.InitWindowedDevice;
import com.oracle.truffle.r.library.fastrGrid.grDevices.SavePlot;
import com.oracle.truffle.r.library.fastrGrid.graphics.CPar;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RInternalCodeBuiltinNode;
import com.oracle.truffle.r.runtime.RInternalCode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;

/**
 * Implements the lookup for externals replaced by the FastR grid package.
 */
public final class FastRGridExternalLookup {

    private FastRGridExternalLookup() {
        // only static members
    }

    public static RExternalBuiltinNode lookupDotExternal(String name) {
        switch (name) {
            case "devholdflush":
                return DevHoldFlush.create();
            case "devcur":
                return new DevCurr();
            case "devoff":
                return DevOff.create();
            case "PDF":
                return new IgnoredGridExternal(RNull.instance);
            default:
                return null;
        }
    }

    public static RExternalBuiltinNode lookupDotExternal2(String name) {
        switch (name) {
            case "C_par":
                return new CPar();
            case "savePlot":
                return SavePlot.create();
            case "X11":
                return new InitWindowedDevice();
            default:
                return null;
        }
    }

    public static RExternalBuiltinNode lookupDotCall(String name) {
        switch (name) {
            case "L_gridDirty":
                return new LGridDirty();
            case "L_initGrid":
                return LInitGrid.create();
            case "L_newpage":
                return new LNewPage();
            case "L_convert":
                return LConvert.create();
            case "L_validUnits":
                return LValidUnit.create();
            case "L_pretty":
                return LPretty.create();

            // Viewport management
            case "L_upviewport":
                return LUpViewPort.create();
            case "L_initViewportStack":
                return new LInitViewPortStack();
            case "L_unsetviewport":
                return LUnsetViewPort.create();
            case "L_setviewport":
            case "L_downviewport":
            case "L_downvppath":
                return getExternalFastRGridBuiltinNode(name);

            // Drawing primitives
            case "L_rect":
                return LRect.create();
            case "L_lines":
                return LLines.create();
            case "L_polygon":
                return LPolygon.create();
            case "L_text":
                return LText.create();
            case "L_textBounds":
                return LTextBounds.create();
            case "L_segments":
                return LSegments.create();
            case "L_circle":
                return LCircle.create();
            case "L_points":
                return LPoints.create();

            // Bounds primitive:
            case "L_rectBounds":
                return LRectBounds.create();
            case "L_locnBounds":
                return LLocnBounds.create();
            case "L_circleBounds":
                return LCircleBounds.create();

            // Simple grid state access
            case "L_getGPar":
                return new GridStateGetNode(GridState::getGpar);
            case "L_setGPar":
                return GridStateSetNode.create((state, val) -> state.setGpar((RList) val));
            case "L_getCurrentGrob":
                return new GridStateGetNode(GridState::getCurrentGrob);
            case "L_setCurrentGrob":
                return GridStateSetNode.create(GridState::setCurrentGrob);
            case "L_currentViewport":
                return new GridStateGetNode(GridState::getViewPort);
            case "L_initGPar":
                return new LInitGPar();

            // Display list stuff
            case "L_getDisplayList":
                return new GridStateGetNode(GridState::getDisplayList);
            case "L_setDisplayList":
                return GridStateSetNode.create((state, val) -> state.setDisplayList((RList) val));
            case "L_getDLindex":
                return new GridStateGetNode(GridState::getDisplayListIndex);
            case "L_setDLindex":
                return GridStateSetNode.create((state, val) -> state.setDisplayListIndex(RRuntime.asInteger(val)));
            case "L_setDLelt":
                return GridStateSetNode.create(GridState::setDisplayListElement);
            case "L_getDLelt":
                return LGetDisplayListElement.create();
            case "L_setDLon":
                return LSetDisplayListOn.create();
            case "L_getDLon":
                return new GridStateGetNode(state -> RRuntime.asLogical(state.isDisplayListOn()));
            case "L_getEngineDLon":
                return new IgnoredGridExternal(RRuntime.LOGICAL_FALSE);
            case "L_initDisplayList":
                return new LInitDisplayList();
            case "L_newpagerecording":
                return new IgnoredGridExternal(RNull.instance);

            default:
                if (name.startsWith("L_")) {
                    throw RInternalError.shouldNotReachHere("Unimplemented grid external " + name);
                } else {
                    return null;
                }
        }
    }

    private static RExternalBuiltinNode getExternalFastRGridBuiltinNode(String name) {
        return new RInternalCodeBuiltinNode(RContext.getInstance(), "grid", RInternalCode.loadSourceRelativeTo(LInitGrid.class, "fastrGrid.R"), name);
    }
}
