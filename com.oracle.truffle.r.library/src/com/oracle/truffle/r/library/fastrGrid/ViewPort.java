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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.call.RExplicitCallNode;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;

/**
 * There is a notion of a view point, which is an ordinary list that user creates to define a view
 * port. One such list is pushed using {@code pushViewpoint} it is transformed to a 'pushed
 * viewpoint', which is a copy of the original view point and it has some additional attributes.
 */
class ViewPort {
    /*
     * Structure of a viewport
     */
    public static final int VP_X = 0;
    public static final int VP_Y = 1;
    public static final int VP_WIDTH = 2;
    public static final int VP_HEIGHT = 3;
    public static final int VP_JUST = 4;
    public static final int VP_GP = 5;
    public static final int VP_CLIP = 6;
    public static final int VP_XSCALE = 7;
    public static final int VP_YSCALE = 8;
    public static final int VP_ANGLE = 9;
    public static final int VP_LAYOUT = 10;
    public static final int VP_LPOSROW = 11;
    public static final int VP_LPOSCOL = 12;
    public static final int VP_VALIDJUST = 13;
    public static final int VP_VALIDLPOSROW = 14;
    public static final int VP_VALIDLPOSCOL = 15;
    public static final int VP_NAME = 16;
    /*
     * Additional structure of a pushedvp
     */
    public static final int PVP_PARENTGPAR = 17;
    private static final int PVP_GPAR = 18;
    public static final int PVP_TRANS = 19;
    public static final int PVP_WIDTHS = 20;
    public static final int PVP_HEIGHTS = 21;
    public static final int PVP_WIDTHCM = 22;
    public static final int PVP_HEIGHTCM = 23;
    public static final int PVP_ROTATION = 24;
    public static final int PVP_CLIPRECT = 25;
    public static final int PVP_PARENT = 26;
    public static final int PVP_CHILDREN = 27;
    public static final int PVP_DEVWIDTHCM = 28;
    public static final int PVP_DEVHEIGHTCM = 29;

    public static final class InitViewPortNode extends Node {
        @Child private ReadVariableNode readGridTopLevel = ReadVariableNode.create("grid.top.level.vp");
        @Child private RExplicitCallNode callNode = RExplicitCallNode.create();
        @Child private DoSetViewPort doSetViewPort = new DoSetViewPort();

        public RList execute(VirtualFrame frame) {
            RFunction gridTopLevel = (RFunction) readGridTopLevel.execute(frame);
            RList topVP = (RList) callNode.execute(frame, gridTopLevel, RArgsValuesAndNames.EMPTY);

            GridDevice device = GridContext.getContext().getCurrentDevice();
            // TODO: properly set the scale according to the current device
            Object[] data = topVP.getDataWithoutCopying();
            data[ViewPort.VP_XSCALE] = RDataFactory.createDoubleVector(new double[]{0, device.getWidth()}, RDataFactory.COMPLETE_VECTOR);
            data[ViewPort.VP_YSCALE] = RDataFactory.createDoubleVector(new double[]{0, device.getHeight()}, RDataFactory.COMPLETE_VECTOR);
            data[ViewPort.PVP_GPAR] = GridContext.getContext().getGridState().getGpar();
            return doSetViewPort.doSetViewPort(topVP, false, true);
        }
    }
}
