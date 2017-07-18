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
import com.oracle.truffle.r.library.fastrGrid.PaletteExternals.CPalette;
import com.oracle.truffle.r.library.fastrGrid.PaletteExternals.CPalette2;
import com.oracle.truffle.r.library.fastrGrid.grDevices.DevCairo;
import com.oracle.truffle.r.library.fastrGrid.grDevices.DevCurr;
import com.oracle.truffle.r.library.fastrGrid.grDevices.DevHoldFlush;
import com.oracle.truffle.r.library.fastrGrid.grDevices.DevOff;
import com.oracle.truffle.r.library.fastrGrid.grDevices.DevSize;
import com.oracle.truffle.r.library.fastrGrid.grDevices.InitWindowedDevice;
import com.oracle.truffle.r.library.fastrGrid.grDevices.SavePlot;
import com.oracle.truffle.r.library.fastrGrid.graphics.CPar;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RInternalCodeBuiltinNode;
import com.oracle.truffle.r.runtime.RInternalCode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
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
            case "devsize":
                return new DevSize();
            case "devcur":
                return new DevCurr();
            case "devoff":
                return DevOff.create();
            case "PDF":
                return new IgnoredGridExternal(RNull.instance);
            case "devCairo":
                return new DevCairo();
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
            case "palette":
                return CPalette.create();
            case "palette2":
                return CPalette2.create();
            case "devAskNewPage":
                return new IgnoredGridExternal(RRuntime.LOGICAL_FALSE);
            default:
                return null;
        }
    }

    public static RExternalBuiltinNode lookupDotCall(String name) {
        switch (name) {
            case "gridDirty":
                return new LGridDirty();
            case "initGrid":
                return LInitGrid.create();
            case "newpage":
                return new LNewPage();
            case "convert":
                return LConvert.create();
            case "validUnits":
                return LValidUnit.create();
            case "pretty":
                return LPretty.create();
            case "stringMetric":
                return LStringMetric.create();

            // Viewport management
            case "upviewport":
                return LUpViewPort.create();
            case "initViewportStack":
                return new LInitViewPortStack();
            case "unsetviewport":
                return LUnsetViewPort.create();
            case "setviewport":
            case "downviewport":
            case "downvppath":
                return getExternalFastRGridBuiltinNode(name);

            // Drawing primitives
            case "rect":
                return LRect.create();
            case "lines":
                return LLines.create();
            case "polygon":
                return LPolygon.create();
            case "text":
                return LText.create();
            case "textBounds":
                return LTextBounds.create();
            case "segments":
                return LSegments.create();
            case "circle":
                return LCircle.create();
            case "points":
                return LPoints.create();
            case "raster":
                return LRaster.create();

            // Bounds primitive:
            case "rectBounds":
                return LRectBounds.create();
            case "locnBounds":
                return LLocnBounds.create();
            case "circleBounds":
                return LCircleBounds.create();

            // Simple grid state access
            case "getGPar":
                return new GridStateGetNode(GridState::getGpar);
            case "setGPar":
                return GridStateSetNode.create((state, val) -> state.setGpar((RList) val));
            case "getCurrentGrob":
                return new GridStateGetNode(GridState::getCurrentGrob);
            case "setCurrentGrob":
                return GridStateSetNode.create(GridState::setCurrentGrob);
            case "currentViewport":
                return new GridStateGetNode(GridState::getViewPort);
            case "initGPar":
                return new LInitGPar();

            // Display list stuff
            case "getDisplayList":
                return new GridStateGetNode(GridState::getDisplayList);
            case "setDisplayList":
                return GridStateSetNode.create((state, val) -> state.setDisplayList((RList) val));
            case "getDLindex":
                return new GridStateGetNode(GridState::getDisplayListIndex);
            case "setDLindex":
                return GridStateSetNode.create((state, val) -> state.setDisplayListIndex(RRuntime.asInteger(val)));
            case "setDLelt":
                return GridStateSetNode.create(GridState::setDisplayListElement);
            case "getDLelt":
                return LGetDisplayListElement.create();
            case "setDLon":
                return LSetDisplayListOn.create();
            case "getDLon":
                return new GridStateGetNode(state -> RRuntime.asLogical(state.isDisplayListOn()));
            case "getEngineDLon":
                return new IgnoredGridExternal(RRuntime.LOGICAL_FALSE);
            case "initDisplayList":
                return new LInitDisplayList();
            case "newpagerecording":
                return new IgnoredGridExternal(RNull.instance);

            default:
                return null;
        }
    }

    private static RExternalBuiltinNode getExternalFastRGridBuiltinNode(String name) {
        return new RInternalCodeBuiltinNode("grid", RInternalCode.loadSourceRelativeTo(LInitGrid.class, "fastrGrid.R"), name);
    }
}
