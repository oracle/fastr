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

/**
 * Defines parameters for drawing, like color, line style etc. The implementations must be
 * immutable.
 */
public interface DrawingContext {
    double INCH_TO_POINTS_FACTOR = 72.27;

    String FONT_FAMILY_MONO = "mono";
    String FONT_FAMILY_SANS = "sans";
    String FONT_FAMILY_SERIF = "serif";

    byte[] GRID_LINE_BLANK = null;
    byte[] GRID_LINE_SOLID = new byte[0];

    enum GridFontStyle {
        PLAIN,
        BOLD,
        ITALIC,
        BOLDITALIC,
        /**
         * Supposed to be symbol font in Adobe symbol encoding.
         */
        SYMBOL;

        /**
         * Return enum's value corresponding to R's value.
         */
        public static GridFontStyle fromInt(int num) {
            assert num > 0 && num <= SYMBOL.ordinal() + 1;
            return values()[num - 1];
        }
    }

    /**
     * Returns either one of the constants {@link #GRID_LINE_BLANK} or {@link #GRID_LINE_SOLID} or
     * an array with a pattern consisting of lengths. Lengths at odd positions are dashes and
     * lengths at the even positions are spaces between them, the pattern should be interpreted as
     * cyclic. Example: '3,2,10,1' means 3 units of line, 2 units of space, 10 units of line, 1 unit
     * of space and repeat. The unit here can be device dependent, but should be something "small",
     * like a pixel.
     */
    byte[] getLineType();

    /**
     * Drawing color of shape borders, lines and text.
     */
    GridColor getColor();

    /**
     * Gets the font size in points.
     *
     * @see #INCH_TO_POINTS_FACTOR
     */
    double getFontSize();

    GridFontStyle getFontStyle();

    /**
     * Gets the font family name. The standard values that any device must implement are "serif",
     * "sans" and "mono". On top of that the device can recognize name of any font that it can
     * support.
     */
    String getFontFamily();

    /**
     * Gets the height of a line in multiplies of the base line height.
     */
    double getLineHeight();

    /**
     * The fill color of shapes.
     */
    GridColor getFillColor();
}
