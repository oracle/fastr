/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
