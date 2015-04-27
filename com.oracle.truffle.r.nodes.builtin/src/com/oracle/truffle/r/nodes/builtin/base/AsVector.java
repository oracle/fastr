/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "as.vector", kind = INTERNAL, parameterNames = {"x", "mode"})
public abstract class AsVector extends RBuiltinNode {

    @Child private CastIntegerNode castInteger;
    @Child private CastDoubleNode castDouble;
    @Child private CastComplexNode castComplex;
    @Child private CastLogicalNode castLogical;
    @Child private CastStringNode castString;
    @Child private CastRawNode castRaw;
    @Child private CastListNode castList;
    @Child private CastSymbolNode castSymbol;
    @Child private CastExpressionNode castExpression;

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    private RIntVector castInteger(VirtualFrame frame, Object operand) {
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castInteger = insert(CastIntegerNodeGen.create(null, false, false, false));
        }
        return (RIntVector) castInteger.executeInt(frame, operand);
    }

    private RDoubleVector castDouble(VirtualFrame frame, Object operand) {
        if (castDouble == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castDouble = insert(CastDoubleNodeGen.create(null, false, false, false));
        }
        return (RDoubleVector) castDouble.executeDouble(frame, operand);
    }

    private RComplexVector castComplex(VirtualFrame frame, Object operand) {
        if (castComplex == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castComplex = insert(CastComplexNodeGen.create(null, false, false, false));
        }
        return (RComplexVector) castComplex.executeComplex(frame, operand);
    }

    private RLogicalVector castLogical(VirtualFrame frame, Object operand) {
        if (castLogical == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castLogical = insert(CastLogicalNodeGen.create(null, false, false, false));
        }
        return (RLogicalVector) castLogical.executeLogical(frame, operand);
    }

    private RStringVector castString(VirtualFrame frame, Object operand) {
        if (castString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castString = insert(CastStringNodeGen.create(null, false, false, false, false));
        }
        return (RStringVector) castString.executeString(frame, operand);
    }

    private RSymbol castSymbol(VirtualFrame frame, Object operand) {
        if (castSymbol == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castSymbol = insert(CastSymbolNodeGen.create(null, false, false, false));
        }
        return (RSymbol) castSymbol.executeSymbol(frame, operand);
    }

    private RExpression castExpression(VirtualFrame frame, Object operand) {
        if (castExpression == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castExpression = insert(CastExpressionNodeGen.create(null, false, false, false));
        }
        return (RExpression) castExpression.executeExpression(frame, operand);
    }

    private RRawVector castRaw(VirtualFrame frame, Object operand) {
        if (castRaw == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castRaw = insert(CastRawNodeGen.create(null, false, false, false));
        }
        return (RRawVector) castRaw.executeRaw(frame, operand);
    }

    private RList castList(VirtualFrame frame, Object operand) {
        if (castList == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castList = insert(CastListNodeGen.create(null, true, false, false));
        }
        return castList.executeList(frame, operand);
    }

    @Specialization
    protected Object asVector(RNull x, @SuppressWarnings("unused") RMissing mode) {
        controlVisibility();
        return x;
    }

    @Specialization(guards = "castToInt(x, mode)")
    protected RAbstractVector asVectorInt(VirtualFrame frame, RAbstractContainer x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castInteger(frame, x);
    }

    @Specialization(guards = "castToDouble(x, mode)")
    protected RAbstractVector asVectorDouble(VirtualFrame frame, RAbstractContainer x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castDouble(frame, x);
    }

    @Specialization(guards = "castToComplex(x, mode)")
    protected RAbstractVector asVectorComplex(VirtualFrame frame, RAbstractContainer x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castComplex(frame, x);
    }

    @Specialization(guards = "castToLogical(x, mode)")
    protected RAbstractVector asVectorLogical(VirtualFrame frame, RAbstractContainer x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castLogical(frame, x);
    }

    @Specialization(guards = "castToString(x, mode)")
    protected RAbstractVector asVectorString(VirtualFrame frame, RAbstractContainer x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castString(frame, x);
    }

    @Specialization(guards = "castToRaw(x, mode)")
    protected RAbstractVector asVectorRaw(VirtualFrame frame, RAbstractContainer x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castRaw(frame, x);
    }

    @Specialization(guards = "castToList(x, mode)")
    protected RAbstractVector asVectorList(VirtualFrame frame, RAbstractContainer x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castList(frame, x);
    }

    @Specialization(guards = "castToList(x, mode)")
    protected RAbstractVector asVectorList(@SuppressWarnings("unused") RNull x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return RDataFactory.createList();
    }

    @Specialization(guards = "castToSymbol(x, mode)")
    protected RSymbol asVectorSymbol(VirtualFrame frame, RAbstractContainer x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castSymbol(frame, x);
    }

    @Specialization(guards = "isSymbol(x, mode)")
    protected RSymbol asVectorSymbol(RSymbol x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return RDataFactory.createSymbol(x.getName());
    }

    protected boolean isSymbol(@SuppressWarnings("unused") RSymbol x, String mode) {
        return RType.Symbol.getName().equals(mode);
    }

    @Specialization(guards = "modeIsAny(mode)")
    protected RAbstractVector asVector(RList x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        RList result = x.copyWithNewDimensions(null);
        result.copyNamesFrom(attrProfiles, x);
        return result;
    }

    @Specialization(guards = "modeIsAny(mode)")
    protected RAbstractVector asVector(RFactor x, @SuppressWarnings("unused") String mode) {
        RVector levels = x.getLevels(attrProfiles);
        RVector result = levels.createEmptySameType(x.getLength(), RDataFactory.COMPLETE_VECTOR);
        RIntVector factorData = x.getVector();
        for (int i = 0; i < result.getLength(); i++) {
            result.transferElementSameType(i, levels, factorData.getDataAt(i) - 1);
        }
        return result;
    }

    @Specialization(guards = "modeIsAny(mode)")
    protected RNull asVector(RNull x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return x;
    }

    @Specialization(guards = "modeIsPairList(mode)")
    protected Object asVectorPairList(RList x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        // TODO implement non-empty element list conversion; this is a placeholder for type test
        if (x.getLength() == 0) {
            return RNull.instance;
        } else {
            throw RError.nyi(getEncapsulatingSourceSection(), "non-empty lists");
        }
    }

    @Specialization(guards = "castToExpression(x, mode)")
    protected RExpression asVectorExpression(VirtualFrame frame, Object x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castExpression(frame, x);
    }

    @Specialization(guards = "modeIsAnyOrMatches(x, mode)")
    protected RAbstractVector asVector(RAbstractVector x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return x.copyWithNewDimensions(null);
    }

    protected boolean castToInt(RAbstractContainer x, String mode) {
        return x.getElementClass() != RInteger.class && RType.Integer.getName().equals(mode);
    }

    protected boolean castToDouble(RAbstractContainer x, String mode) {
        return x.getElementClass() != RDouble.class && (RType.Numeric.getName().equals(mode) || RType.Double.getName().equals(mode));
    }

    protected boolean castToComplex(RAbstractContainer x, String mode) {
        return x.getElementClass() != RComplex.class && RType.Complex.getName().equals(mode);
    }

    protected boolean castToLogical(RAbstractContainer x, String mode) {
        return x.getElementClass() != RLogical.class && RType.Logical.getName().equals(mode);
    }

    protected boolean castToString(RAbstractContainer x, String mode) {
        return x.getElementClass() != RString.class && RType.Character.getName().equals(mode);
    }

    protected boolean castToRaw(RAbstractContainer x, String mode) {
        return x.getElementClass() != RRaw.class && RType.Raw.getName().equals(mode);
    }

    protected boolean castToList(RAbstractContainer x, String mode) {
        return x.getElementClass() != Object.class && RType.List.getName().equals(mode);
    }

    protected boolean castToList(@SuppressWarnings("unused") Object x, String mode) {
        return RType.List.getName().equals(mode);
    }

    protected boolean castToSymbol(RAbstractContainer x, String mode) {
        return x.getElementClass() != Object.class && RType.Symbol.getName().equals(mode);
    }

    protected boolean castToExpression(@SuppressWarnings("unused") Object x, String mode) {
        return RType.Expression.getName().equals(mode);
    }

    protected boolean modeIsAnyOrMatches(RAbstractVector x, String mode) {
        return RType.Any.getName().equals(mode) || RRuntime.classToString(x.getElementClass()).equals(mode) || x.getElementClass() == RDouble.class && RType.Double.getName().equals(mode);
    }

    protected boolean modeIsAny(String mode) {
        return RType.Any.getName().equals(mode);
    }

    protected boolean modeIsPairList(String mode) {
        return RType.PairList.getName().equals(mode);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected RAbstractVector asVectorWrongMode(Object x, Object mode) {
        controlVisibility();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "mode");
    }
}
