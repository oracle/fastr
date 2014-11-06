/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

// TODO: turn into .External2
// TODO: cover remaining cases (in particular complex numbers)
@RBuiltin(name = "type.convert", kind = INTERNAL, parameterNames = {"x", "na.strings", "as.is", "dec", "numeral"})
public abstract class TypeConvert extends RBuiltinNode {

    private static boolean isNA(String s, RAbstractStringVector naStrings) {
        for (int i = 0; i < naStrings.getLength(); i++) {
            if (s.equals(naStrings.getDataAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static RIntVector readIntVector(RAbstractStringVector x, int firstPos, int firstVal, RAbstractStringVector naStrings) {
        int[] data = new int[x.getLength()];
        Arrays.fill(data, 0, firstPos, RRuntime.INT_NA);
        data[firstPos] = firstVal;
        for (int i = firstPos + 1; i < data.length; i++) {
            String s = x.getDataAt(i);
            data[i] = isNA(s, naStrings) ? RRuntime.INT_NA : RRuntime.string2intNoCheck(s, true);
        }
        return RDataFactory.createIntVector(data, firstPos > 0 ? RDataFactory.INCOMPLETE_VECTOR : RDataFactory.COMPLETE_VECTOR);
    }

    private static RDoubleVector readDoubleVector(RAbstractStringVector x, int firstPos, double firstVal, RAbstractStringVector naStrings) {
        double[] data = new double[x.getLength()];
        Arrays.fill(data, 0, firstPos, RRuntime.DOUBLE_NA);
        data[firstPos] = firstVal;
        for (int i = firstPos + 1; i < data.length; i++) {
            String s = x.getDataAt(i);
            data[i] = isNA(s, naStrings) ? RRuntime.DOUBLE_NA : RRuntime.string2doubleNoCheck(s, true);
        }
        return RDataFactory.createDoubleVector(data, firstPos > 0 ? RDataFactory.INCOMPLETE_VECTOR : RDataFactory.COMPLETE_VECTOR);
    }

    private static RLogicalVector readLogicalVector(RAbstractStringVector x, int firstPos, byte firstVal, RAbstractStringVector naStrings) {
        byte[] data = new byte[x.getLength()];
        Arrays.fill(data, 0, firstPos, RRuntime.LOGICAL_NA);
        data[firstPos] = firstVal;
        for (int i = firstPos + 1; i < data.length; i++) {
            String s = x.getDataAt(i);
            data[i] = isNA(s, naStrings) ? RRuntime.LOGICAL_NA : RRuntime.string2logicalNoCheck(s, true);
        }
        return RDataFactory.createLogicalVector(data, firstPos > 0 ? RDataFactory.INCOMPLETE_VECTOR : RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    @TruffleBoundary
    protected Object typeConvert(RAbstractStringVector x, RAbstractStringVector naStrings, byte asIs, @SuppressWarnings("unused") String numeral) {
        controlVisibility();
        if (x.getLength() == 0) {
            return RDataFactory.createEmptyLogicalVector();
        }

        int i = 0;
        while (isNA(x.getDataAt(i), naStrings) && i < x.getLength()) {
            i++;
        }

        if (i == x.getLength()) {
            // all NAs
            byte[] data = new byte[i];
            Arrays.fill(data, RRuntime.LOGICAL_NA);
            return RDataFactory.createLogicalVector(data, RDataFactory.INCOMPLETE_VECTOR);
        }

        String s = x.getDataAt(i);
        try {
            int intVal = RRuntime.string2intNoCheck(s, true);
            try {
                return readIntVector(x, i, intVal, naStrings);
            } catch (NumberFormatException lx) {
                // fall through
            }
        } catch (NumberFormatException ix) {
            try {
                double doubleVal = RRuntime.string2doubleNoCheck(s, true);
                try {
                    return readDoubleVector(x, i, doubleVal, naStrings);
                } catch (NumberFormatException lx) {
                    // fall through
                }
            } catch (NumberFormatException dx) {
                try {
                    byte logicalVal = RRuntime.string2logicalNoCheck(s, true);
                    try {
                        return readLogicalVector(x, i, logicalVal, naStrings);
                    } catch (NumberFormatException lx) {
                        // fall through
                    }
                } catch (NumberFormatException lx) {
                    // fall through
                }
            }
        }

        // fall through target - conversion to int, double or logical failed

        if (asIs == RRuntime.LOGICAL_TRUE) {
            return x;
        } else {
            // create a factor
            TreeSet<String> levels = new TreeSet<>();
            for (int j = 0; j < x.getLength(); j++) {
                s = x.getDataAt(j);
                if (!isNA(s, naStrings)) {
                    levels.add(s);
                }
            }
            String[] levelsArray = new String[levels.size()];
            levels.toArray(levelsArray);

            int[] data = new int[x.getLength()];
            boolean complete = true;
            for (int j = 0; j < data.length; j++) {
                s = x.getDataAt(j);
                if (!isNA(s, naStrings)) {
                    for (int k = 0; k < levelsArray.length; k++) {
                        if (levelsArray[k].equals(s)) {
                            data[j] = k + 1;
                            break;
                        }
                    }
                } else {
                    data[j] = RRuntime.INT_NA;
                    complete = false;
                }
            }
            RIntVector res = RDataFactory.createIntVector(data, complete);
            res.setAttr("levels", RDataFactory.createStringVector(levelsArray, RDataFactory.COMPLETE_VECTOR));
            res.setAttr("class", RDataFactory.createStringVector("factor"));
            return RDataFactory.createFactor(res);
        }
    }
}
