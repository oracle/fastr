/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 2001-3 Paul Murrell
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.fastrGrid;

import static com.oracle.truffle.r.runtime.nmath.RMath.fmax2;
import static com.oracle.truffle.r.runtime.nmath.RMath.fmin2;

import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.r.library.fastrGrid.Unit.UnitLengthNode;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

final class GridUtils {
    private GridUtils() {
        // only static members
    }

    static double justify(double coord, double size, double justification) {
        // justification is supposed to be either between 0 and 1
        return coord - size * justification;
    }

    /**
     * Returns the amount of justification required. I.e. transforms the justification from value
     * between 0 and 1 to the value within size.
     */
    static double justification(double size, double justification) {
        return -size * justification;
    }

    static double getDataAtMod(RAbstractDoubleVector vec, int idx) {
        return vec.getDataAt(idx % vec.getLength());
    }

    @ExplodeLoop
    static int maxLength(UnitLengthNode unitLength, RAbstractVector... units) {
        int result = 0;
        for (RAbstractVector unit : units) {
            result = Math.max(result, unitLength.execute(unit));
        }
        return result;
    }

    @ExplodeLoop
    static double fmax(double firstVal, double... vals) {
        double result = firstVal;
        for (double val : vals) {
            result = fmax2(result, val);
        }
        return result;
    }

    @ExplodeLoop
    static double fmin(double firstVal, double... vals) {
        double result = firstVal;
        for (double val : vals) {
            result = fmin2(result, val);
        }
        return result;
    }
}
