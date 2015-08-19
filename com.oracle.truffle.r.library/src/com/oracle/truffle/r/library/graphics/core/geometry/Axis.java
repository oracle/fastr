/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2014, The R Core Team
 * Copyright (c) 2002--2010, The R Foundation
 * Copyright (C) 2005--2006, Morten Welinder
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.graphics.core.geometry;

public final class Axis {
    private final double minValue;
    private final double maxValue;
    private final AxisDirection direction;

    public Axis(double minValue, double maxValue, AxisDirection direction) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.direction = direction;
    }

    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public AxisDirection getDirection() {
        return direction;
    }

    public double getRange() {
        return maxValue - minValue;
    }
}
