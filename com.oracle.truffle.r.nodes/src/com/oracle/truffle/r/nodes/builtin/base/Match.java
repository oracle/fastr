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
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;

@RBuiltin("match")
public abstract class Match extends RBuiltinNode {

    private static final Object[] PARAMETER_NAMES = new Object[]{"x", "table", "nomatch", "incomparables"};

    @Child private CastStringNode castString;

    private String castString(VirtualFrame frame, Object operand) {
        if (castString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castString = insert(CastStringNodeFactory.create(null, false, false, false));
        }
        return (String) castString.executeCast(frame, operand);
    }

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.INT_NA), ConstantNode.create(RNull.instance)};
    }

    @Child protected BooleanOperation eq = BinaryCompare.EQUAL.create();

    // FIXME deal with nomatch and incomparables parameters
    // FIXME deal with NA etc.

    @Specialization(order = 5)
    @SuppressWarnings("unused")
    public RLogicalVector match(RAbstractIntVector x, RAbstractIntVector table, int nomatch, Object incomparables) {
        controlVisibility();
        byte[] result = new byte[x.getLength()]; // pre-initialised with LOGICAL_FALSE (0)
        for (int i = 0; i < result.length; ++i) {
            int xx = x.getDataAt(i);
            for (int k = 0; k < table.getLength(); ++k) {
                if (eq.op(xx, table.getDataAt(k)) == RRuntime.LOGICAL_TRUE) {
                    result[i] = RRuntime.LOGICAL_TRUE;
                    break;
                }
            }
        }
        return RDataFactory.createLogicalVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(order = 6)
    @SuppressWarnings("unused")
    public RLogicalVector match(RAbstractDoubleVector x, RAbstractIntVector table, int nomatch, Object incomparables) {
        controlVisibility();
        byte[] result = new byte[x.getLength()]; // pre-initialised with LOGICAL_FALSE (0)
        for (int i = 0; i < result.length; ++i) {
            double xx = x.getDataAt(i);
            for (int k = 0; k < table.getLength(); ++k) {
                if (eq.op(xx, RRuntime.int2double(table.getDataAt(k))) == RRuntime.LOGICAL_TRUE) {
                    result[i] = RRuntime.LOGICAL_TRUE;
                    break;
                }
            }
        }
        return RDataFactory.createLogicalVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(order = 10)
    @SuppressWarnings("unused")
    public RLogicalVector match(RAbstractIntVector x, RAbstractDoubleVector table, int nomatch, Object incomparables) {
        controlVisibility();
        byte[] result = new byte[x.getLength()]; // pre-initialised with LOGICAL_FALSE (0)
        for (int i = 0; i < result.length; ++i) {
            double xx = RRuntime.int2double(x.getDataAt(i));
            for (int k = 0; k < table.getLength(); ++k) {
                if (eq.op(xx, table.getDataAt(k)) == RRuntime.LOGICAL_TRUE) {
                    result[i] = RRuntime.LOGICAL_TRUE;
                    break;
                }
            }
        }
        return RDataFactory.createLogicalVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(order = 11)
    @SuppressWarnings("unused")
    public RLogicalVector match(RAbstractDoubleVector x, RAbstractDoubleVector table, int nomatch, Object incomparables) {
        controlVisibility();
        byte[] result = new byte[x.getLength()]; // pre-initialised with LOGICAL_FALSE (0)
        for (int i = 0; i < result.length; ++i) {
            double xx = x.getDataAt(i);
            for (int k = 0; k < table.getLength(); ++k) {
                if (eq.op(xx, table.getDataAt(k)) == RRuntime.LOGICAL_TRUE) {
                    result[i] = RRuntime.LOGICAL_TRUE;
                    break;
                }
            }
        }
        return RDataFactory.createLogicalVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(order = 21)
    @SuppressWarnings("unused")
    public RLogicalVector match(RAbstractStringVector x, RAbstractStringVector table, int nomatch, Object incomparables) {
        controlVisibility();
        byte[] result = new byte[x.getLength()]; // pre-initialised with LOGICAL_FALSE (0)
        for (int i = 0; i < result.length; ++i) {
            String xx = x.getDataAt(i);
            for (int k = 0; k < table.getLength(); ++k) {
                if (eq.op(xx, table.getDataAt(k)) == RRuntime.LOGICAL_TRUE) {
                    result[i] = RRuntime.LOGICAL_TRUE;
                    break;
                }
            }
        }
        return RDataFactory.createLogicalVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(order = 15)
    @SuppressWarnings("unused")
    public RLogicalVector match(RAbstractLogicalVector x, RAbstractLogicalVector table, int nomatch, Object incomparables) {
        controlVisibility();
        byte[] result = new byte[x.getLength()]; // pre-initialised with LOGICAL_FALSE (0)
        for (int i = 0; i < result.length; ++i) {
            byte xx = x.getDataAt(i);
            for (int k = 0; k < table.getLength(); ++k) {
                if (eq.op(xx, table.getDataAt(k)) == RRuntime.LOGICAL_TRUE) {
                    result[i] = RRuntime.LOGICAL_TRUE;
                    break;
                }
            }
        }
        return RDataFactory.createLogicalVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(order = 22, guards = "!isStringVectorX")
    @SuppressWarnings("unused")
    public RLogicalVector match(VirtualFrame frame, RAbstractStringVector x, RAbstractVector table, int nomatch, Object incomparables) {
        controlVisibility();
        byte[] result = new byte[x.getLength()]; // pre-initialised with LOGICAL_FALSE (0)
        for (int i = 0; i < result.length; ++i) {
            String xx = x.getDataAt(i);
            for (int k = 0; k < table.getLength(); ++k) {
                if (eq.op(xx, castString(frame, table.getDataAtAsObject(k))) == RRuntime.LOGICAL_TRUE) {
                    result[i] = RRuntime.LOGICAL_TRUE;
                    break;
                }
            }
        }
        return RDataFactory.createLogicalVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(order = 25)
    @SuppressWarnings("unused")
    public RLogicalVector match(RAbstractComplexVector x, RAbstractComplexVector table, int nomatch, Object incomparables) {
        controlVisibility();
        byte[] result = new byte[x.getLength()]; // pre-initialised with LOGICAL_FALSE (0)
        for (int i = 0; i < result.length; ++i) {
            RComplex xx = x.getDataAt(i);
            for (int k = 0; k < table.getLength(); ++k) {
                if (eq.op(xx, table.getDataAt(k)) == RRuntime.LOGICAL_TRUE) {
                    result[i] = RRuntime.LOGICAL_TRUE;
                    break;
                }
            }
        }
        return RDataFactory.createLogicalVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(order = 100)
    @SuppressWarnings("unused")
    public RLogicalVector match(RFunction x, Object table, int nomatch, Object incomparables) {
        throw RError.getMatchVectorArgs(getEncapsulatingSourceSection());
    }

    @Specialization(order = 101)
    @SuppressWarnings("unused")
    public RLogicalVector match(Object x, RFunction table, int nomatch, Object incomparables) {
        throw RError.getMatchVectorArgs(getEncapsulatingSourceSection());
    }

    protected boolean isStringVectorX(RAbstractVector x) {
        return x.getElementClass() == String.class;
    }
}
