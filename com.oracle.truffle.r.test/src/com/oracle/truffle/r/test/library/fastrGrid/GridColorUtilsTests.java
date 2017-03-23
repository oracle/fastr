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
package com.oracle.truffle.r.test.library.fastrGrid;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.oracle.truffle.r.library.fastrGrid.GridColorUtils;
import com.oracle.truffle.r.library.fastrGrid.device.GridColor;
import com.oracle.truffle.r.test.TestBase;

public class GridColorUtilsTests extends TestBase {
    @Test
    public void convertFromHex() {
        GridColor color = GridColorUtils.gridColorFromString("#FF01FE");
        assertEquals(255, color.getRed());
        assertEquals(1, color.getGreen());
        assertEquals(254, color.getBlue());
        assertEquals(255, color.getAlpha());
    }

    @Test
    public void convertFromHexWithAlpha() {
        GridColor color = GridColorUtils.gridColorFromString("#FF00FE02");
        assertEquals(255, color.getRed());
        assertEquals(0, color.getGreen());
        assertEquals(254, color.getBlue());
        assertEquals(02, color.getAlpha());
    }

    @Test
    public void convertSynonymBlack() {
        GridColor black = GridColorUtils.gridColorFromString("black");
        assertEquals(0, black.getRed());
        assertEquals(0, black.getGreen());
        assertEquals(0, black.getBlue());
        assertEquals(255, black.getAlpha());
    }

    @Test
    public void convertSynonymUpercaseRed() {
        GridColor black = GridColorUtils.gridColorFromString("RED");
        assertEquals(255, black.getRed());
        assertEquals(0, black.getGreen());
        assertEquals(0, black.getBlue());
        assertEquals(255, black.getAlpha());
    }

    @Test
    public void convertSynonymLightGreenWithSpace() {
        GridColor black = GridColorUtils.gridColorFromString("light green");
        assertEquals(0x90, black.getRed());
        assertEquals(0xee, black.getGreen());
        assertEquals(0x90, black.getBlue());
        assertEquals(255, black.getAlpha());
    }

    @Test
    public void convertSynonymLightGreen() {
        GridColor black = GridColorUtils.gridColorFromString("light green");
        assertEquals(0x90, black.getRed());
        assertEquals(0xee, black.getGreen());
        assertEquals(0x90, black.getBlue());
        assertEquals(255, black.getAlpha());
    }

    @Test
    public void convertSynonymTransparent() {
        GridColor transparent = GridColorUtils.gridColorFromString("transparent");
        assertEquals(0, transparent.getAlpha());
    }
}
