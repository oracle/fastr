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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;

/**
 * Holds the data of a viewport needed to perform transformations.
 */
public final class ViewPortTransform {
    /**
     * Angle in radians.
     */
    public final double rotationAngle;
    public final double[][] transform;
    public final Size size;

    private ViewPortTransform(double width, double height, double rotationAngle, double[][] transform) {
        this.size = new Size(width, height);
        this.rotationAngle = rotationAngle;
        this.transform = transform;
    }

    public static final class GetViewPortTransformNode extends Node {
        @Child private CastNode castDoubleVector = newCastBuilder().mustBe(numericValue()).asDoubleVector().buildCastNode();
        @Child private CastNode castScalarDouble = newCastBuilder().mustBe(numericValue()).asDoubleVector().findFirst().buildCastNode();
        @Child private DoSetViewPort doSetViewPort;

        public ViewPortTransform execute(RList viewPort, GridDevice device) {
            if (ViewPort.updateDeviceSizeInVP(viewPort, device)) {
                // Note: GnuR sets incremental parameter to true, but don't we need to recalculate
                // the parent(s) as well?
                initDoSetViewportNode();
                doSetViewPort.calcViewportTransform(viewPort, viewPort.getDataAt(ViewPort.PVP_PARENT), true, device, GridState.getInitialGPar(device));
            }
            double width = Unit.cmToInches(getScalar(viewPort.getDataAt(ViewPort.PVP_WIDTHCM)));
            double height = Unit.cmToInches(getScalar(viewPort.getDataAt(ViewPort.PVP_HEIGHTCM)));
            double rotationAngle = getScalar(viewPort.getDataAt(ViewPort.VP_ANGLE));
            RAbstractDoubleVector trans = (RAbstractDoubleVector) castDoubleVector.execute(viewPort.getDataAt(ViewPort.PVP_TRANS));
            double[][] transform = TransformMatrix.fromFlat(trans.materialize().getDataWithoutCopying());
            return new ViewPortTransform(width, height, rotationAngle, transform);
        }

        private void initDoSetViewportNode() {
            if (doSetViewPort == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                doSetViewPort = new DoSetViewPort();
            }
        }

        private double getScalar(Object value) {
            return (double) castScalarDouble.execute(value);
        }
    }
}
