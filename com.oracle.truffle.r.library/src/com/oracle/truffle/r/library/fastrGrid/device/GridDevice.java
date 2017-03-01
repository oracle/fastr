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
 * in inches.
 */
public interface GridDevice {
    void openNewPage();

    void drawRect(DrawingContext ctx, double leftX, double topY, double heigh, double width);

    void drawPolyLines(DrawingContext ctx, double[] x, double[] y, int startIndex, int length);

    void drawString(DrawingContext ctx, double x, double y, double rotation, String text);

    /**
     * @return The width of the device in inches.
     */
    double getWidth();

    /**
     * @return The height of the device in inches.
     */
    double getHeight();

    /**
     * May change the default values the of the initial drawing context instance.
     * 
     * @param ctx instance of drawing context to be altered.
     */
    default void initDrawingContext(DrawingContext ctx) {
        // nop
    }

    double getStringWidth(DrawingContext ctx, String text);

    /**
     * Gets the height of a line of text in inches, the default implementation uses only the
     * parameters from the drawing context, but we allow the device to override this calculation
     * with something more precise.
     */
    default double getStringHeight(DrawingContext ctx, String text) {
        return (ctx.getLineHeight() * ctx.getFontSize()) / INCH_TO_POINTS_FACTOR;
    }
}
