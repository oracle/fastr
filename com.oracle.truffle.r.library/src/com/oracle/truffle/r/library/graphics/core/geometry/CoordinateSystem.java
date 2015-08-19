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
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.graphics.core.geometry;

import java.util.stream.DoubleStream;

/**
 * Denotes X-Y coordinate system by specifying max and min values for X-Y axis. Able to convert
 * coordinates given in another {@link CoordinateSystem}
 */
public final class CoordinateSystem {
    private final Axis xAxis;
    private final Axis yAxis;

    /**
     * Uses Java graphics default axis orientation: x increases to the right, y increases to the
     * bottom.
     */
    public CoordinateSystem(double minX, double maxX, double minY, double maxY) {
        this(minX, maxX, minY, maxY, AxisDirection.EAST, AxisDirection.SOUTH);
    }

    public CoordinateSystem(double minX, double maxX, double minY, double maxY, AxisDirection xDirection, AxisDirection yDirection) {
        this(new Axis(minX, maxX, xDirection), new Axis(minY, maxY, yDirection));
    }

    public CoordinateSystem(Axis xAxis, Axis yAxis) {
        this.xAxis = xAxis;
        this.yAxis = yAxis;
    }

    /**
     * Transforms <code> otherCoordinates </code> given in <code> otherCoordinateSystem</code> to
     * this coordinate system. Also applies the affine transformation defined by ratio and shifts.
     */
    public Coordinates convertCoordinatesFrom(CoordinateSystem otherCoordinateSystem, Coordinates otherCoordinates, double ratio, double xAxisShift, double yAxisShift) {
        double[] resultX = convertCoordinatesBetweenAxises(getXAxis(), otherCoordinateSystem.getXAxis(), otherCoordinates.getXCoordinatesAsDoubles(), ratio, xAxisShift);
        double[] resultY = convertCoordinatesBetweenAxises(getYAxis(), otherCoordinateSystem.getYAxis(), otherCoordinates.getYCoordinatesAsDoubles(), ratio, yAxisShift);
        return new DoubleCoordinates(resultX, resultY);
    }

    public Coordinates convertCoordinatesFrom(CoordinateSystem otherCoordinateSystem, Coordinates otherCoordinates) {
        double noRatio = 1;
        double noShift = 0;
        return convertCoordinatesFrom(otherCoordinateSystem, otherCoordinates, noRatio, noShift, noShift);
    }

    private static double[] convertCoordinatesBetweenAxises(Axis toAxis, Axis fromAxis, double[] coords, double givenRatio, double givenShift) {
        boolean sameDirection = toAxis.getDirection() == fromAxis.getDirection();
        double ratio = toAxis.getRange() / fromAxis.getRange();
        ratio = sameDirection ? ratio : -ratio;
        ratio *= givenRatio; // adding given ratio
        double shift = sameDirection ? 0 : toAxis.getMaxValue();
        shift += givenShift * ratio; // adding given shift
        return applyShiftAndRatio(coords, ratio, shift);
    }

    private static double[] applyShiftAndRatio(double[] coords, double ratio, double shift) {
        return DoubleStream.of(coords).map(d -> d * ratio + shift).toArray();
    }

    private Axis getXAxis() {
        return xAxis;
    }

    private Axis getYAxis() {
        return yAxis;
    }
}
