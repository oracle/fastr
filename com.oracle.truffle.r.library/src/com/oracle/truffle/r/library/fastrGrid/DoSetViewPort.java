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

import static com.oracle.truffle.r.library.fastrGrid.TransformMatrix.flatten;
import static com.oracle.truffle.r.library.fastrGrid.TransformMatrix.fromFlat;
import static com.oracle.truffle.r.library.fastrGrid.TransformMatrix.identity;
import static com.oracle.truffle.r.library.fastrGrid.TransformMatrix.multiply;
import static com.oracle.truffle.r.library.fastrGrid.TransformMatrix.translation;
import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;

import com.oracle.truffle.r.library.fastrGrid.Unit.UnitConversionContext;
import com.oracle.truffle.r.library.fastrGrid.ViewPortContext.VPContextFromVPNode;
import com.oracle.truffle.r.library.fastrGrid.ViewPortLocation.VPLocationFromVPNode;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

class DoSetViewPort extends RBaseNode {
    @Child private CastNode castScalarDouble = newCastBuilder().asDoubleVector().findFirst().buildCastNode();
    @Child private CastNode castDoubleVector = newCastBuilder().asDoubleVector().buildCastNode();
    @Child private CastNode castChildrenEnv = newCastBuilder().mustBe(REnvironment.class).buildCastNode();
    @Child private Unit.UnitToInchesNode unitsToInches = Unit.UnitToInchesNode.create();
    @Child private VPLocationFromVPNode vpLocationFromVP = new VPLocationFromVPNode();
    @Child private VPContextFromVPNode vpContextFromVP = new VPContextFromVPNode();

    public RList doSetViewPort(RList pushedViewPort, boolean hasParent, boolean pushing) {
        GridState gridState = GridContext.getContext().getGridState();
        Object[] pushedVPData = pushedViewPort.getDataWithoutCopying();
        if (hasParent && pushing) {
            RList parent = gridState.getViewPort();
            pushedVPData[ViewPort.PVP_PARENT] = parent;
            REnvironment children = (REnvironment) castChildrenEnv.execute(parent.getDataAt(ViewPort.PVP_CHILDREN));
            safePutToEnv(pushedViewPort, pushedVPData[ViewPort.VP_NAME], children);
        }

        GridDevice currentDevice = GridContext.getContext().getCurrentDevice();
        calcViewportTransform(pushedViewPort, pushedViewPort.getDataAt(ViewPort.PVP_PARENT), !hasParent, currentDevice, GPar.asDrawingContext(gridState.getGpar()));

        // TODO: clipping
        pushedVPData[ViewPort.PVP_CLIPRECT] = RDataFactory.createDoubleVector(new double[]{0, 0, 0, 0}, RDataFactory.COMPLETE_VECTOR);
        pushedVPData[ViewPort.PVP_DEVWIDTHCM] = scalar(Unit.inchesToCm(currentDevice.getWidth()));
        pushedVPData[ViewPort.PVP_DEVHEIGHTCM] = scalar(Unit.inchesToCm(currentDevice.getHeight()));
        return pushedViewPort;
    }

    private void calcViewportTransform(RList viewPort, Object parent, boolean incremental, GridDevice device, DrawingContext drawingContext) {
        double[][] parentTransform;
        ViewPortContext parentContext;
        ViewPortLocation vpl;
        Size parentSize;
        if (parent == null || parent == RNull.instance) {
            parentTransform = TransformMatrix.identity();
            parentContext = ViewPortContext.createDefault();
            parentSize = new Size(device.getWidth(), device.getHeight());
            vpl = vpLocationFromVP.execute(viewPort);
        } else {
            assert parent instanceof RList : "inconsistent data: parent of a viewport must be a list";
            RList parentList = (RList) parent;
            Object[] parentData = parentList.getDataWithoutCopying();
            if (!incremental) {
                calcViewportTransform(parentList, parentData[ViewPort.PVP_PARENT], false, device, drawingContext);
            }
            parentSize = new Size(Unit.cmToInches(castScalar(parentData[ViewPort.PVP_WIDTHCM])), Unit.cmToInches(castScalar(parentData[ViewPort.PVP_HEIGHTCM])));
            parentTransform = fromFlat(castDoubleVector(parentData[ViewPort.PVP_TRANS]).materialize().getDataWithoutCopying());
            parentContext = vpContextFromVP.execute(parentList);

            // TODO: gcontextFromgpar(viewportParentGPar(vp), 0, &parentgc, dd);
            // TODO: if (....)
            vpl = vpLocationFromVP.execute(viewPort);
        }

        UnitConversionContext conversionCtx = new UnitConversionContext(parentSize, parentContext, drawingContext);
        double xInches = unitsToInches.convertX(vpl.x, 0, conversionCtx);
        double yInches = unitsToInches.convertY(vpl.y, 0, conversionCtx);
        double width = unitsToInches.convertX(vpl.width, 0, conversionCtx);
        double height = unitsToInches.convertY(vpl.height, 0, conversionCtx);

        if (!Double.isFinite(xInches) || !Double.isFinite(yInches) || !Double.isFinite(width) || !Double.isFinite(height)) {
            error(Message.GENERIC, "non-finite location and/or size for viewport");
        }

        double xadj = GridUtils.justification(width, vpl.hjust);
        double yadj = GridUtils.justification(height, vpl.vjust);

        // Produce transform for this viewport
        double[][] thisLocation = translation(xInches, yInches);
        double[][] thisRotation = identity();
        // TODO: if (viewportAngle(vp) != 0) rotation(viewportAngle(vp), thisRotation);

        double[][] thisJustification = translation(xadj, yadj);
        // Position relative to origin of rotation THEN rotate.
        double[][] tempTransform = multiply(thisJustification, thisRotation);
        // Translate to bottom-left corner.
        double[][] thisTransform = multiply(tempTransform, thisLocation);
        // Combine with parent's transform
        double[][] transform = multiply(thisTransform, parentTransform);

        // Sum up the rotation angles
        // TODO: rotationAngle = parentAngle + viewportAngle(vp);
        double rotationAngle = 0;
        // TODO: Finally, allocate the rows and columns for this viewport's layout if it has one

        Object[] viewPortData = viewPort.getDataWithoutCopying();
        viewPortData[ViewPort.PVP_WIDTHCM] = scalar(Unit.inchesToCm(width));
        viewPortData[ViewPort.PVP_HEIGHTCM] = scalar(Unit.inchesToCm(height));
        viewPortData[ViewPort.PVP_ROTATION] = scalar(rotationAngle);
        viewPortData[ViewPort.PVP_TRANS] = RDataFactory.createDoubleVector(flatten(transform), RDataFactory.COMPLETE_VECTOR, new int[]{3, 3});
    }

    private RAbstractDoubleVector castDoubleVector(Object obj) {
        return (RAbstractDoubleVector) castDoubleVector.execute(obj);
    }

    private double castScalar(Object obj) {
        return (double) castScalarDouble.execute(obj);
    }

    private static RDoubleVector scalar(double val) {
        return RDataFactory.createDoubleVectorFromScalar(val);
    }

    private static void safePutToEnv(RList pushedViewPort, Object pushedVPDatum, REnvironment children) {
        try {
            children.put(RRuntime.asString(pushedVPDatum), pushedViewPort);
        } catch (PutException e) {
            RInternalError.shouldNotReachHere("Cannot update children environment in a view port list");
        }
    }
}
