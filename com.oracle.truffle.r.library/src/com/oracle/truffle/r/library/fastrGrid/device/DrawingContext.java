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

        public boolean isBold() {
            return this == BOLD || this == BOLDITALIC;
        }

        public boolean isItalic() {
            return this == ITALIC || this == BOLDITALIC;
        }
    }

    enum GridLineJoin {
        ROUND,
        MITRE,
        BEVEL;

        public static final int LAST_VALUE = BEVEL.ordinal();

        /**
         * Return enum's value corresponding to R's value.
         */
        public static GridLineJoin fromInt(int num) {
            return values()[num];
        }
    }

    enum GridLineEnd {
        ROUND,
        BUTT,
        SQUARE;

        public static final int LAST_VALUE = SQUARE.ordinal();

        /**
         * Return enum's value corresponding to R's value.
         */
        public static GridLineEnd fromInt(int num) {
            return values()[num];
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
     * Line width in multiplies of what is considered the basic "thin" line for given device.
     */
    double getLineWidth();

    GridLineJoin getLineJoin();

    GridLineEnd getLineEnd();

    /**
     * The mitre limit, larger than 1, default is 10. The unit should be interpreted the way as in
     * {@link #getLineType()}.
     */
    double getLineMitre();

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
     * Gets the height of a text line in multiplies of the base line height. This is typically not a
     * concern of devices, since they always receive single line strings for drawing.
     */
    double getLineHeight();

    /**
     * The fill color of shapes.
     */
    GridColor getFillColor();

    static boolean areSame(DrawingContext ctx1, DrawingContext ctx2) {
        return ctx1 == ctx2 || (ctx1.getColor().equals(ctx2.getColor()) &&
                        ctx1.getLineEnd() == ctx2.getLineEnd() &&
                        ctx1.getLineJoin() == ctx2.getLineJoin() &&
                        ctx1.getLineType() == ctx2.getLineType() &&
                        ctx1.getLineHeight() == ctx2.getLineHeight() &&
                        ctx1.getFontStyle() == ctx2.getFontStyle() &&
                        ctx1.getFontSize() == ctx2.getFontSize() &&
                        ctx1.getFontFamily().equals(ctx2.getFontFamily()) &&
                        ctx1.getLineWidth() == ctx2.getLineWidth() &&
                        ctx1.getLineMitre() == ctx2.getLineMitre() &&
                        ctx1.getFillColor().equals(ctx2.getFillColor()));
    }
}
