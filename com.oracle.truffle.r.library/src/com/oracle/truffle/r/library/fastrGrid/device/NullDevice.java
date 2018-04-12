/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.library.fastrGrid.device;

public class NullDevice implements GridDevice {

    @Override
    public void openNewPage() {
    }

    @Override
    public void drawRect(DrawingContext ctx, double leftX, double bottomY, double width, double height, double rotationAnticlockWise) {
    }

    @Override
    public void drawPolyLines(DrawingContext ctx, double[] x, double[] y, int startIndex, int length) {
    }

    @Override
    public void drawPolygon(DrawingContext ctx, double[] x, double[] y, int startIndex, int length) {
    }

    @Override
    public void drawCircle(DrawingContext ctx, double centerX, double centerY, double radius) {
    }

    @Override
    public void drawRaster(double leftX, double bottomY, double width, double height, int[] pixels, int pixelsColumnsCount, ImageInterpolation interpolation) {
    }

    @Override
    public void drawString(DrawingContext ctx, double leftX, double bottomY, double rotationAnticlockWise, String text) {
    }

    @Override
    public double getWidth() {
        return 0;
    }

    @Override
    public double getHeight() {
        return 0;
    }

    @Override
    public int getNativeWidth() {
        return 0;
    }

    @Override
    public int getNativeHeight() {
        return 0;
    }

    @Override
    public double getStringWidth(DrawingContext ctx, String text) {
        return 0;
    }

    @Override
    public double getStringHeight(DrawingContext ctx, String text) {
        return 0;
    }

}
