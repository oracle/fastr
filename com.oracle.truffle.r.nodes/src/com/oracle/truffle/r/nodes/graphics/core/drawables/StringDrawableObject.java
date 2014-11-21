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
package com.oracle.truffle.r.nodes.graphics.core.drawables;

import com.oracle.truffle.r.nodes.graphics.core.geometry.CoordinateSystem;
import com.oracle.truffle.r.nodes.graphics.core.geometry.Coordinates;

import java.awt.*;
import java.util.stream.IntStream;

/**
 * Able to render a text on {@link Graphics2D}
 */
public class StringDrawableObject extends CoordinatesDrawableObject {
    private final String[] strings;

    public StringDrawableObject(CoordinateSystem coordinateSystem, Coordinates coordinates, String[] strings) {
        super(coordinateSystem, coordinates);
        this.strings = strings;
    }

    @Override
    public void drawOn(Graphics2D g2) {
        Coordinates dstCoordinates = getDstCoordinates();
        int[] xCoords = dstCoordinates.getXCoordinatesAsInts();
        int[] yCoords = dstCoordinates.getYCoordinatesAsInts();
        IntStream.range(0, strings.length).forEach(i -> g2.drawString(strings[i], xCoords[i], yCoords[i]));
    }
}
