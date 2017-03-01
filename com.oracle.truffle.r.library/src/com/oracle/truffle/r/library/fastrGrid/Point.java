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

public final class Point {
    final double x;
    final double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public static Point fromUnits(UnitToInchesNode unitToInches, RAbstractVector x, RAbstractVector y, int index, UnitConversionContext ctx) {
        double newX = unitToInches.convertX(x, index, ctx);
        double newY = unitToInches.convertY(y, index, ctx);
        return new Point(newX, newY);
    }

    public Point justify(double width, double height, double hjust, double vjust) {
        return new Point(GridUtils.justify(x, width, hjust), GridUtils.justify(y, height, vjust));
    }

    public boolean isFinite() {
        return Double.isFinite(x) && Double.isFinite(y);
    }
}
