/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.utils;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asLogicalVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.chain;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.constant;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.findFirst;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.map;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RError.Message.FIRST_ARGUMENT_MUST_BE_CHARACTER;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_ARG;
import static com.oracle.truffle.r.runtime.RRuntime.LOGICAL_FALSE;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.SetFixedAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class TypeConvert extends RExternalBuiltinNode.Arg5 {

    @Child private SetFixedAttributeNode setLevelsAttrNode = SetFixedAttributeNode.create(RRuntime.LEVELS_ATTR_KEY);

    static {
        Casts casts = new Casts(TypeConvert.class);
        casts.arg(0).mustBe(stringValue(), FIRST_ARGUMENT_MUST_BE_CHARACTER).asStringVector();
        casts.arg(1).mustBe(stringValue(), INVALID_ARG, "'na.strings'").asStringVector();
        casts.arg(2).mapIf(logicalValue(),
                        chain(asLogicalVector()).with(findFirst().logicalElement(LOGICAL_FALSE)).with(toBoolean()).end(),
                        chain(map(constant(LOGICAL_FALSE))).with(toBoolean()).end());
    }

    @Override
    protected RBaseNode getErrorContext() {
        return RError.SHOW_CALLER;
    }

    private static boolean isNA(String s, RAbstractStringVector naStrings) {
        // naStrings are in addition to NA_character_
        if (RRuntime.isNA(s)) {
            return true;
        }
        for (int i = 0; i < naStrings.getLength(); i++) {
            if (s.equals(naStrings.getDataAt(i))) {
                return true;
            }
        }
        return false;
    }

    /*
     * In the next three methods, firstPos is the index of the first element in the vector that is
     * not isNA(elem, naStrings). However, there may be isNA values after that.
     */

    private static RIntVector readIntVector(RAbstractStringVector x, int firstPos, int firstVal, RAbstractStringVector naStrings) {
        int[] data = new int[x.getLength()];
        Arrays.fill(data, 0, firstPos, RRuntime.INT_NA);
        boolean complete = canBeComplete(firstPos);
        data[firstPos] = firstVal;
        for (int i = firstPos + 1; i < data.length; i++) {
            String s = x.getDataAt(i);
            if (isNA(s, naStrings)) {
                data[i] = RRuntime.INT_NA;
                complete = false;
            } else {
                int result = RRuntime.parseInt(s);
                if (result == RRuntime.INT_NA) {
                    throw new NumberFormatException();
                }
                data[i] = result;
            }
        }
        return RDataFactory.createIntVector(data, complete);
    }

    private static RDoubleVector readDoubleVector(RAbstractStringVector x, int firstPos, double firstVal, RAbstractStringVector naStrings) {
        double[] data = new double[x.getLength()];
        Arrays.fill(data, 0, firstPos, RRuntime.DOUBLE_NA);
        boolean complete = canBeComplete(firstPos);
        data[firstPos] = firstVal;
        for (int i = firstPos + 1; i < data.length; i++) {
            String s = x.getDataAt(i);
            boolean isNA = isNA(s, naStrings);
            data[i] = isNA ? RRuntime.DOUBLE_NA : RRuntime.string2doubleNoCheck(s, true);
            complete = complete && !isNA;
        }
        return RDataFactory.createDoubleVector(data, complete);
    }

    private static RLogicalVector readLogicalVector(RAbstractStringVector x, int firstPos, byte firstVal, RAbstractStringVector naStrings) {
        byte[] data = new byte[x.getLength()];
        Arrays.fill(data, 0, firstPos, RRuntime.LOGICAL_NA);
        boolean complete = canBeComplete(firstPos);
        data[firstPos] = firstVal;
        for (int i = firstPos + 1; i < data.length; i++) {
            String s = x.getDataAt(i);
            boolean isNA = isNA(s, naStrings);
            data[i] = isNA ? RRuntime.LOGICAL_NA : RRuntime.string2logicalNoCheck(s, true);
            complete = complete && !isNA;
        }
        return RDataFactory.createLogicalVector(data, complete);
    }

    private static boolean canBeComplete(int firstNonNAPos) {
        return firstNonNAPos == 0 ? RDataFactory.COMPLETE_VECTOR : RDataFactory.INCOMPLETE_VECTOR;
    }

    @Specialization
    @TruffleBoundary
    protected Object typeConvert(RAbstractStringVector x, RAbstractStringVector naStrings, boolean asIs, @SuppressWarnings("unused") Object dec, @SuppressWarnings("unused") Object numeral) {
        if (x.getLength() == 0) {
            return RDataFactory.createEmptyLogicalVector();
        }

        int i = 0;
        while (i < x.getLength() && (x.getDataAt(i).isEmpty() || isNA(x.getDataAt(i), naStrings))) {
            i++;
        }

        if (i == x.getLength()) {
            // all NAs
            byte[] data = new byte[i];
            Arrays.fill(data, RRuntime.LOGICAL_NA);
            return RDataFactory.createLogicalVector(data, RDataFactory.INCOMPLETE_VECTOR);
        }

        String s = x.getDataAt(i);
        if (RRuntime.hasHexPrefix(s)) {
            // this is a mess
            // double takes precedence even if s is a hexadecimal integer
            try {
                double doubleVal = RRuntime.string2doubleNoCheck(s, true);
                return readDoubleVector(x, i, doubleVal, naStrings);
            } catch (NumberFormatException ix) {
                // fall through
            }
        } else {
            try {
                int intVal = RRuntime.string2intNoCheck(s, true);
                return readIntVector(x, i, intVal, naStrings);
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
        }
        // fall through target - conversion to int, double or logical failed

        if (asIs) {
            return x;
        } else {
            // collect levels for a factor result
            TreeMap<String, Integer> levels = new TreeMap<>();
            for (int j = 0; j < x.getLength(); j++) {
                s = x.getDataAt(j);
                if (!isNA(s, naStrings)) {
                    levels.put(s, 0);
                }
            }
            // assign levels IDs
            int pos = 1;
            for (Map.Entry<String, Integer> entry : levels.entrySet()) {
                entry.setValue(pos++);
            }

            int[] data = new int[x.getLength()];
            boolean complete = true;
            for (int j = 0; j < data.length; j++) {
                s = x.getDataAt(j);
                if (!isNA(s, naStrings)) {
                    data[j] = levels.get(s);
                } else {
                    data[j] = RRuntime.INT_NA;
                    complete = false;
                }
            }
            RIntVector res = RDataFactory.createIntVector(data, complete);
            setLevelsAttrNode.execute(res, RDataFactory.createStringVector(levels.keySet().toArray(new String[0]), RDataFactory.COMPLETE_VECTOR));
            return RVector.setVectorClassAttr(res, RDataFactory.createStringVector("factor"));
        }
    }
}
