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
package com.oracle.truffle.r.library.fastrGrid;

import com.oracle.truffle.r.library.fastrGrid.Unit.UnitConversionContext;
import com.oracle.truffle.r.library.fastrGrid.Unit.UnitToInchesNode;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public final class Size {
    private final double width;
    private final double height;

    public Size(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public static Size fromUnits(UnitToInchesNode unitToInches, RAbstractVector wVec, RAbstractVector hVec, int index, UnitConversionContext conversionCtx) {
        double w = unitToInches.convertWidth(wVec, index, conversionCtx);
        double h = unitToInches.convertHeight(hVec, index, conversionCtx);
        return new Size(w, h);
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public boolean isFinite() {
        return Double.isFinite(width) && Double.isFinite(height);
    }
}
