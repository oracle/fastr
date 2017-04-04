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

import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asAbstractContainer;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asDouble;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asList;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asListOrNull;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.sum;
import static com.oracle.truffle.r.library.fastrGrid.TransformMatrix.flatten;
import static com.oracle.truffle.r.library.fastrGrid.TransformMatrix.fromFlat;
import static com.oracle.truffle.r.library.fastrGrid.TransformMatrix.multiply;
import static com.oracle.truffle.r.library.fastrGrid.TransformMatrix.rotation;
import static com.oracle.truffle.r.library.fastrGrid.TransformMatrix.translation;
import static com.oracle.truffle.r.library.fastrGrid.Unit.newUnit;
import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.library.fastrGrid.Unit.IsRelativeUnitNode;
import com.oracle.truffle.r.library.fastrGrid.Unit.UnitConversionContext;
import com.oracle.truffle.r.library.fastrGrid.ViewPort.LayoutPos;
import com.oracle.truffle.r.library.fastrGrid.ViewPort.LayoutSize;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

final class DoSetViewPort extends RBaseNode {
    @Child private CastNode castScalarDouble = newCastBuilder().asDoubleVector().findFirst().buildCastNode();
    @Child private CastNode castDoubleVector = newCastBuilder().asDoubleVector().buildCastNode();
    @Child private CastNode castChildrenEnv = newCastBuilder().mustBe(REnvironment.class).buildCastNode();
    @Child private Unit.UnitToInchesNode unitsToInches = Unit.UnitToInchesNode.create();
    @Child private IsRelativeUnitNode isRelativeUnit = new IsRelativeUnitNode();

    @TruffleBoundary
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
        GPar gpar = GridState.getInitialGPar(currentDevice);

        RList parent = asListOrNull(pushedViewPort.getDataAt(ViewPort.PVP_PARENT));
        boolean doNotRecalculateParent = hasParent && !ViewPort.updateDeviceSizeInVP(parent, currentDevice);
        calcViewportTransform(pushedViewPort, parent, doNotRecalculateParent, currentDevice, gpar);

        // TODO: clipping
        pushedVPData[ViewPort.PVP_CLIPRECT] = RDataFactory.createDoubleVector(new double[]{0, 0, 0, 0}, RDataFactory.COMPLETE_VECTOR);
        pushedVPData[ViewPort.PVP_DEVWIDTHCM] = scalar(Unit.inchesToCm(currentDevice.getWidth()));
        pushedVPData[ViewPort.PVP_DEVHEIGHTCM] = scalar(Unit.inchesToCm(currentDevice.getHeight()));

        assert pushedViewPort.verify();
        return pushedViewPort;
    }

    /**
     * Calculates and sets the view-port width and height in inches, and transformation matrix and
     * rotation angle.
     *
     * @param viewPort The view-port to be updated.
     * @param parent The parent of the view-port, null if the view-port is top level.
     * @param incremental If {@code true} it is assumed that we can just take the transformation
     *            matrix and other values from the parent without re-calculating them recursively.
     * @param device This method needs the device in order to convert units
     * @param deviceTopLevelGpar This method needs to know the device default drawing context in
     *            order to convert units for the top level view port
     */
    @TruffleBoundary
    public void calcViewportTransform(RList viewPort, Object parent, boolean incremental, GridDevice device, GPar deviceTopLevelGpar) {
        double[][] parentTransform;
        ViewPortContext parentContext;
        ViewPortLocation vpl;
        Size parentSize;
        GPar drawingContext;
        double parentAngle;
        if (parent == null || parent == RNull.instance) {
            parentTransform = TransformMatrix.identity();
            parentContext = ViewPortContext.createDefault();
            parentSize = new Size(device.getWidth(), device.getHeight());
            vpl = ViewPortLocation.fromViewPort(viewPort);
            drawingContext = deviceTopLevelGpar;
            parentAngle = 0;
        } else {
            assert parent instanceof RList : "inconsistent data: parent of a viewport must be a list";
            RList parentVPList = (RList) parent;
            Object[] parentData = parentVPList.getDataWithoutCopying();
            if (!incremental) {
                calcViewportTransform(parentVPList, parentData[ViewPort.PVP_PARENT], false, device, deviceTopLevelGpar);
            }
            parentSize = new Size(Unit.cmToInches(castScalar(parentData[ViewPort.PVP_WIDTHCM])), Unit.cmToInches(castScalar(parentData[ViewPort.PVP_HEIGHTCM])));
            parentTransform = fromFlat(castDoubleVector(parentData[ViewPort.PVP_TRANS]).materialize().getDataWithoutCopying());
            parentContext = ViewPortContext.fromViewPort(parentVPList);
            parentAngle = asDouble(parentData[ViewPort.PVP_ROTATION]);

            drawingContext = GPar.create(asList(viewPort.getDataAt(ViewPort.PVP_PARENTGPAR)));
            boolean noLayout = (isNull(viewPort.getDataAt(ViewPort.VP_VALIDLPOSROW)) && isNull(viewPort.getDataAt(ViewPort.VP_VALIDLPOSCOL))) || isNull(parentData[ViewPort.VP_LAYOUT]);
            if (noLayout) {
                vpl = ViewPortLocation.fromViewPort(viewPort);
            } else {
                vpl = calcViewportLocationFromLayout(getLayoutPos(viewPort, parentVPList), parentVPList, parentSize);
            }
        }

        UnitConversionContext conversionCtx = new UnitConversionContext(parentSize, parentContext, device, drawingContext);
        double xInches = unitsToInches.convertX(vpl.x, 0, conversionCtx);
        double yInches = unitsToInches.convertY(vpl.y, 0, conversionCtx);
        double width = unitsToInches.convertWidth(vpl.width, 0, conversionCtx);
        double height = unitsToInches.convertHeight(vpl.height, 0, conversionCtx);

        if (!Double.isFinite(xInches) || !Double.isFinite(yInches) || !Double.isFinite(width) || !Double.isFinite(height)) {
            throw error(Message.GENERIC, "non-finite location and/or size for viewport");
        }

        double xadj = GridUtils.justification(width, vpl.hjust);
        double yadj = GridUtils.justification(height, vpl.vjust);

        // Produce transform for this viewport
        double[][] thisLocation = translation(xInches, yInches);
        double[][] thisJustification = translation(xadj, yadj);
        // Position relative to origin of rotation THEN rotate.
        double viewPortAngle = asDouble(viewPort.getDataAt(ViewPort.VP_ANGLE));
        double[][] thisRotation = rotation(viewPortAngle);
        double[][] tempTransform = multiply(thisJustification, thisRotation);
        // Translate to bottom-left corner.
        double[][] thisTransform = multiply(tempTransform, thisLocation);
        // Combine with parent's transform
        double[][] transform = multiply(thisTransform, parentTransform);

        // Sum up the rotation angles
        double rotationAngle = parentAngle + viewPortAngle;

        // Finally, allocate the rows and columns for this viewport's layout if it has one
        if (!isNull(viewPort.getDataAt(ViewPort.VP_LAYOUT))) {
            ViewPortContext vpCtx = ViewPortContext.fromViewPort(viewPort);
            calcViewPortLayout(viewPort, new Size(width, height), vpCtx, device, GPar.create(asList(viewPort.getDataAt(ViewPort.PVP_GPAR))));
        }

        Object[] viewPortData = viewPort.getDataWithoutCopying();
        viewPortData[ViewPort.PVP_WIDTHCM] = scalar(Unit.inchesToCm(width));
        viewPortData[ViewPort.PVP_HEIGHTCM] = scalar(Unit.inchesToCm(height));
        viewPortData[ViewPort.PVP_ROTATION] = scalar(rotationAngle);
        viewPortData[ViewPort.PVP_TRANS] = RDataFactory.createDoubleVector(flatten(transform), RDataFactory.COMPLETE_VECTOR, new int[]{3, 3});
    }

    private void calcViewPortLayout(RList viewPort, Size size, ViewPortContext parentVPCtx, GridDevice device, GPar gpar) {
        LayoutSize layoutSize = LayoutSize.fromViewPort(viewPort);
        double[] npcWidths = new double[layoutSize.ncol];
        double[] npcHeights = new double[layoutSize.nrow];
        boolean[] relativeWidths = new boolean[layoutSize.ncol];
        boolean[] relativeHeights = new boolean[layoutSize.nrow];
        UnitConversionContext conversionCtx = new UnitConversionContext(size, parentVPCtx, device, gpar);

        // For both dimensions we find out which units are other than "null" for those we can
        // immediately calculate the physical size in npcWidth/npcHeights. The reducedWidth/Height
        // gives us how much of width/height is left after we take up the space by physical units
        Object[] layoutData = asList(viewPort.getDataAt(ViewPort.VP_LAYOUT)).getDataWithoutCopying();
        RAbstractContainer layoutWidths = asAbstractContainer(layoutData[ViewPort.LAYOUT_WIDTHS]);
        assert Unit.getLength(layoutWidths) == layoutSize.ncol : "inconsistent layout size with layout widths";
        double reducedWidth = getReducedDimension(layoutWidths, npcWidths, relativeWidths, size.getWidth(), conversionCtx, true);

        RAbstractContainer layoutHeights = asAbstractContainer(layoutData[ViewPort.LAYOUT_HEIGHTS]);
        assert Unit.getLength(layoutHeights) == layoutSize.nrow : "inconsistent layout size with layout height";
        double reducedHeight = getReducedDimension(layoutHeights, npcHeights, relativeHeights, size.getHeight(), conversionCtx, false);

        // npcHeight (and npcWidth) has some 'holes' in them, at indexes with
        // relativeHeights[index]==true, we will calculate the values for them now.
        // Firstly allocate the respected widths/heights and calculate how much space remains
        RList layoutAsList = asList(viewPort.getDataAt(ViewPort.VP_LAYOUT));
        int respect = RRuntime.asInteger(layoutAsList.getDataAt(ViewPort.LAYOUT_VRESPECT));
        int[] layoutRespectMat = ((RAbstractIntVector) layoutAsList.getDataAt(ViewPort.LAYOUT_MRESPECT)).materialize().getDataWithoutCopying();
        if ((reducedHeight > 0 || reducedWidth > 0) && respect > 0) {
            double sumRelWidth = sumRelativeDimension(layoutWidths, relativeWidths, parentVPCtx, device, gpar, true);
            double sumRelHeight = sumRelativeDimension(layoutHeights, relativeHeights, parentVPCtx, device, gpar, false);
            double tempWidth = reducedWidth;
            double tempHeight = reducedHeight;
            double denom;
            double mult;
            // Determine whether aspect ratio of available space is bigger or smaller than
            // aspect ratio of layout
            if (tempHeight * sumRelWidth > sumRelHeight * tempWidth) {
                denom = sumRelWidth;
                mult = tempWidth;
            } else {
                denom = sumRelHeight;
                mult = tempHeight;
            }
            for (int i = 0; i < layoutSize.ncol; i++) {
                if (relativeWidths[i] && colRespected(respect, i, layoutRespectMat, layoutSize)) {
                    /*
                     * Special case of respect, but sumHeight = 0. Action is to allocate widths as
                     * if unrespected. Ok to test == 0 because will only be 0 if all relative
                     * heights are actually exactly 0.
                     */
                    if (sumRelHeight == 0) {
                        denom = sumRelWidth;
                        mult = tempWidth;
                    }
                    // Build a unit SEXP with a single value and no data
                    npcWidths[i] = Unit.pureNullUnitValue(layoutWidths, i) / denom * mult;
                    reducedWidth -= npcWidths[i];
                }
            }
            for (int i = 0; i < layoutSize.nrow; i++) {
                if (relativeHeights[i] && rowRespected(respect, i, layoutRespectMat, layoutSize)) {
                    if (sumRelWidth == 0) {
                        denom = sumRelHeight;
                        mult = tempHeight;
                    }
                    npcHeights[i] = Unit.pureNullUnitValue(layoutHeights, i) / denom * mult;
                    reducedHeight -= npcHeights[i];
                }
            }
        } else if (respect > 0) {
            for (int i = 0; i < layoutSize.ncol; i++) {
                if (relativeWidths[i] && colRespected(respect, i, layoutRespectMat, layoutSize)) {
                    npcWidths[i] = 0;
                }
            }
            for (int i = 0; i < layoutSize.nrow; i++) {
                if (relativeHeights[i] && rowRespected(respect, i, layoutRespectMat, layoutSize)) {
                    npcHeights[i] = 0;
                }
            }
        }

        // Secondly, allocate remaining relative widths and heights in the remaining space
        allocateRelativeDim(layoutSize, layoutWidths, npcWidths, relativeWidths, reducedWidth, respect, layoutRespectMat, device, gpar, parentVPCtx, true);
        allocateRelativeDim(layoutSize, layoutHeights, npcHeights, relativeHeights, reducedHeight, respect, layoutRespectMat, device, gpar, parentVPCtx, false);

        // Create the result
        Object[] vpData = viewPort.getDataWithoutCopying();
        vpData[ViewPort.PVP_WIDTHS] = RDataFactory.createDoubleVector(npcWidths, RDataFactory.COMPLETE_VECTOR);
        vpData[ViewPort.PVP_HEIGHTS] = RDataFactory.createDoubleVector(npcHeights, RDataFactory.COMPLETE_VECTOR);
    }

    private void allocateRelativeDim(LayoutSize layoutSize, RAbstractContainer layoutItems, double[] npcItems, boolean[] relativeItems, double reducedDim, int respect, int[] layoutRespectMat,
                    GridDevice device, GPar gpar, ViewPortContext parentVPCtx, boolean isWidth) {
        assert relativeItems.length == npcItems.length;
        UnitConversionContext layoutModeCtx = new UnitConversionContext(new Size(0, 0), parentVPCtx, device, gpar, 1, 0);
        double totalUnrespectedSize = 0;
        if (reducedDim > 0) {
            for (int i = 0; i < relativeItems.length; i++) {
                if (relativeItems[i] && !rowColRespected(respect, i, layoutRespectMat, layoutSize, isWidth)) {
                    totalUnrespectedSize += unitsToInches.convertDimension(layoutItems, i, layoutModeCtx, isWidth);
                }
            }
        }
        // set the remaining width/height to zero or to proportion of totalUnrespectedSize
        for (int i = 0; i < relativeItems.length; i++) {
            if (relativeItems[i] && !rowColRespected(respect, i, layoutRespectMat, layoutSize, isWidth)) {
                npcItems[i] = 0;
                if (totalUnrespectedSize > 0) {
                    // if there was some with left, then totalUnrespectedSize contains sum of it
                    npcItems[i] = reducedDim * unitsToInches.convertDimension(layoutItems, i, layoutModeCtx, isWidth) / totalUnrespectedSize;
                }
            }
        }
    }

    private boolean rowColRespected(int respected, int rowOrCol, int[] layoutRespectMat, LayoutSize layoutSize, boolean isColumn) {
        return isColumn ? colRespected(respected, rowOrCol, layoutRespectMat, layoutSize) : rowRespected(respected, rowOrCol, layoutRespectMat, layoutSize);
    }

    private boolean rowRespected(int respected, int row, int[] layoutRespectMat, LayoutSize layoutSize) {
        if (respected == 1) {
            return true;
        }
        for (int i = 0; i < layoutSize.ncol; i++) {
            if (layoutRespectMat[i * layoutSize.nrow + row] != 0) {
                return true;
            }
        }
        return false;
    }

    private boolean colRespected(int respected, int col, int[] layoutRespectMat, LayoutSize layoutSize) {
        if (respected == 1) {
            return true;
        }
        for (int i = 0; i < layoutSize.nrow; i++) {
            if (layoutRespectMat[col * layoutSize.nrow + i] != 0) {
                return true;
            }
        }
        return false;
    }

    private double sumRelativeDimension(RAbstractContainer layoutItems, boolean[] relativeItems, ViewPortContext parentVPCtx, GridDevice device, GPar gpar,
                    boolean isWidth) {
        UnitConversionContext layoutModeCtx = new UnitConversionContext(new Size(0, 0), parentVPCtx, device, gpar, 1, 0);
        double totalWidth = 0;
        for (int i = 0; i < relativeItems.length; i++) {
            if (relativeItems[i]) {
                totalWidth += unitsToInches.convertDimension(layoutItems, i, layoutModeCtx, isWidth);
            }
        }
        return totalWidth;
    }

    private double getReducedDimension(RAbstractContainer layoutItems, double[] npcItems, boolean[] relativeItems, double initialSize, UnitConversionContext conversionCtx,
                    boolean isWidth) {
        double reducedSize = initialSize;
        for (int i = 0; i < npcItems.length; i++) {
            boolean currIsRel = isRelativeUnit.execute(layoutItems, i);
            relativeItems[i] = currIsRel;
            if (!currIsRel) {
                npcItems[i] = unitsToInches.convertDimension(layoutItems, i, conversionCtx, isWidth);
                reducedSize -= npcItems[i];
            }
        }
        return reducedSize;
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
            throw RInternalError.shouldNotReachHere("Cannot update children environment in a view port list");
        }
    }

    // Note: unlike the GnuR counterpart of this method, we expect the LayoutPos to have the NULL
    // positions replaced with nrow/ncol already.
    private ViewPortLocation calcViewportLocationFromLayout(LayoutPos pos, RList parentVP, Size parentSize) {
        // unlike in GnuR, we maintain parent viewport widths/heights in inches like anything else
        double[] widths = castDoubleVector(parentVP.getDataAt(ViewPort.PVP_WIDTHS)).materialize().getDataWithoutCopying();
        double[] heights = castDoubleVector(parentVP.getDataAt(ViewPort.PVP_HEIGHTS)).materialize().getDataWithoutCopying();
        double totalWidth = sum(widths, 0, pos.layoutSize.ncol);
        double totalHeight = sum(heights, 0, pos.layoutSize.nrow);
        double width = sum(widths, pos.colMin, pos.colMax - pos.colMin + 1);
        double height = sum(heights, pos.rowMin, pos.rowMax - pos.rowMin + 1);
        double left = parentSize.getWidth() * pos.layoutSize.hjust - totalWidth * pos.layoutSize.hjust + sum(widths, 0, pos.colMin);
        double bottom = parentSize.getHeight() * pos.layoutSize.vjust + (1 - pos.layoutSize.vjust) * totalHeight - sum(heights, 0, pos.rowMax + 1);
        ViewPortLocation result = new ViewPortLocation();
        result.width = newUnit(width, Unit.INCHES);
        result.height = newUnit(height, Unit.INCHES);
        result.x = newUnit(left, Unit.INCHES);
        result.y = newUnit(bottom, Unit.INCHES);
        result.hjust = result.vjust = 0;
        return result;
    }

    private LayoutPos getLayoutPos(RList vp, RList parent) {
        LayoutSize size = LayoutSize.fromViewPort(parent);
        Object rowObj = vp.getDataAt(ViewPort.VP_VALIDLPOSROW);
        int rowMin = 1;
        int rowMax = size.nrow;
        if (rowObj instanceof RAbstractIntVector) {
            rowMin = ((RAbstractIntVector) rowObj).getDataAt(0);
            rowMax = ((RAbstractIntVector) rowObj).getDataAt(1);
            if (rowMin < 1 || rowMax > size.nrow) {
                throw error(Message.GENERIC, "invalid 'layout.pos.row'");
            }
        }
        Object colObj = vp.getDataAt(ViewPort.VP_VALIDLPOSCOL);
        int colMin = 1;
        int colMax = size.ncol;
        if (colObj instanceof RAbstractIntVector) {
            colMin = ((RAbstractIntVector) colObj).getDataAt(0);
            colMax = ((RAbstractIntVector) colObj).getDataAt(1);
            if (colMin < 1 || colMax > size.ncol) {
                throw error(Message.GENERIC, "invalid 'layout.pos.row'");
            }
        }
        // the indexes in LayoutPos are to be interpreted as 0-based
        return new LayoutPos(colMin - 1, colMax - 1, rowMin - 1, rowMax - 1, size);
    }

    private static boolean isNull(Object value) {
        return value == null || value == RNull.instance;
    }
}
