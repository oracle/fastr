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
 * Defines parameters for drawing, like color, line style etc.
 */
public interface DrawingContext {
    double INCH_TO_POINTS_FACTOR = 72;

    /**
     * @return Hexadecimal string of the color with leading '#', e.g. '#FFA8B2'. Never returns a
     *         synonym.
     */
    String getColor();

    /**
     * Alows to set the color. The parameter may also be a synonym defined in
     * {@link com.oracle.truffle.r.library.fastrGrid.ColorNames}.
     */
    void setColor(String hexCode);

    /**
     * Gets the font size in points.
     *
     * @see #INCH_TO_POINTS_FACTOR
     */
    double getFontSize();

    /**
     * Gets the height of a line in multiplies of the base line height.
     */
    double getLineHeight();

    /**
     * @return Hexadecimal string of the color with leading '#', e.g. '#FFA8B2'. Never returns a
     *         synonym.
     */
    String getFillColor();

    /**
     * Alows to set the fill color. The parameter may also be a synonym defined in
     * {@link com.oracle.truffle.r.library.fastrGrid.ColorNames}.
     */
    void setFillColor(String hexCode);
}
