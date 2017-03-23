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

    enum GridLineType {
        // The order is important!
        BLANK,
        SOLID,
        DASHED,
        DOTTED,
        DOTDASHED,
        LONGDASH,
        TWODASH;

        private static final int LINE_TYPES_COUNT = 7;
        private static final GridLineType[] allValues = values();

        public static GridLineType fromInt(int num) {
            if (num == -1) {
                return BLANK;
            }
            assert num >= 1;
            return allValues[(num - 1) % LINE_TYPES_COUNT + 1];
        }
    }

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

    GridLineType getLineType();

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
