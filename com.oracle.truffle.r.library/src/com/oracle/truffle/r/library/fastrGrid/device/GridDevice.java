/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.fastrGrid.device;

import static com.oracle.truffle.r.library.fastrGrid.device.DrawingContext.INCH_TO_POINTS_FACTOR;

/**
 * Abstract device that can draw primitive shapes and text. All sizes and coordinates are specified
 * in inches and angles in radians unless stated otherwise.
 */
public interface GridDevice {
    void openNewPage();

    /**
     * If the device is capable of buffering, calling {@code hold} should start buffering, e.g.
     * nothing is displayed on the device, until {@link #flush()} is called.
     */
    default void hold() {
    }

    /**
     * Should display the whole buffer at once.
     *
     * @see #hold()
     */
    default void flush() {
    }

    /**
     * Draws a rectangle at given position, the center of the rotation should be the center of the
     * rectangle. The rotation is given in radians.
     */
    void drawRect(DrawingContext ctx, double leftX, double topY, double width, double height, double rotationAnticlockWise);

    /**
     * Connects given points with a line, there has to be at least two points in order to actually
     * draw somethig.
     */
    void drawPolyLines(DrawingContext ctx, double[] x, double[] y, int startIndex, int length);

    /**
     * Version of {@link #drawPolyLines(DrawingContext, double[], double[], int, int)}, which should
     * fill in the area bounded by the lines. Note that it is responsibility of the caller to
     * connect the first and the last point if the caller wishes to draw actual polygon.
     *
     * @see DrawingContext#getFillColor()
     */
    void drawPolygon(DrawingContext ctx, double[] x, double[] y, int startIndex, int length);

    void drawCircle(DrawingContext ctx, double centerX, double centerY, double radius);

    /**
     * Prints a string with left bottom corner at given position rotates by given angle anti clock
     * wise, the centre of the rotation should be the bottom left corer.
     */
    void drawString(DrawingContext ctx, double leftX, double bottomY, double rotationAnticlockWise, String text);

    /**
     * @return The width of the device in inches.
     */
    double getWidth();

    /**
     * @return The height of the device in inches.
     */
    double getHeight();

    /**
     * May change the default values the of the initial drawing context instance. Must return
     * non-null value.
     */
    default DrawingContextDefaults getDrawingContextDefaults() {
        return new DrawingContextDefaults();
    }

    double getStringWidth(DrawingContext ctx, String text);

    /**
     * Gets the height of a line of text in inches, the default implementation uses only the
     * parameters from the drawing context, but we allow the device to override this calculation
     * with something more precise.
     */
    default double getStringHeight(DrawingContext ctx, @SuppressWarnings("unused") String text) {
        return (ctx.getLineHeight() * ctx.getFontSize()) / INCH_TO_POINTS_FACTOR;
    }
}
