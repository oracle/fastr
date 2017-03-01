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

/**
 * Operations on transformation (3x3) matrices.
 */
final class TransformMatrix {
    private TransformMatrix() {
        // only static members
    }

    static double[][] translation(double tx, double ty) {
        double[][] m = identity();
        m[2][0] = tx;
        m[2][1] = ty;
        return m;
    }

    static double[][] multiply(double[][] m1, double[][] m2) {
        double[][] m = new double[3][3];
        m[0][0] = m1[0][0] * m2[0][0] + m1[0][1] * m2[1][0] + m1[0][2] * m2[2][0];
        m[0][1] = m1[0][0] * m2[0][1] + m1[0][1] * m2[1][1] + m1[0][2] * m2[2][1];
        m[0][2] = m1[0][0] * m2[0][2] + m1[0][1] * m2[1][2] + m1[0][2] * m2[2][2];
        m[1][0] = m1[1][0] * m2[0][0] + m1[1][1] * m2[1][0] + m1[1][2] * m2[2][0];
        m[1][1] = m1[1][0] * m2[0][1] + m1[1][1] * m2[1][1] + m1[1][2] * m2[2][1];
        m[1][2] = m1[1][0] * m2[0][2] + m1[1][1] * m2[1][2] + m1[1][2] * m2[2][2];
        m[2][0] = m1[2][0] * m2[0][0] + m1[2][1] * m2[1][0] + m1[2][2] * m2[2][0];
        m[2][1] = m1[2][0] * m2[0][1] + m1[2][1] * m2[1][1] + m1[2][2] * m2[2][1];
        m[2][2] = m1[2][0] * m2[0][2] + m1[2][1] * m2[1][2] + m1[2][2] * m2[2][2];
        return m;
    }

    static double[][] identity() {
        double[][] result = new double[3][3];
        result[0][0] = 1;
        result[1][1] = 1;
        result[2][2] = 1;
        return result;
    }

    private static double[] location(double x, double y) {
        return new double[]{x, y, 1};
    }

    private static double[] transLocation(double[] location, double[][] m) {
        double[] res = new double[3];
        res[0] = location[0] * m[0][0] + location[1] * m[1][0] + location[2] * m[2][0];
        res[1] = location[0] * m[0][1] + location[1] * m[1][1] + location[2] * m[2][1];
        res[2] = location[0] * m[0][2] + location[1] * m[1][2] + location[2] * m[2][2];
        return res;
    }

    static Point transLocation(Point loc, double[][] m) {
        double[] newLoc = transLocation(location(loc.x, loc.y), m);
        return new Point(locationX(newLoc), locationY(newLoc));
    }

    private static double locationX(double[] loc) {
        return loc[0];
    }

    private static double locationY(double[] loc) {
        return loc[1];
    }

    /**
     * Transforms the internal double matrix to R matrix flat array.
     */
    static double[] flatten(double[][] m) {
        double[] res = new double[9];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                res[i + j * 3] = m[i][j];
            }
        }
        return res;
    }

    /**
     * Reverse operation to {@link #flatten(double[][])}.
     */
    static double[][] fromFlat(double[] flat) {
        double[][] res = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                res[i][j] = flat[i + j * 3];
            }
        }
        return res;
    }
}
