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

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.SUBSTITUTE;

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

@RBuiltin(name = "as.vector", kind = SUBSTITUTE)
// TODO should be INTERNAL
public abstract class AsVector extends RBuiltinNode {

    private static final String[] PARAMETER_NAMES = new String[]{"x", "mode"};

    @Child private CastIntegerNode castInteger;
    @Child private CastDoubleNode castDouble;
    @Child private CastComplexNode castComplex;
    @Child private CastLogicalNode castLogical;
    @Child private CastStringNode castString;
    @Child private CastRawNode castRaw;
    @Child private CastListNode castList;

    private RIntVector castInteger(VirtualFrame frame, Object operand) {
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castInteger = insert(CastIntegerNodeFactory.create(null, false, false, false));
        }
        return (RIntVector) castInteger.executeInt(frame, operand);
    }

    private RDoubleVector castDouble(VirtualFrame frame, Object operand) {
        if (castDouble == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castDouble = insert(CastDoubleNodeFactory.create(null, false, false, false));
        }
        return (RDoubleVector) castDouble.executeDouble(frame, operand);
    }

    private RComplexVector castComplex(VirtualFrame frame, Object operand) {
        if (castComplex == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castComplex = insert(CastComplexNodeFactory.create(null, false, false, false));
        }
        return (RComplexVector) castComplex.executeComplex(frame, operand);
    }

    private RLogicalVector castLogical(VirtualFrame frame, Object operand) {
        if (castLogical == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castLogical = insert(CastLogicalNodeFactory.create(null, false, false, false));
        }
        return (RLogicalVector) castLogical.executeLogical(frame, operand);
    }

    private RStringVector castString(VirtualFrame frame, Object operand) {
        if (castString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castString = insert(CastStringNodeFactory.create(null, false, false, false, false));
        }
        return (RStringVector) castString.executeString(frame, operand);
    }

    private RRawVector castRaw(VirtualFrame frame, Object operand) {
        if (castRaw == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castRaw = insert(CastRawNodeFactory.create(null, false, false, false));
        }
        return (RRawVector) castRaw.executeRaw(frame, operand);
    }

    private RList castList(VirtualFrame frame, Object operand) {
        if (castList == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castList = insert(CastListNodeFactory.create(null, true, false, false));
        }
        return castList.executeList(frame, operand);
    }

    private RSymbol castSymbol(VirtualFrame frame, Object operand) {
        RStringVector vec = castString(frame, operand);
        return RDataFactory.createSymbol(vec.getDataAt(0));
    }

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.TYPE_ANY)};
    }

    @Specialization(order = 10)
    public Object asVector(RNull x, @SuppressWarnings("unused") RMissing mode) {
        controlVisibility();
        return x;
    }

    @Specialization(order = 100, guards = "castToInt")
    public RAbstractVector asVectorInt(VirtualFrame frame, RAbstractVector x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castInteger(frame, x);
    }

    @Specialization(order = 200, guards = "castToDouble")
    public RAbstractVector asVectorDouble(VirtualFrame frame, RAbstractVector x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castDouble(frame, x);
    }

    @Specialization(order = 300, guards = "castToComplex")
    public RAbstractVector asVectorComplex(VirtualFrame frame, RAbstractVector x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castComplex(frame, x);
    }

    @Specialization(order = 400, guards = "castToLogical")
    public RAbstractVector asVectorLogical(VirtualFrame frame, RAbstractVector x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castLogical(frame, x);
    }

    @Specialization(order = 500, guards = "castToString")
    public RAbstractVector asVectorString(VirtualFrame frame, RAbstractVector x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castString(frame, x);
    }

    @Specialization(order = 600, guards = "castToRaw")
    public RAbstractVector asVectorRaw(VirtualFrame frame, RAbstractVector x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castRaw(frame, x);
    }

    @Specialization(order = 700, guards = "castToList")
    public RAbstractVector asVectorList(VirtualFrame frame, RAbstractVector x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castList(frame, x);
    }

    @Specialization(order = 800, guards = "castToSymbol")
    public RSymbol asVectorSymbol(VirtualFrame frame, RAbstractVector x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castSymbol(frame, x);
    }

    @Specialization(order = 1000)
    public RAbstractVector asVector(RList x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        RList result = x.copyWithNewDimensions(null);
        result.copyNamesFrom(x);
        return result;
    }

    @Specialization(order = 1001, guards = "modeIsAnyOrMatches")
    public RAbstractVector asVector(RAbstractVector x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return x.copyWithNewDimensions(null);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 1002, guards = "invalidMode")
    public RAbstractVector asVectorWrongMode(RAbstractVector x, String mode) {
        controlVisibility();
        CompilerDirectives.transferToInterpreter();
        throw RError.getInvalidMode(getEncapsulatingSourceSection());
    }

    protected boolean castToInt(RAbstractVector x, String mode) {
        return x.getElementClass() != RInt.class && RRuntime.TYPE_INTEGER.equals(mode);
    }

    protected boolean castToDouble(RAbstractVector x, String mode) {
        return x.getElementClass() != RDouble.class && (RRuntime.TYPE_NUMERIC.equals(mode) || RRuntime.TYPE_DOUBLE.equals(mode));
    }

    protected boolean castToComplex(RAbstractVector x, String mode) {
        return x.getElementClass() != RComplex.class && RRuntime.TYPE_COMPLEX.equals(mode);
    }

    protected boolean castToLogical(RAbstractVector x, String mode) {
        return x.getElementClass() != RLogical.class && RRuntime.TYPE_LOGICAL.equals(mode);
    }

    protected boolean castToString(RAbstractVector x, String mode) {
        return x.getElementClass() != RString.class && RRuntime.TYPE_CHARACTER.equals(mode);
    }

    protected boolean castToRaw(RAbstractVector x, String mode) {
        return x.getElementClass() != RRaw.class && RRuntime.TYPE_RAW.equals(mode);
    }

    protected boolean castToList(RAbstractVector x, String mode) {
        return x.getElementClass() != Object.class && mode.equals("list");
    }

    protected boolean castToSymbol(RAbstractVector x, String mode) {
        return x.getElementClass() != Object.class && mode.equals("symbol");
    }

    protected boolean modeIsAnyOrMatches(RAbstractVector x, String mode) {
        return RRuntime.TYPE_ANY.equals(mode) || RRuntime.classToString(x.getElementClass()).equals(mode) || x.getElementClass() == RDouble.class && RRuntime.TYPE_DOUBLE.equals(mode);
    }

    protected boolean invalidMode(@SuppressWarnings("unused") RAbstractVector x, String mode) {
        return !RRuntime.TYPE_ANY.equals(mode) && !RRuntime.TYPE_ARRAY.equals(mode) && !RRuntime.TYPE_CHARACTER.equals(mode) && !RRuntime.TYPE_COMPLEX.equals(mode) &&
                        !RRuntime.TYPE_DOUBLE.equals(mode) && !RRuntime.TYPE_INTEGER.equals(mode) && !RRuntime.TYPE_LIST.equals(mode) && !RRuntime.TYPE_LOGICAL.equals(mode) &&
                        !RRuntime.TYPE_MATRIX.equals(mode) && !RRuntime.TYPE_NUMERIC.equals(mode) && !RRuntime.TYPE_RAW.equals(mode) && !RRuntime.TYPE_SYMBOL.equals(mode);
    }

}
