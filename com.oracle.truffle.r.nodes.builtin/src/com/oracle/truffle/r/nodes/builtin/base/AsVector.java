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

    private RSymbol castSymbol(VirtualFrame frame, Object operand) {
        if (castSymbol == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castSymbol = insert(CastSymbolNodeFactory.create(null, false, false, false, false));
        }
        return (RSymbol) castSymbol.executeSymbol(frame, operand);
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

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RType.Any.getName())};
    }

    @Specialization
    protected Object asVector(RNull x, @SuppressWarnings("unused") RMissing mode) {
        controlVisibility();
        return x;
    }

    @Specialization(guards = "castToInt")
    protected RAbstractVector asVectorInt(VirtualFrame frame, RAbstractVector x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castInteger(frame, x);
    }

    @Specialization(guards = "castToDouble")
    protected RAbstractVector asVectorDouble(VirtualFrame frame, RAbstractVector x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castDouble(frame, x);
    }

    @Specialization(guards = "castToComplex")
    protected RAbstractVector asVectorComplex(VirtualFrame frame, RAbstractVector x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castComplex(frame, x);
    }

    @Specialization(guards = "castToLogical")
    protected RAbstractVector asVectorLogical(VirtualFrame frame, RAbstractVector x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castLogical(frame, x);
    }

    @Specialization(guards = "castToString")
    protected RAbstractVector asVectorString(VirtualFrame frame, RAbstractVector x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castString(frame, x);
    }

    @Specialization(guards = "castToRaw")
    protected RAbstractVector asVectorRaw(VirtualFrame frame, RAbstractVector x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castRaw(frame, x);
    }

    @Specialization(guards = "castToList")
    protected RAbstractVector asVectorList(VirtualFrame frame, RAbstractVector x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castList(frame, x);
    }

    @Specialization(guards = "castToList")
    protected RAbstractVector asVectorList(VirtualFrame frame, RLanguage x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castList(frame, x);
    }

    @Specialization(guards = "castToList")
    protected RAbstractVector asVectorList(VirtualFrame frame, RExpression x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castList(frame, x);
    }

    @Specialization(guards = "castToList")
    protected RAbstractVector asVectorList(@SuppressWarnings("unused") RNull x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return RDataFactory.createList();
    }

    @Specialization(guards = "castToSymbol")
    protected RSymbol asVectorSymbol(VirtualFrame frame, RAbstractVector x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return castSymbol(frame, x);
    }

    @Specialization(guards = "isSymbol")
    protected RSymbol asVectorSymbol(RSymbol x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return RDataFactory.createSymbol(x.getName());
    }

    protected boolean isSymbol(@SuppressWarnings("unused") RSymbol x, String mode) {
        return mode.equals("symbol");
    }

    @Specialization(guards = "modeIsAny")
    protected RAbstractVector asVector(RList x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        RList result = x.copyWithNewDimensions(null);
        result.copyNamesFrom(x);
        return result;
    }

    @Specialization(guards = "modeIsAnyOrMatches")
    protected RAbstractVector asVector(RAbstractVector x, @SuppressWarnings("unused") String mode) {
        controlVisibility();
        return x.copyWithNewDimensions(null);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "invalidMode")
    protected RAbstractVector asVectorWrongMode(RAbstractVector x, String mode) {
        controlVisibility();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "mode");
    }

    protected boolean castToInt(RAbstractVector x, String mode) {
        return x.getElementClass() != RInt.class && RType.Integer.getName().equals(mode);
    }

    protected boolean castToDouble(RAbstractVector x, String mode) {
        return x.getElementClass() != RDouble.class && (RType.Numeric.getName().equals(mode) || RType.Double.getName().equals(mode));
    }

    protected boolean castToComplex(RAbstractVector x, String mode) {
        return x.getElementClass() != RComplex.class && RType.Complex.getName().equals(mode);
    }

    protected boolean castToLogical(RAbstractVector x, String mode) {
        return x.getElementClass() != RLogical.class && RType.Logical.getName().equals(mode);
    }

    protected boolean castToString(RAbstractVector x, String mode) {
        return x.getElementClass() != RString.class && RType.Character.getName().equals(mode);
    }

    protected boolean castToRaw(RAbstractVector x, String mode) {
        return x.getElementClass() != RRaw.class && RType.Raw.getName().equals(mode);
    }

    protected boolean castToList(RAbstractVector x, String mode) {
        return x.getElementClass() != Object.class && mode.equals("list");
    }

    protected boolean castToList(@SuppressWarnings("unused") RLanguage x, String mode) {
        return mode.equals("list");
    }

    protected boolean castToList(@SuppressWarnings("unused") RExpression x, String mode) {
        return mode.equals("list");
    }

    protected boolean castToList(@SuppressWarnings("unused") RNull x, String mode) {
        return mode.equals("list");
    }

    protected boolean castToSymbol(RAbstractVector x, String mode) {
        return x.getElementClass() != Object.class && mode.equals("symbol");
    }

    protected boolean modeIsAnyOrMatches(RAbstractVector x, String mode) {
        return RType.Any.getName().equals(mode) || RRuntime.classToString(x.getElementClass()).equals(mode) || x.getElementClass() == RDouble.class && RType.Double.getName().equals(mode);
    }

    protected boolean modeIsAny(@SuppressWarnings("unused") RAbstractVector x, String mode) {
        return RType.Any.getName().equals(mode);
    }

    protected boolean invalidMode(@SuppressWarnings("unused") RAbstractVector x, String mode) {
        RType modeType = RType.fromString(mode);
        if (modeType == null) {
            return true;
        }
        switch (modeType) {
            case Any:
            case Array:
            case Character:
            case Complex:
            case Double:
            case Integer:
            case List:
            case Logical:
            case Matrix:
            case Numeric:
            case Raw:
            case Symbol:
                return false;
            default:
                return true;
        }
    }
}
