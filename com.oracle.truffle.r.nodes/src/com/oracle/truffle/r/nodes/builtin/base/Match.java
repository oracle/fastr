/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;

@RBuiltin(".Internal.match")
public abstract class Match extends RBuiltinNode {

    @Child private CastStringNode castString;
    @Child private CastIntegerNode castInt;

    private String castString(VirtualFrame frame, Object operand) {
        if (castString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castString = insert(CastStringNodeFactory.create(null, false, false, false, false));
        }
        return (String) castString.executeCast(frame, operand);
    }

    private int castInt(VirtualFrame frame, Object operand) {
        if (castInt == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castInt = insert(CastIntegerNodeFactory.create(null, false, false, false));
        }
        return (int) castInt.executeCast(frame, operand);
    }

    @Child protected BooleanOperation eq = BinaryCompare.EQUAL.create();

    // FIXME deal incomparables parameter

    @Specialization(order = 5)
    @SuppressWarnings("unused")
    public RIntVector match(VirtualFrame frame, RAbstractIntVector x, RAbstractIntVector table, Object nomatchObj, Object incomparables) {
        controlVisibility();
        int nomatch = castInt(frame, nomatchObj);
        int[] result = initResult(x.getLength(), nomatch);
        boolean matchAll = true;
        for (int i = 0; i < result.length; ++i) {
            int xx = x.getDataAt(i);
            boolean match = false;
            for (int k = 0; k < table.getLength(); ++k) {
                if (eq.op(xx, table.getDataAt(k)) == RRuntime.LOGICAL_TRUE) {
                    result[i] = k + 1;
                    match = true;
                    break;
                }
            }
            matchAll &= match;
        }
        return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
    }

    @Specialization(order = 6)
    @SuppressWarnings("unused")
    public RIntVector match(VirtualFrame frame, RAbstractDoubleVector x, RAbstractIntVector table, Object nomatchObj, Object incomparables) {
        controlVisibility();
        int nomatch = castInt(frame, nomatchObj);
        int[] result = initResult(x.getLength(), nomatch);
        boolean matchAll = true;
        for (int i = 0; i < result.length; ++i) {
            double xx = x.getDataAt(i);
            boolean match = false;
            for (int k = 0; k < table.getLength(); ++k) {
                if (eq.op(xx, RRuntime.int2double(table.getDataAt(k))) == RRuntime.LOGICAL_TRUE) {
                    result[i] = k + 1;
                    match = true;
                    break;
                }
            }
            matchAll &= match;
        }
        return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
    }

    @Specialization(order = 10)
    @SuppressWarnings("unused")
    public RIntVector match(VirtualFrame frame, RAbstractIntVector x, RAbstractDoubleVector table, Object nomatchObj, Object incomparables) {
        controlVisibility();
        int nomatch = castInt(frame, nomatchObj);
        int[] result = initResult(x.getLength(), nomatch);
        boolean matchAll = true;
        for (int i = 0; i < result.length; ++i) {
            double xx = RRuntime.int2double(x.getDataAt(i));
            boolean match = false;
            for (int k = 0; k < table.getLength(); ++k) {
                if (eq.op(xx, table.getDataAt(k)) == RRuntime.LOGICAL_TRUE) {
                    result[i] = k + 1;
                    match = true;
                    break;
                }
            }
            matchAll &= match;
        }
        return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
    }

    @Specialization(order = 11)
    @SuppressWarnings("unused")
    public RIntVector match(VirtualFrame frame, RAbstractDoubleVector x, RAbstractDoubleVector table, Object nomatchObj, Object incomparables) {
        controlVisibility();
        int nomatch = castInt(frame, nomatchObj);
        int[] result = initResult(x.getLength(), nomatch);
        boolean matchAll = true;
        for (int i = 0; i < result.length; ++i) {
            double xx = x.getDataAt(i);
            boolean match = false;
            for (int k = 0; k < table.getLength(); ++k) {
                if (eq.op(xx, table.getDataAt(k)) == RRuntime.LOGICAL_TRUE) {
                    result[i] = k + 1;
                    match = true;
                    break;
                }
            }
            matchAll &= match;
        }
        return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
    }

    @Specialization(order = 21)
    @SuppressWarnings("unused")
    public RIntVector match(VirtualFrame frame, RAbstractStringVector x, RAbstractStringVector table, Object nomatchObj, Object incomparables) {
        controlVisibility();
        int nomatch = castInt(frame, nomatchObj);
        int[] result = initResult(x.getLength(), nomatch);
        boolean matchAll = true;
        for (int i = 0; i < result.length; ++i) {
            String xx = x.getDataAt(i);
            boolean match = false;
            for (int k = 0; k < table.getLength(); ++k) {
                if (eq.op(xx, table.getDataAt(k)) == RRuntime.LOGICAL_TRUE) {
                    result[i] = k + 1;
                    match = true;
                    break;
                }
            }
            matchAll &= match;
        }
        return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
    }

    @Specialization(order = 15)
    @SuppressWarnings("unused")
    public RIntVector match(VirtualFrame frame, RAbstractLogicalVector x, RAbstractLogicalVector table, Object nomatchObj, Object incomparables) {
        controlVisibility();
        int nomatch = castInt(frame, nomatchObj);
        int[] result = initResult(x.getLength(), nomatch);
        boolean matchAll = true;
        for (int i = 0; i < result.length; ++i) {
            byte xx = x.getDataAt(i);
            boolean match = false;
            for (int k = 0; k < table.getLength(); ++k) {
                if (eq.op(xx, table.getDataAt(k)) == RRuntime.LOGICAL_TRUE) {
                    result[i] = k + 1;
                    break;
                }
            }
            matchAll &= match;
        }
        return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
    }

    @Specialization(order = 22, guards = "!isStringVectorX")
    @SuppressWarnings("unused")
    public RIntVector match(VirtualFrame frame, RAbstractStringVector x, RAbstractVector table, Object nomatchObj, Object incomparables) {
        controlVisibility();
        int nomatch = castInt(frame, nomatchObj);
        int[] result = initResult(x.getLength(), nomatch);
        boolean matchAll = true;
        for (int i = 0; i < result.length; ++i) {
            String xx = x.getDataAt(i);
            boolean match = false;
            for (int k = 0; k < table.getLength(); ++k) {
                if (eq.op(xx, castString(frame, table.getDataAtAsObject(k))) == RRuntime.LOGICAL_TRUE) {
                    result[i] = k + 1;
                    match = true;
                    break;
                }
            }
            matchAll &= match;
        }
        return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
    }

    @Specialization(order = 25)
    @SuppressWarnings("unused")
    public RIntVector match(VirtualFrame frame, RAbstractComplexVector x, RAbstractComplexVector table, Object nomatchObj, Object incomparables) {
        controlVisibility();
        int nomatch = castInt(frame, nomatchObj);
        int[] result = initResult(x.getLength(), nomatch);
        boolean matchAll = true;
        for (int i = 0; i < result.length; ++i) {
            RComplex xx = x.getDataAt(i);
            boolean match = false;
            for (int k = 0; k < table.getLength(); ++k) {
                if (eq.op(xx, table.getDataAt(k)) == RRuntime.LOGICAL_TRUE) {
                    result[i] = k + 1;
                    match = true;
                    break;
                }
            }
            matchAll &= match;
        }
        return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
    }

    @Specialization(order = 100)
    @SuppressWarnings("unused")
    public RIntVector match(RFunction x, Object table, Object nomatchObj, Object incomparables) {
        throw RError.getMatchVectorArgs(getEncapsulatingSourceSection());
    }

    @Specialization(order = 101)
    @SuppressWarnings("unused")
    public RIntVector match(Object x, RFunction table, Object nomatchObj, Object incomparables) {
        throw RError.getMatchVectorArgs(getEncapsulatingSourceSection());
    }

    protected boolean isStringVectorX(RAbstractVector x) {
        return x.getElementClass() == String.class;
    }

    private static int[] initResult(int length, int nomatch) {
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = nomatch;
        }
        return result;
    }

    /**
     * Set the "complete" status. If {@code nomatch} is not NA (uncommon), then the result vector is
     * always COMPLETE, otherwise it is INCOMPLETE unless everything matched.
     */
    private static boolean setCompleteState(boolean matchAll, int nomatch) {
        return nomatch != RRuntime.INT_NA || matchAll ? RDataFactory.COMPLETE_VECTOR : RDataFactory.INCOMPLETE_VECTOR;
    }
}
