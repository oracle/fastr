/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2014, The R Core Team
 * Copyright (c) 2002--2010, The R Foundation
 * Copyright (C) 2005--2006, Morten Welinder
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.grDevices.fastrgd;

import static com.oracle.truffle.r.library.graphics.core.geometry.AxisDirection.EAST;
import static com.oracle.truffle.r.library.graphics.core.geometry.AxisDirection.NORTH;

import java.util.Arrays;
import java.util.function.Function;

import com.oracle.truffle.r.library.graphics.FastRFrame;
import com.oracle.truffle.r.library.graphics.core.DrawingParameters;
import com.oracle.truffle.r.library.graphics.core.GraphicsDevice;
import com.oracle.truffle.r.library.graphics.core.drawables.DrawableObject;
import com.oracle.truffle.r.library.graphics.core.drawables.PolylineDrawableObject;
import com.oracle.truffle.r.library.graphics.core.drawables.StringDrawableObject;
import com.oracle.truffle.r.library.graphics.core.geometry.Axis;
import com.oracle.truffle.r.library.graphics.core.geometry.CoordinateSystem;
import com.oracle.truffle.r.library.graphics.core.geometry.Coordinates;
import com.oracle.truffle.r.library.graphics.core.geometry.CoordinatesFactory;
import com.oracle.truffle.r.library.graphics.core.geometry.DoubleCoordinates;

/**
 * Default interactive FastR graphics device.
 */
public class FastRGraphicsDevice implements GraphicsDevice {
    private static final double GNUR_DEFAULT_MAX_X = 1;
    private static final Axis GNUR_DEFAULT_X_AXIS = new Axis(0, GNUR_DEFAULT_MAX_X, EAST);
    private static final Axis GNUR_DEFAULT_Y_AXIS = new Axis(0, 1, NORTH);
    private static final double MARGIN = GNUR_DEFAULT_MAX_X * 0.1; // the margin for each side of
    // 10% of a screen
    // compress resulting image to have a small margin on all sides
    private static final double COMPRESS_RATION = 1. - MARGIN * 1.8;

    private Mode mode = Mode.GRAPHICS_OFF;
    private FastRFrame fastRFrame;
    private CoordinateSystem currentCoordinateSystem = new CoordinateSystem(GNUR_DEFAULT_X_AXIS, GNUR_DEFAULT_Y_AXIS);

    @Override
    public void deactivate() {
        // todo impl
    }

    @Override
    public void activate() {
        // todo impl
    }

    @Override
    public void close() {
        // todo impl
    }

    @Override
    public DrawingParameters getDrawingParameters() {
        return null;
    }

    @Override
    public void setMode(Mode newMode) {
        mode = newMode;
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    @Override
    public void setClipRect(double x1, double y1, double x2, double y2) {
        // todo impl
    }

    @Override
    public void drawPolyline(Coordinates coordinates, DrawingParameters drawingParameters) {
        // todo continue from GEPolyline() of engine.c
        Coordinates convertedCoords = CoordinatesFactory.withRatioAndShift(coordinates, COMPRESS_RATION, MARGIN);
        addDrawableObject(new PolylineDrawableObject(currentCoordinateSystem, convertedCoords));
        drawBounds();
        drawXYLabelsFor(coordinates);
    }

    private void drawBounds() {
        // x,y in range [0,1]
        double[] boundsXYPairs = {0, 0, 1, 0, 1, 0, 1, 1, 1, 1, 0, 1, 0, 1, 0, 0};
        Coordinates bounds = CoordinatesFactory.createByXYPairs(boundsXYPairs);
        Coordinates compressedBounds = CoordinatesFactory.withRatioAndShift(bounds, COMPRESS_RATION, MARGIN);
        addDrawableObject(new PolylineDrawableObject(currentCoordinateSystem, compressedBounds));
    }

    private void drawXYLabelsFor(Coordinates coordinates) {
        drawLabelsForCoordinates(coordinates.getXCoordinatesAsDoubles(), MARGIN, 0.01, // just small
                        // shift
                        d -> CoordinatesFactory.createWithSameY(d, 0));
        drawLabelsForCoordinates(coordinates.getYCoordinatesAsDoubles(), 0, MARGIN, d -> CoordinatesFactory.createWithSameX(0, d));
    }

    private void drawLabelsForCoordinates(double[] coordinates, double xShift, double yShift, Function<double[], DoubleCoordinates> xYConverter) {
        int length = coordinates.length;
        double[] sortedCoords = new double[length];
        // copy to avoid side-effects on a caller side
        System.arraycopy(coordinates, 0, sortedCoords, 0, length);
        Arrays.sort(sortedCoords);
        String[] labels = composeLabelsFor(sortedCoords);
        DoubleCoordinates xYCoords = xYConverter.apply(sortedCoords);
        Coordinates shiftedCoords = CoordinatesFactory.withRatioAndShift(xYCoords, COMPRESS_RATION, xShift, yShift);
        addDrawableObject(new StringDrawableObject(currentCoordinateSystem, shiftedCoords, labels));
    }

    private static String[] composeLabelsFor(double[] doubles) {
        return Arrays.stream(doubles).mapToObj(String::valueOf).toArray(String[]::new);
    }

    private FastRFrame getFastRFrame() {
        if (fastRFrame == null || !fastRFrame.isVisible()) {
            fastRFrame = new FastRFrame();
            fastRFrame.setVisible(true);
        }
        return fastRFrame;
    }

    private void addDrawableObject(DrawableObject drawableObject) {
        getFastRFrame().getFastRComponent().addDrawableObject(drawableObject);
    }
}
