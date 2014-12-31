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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.closures.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin(name = "match", kind = INTERNAL, parameterNames = {"x", "table", "nomatch", "incomparables"})
@GenerateNodeFactory
public abstract class Match extends RBuiltinNode {

    private static final long BIG_THRESHOLD = 1000;

    protected abstract RIntVector executeRIntVector(VirtualFrame frame, Object x, Object table, Object noMatch, Object incomparables);

    @Child private CastStringNode castString;
    @Child private CastIntegerNode castInt;

    @Child private Match matchRecursive;

    private final NACheck naCheck = new NACheck();
    private final ConditionProfile bigProfile = ConditionProfile.createBinaryProfile();

    @Override
    public RNode[] getParameterValues() {
        // x, table, nomatch = NA_integer_, incomparables = NULL
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.INT_NA), ConstantNode.create(RNull.instance)};
    }

    private String castString(VirtualFrame frame, Object operand) {
        if (castString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castString = insert(CastStringNodeGen.create(null, false, false, false, false));
        }
        return (String) castString.executeCast(frame, operand);
    }

    private int castInt(VirtualFrame frame, Object operand) {
        if (castInt == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castInt = insert(CastIntegerNodeGen.create(null, false, false, false));
        }
        return (int) castInt.executeCast(frame, operand);
    }

    private RIntVector matchRecursive(VirtualFrame frame, Object x, Object table, Object noMatch, Object incomparables) {
        if (matchRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            matchRecursive = insert(MatchFactory.create(new RNode[4], getBuiltin(), getSuppliedArgsNames()));
        }
        return matchRecursive.executeRIntVector(frame, x, table, noMatch, incomparables);
    }

    @Child private BooleanOperation eq = BinaryCompare.EQUAL.create();

    // FIXME deal incomparables parameter

    @Specialization
    @SuppressWarnings("unused")
    protected RIntVector match(VirtualFrame frame, RNull x, RAbstractVector table, Object nomatchObj, Object incomparables) {
        return RDataFactory.createIntVector(0);
    }

    @Specialization
    @SuppressWarnings("unused")
    protected RIntVector match(VirtualFrame frame, RAbstractVector x, RNull table, Object nomatchObj, Object incomparables) {
        return RDataFactory.createIntVector(x.getLength());
    }

    @Specialization
    protected RIntVector match(VirtualFrame frame, RFactor x, RFactor table, Object nomatchObj, Object incomparables) {
        naCheck.enable(!x.getVector().isComplete() || table.getVector().isComplete());
        return matchRecursive(frame, RClosures.createFactorToVector(x, naCheck), RClosures.createFactorToVector(table, naCheck), nomatchObj, incomparables);
    }

    @Specialization
    @SuppressWarnings("unused")
    protected RIntVector match(VirtualFrame frame, RAbstractIntVector x, RAbstractIntVector table, Object nomatchObj, Object incomparables) {
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

    @Specialization
    @SuppressWarnings("unused")
    protected RIntVector match(VirtualFrame frame, RAbstractDoubleVector x, RAbstractIntVector table, Object nomatchObj, Object incomparables) {
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

    @Specialization
    @SuppressWarnings("unused")
    protected RIntVector match(VirtualFrame frame, RAbstractIntVector x, RAbstractDoubleVector table, Object nomatchObj, Object incomparables) {
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

    @Specialization
    @SuppressWarnings("unused")
    protected RIntVector match(VirtualFrame frame, RAbstractDoubleVector x, RAbstractDoubleVector table, Object nomatchObj, Object incomparables) {
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

    @Specialization()
    @SuppressWarnings("unused")
    protected RIntVector match(VirtualFrame frame, RAbstractIntVector x, RAbstractLogicalVector table, Object nomatchObj, Object incomparables) {
        controlVisibility();
        int nomatch = castInt(frame, nomatchObj);
        int[] result = initResult(x.getLength(), nomatch);
        boolean matchAll = true;
        for (int i = 0; i < result.length; ++i) {
            double xx = x.getDataAt(i);
            boolean match = false;
            for (int k = 0; k < table.getLength(); ++k) {
                if (eq.op(xx, table.getDataAt(k)) == RRuntime.LOGICAL_TRUE) {
                    result[i] = 1;
                    match = true;
                    break;
                }
            }
            matchAll &= match;
        }
        return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
    }

    @TruffleBoundary
    protected RIntVector matchInternal(int nomatch, RAbstractStringVector x, RAbstractStringVector table) {
        int[] result = initResult(x.getLength(), nomatch);
        boolean matchAll = true;
        if (bigProfile.profile(x.getLength() * (long) table.getLength() > BIG_THRESHOLD)) {
            HashMap<String, Integer> hashTable = new HashMap<>(table.getLength());
            for (int i = table.getLength() - 1; i >= 0; i--) {
                String entry = table.getDataAt(i);
                if (!RRuntime.isNA(entry)) {
                    hashTable.put(entry, i);
                }
            }
            for (int i = 0; i < result.length; ++i) {
                String xx = x.getDataAt(i);
                Integer index = hashTable.get(xx);
                if (index != null) {
                    result[i] = index + 1;
                } else {
                    matchAll = false;
                }
            }
        } else {
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
        }
        return RDataFactory.createIntVector(result, setCompleteState(matchAll, nomatch));
    }

    @Specialization
    protected RIntVector match(VirtualFrame frame, RAbstractStringVector x, RAbstractStringVector table, Object nomatchObj, @SuppressWarnings("unused") Object incomparables) {
        controlVisibility();
        int nomatch = castInt(frame, nomatchObj);
        return matchInternal(nomatch, x, table);
    }

    @Specialization
    protected RIntVector match(VirtualFrame frame, RAbstractLogicalVector x, RAbstractStringVector table, Object nomatchObj, Object incomparables) {
        naCheck.enable(x);
        return match(frame, RClosures.createLogicalToStringVector(x, naCheck), table, nomatchObj, incomparables);
    }

    @Specialization
    @SuppressWarnings("unused")
    protected RIntVector match(VirtualFrame frame, RAbstractLogicalVector x, RAbstractLogicalVector table, Object nomatchObj, Object incomparables) {
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

    @Specialization(guards = "!isStringVectorTable")
    @SuppressWarnings("unused")
    protected RIntVector match(VirtualFrame frame, RAbstractStringVector x, RAbstractVector table, Object nomatchObj, Object incomparables) {
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

    @Specialization
    @SuppressWarnings("unused")
    protected RIntVector match(VirtualFrame frame, RAbstractComplexVector x, RAbstractComplexVector table, Object nomatchObj, Object incomparables) {
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

    @Specialization
    @SuppressWarnings("unused")
    protected RIntVector match(RFunction x, Object table, Object nomatchObj, Object incomparables) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.MATCH_VECTOR_ARGS);
    }

    @Specialization
    @SuppressWarnings("unused")
    protected RIntVector match(Object x, RFunction table, Object nomatchObj, Object incomparables) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.MATCH_VECTOR_ARGS);
    }

    protected boolean isStringVectorTable(@SuppressWarnings("unused") RAbstractStringVector x, RAbstractVector table) {
        return table.getElementClass() == String.class;
    }

    private static int[] initResult(int length, int nomatch) {
        int[] result = new int[length];
        Arrays.fill(result, nomatch);
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
