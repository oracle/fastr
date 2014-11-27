/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.graphics.core.geometry;

import com.oracle.truffle.r.runtime.data.RDoubleVector;

import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public final class CoordinatesFactory {
    private CoordinatesFactory() {
    }

    public static DoubleCoordinates createWithSameX(double x, double[] yCoords) {
        double[] xCoords = createdFilledArray(x, yCoords.length);
        return new DoubleCoordinates(xCoords, yCoords);
    }

    public static DoubleCoordinates createWithSameY(double[] xCoords, double y) {
        double[] yCoords = createdFilledArray(y, xCoords.length);
        return new DoubleCoordinates(xCoords, yCoords);
    }

    private static double[] createdFilledArray(double value, int size) {
        double[] result = new double[size];
        Arrays.fill(result, value);
        return result;
    }

    public static DoubleCoordinates createByXYVector(RDoubleVector xyVector) {
        int length = xyVector.getLength();
        double[] xCoords = IntStream.range(0, length).filter(i -> i % 2 == 0).mapToDouble(xyVector::getDataAt).toArray();
        double[] yCoords = IntStream.range(0, length).filter(i -> i % 2 != 0).mapToDouble(xyVector::getDataAt).toArray();
        return new DoubleCoordinates(xCoords, yCoords);
    }

    public static DoubleCoordinates createByXYPairs(double[] xyPairs) {
        int length = xyPairs.length;
        double[] xCoords = IntStream.range(0, length).filter(i -> i % 2 == 0).mapToDouble(i -> xyPairs[i]).toArray();
        double[] yCoords = IntStream.range(0, length).filter(i -> i % 2 != 0).mapToDouble(i -> xyPairs[i]).toArray();
        return new DoubleCoordinates(xCoords, yCoords);
    }

    public static DoubleCoordinates withRatioAndShift(Coordinates coordinates, double ratio, double shift) {
        return withRatioAndShift(coordinates, ratio, shift, shift);
    }

    public static DoubleCoordinates withRatioAndShift(Coordinates coordinates, double ratio, double xShift, double yShift) {
        double[] convertedX = applyRatioAndShiftTo(coordinates.getXCoordinatesAsDoubles(), ratio, xShift);
        double[] convertedY = applyRatioAndShiftTo(coordinates.getYCoordinatesAsDoubles(), ratio, yShift);
        return new DoubleCoordinates(convertedX, convertedY);
    }

    private static double[] applyRatioAndShiftTo(double[] coordinates, double ratio, double shift) {
        return DoubleStream.of(coordinates).map(d -> d * ratio + shift).toArray();
    }
}
