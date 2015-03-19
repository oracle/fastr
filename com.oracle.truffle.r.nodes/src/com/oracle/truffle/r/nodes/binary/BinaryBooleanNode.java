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
package com.oracle.truffle.r.nodes.binary;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.Node.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.CastLogicalNode.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.conn.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.closures.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.na.*;

import static com.oracle.truffle.r.runtime.RRuntime.*;

@SuppressWarnings("unused")
public abstract class BinaryBooleanNode extends RBuiltinNode {

    protected abstract Object execute(VirtualFrame frame, Object left, Object right);

    private final BooleanOperationFactory factory;
    @Child private BooleanOperation logic;
    @Child private BinaryBooleanNode recursiveOp;

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    private Object recursiveOp(VirtualFrame frame, Object left, Object right) {
        if (recursiveOp == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveOp = insert(BinaryBooleanNodeFactory.create(factory, new RNode[2], getBuiltin(), getSuppliedSignature()));
        }
        return recursiveOp.execute(frame, left, right);
    }

    private final NACheck leftNACheck = NACheck.create();
    private final NACheck rightNACheck = NACheck.create();

    private final NAProfile resultNAProfile = NAProfile.create();
    private final ConditionProfile noDimensionsProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile logicOpProfile = ConditionProfile.createBinaryProfile();

    public BinaryBooleanNode(BooleanOperationFactory factory) {
        this.factory = factory;
        this.logic = factory.create();
    }

    public BinaryBooleanNode(BinaryBooleanNode op) {
        this(op.factory);
    }

    // There are a lot of similarities between vectorized logic operators and
    // comparison operators, but they do not extend to handling data of raw type. The short story is
    // that the comparison operators produce a result when one of the operands is of type raw or raw
    // vector whereas logical operators signal an error (unless another operand is of type raw or
    // raw vector).

    // empty raw vectors

    public static boolean isEmpty(RAbstractVector left) {
        return left.getLength() == 0;
    }

    @Specialization(guards = {"isEmpty(left)", "expectLogical()"})
    protected RLogicalVector doEmptyLogical(RAbstractRawVector left, RRaw right) {
        return RDataFactory.createLogicalVector(0);
    }

    @Specialization(guards = {"isEmpty(right)", "expectLogical()"})
    protected RLogicalVector doEmptyLogical(RRaw left, RAbstractRawVector right) {
        return RDataFactory.createLogicalVector(0);
    }

    @Specialization(guards = {"isEmpty(left)", "!expectLogical()"})
    protected RAbstractRawVector doEmptyRaw(RAbstractRawVector left, RRaw right) {
        return RDataFactory.createRawVector(0);
    }

    @Specialization(guards = {"isEmpty(right)", "!expectLogical()"})
    protected RAbstractRawVector doEmptyRaw(RRaw left, RAbstractRawVector right) {
        return RDataFactory.createRawVector(0);
    }

    @Specialization(guards = {"isEmpty(left) || isEmpty(right)", "expectLogical()"})
    protected RLogicalVector doEmptyLogical(RAbstractRawVector left, RAbstractRawVector right) {
        return RDataFactory.createLogicalVector(0);
    }

    @Specialization(guards = {"isEmpty(left) || isEmpty(right)", "!expectLogical()"})
    protected RAbstractRawVector doEmptyRaw(RAbstractRawVector left, RAbstractRawVector right) {
        return RDataFactory.createRawVector(0);
    }

    // int

    @Specialization
    public byte doInt(int left, int right) {
        return logic.op(left, right);
    }

    @Specialization
    protected byte doInt(int left, double right) {
        return logic.op(RRuntime.int2double(left), right);
    }

    @Specialization
    protected byte doInt(double left, int right) {
        return logic.op(left, RRuntime.int2double(right));
    }

    @Specialization
    protected byte doInt(int left, byte right) {
        return logic.op(left, RRuntime.logical2int(right));
    }

    @Specialization
    protected byte doInt(byte left, int right) {
        return logic.op(RRuntime.logical2int(left), right);
    }

    @Specialization
    protected byte doInt(int left, String right) {
        return logic.op(RRuntime.intToString(left), right);
    }

    @Specialization
    protected byte doInt(String left, int right) {
        return logic.op(left, RRuntime.intToString(right));
    }

    @Specialization
    protected byte doInt(int left, RComplex right) {
        return logic.op(RRuntime.int2complex(left), right);
    }

    @Specialization
    protected byte doInt(RComplex left, int right) {
        return logic.op(left, RRuntime.int2complex(right));
    }

    // double

    @Specialization
    public byte doDouble(double left, double right) {
        return logic.op(left, right);
    }

    @Specialization
    protected byte doDouble(double left, byte right) {
        return logic.op(left, RRuntime.logical2double(right));
    }

    @Specialization
    protected byte doDouble(byte left, double right) {
        return logic.op(RRuntime.logical2double(left), right);
    }

    @Specialization
    protected byte doDouble(double left, String right) {
        return logic.op(RRuntime.doubleToString(left), right);
    }

    @Specialization
    protected byte doDouble(String left, double right) {
        return logic.op(left, RRuntime.doubleToString(right));
    }

    @Specialization
    protected byte doDouble(double left, RComplex right) {
        return logic.op(RRuntime.double2complex(left), right);
    }

    @Specialization
    protected byte doDouble(RComplex left, double right) {
        return logic.op(left, RRuntime.double2complex(right));
    }

    @Specialization(guards = "convertRawToNumericObject()")
    protected byte doDouble(double left, RRaw right) {
        return logic.op(left, RRuntime.raw2double(right));
    }

    @Specialization(guards = "convertRawToNumericObject()")
    protected byte doDouble(RRaw left, double right) {
        return logic.op(RRuntime.raw2double(left), right);
    }

    // logical

    @Specialization
    protected byte doLogical(byte left, byte right) {
        return logic.op(RRuntime.logical2int(left), RRuntime.logical2int(right));
    }

    @Specialization
    protected byte doBoolean(byte left, String right) {
        return logic.op(RRuntime.logicalToString(left), right);
    }

    @Specialization
    protected byte doBoolean(String left, byte right) {
        return logic.op(left, RRuntime.logicalToString(right));
    }

    @Specialization
    protected byte doLogical(byte left, RComplex right) {
        return logic.op(RRuntime.logical2complex(left), right);
    }

    @Specialization
    protected byte doLogical(RComplex left, byte right) {
        return logic.op(left, RRuntime.logical2complex(right));
    }

    @Specialization(guards = "convertRawToNumericObject()")
    protected byte doLogical(byte left, RRaw right) {
        return logic.op(left, RRuntime.raw2int(right));
    }

    @Specialization(guards = "convertRawToNumericObject()")
    protected byte doLogical(RRaw left, byte right) {
        return logic.op(RRuntime.raw2int(left), right);
    }

    // string

    @Specialization
    public byte doString(String left, String right) {
        return logic.op(left, right);
    }

    @Specialization
    protected byte doString(String left, RComplex right) {
        return logic.op(left, RRuntime.complexToString(right));
    }

    @Specialization
    protected byte doString(RComplex left, String right) {
        return logic.op(RRuntime.complexToString(left), right);
    }

    @Specialization(guards = "convertRawToNumericObject()")
    protected byte doString(String left, RRaw right) {
        return logic.op(left, RRuntime.rawToString(right));
    }

    @Specialization(guards = "convertRawToNumericObject()")
    protected byte doString(RRaw left, String right) {
        return logic.op(RRuntime.rawToString(left), right);
    }

    // complex

    @Specialization
    protected byte doComplex(RComplex left, RComplex right) {
        return logic.op(left, right);
    }

    @Specialization(guards = "convertRawToNumericObject()")
    protected byte doComplex(RComplex left, RRaw right) {
        return logic.op(left, RRuntime.raw2complex(right));
    }

    @Specialization(guards = "convertRawToNumericObject()")
    protected byte doComplex(RRaw left, RComplex right) {
        return logic.op(RRuntime.raw2complex(left), right);
    }

    // raw

    @Specialization(guards = "!convertRawToNumeric()")
    protected RRaw doRawRaw(RRaw left, RRaw right) {
        return logic.op(left, right);
    }

    @Specialization(guards = "convertRawToNumeric()")
    protected byte doRawLogical(RRaw left, RRaw right) {
        return logic.op(RRuntime.raw2int(left), RRuntime.raw2int(right));
    }

    // null

    @Specialization
    protected RLogicalVector doNull(RNull left, RNull right) {
        return RDataFactory.createLogicalVector(0);
    }

    @Specialization(guards = "!isFactor(right)")
    protected RLogicalVector doNull(RNull left, RAbstractContainer right) {
        return RDataFactory.createLogicalVector(0);
    }

    @Specialization(guards = "!isFactor(left)")
    protected RLogicalVector doNull(RAbstractContainer left, RNull right) {
        return RDataFactory.createLogicalVector(0);
    }

    @Specialization(guards = "!meaningfulOp(left)")
    protected RLogicalVector doFactorOpError(RFactor left, RNull right) {
        RError.warning(getEncapsulatingSourceSection(), RError.Message.NOT_MEANINGFUL_FOR_FACTORS, logic.opName());
        return RDataFactory.createNAVector(left.getLength() == 0 ? 1 : left.getLength());
    }

    @Specialization(guards = "!meaningfulOp(right)")
    protected RLogicalVector doFactorOpError(RNull left, RFactor right) {
        RError.warning(getEncapsulatingSourceSection(), RError.Message.NOT_MEANINGFUL_FOR_FACTORS, logic.opName());
        return RDataFactory.createNAVector(right.getLength() == 0 ? 1 : right.getLength());
    }

    @Specialization(guards = "meaningfulOp(left)")
    protected RLogicalVector doFactorOp(RFactor left, RNull right) {
        return RDataFactory.createLogicalVector(0);
    }

    @Specialization(guards = "meaningfulOp(right)")
    protected RLogicalVector doFactorOp(RNull left, RFactor right) {
        return RDataFactory.createLogicalVector(0);
    }

    // empty vectors

    @Specialization(guards = "isEmpty(left)")
    protected RLogicalVector doEmpty(RAbstractVector left, Object right) {
        return RDataFactory.createLogicalVector(0);
    }

    @Specialization(guards = "isEmpty(right)")
    protected RLogicalVector doEmpty(Object left, RAbstractVector right) {
        return RDataFactory.createLogicalVector(0);
    }

    // int vector and scalar

    @Specialization
    protected RLogicalVector doIntVectorOp(RAbstractIntVector left, int right) {
        return performIntVectorOp(left, RRuntime.int2double(right), false);
    }

    @Specialization
    protected RLogicalVector doIntVectorOp(int left, RAbstractIntVector right) {
        return performIntVectorOp(right, RRuntime.int2double(left), true);
    }

    @Specialization
    protected RLogicalVector doIntVectorOp(RAbstractIntVector left, double right) {
        return performIntVectorOp(left, right, false);
    }

    @Specialization
    protected RLogicalVector doIntVectorOp(double left, RAbstractIntVector right) {
        return performIntVectorOp(right, left, true);
    }

    @Specialization
    protected RLogicalVector doIntVectorOp(RAbstractIntVector left, byte right) {
        return performIntVectorOp(left, RRuntime.logical2double(right), false);
    }

    @Specialization
    protected RLogicalVector doIntVectorOp(byte left, RAbstractIntVector right) {
        return performIntVectorOp(right, RRuntime.logical2double(left), true);
    }

    @Specialization
    protected RLogicalVector doIntVectorOp(RAbstractIntVector left, String right) {
        return performIntVectorOp(left, right, false);
    }

    @Specialization
    protected RLogicalVector doIntVectorOp(String left, RAbstractIntVector right) {
        return performIntVectorOp(right, left, true);
    }

    @Specialization
    protected RLogicalVector doIntVectorOp(RAbstractIntVector left, RComplex right) {
        return performIntVectorOp(left, right, false);
    }

    @Specialization
    protected RLogicalVector doIntVectorOp(RComplex left, RAbstractIntVector right) {
        return performIntVectorOp(right, left, true);
    }

    @Specialization(guards = "convertRawToNumericVector()")
    protected RLogicalVector doIntVectorOp(RAbstractIntVector left, RRaw right) {
        return performIntVectorOp(left, RRuntime.raw2double(right), false);
    }

    @Specialization(guards = "convertRawToNumericVector()")
    protected RLogicalVector doIntVectorOp(RRaw left, RAbstractIntVector right) {
        return performIntVectorOp(right, RRuntime.raw2double(left), true);
    }

    // double vector and scalar

    @Specialization
    protected RLogicalVector doDoubleVectorOp(RAbstractDoubleVector left, int right) {
        return performDoubleVectorOp(left, RRuntime.int2double(right), false);
    }

    @Specialization
    protected RLogicalVector doDoubleVectorOp(int left, RAbstractDoubleVector right) {
        return performDoubleVectorOp(right, RRuntime.int2double(left), true);
    }

    @Specialization
    protected RLogicalVector doDoubleVectorOp(RAbstractDoubleVector left, double right) {
        return performDoubleVectorOp(left, right, false);
    }

    @Specialization
    protected RLogicalVector doDoubleVectorOp(double left, RAbstractDoubleVector right) {
        return performDoubleVectorOp(right, left, true);
    }

    @Specialization
    protected RLogicalVector doDoubleVectorOp(RAbstractDoubleVector left, byte right) {
        return performDoubleVectorOp(left, RRuntime.logical2double(right), false);
    }

    @Specialization
    protected RLogicalVector doDoubleVectorOp(byte left, RAbstractDoubleVector right) {
        return performDoubleVectorOp(right, RRuntime.logical2double(left), true);
    }

    @Specialization
    protected RLogicalVector doDoubleVectorOp(RAbstractDoubleVector left, String right) {
        return performDoubleVectorOp(left, right, false);
    }

    @Specialization
    protected RLogicalVector doDoubleVectorOp(String left, RAbstractDoubleVector right) {
        return performDoubleVectorOp(right, left, true);
    }

    @Specialization
    protected RLogicalVector doDoubleVectorOp(RAbstractDoubleVector left, RComplex right) {
        return performDoubleVectorOp(left, right, false);
    }

    @Specialization
    protected RLogicalVector doDoubleVectorOp(RComplex left, RAbstractDoubleVector right) {
        return performDoubleVectorOp(right, left, true);
    }

    @Specialization(guards = "convertRawToNumericVector()")
    protected RLogicalVector doDoubleVectorOp(RAbstractDoubleVector left, RRaw right) {
        return performDoubleVectorOp(left, RRuntime.raw2double(right), false);
    }

    @Specialization(guards = "convertRawToNumericVector()")
    protected RLogicalVector doDoubleVectorOp(RRaw left, RAbstractDoubleVector right) {
        return performDoubleVectorOp(right, RRuntime.raw2double(left), true);
    }

    // logical vector and scalar

    @Specialization
    protected RLogicalVector doLogicalVectorOp(RLogicalVector left, int right) {
        return performLogicalVectorOp(left, RRuntime.int2double(right), false);
    }

    @Specialization
    protected RLogicalVector doLogicalVectorOp(int left, RLogicalVector right) {
        return performLogicalVectorOp(right, RRuntime.int2double(left), true);
    }

    @Specialization
    protected RLogicalVector doLogicalVectorOp(RLogicalVector left, double right) {
        return performLogicalVectorOp(left, right, false);
    }

    @Specialization
    protected RLogicalVector doLogicalVectorOp(double left, RLogicalVector right) {
        return performLogicalVectorOp(right, left, true);
    }

    @Specialization
    protected RLogicalVector doLogicalVectorOp(RLogicalVector left, byte right) {
        return performLogicalVectorOp(left, RRuntime.logical2int(right), false);
    }

    @Specialization
    protected RLogicalVector doLogicalVectorOp(byte left, RLogicalVector right) {
        return performLogicalVectorOp(right, RRuntime.logical2double(left), true);
    }

    @Specialization
    protected RLogicalVector doLogicalVectorOp(RLogicalVector left, String right) {
        return performLogicalVectorOp(left, right, false);
    }

    @Specialization
    protected RLogicalVector doLogicalVectorOp(String left, RLogicalVector right) {
        return performLogicalVectorOp(right, left, true);
    }

    @Specialization
    protected RLogicalVector doLogicalVectorOp(RLogicalVector left, RComplex right) {
        return performLogicalVectorOp(left, right, false);
    }

    @Specialization
    protected RLogicalVector doLogicalVectorOp(RComplex left, RLogicalVector right) {
        return performLogicalVectorOp(right, left, true);
    }

    @Specialization(guards = "convertRawToNumericVector()")
    protected RLogicalVector doLogicalVectorOp(RLogicalVector left, RRaw right) {
        return performLogicalVectorOp(left, RRuntime.raw2double(right), false);
    }

    @Specialization(guards = "convertRawToNumericVector()")
    protected RLogicalVector doLogicalVectorOp(RRaw left, RLogicalVector right) {
        return performLogicalVectorOp(right, RRuntime.raw2double(left), true);
    }

    // string vector and scalar

    @Specialization
    protected RLogicalVector doStringVectorOp(RAbstractStringVector left, int right) {
        return performStringVectorOp(left, RRuntime.intToString(right), false);
    }

    @Specialization
    protected RLogicalVector doStringVectorOp(int left, RAbstractStringVector right) {
        return performStringVectorOp(right, RRuntime.intToString(left), true);
    }

    @Specialization
    protected RLogicalVector doStringVectorOp(RAbstractStringVector left, double right) {
        return performStringVectorOp(left, RRuntime.doubleToString(right), false);
    }

    @Specialization
    protected RLogicalVector doStringVectorOp(double left, RAbstractStringVector right) {
        return performStringVectorOp(right, RRuntime.doubleToString(left), true);
    }

    @Specialization
    protected RLogicalVector doStringVectorOp(RAbstractStringVector left, byte right) {
        return performStringVectorOp(left, RRuntime.logicalToString(right), false);
    }

    @Specialization
    protected RLogicalVector doStringVectorOp(byte left, RAbstractStringVector right) {
        return performStringVectorOp(right, RRuntime.logicalToString(left), false);
    }

    @Specialization
    protected RLogicalVector doStringVectorOp(RAbstractStringVector left, String right) {
        return performStringVectorOp(left, right, false);
    }

    @Specialization
    protected RLogicalVector doStringVectorOp(String left, RAbstractStringVector right) {
        return performStringVectorOp(right, left, true);
    }

    @Specialization
    protected RLogicalVector doStringVectorOp(RAbstractStringVector left, RComplex right) {
        return performStringVectorOp(left, RRuntime.complexToString(right), false);
    }

    @Specialization
    protected RLogicalVector doStringVectorOp(RComplex left, RAbstractStringVector right) {
        return performStringVectorOp(right, RRuntime.complexToString(left), true);
    }

    @Specialization(guards = "convertRawToNumericVector()")
    protected RLogicalVector doStringVectorOp(RAbstractStringVector left, RRaw right) {
        return performStringVectorOp(left, RRuntime.rawToString(right), false);
    }

    @Specialization(guards = "convertRawToNumericVector()")
    protected RLogicalVector doStringVectorOp(RRaw left, RAbstractStringVector right) {
        return performStringVectorOp(right, RRuntime.rawToString(left), true);
    }

    // complex vector and scalar

    @Specialization
    protected RLogicalVector doComplexVectorOp(RAbstractComplexVector left, int right) {
        return performComplexVectorOp(left, RRuntime.int2complex(right), false);
    }

    @Specialization
    protected RLogicalVector doComplexVectorOp(int left, RAbstractComplexVector right) {
        return performComplexVectorOp(right, RRuntime.int2complex(left), true);
    }

    @Specialization
    protected RLogicalVector doComplexVectorOp(RAbstractComplexVector left, double right) {
        return performComplexVectorOp(left, RRuntime.double2complex(right), false);
    }

    @Specialization
    protected RLogicalVector doComplexVectorOp(double left, RAbstractComplexVector right) {
        return performComplexVectorOp(right, RRuntime.double2complex(left), true);
    }

    @Specialization
    protected RLogicalVector doComplexVectorOp(RAbstractComplexVector left, byte right) {
        return performComplexVectorOp(left, RRuntime.logical2complex(right), false);
    }

    @Specialization
    protected RLogicalVector doComplexVectorOp(byte left, RAbstractComplexVector right) {
        return performComplexVectorOp(right, RRuntime.logical2complex(left), true);
    }

    @Specialization
    protected RLogicalVector doComplexVectorOp(RAbstractComplexVector left, String right) {
        return performComplexVectorOp(left, right, false);
    }

    @Specialization
    protected RLogicalVector doComplexVectorOp(String left, RAbstractComplexVector right) {
        return performComplexVectorOp(right, left, true);
    }

    @Specialization
    protected RLogicalVector doComplexVectorOp(RAbstractComplexVector left, RComplex right) {
        return performComplexVectorOp(left, right, false);
    }

    @Specialization
    protected RLogicalVector doComplexVectorOp(RComplex left, RAbstractComplexVector right) {
        return performComplexVectorOp(right, left, true);
    }

    @Specialization(guards = "convertRawToNumericVector()")
    protected RLogicalVector doComplexVectorOp(RAbstractComplexVector left, RRaw right) {
        return performComplexVectorOp(left, RRuntime.raw2complex(right), false);
    }

    @Specialization(guards = "convertRawToNumericVector()")
    protected RLogicalVector doComplexVectorOp(RRaw left, RAbstractComplexVector right) {
        return performComplexVectorOp(right, RRuntime.raw2complex(left), true);
    }

    // raw vector and scalar

    @Specialization(guards = "convertRawToNumericObject()")
    protected RLogicalVector doRawVectorOp(RAbstractRawVector left, int right) {
        return performRawVectorOp(left, RRuntime.int2double(right), false);
    }

    @Specialization(guards = "convertRawToNumericObject()")
    protected RLogicalVector doRawVectorOp(int left, RAbstractRawVector right) {
        return performRawVectorOp(right, RRuntime.int2double(left), true);
    }

    @Specialization(guards = "convertRawToNumericObject()")
    protected RLogicalVector doRawVectorOp(RAbstractRawVector left, double right) {
        return performRawVectorOp(left, right, false);
    }

    @Specialization(guards = "convertRawToNumericObject()")
    protected RLogicalVector doRawVectorOp(double left, RAbstractRawVector right) {
        return performRawVectorOp(right, left, true);
    }

    @Specialization(guards = "convertRawToNumericObject()")
    protected RLogicalVector doRawVectorOp(RAbstractRawVector left, byte right) {
        return performRawVectorOp(left, RRuntime.logical2int(right), false);
    }

    @Specialization(guards = "convertRawToNumericObject()")
    protected RLogicalVector doRawVectorOp(byte left, RAbstractRawVector right) {
        return performRawVectorOp(right, RRuntime.logical2int(left), true);
    }

    @Specialization(guards = "convertRawToNumericObject()")
    protected RLogicalVector doRawVectorOp(RAbstractRawVector left, String right) {
        return performRawVectorOp(left, right, false);
    }

    @Specialization(guards = "convertRawToNumericObject()")
    protected RLogicalVector doRawVectorOp(String left, RAbstractRawVector right) {
        return performRawVectorOp(right, left, true);
    }

    @Specialization(guards = "convertRawToNumericObject()")
    protected RLogicalVector doRawVectorOp(RAbstractRawVector left, RComplex right) {
        return performRawVectorOp(left, right, false);
    }

    @Specialization(guards = "convertRawToNumericObject()")
    protected RLogicalVector doRawVectorOp(RComplex left, RAbstractRawVector right) {
        return performRawVectorOp(right, left, true);
    }

    @Specialization(guards = "convertRawToNumeric()")
    protected RLogicalVector doRawVectorOpLogical(RAbstractRawVector left, RRaw right) {
        return performRawVectorOp(left, RRuntime.raw2int(right), false);
    }

    @Specialization(guards = "convertRawToNumeric()")
    protected RLogicalVector doRawVectorOpLogical(RRaw left, RAbstractRawVector right) {
        return performRawVectorOp(right, RRuntime.raw2int(left), true);
    }

    @Specialization(guards = "!convertRawToNumeric()")
    protected RAbstractRawVector doRawVectorOpRaw(RAbstractRawVector left, RRaw right) {
        return performRawVectorOp(left, right, false);
    }

    @Specialization(guards = "!convertRawToNumeric()")
    protected RAbstractRawVector doRawVectorOpRaw(RRaw left, RAbstractRawVector right) {
        return performRawVectorOp(right, left, true);
    }

    @Specialization(guards = "differentDimensions(left, right)")
    protected RLogicalVector doIntVectorDifferentLength(RAbstractVector left, RAbstractVector right) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.NON_CONFORMABLE_ARRAYS);
    }

    protected boolean differentDimensions(RAbstractVector left, RAbstractVector right) {
        if (noDimensionsProfile.profile(!left.hasDimensions() || !right.hasDimensions())) {
            return false;
        }
        int[] leftDimensions = left.getDimensions();
        int[] rightDimensions = right.getDimensions();
        assert (leftDimensions != null && rightDimensions != null);
        if (leftDimensions.length != rightDimensions.length) {
            return true;
        }
        for (int i = 0; i < leftDimensions.length; i++) {
            if (leftDimensions[i] != rightDimensions[i]) {
                return true;
            }
        }
        return false;
    }

    // int vector and vectors

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doIntVectorDifferentLength(RAbstractIntVector left, RAbstractIntVector right) {
        return performIntVectorOpDifferentLength(left, right);
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doIntVectorSameLength(RAbstractIntVector left, RAbstractIntVector right) {
        return performIntVectorOpSameLength(left, right);
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doIntVectorDifferentLength(RAbstractIntVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpDifferentLength(RClosures.createIntToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doIntVectorSameLength(RAbstractIntVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpSameLength(RClosures.createIntToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doIntVectorDifferentLength(RAbstractDoubleVector left, RAbstractIntVector right) {
        return performDoubleVectorOpDifferentLength(left, RClosures.createIntToDoubleVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doIntVectorIntVectorSameLength(RAbstractDoubleVector left, RAbstractIntVector right) {
        return performDoubleVectorOpSameLength(left, RClosures.createIntToDoubleVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doIntVectorDifferentLength(RAbstractIntVector left, RAbstractLogicalVector right) {
        return performIntVectorOpDifferentLength(left, RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doIntVectorSameLength(RAbstractIntVector left, RAbstractLogicalVector right) {
        return performIntVectorOpSameLength(left, RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doIntVectorDifferentLength(RAbstractLogicalVector left, RAbstractIntVector right) {
        return performIntVectorOpDifferentLength(RClosures.createLogicalToIntVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doIntVectorSameLength(RAbstractLogicalVector left, RAbstractIntVector right) {
        return performIntVectorOpSameLength(RClosures.createLogicalToIntVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doIntVectorDifferentLength(RAbstractIntVector left, RAbstractStringVector right) {
        return performStringVectorOpDifferentLength(RClosures.createIntToStringVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doIntVectorSameLength(RAbstractIntVector left, RAbstractStringVector right) {
        return performStringVectorOpSameLength(RClosures.createIntToStringVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doIntVectorDifferentLength(RAbstractStringVector left, RAbstractIntVector right) {
        return performStringVectorOpDifferentLength(left, RClosures.createIntToStringVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doIntVectorSameLength(RAbstractStringVector left, RAbstractIntVector right) {
        return performStringVectorOpSameLength(left, RClosures.createIntToStringVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doIntVectorDifferentLength(RAbstractIntVector left, RAbstractComplexVector right) {
        return performComplexVectorOpDifferentLength(RClosures.createIntToComplexVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doIntVectorSameLength(RAbstractIntVector left, RAbstractComplexVector right) {
        return performComplexVectorOpSameLength(RClosures.createIntToComplexVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doIntVectorDifferentLength(RAbstractComplexVector left, RAbstractIntVector right) {
        return performComplexVectorOpDifferentLength(left, RClosures.createIntToComplexVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doIntVectorSameLength(RAbstractComplexVector left, RAbstractIntVector right) {
        return performComplexVectorOpSameLength(left, RClosures.createIntToComplexVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength(left, right)", "convertRawToNumericVector()"})
    protected RLogicalVector doIntVectorDifferentLength(RAbstractIntVector left, RAbstractRawVector right) {
        return performIntVectorOpDifferentLength(left, RClosures.createRawToIntVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength(left, right)", "convertRawToNumericVector()"})
    protected RLogicalVector doIntVectorSameLength(RAbstractIntVector left, RAbstractRawVector right) {
        return performIntVectorOpSameLength(left, RClosures.createRawToIntVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength(left, right)", "convertRawToNumericVector()"})
    protected RLogicalVector doIntVectorDifferentLength(RAbstractRawVector left, RAbstractIntVector right) {
        return performIntVectorOpDifferentLength(RClosures.createRawToIntVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"areSameLength(left, right)", "convertRawToNumericVector()"})
    protected RLogicalVector doIntVectorSameLength(RAbstractRawVector left, RAbstractIntVector right) {
        return performIntVectorOpSameLength(RClosures.createRawToIntVector(left, leftNACheck), right);
    }

    // double vector and vectors

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doDoubleVectorDifferentLength(RAbstractDoubleVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpDifferentLength(left, right);
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doDoubleVectorSameLength(RAbstractDoubleVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpSameLength(left, right);
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doDoubleVectorDifferentLength(RAbstractDoubleVector left, RAbstractLogicalVector right) {
        return performDoubleVectorOpDifferentLength(left, RClosures.createLogicalToDoubleVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doDoubleVectorSameLength(RAbstractDoubleVector left, RAbstractLogicalVector right) {
        return performDoubleVectorOpSameLength(left, RClosures.createLogicalToDoubleVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doDoubleVectorDifferentLength(RAbstractLogicalVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpDifferentLength(RClosures.createLogicalToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doDoubleVectorSameLength(RAbstractLogicalVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpSameLength(RClosures.createLogicalToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doDoubleVectorDifferentLength(RAbstractDoubleVector left, RAbstractStringVector right) {
        return performStringVectorOpDifferentLength(RClosures.createDoubleToStringVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doDoubleVectorSameLength(RAbstractDoubleVector left, RAbstractStringVector right) {
        return performStringVectorOpSameLength(RClosures.createDoubleToStringVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doDoubleVectorDifferentLength(RAbstractStringVector left, RAbstractDoubleVector right) {
        return performStringVectorOpDifferentLength(left, RClosures.createDoubleToStringVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doDoubleVectorSameLength(RAbstractStringVector left, RAbstractDoubleVector right) {
        return performStringVectorOpSameLength(left, RClosures.createDoubleToStringVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doDoubleVectorDifferentLength(RAbstractDoubleVector left, RAbstractComplexVector right) {
        return performComplexVectorOpDifferentLength(RClosures.createDoubleToComplexVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doDoubleVectorSameLength(RAbstractDoubleVector left, RAbstractComplexVector right) {
        return performComplexVectorOpSameLength(RClosures.createDoubleToComplexVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doDoubleVectorDifferentLength(RAbstractComplexVector left, RAbstractDoubleVector right) {
        return performComplexVectorOpDifferentLength(left, RClosures.createDoubleToComplexVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)", "!differentDimensions(left, right)"})
    protected RLogicalVector doDoubleVectorSameLength(RAbstractComplexVector left, RAbstractDoubleVector right) {
        return performComplexVectorOpSameLength(left, RClosures.createDoubleToComplexVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength(left, right)", "convertRawToNumericVector()"})
    protected RLogicalVector doDoubleVectorDifferentLength(RAbstractDoubleVector left, RAbstractRawVector right) {
        return performDoubleVectorOpDifferentLength(left, RClosures.createRawToDoubleVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength(left, right)", "convertRawToNumericVector()"})
    protected RLogicalVector doDoubleVectorSameLength(RAbstractDoubleVector left, RAbstractRawVector right) {
        return performDoubleVectorOpSameLength(left, RClosures.createRawToDoubleVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength(left, right)", "convertRawToNumericVector()"})
    protected RLogicalVector doDoubleVectorDifferentLength(RAbstractRawVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpDifferentLength(RClosures.createRawToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"areSameLength(left, right)", "convertRawToNumericVector()"})
    protected RLogicalVector doDoubleVectorSameLength(RAbstractRawVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpSameLength(RClosures.createRawToDoubleVector(left, leftNACheck), right);
    }

    // logical vector and vectors

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)"})
    protected RLogicalVector doLogicalVectorDifferentLength(RLogicalVector left, RLogicalVector right) {
        return performIntVectorOpDifferentLength(RClosures.createLogicalToIntVector(left, leftNACheck), RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)"})
    protected RLogicalVector doLogicalVectorSameLength(RLogicalVector left, RLogicalVector right) {
        return performIntVectorOpSameLength(RClosures.createLogicalToIntVector(left, leftNACheck), RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)"})
    protected RLogicalVector doLogicalVectorDifferentLength(RAbstractLogicalVector left, RAbstractStringVector right) {
        return performStringVectorOpDifferentLength(RClosures.createLogicalToStringVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)"})
    protected RLogicalVector doLogicalVectorSameLength(RAbstractLogicalVector left, RAbstractStringVector right) {
        return performStringVectorOpSameLength(RClosures.createLogicalToStringVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)"})
    protected RLogicalVector doLogicalVectorDifferentLength(RAbstractStringVector left, RAbstractLogicalVector right) {
        return performStringVectorOpDifferentLength(left, RClosures.createLogicalToStringVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)"})
    protected RLogicalVector doLogicalVectorSameLength(RAbstractStringVector left, RAbstractLogicalVector right) {
        return performStringVectorOpSameLength(left, RClosures.createLogicalToStringVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)"})
    protected RLogicalVector doLogicalVectorDifferentLength(RAbstractLogicalVector left, RAbstractComplexVector right) {
        return performComplexVectorOpDifferentLength(RClosures.createLogicalToComplexVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)"})
    protected RLogicalVector doLogicalVectorSameLength(RAbstractLogicalVector left, RAbstractComplexVector right) {
        return performComplexVectorOpSameLength(RClosures.createLogicalToComplexVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)"})
    protected RLogicalVector doLogicalVectorDifferentLength(RAbstractComplexVector left, RAbstractLogicalVector right) {
        return performComplexVectorOpDifferentLength(left, RClosures.createLogicalToComplexVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)"})
    protected RLogicalVector doLogicalVectorSameLength(RAbstractComplexVector left, RAbstractLogicalVector right) {
        return performComplexVectorOpSameLength(left, RClosures.createLogicalToComplexVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength(left, right)", "convertRawToNumericVector()"})
    protected RLogicalVector doLogicalVectorDifferentLength(RAbstractLogicalVector left, RAbstractRawVector right) {
        return performIntVectorOpDifferentLength(RClosures.createLogicalToIntVector(left, leftNACheck), RClosures.createRawToIntVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength(left, right)", "convertRawToNumericVector()"})
    protected RLogicalVector doLogicalVectorSameLength(RAbstractLogicalVector left, RAbstractRawVector right) {
        return performIntVectorOpSameLength(RClosures.createLogicalToIntVector(left, leftNACheck), RClosures.createRawToIntVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength(left, right)", "convertRawToNumericVector()"})
    protected RLogicalVector doLogicalVectorDifferentLength(RAbstractRawVector left, RAbstractLogicalVector right) {
        return performIntVectorOpDifferentLength(RClosures.createRawToIntVector(left, leftNACheck), RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength(left, right)", "convertRawToNumericVector()"})
    protected RLogicalVector doLogicalVectorSameLength(RAbstractRawVector left, RAbstractLogicalVector right) {
        return performIntVectorOpSameLength(RClosures.createRawToIntVector(left, leftNACheck), RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    // string vector and vectors

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)"})
    protected RLogicalVector doStringVectorDifferentLength(RAbstractStringVector left, RAbstractStringVector right) {
        return performStringVectorOpDifferentLength(left, right);
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)"})
    protected RLogicalVector doStringVectorSameLength(RAbstractStringVector left, RAbstractStringVector right) {
        return performStringVectorOpSameLength(left, right);
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)"})
    protected RLogicalVector doStringVectorDifferentLength(RAbstractStringVector left, RAbstractComplexVector right) {
        return performStringVectorOpDifferentLength(left, RClosures.createComplexToStringVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)"})
    protected RLogicalVector doStringVectorSameLength(RAbstractStringVector left, RAbstractComplexVector right) {
        return performStringVectorOpSameLength(left, RClosures.createComplexToStringVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)"})
    protected RLogicalVector doStringVectorDifferentLength(RAbstractComplexVector left, RAbstractStringVector right) {
        return performStringVectorOpDifferentLength(RClosures.createComplexToStringVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)"})
    protected RLogicalVector doStringVectorSameLength(RAbstractComplexVector left, RAbstractStringVector right) {
        return performStringVectorOpSameLength(RClosures.createComplexToStringVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"!areSameLength(left, right)", "convertRawToNumericVector()"})
    protected RLogicalVector doStringVectorDifferentLength(RAbstractStringVector left, RAbstractRawVector right) {
        return performStringVectorOpDifferentLength(left, RClosures.createRawToStringVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength(left, right)", "convertRawToNumericVector()"})
    protected RLogicalVector doStringVectorSameLength(RAbstractStringVector left, RAbstractRawVector right) {
        return performStringVectorOpSameLength(left, RClosures.createRawToStringVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength(left, right)", "convertRawToNumericVector()"})
    protected RLogicalVector doStringVectorDifferentLengthRAbstractRawVector(RAbstractRawVector left, RAbstractStringVector right) {
        return performStringVectorOpDifferentLength(RClosures.createRawToStringVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"areSameLength(left, right)", "convertRawToNumericVector()"})
    protected RLogicalVector doStringVectorSameLengthRAbstractRawVector(RAbstractRawVector left, RAbstractStringVector right) {
        return performStringVectorOpSameLength(RClosures.createRawToStringVector(left, leftNACheck), right);
    }

    // factor and vectors

    @Specialization(guards = "meaningfulOp(left, right)")
    protected Object doFactorOp(VirtualFrame frame, RFactor left, RFactor right) {
        return recursiveOp(frame, RClosures.createFactorToVector(left, leftNACheck), RClosures.createFactorToVector(right, rightNACheck));
    }

    @Specialization(guards = {"meaningfulOp(left)", "!isFactor(right)"})
    protected Object doFactorOp(VirtualFrame frame, RFactor left, RAbstractContainer right) {
        return recursiveOp(frame, RClosures.createFactorToVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"meaningfulOp(right)", "!isFactor(right)"})
    protected Object doFactorOp(VirtualFrame frame, RAbstractContainer left, RFactor right) {
        return recursiveOp(frame, left, RClosures.createFactorToVector(right, rightNACheck));
    }

    @Specialization(guards = "!meaningfulOp(left, right)")
    protected RLogicalVector doFactorOpNotMeaningful(RFactor left, RFactor right) {
        RError.warning(getEncapsulatingSourceSection(), RError.Message.NOT_MEANINGFUL_FOR_FACTORS, logic.opName());
        return RDataFactory.createNAVector(Math.max(left.getLength(), right.getLength()));
    }

    @Specialization(guards = {"!meaningfulOp(left)", "!isFactor(right)"})
    protected RLogicalVector doFactorOpNotMeaningful(RFactor left, RAbstractContainer right) {
        RError.warning(getEncapsulatingSourceSection(), RError.Message.NOT_MEANINGFUL_FOR_FACTORS, logic.opName());
        return RDataFactory.createNAVector(Math.max(left.getLength(), right.getLength()));
    }

    @Specialization(guards = {"!meaningfulOp(right)", "!isFactor(right)"})
    protected RLogicalVector doFactorOpNotMeaningful(RAbstractContainer left, RFactor right) {
        RError.warning(getEncapsulatingSourceSection(), RError.Message.NOT_MEANINGFUL_FOR_FACTORS, logic.opName());
        return RDataFactory.createNAVector(Math.max(left.getLength(), right.getLength()));
    }

    // connection and vectors

    @Specialization
    protected Object doConnectionOp(VirtualFrame frame, RConnection left, RConnection right) {
        return recursiveOp(frame, left.getDescriptor(), right.getDescriptor());
    }

    @Specialization
    protected Object doConnectionOp(VirtualFrame frame, RConnection left, RAbstractContainer right) {
        return recursiveOp(frame, left.getDescriptor(), right);
    }

    @Specialization
    protected Object doConnectionOp(VirtualFrame frame, RAbstractContainer left, RConnection right) {
        return recursiveOp(frame, left, right.getDescriptor());
    }

    // complex vector and vectors

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)"})
    protected RLogicalVector doComplexVectorDifferentLength(RAbstractComplexVector left, RAbstractComplexVector right) {
        return performComplexVectorOpDifferentLength(left, right);
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)"})
    protected RLogicalVector doComplexVectorSameLength(RAbstractComplexVector left, RAbstractComplexVector right) {
        return performComplexVectorOpSameLength(left, right);
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)", "convertRawToNumericVector()"})
    protected RLogicalVector doComplexVectorDifferentLength(RAbstractComplexVector left, RAbstractRawVector right) {
        return performComplexVectorOpDifferentLength(left, RClosures.createRawToComplexVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)", "convertRawToNumericVector()"})
    protected RLogicalVector doComplexVectorSameLength(RAbstractComplexVector left, RAbstractRawVector right) {
        return performComplexVectorOpSameLength(left, RClosures.createRawToComplexVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)", "convertRawToNumericVector()"})
    protected RLogicalVector doComplexVectorDifferentLength(RAbstractRawVector left, RAbstractComplexVector right) {
        return performComplexVectorOpDifferentLength(RClosures.createRawToComplexVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)", "convertRawToNumericVector()"})
    protected RLogicalVector doComplexVectorSameLength(RAbstractRawVector left, RAbstractComplexVector right) {
        return performComplexVectorOpSameLength(RClosures.createRawToComplexVector(left, leftNACheck), right);
    }

    // raw vector and vectors

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)", "convertRawToNumeric()"})
    protected RLogicalVector doRawVectorDifferentLengthLogical(RAbstractRawVector left, RAbstractRawVector right) {
        return performIntVectorOpDifferentLength(RClosures.createRawToIntVector(left, leftNACheck), RClosures.createRawToIntVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)", "convertRawToNumeric()"})
    protected RLogicalVector doRawVectorSameLengthLogical(RAbstractRawVector left, RAbstractRawVector right) {
        return performIntVectorOpSameLength(RClosures.createRawToIntVector(left, leftNACheck), RClosures.createRawToIntVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength(left, right)", "notEmpty(left, right)", "!convertRawToNumeric()"})
    protected RAbstractRawVector doRawVectorDifferentLengthRaw(RAbstractRawVector left, RAbstractRawVector right) {
        return performRawVectorOpDifferentLength(left, right);
    }

    @Specialization(guards = {"areSameLength(left, right)", "notEmpty(left, right)", "!convertRawToNumeric()"})
    protected RAbstractRawVector doRawVectorSameLengthRaw(RAbstractRawVector left, RAbstractRawVector right) {
        return performRawVectorOpSameLength(left, right);
    }

    // non-convertible raw - other cases are guarded with convertRawToNumeric and
    // convertRawToNumericVector

    @Specialization
    protected byte doRaw(RRaw left, Object right) {
        return logic.op(left, right);
    }

    @Specialization
    protected byte doRaw(Object left, RRaw right) {
        return logic.op(left, right);
    }

    @Specialization(guards = "isNotAVector(right)")
    protected byte doRaw(RAbstractRawVector left, Object right) {
        // perhaps not the cleanest solution but others would be (unnecessarily) more verbose (e.g.
        // introduce another abstract method to BooleanOperation just to signal an error in one
        // case)
        assert left.getLength() > 0; // checked by isEmpty guard
        return logic.op(left.getDataAt(0), right);
    }

    @Specialization(guards = "isNotAVector(left)")
    protected byte doRaw(Object left, RAbstractRawVector right) {
        // perhaps not the cleanest solution but others would be (unnecessarily) more verbose (e.g.
        // introduce another abstract method to BooleanOperation just to signal an error in one
        // case)
        assert right.getLength() > 0; // checked by isEmpty guard
        return logic.op(left, right.getDataAt(0));
    }

    // guards

    protected static boolean isNotAVector(Object operand) {
        return !(operand instanceof RRawVector);
    }

    public boolean isFactor(RAbstractContainer operand) {
        return operand.getElementClass() == RFactor.class;
    }

    public boolean isDataFrame(RDataFrame left, RAbstractContainer right) {
        return right.getElementClass() == RDataFrame.class;
    }

    public boolean isDataFrame(RAbstractContainer left, RDataFrame right) {
        return left.getElementClass() == RDataFrame.class;
    }

    protected boolean meaningfulOp(RFactor left, RFactor right) {
        return logic instanceof BinaryCompare.Equal || logic instanceof BinaryCompare.NotEqual || (left.isOrdered() && right.isOrdered());
    }

    protected boolean meaningfulOp(RFactor left) {
        return logic instanceof BinaryCompare.Equal || logic instanceof BinaryCompare.NotEqual || (left.isOrdered());
    }

    private boolean isVectorizedLogicalOp() {
        return !(logic instanceof BinaryLogic.And || logic instanceof BinaryLogic.Or);
    }

    protected boolean convertRawToNumericObject() {
        return isVectorizedLogicalOp();
    }

    protected boolean convertRawToNumericVector() {
        return isVectorizedLogicalOp();
    }

    protected boolean convertRawToNumeric() {
        return isVectorizedLogicalOp();
    }

    protected boolean expectLogical() {
        return isVectorizedLogicalOp();
    }

    // int vector and scalar implementation

    private RLogicalVector createResult(RAbstractVector left, byte[] result, boolean naResult) {
        RLogicalVector ret = RDataFactory.createLogicalVector(result, !naResult && leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.getDimensions());
        ret.copyNamesFrom(attrProfiles, left);
        return ret;
    }

    private RLogicalVector performIntVectorOp(RAbstractIntVector left, double rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(left);
        rightNACheck.enable(rightValue);
        boolean naResult = false;
        for (int i = 0; i < length; ++i) {
            int leftValue = left.getDataAt(i);
            byte resultValue;
            if (logicOpProfile.profile(isLogicOp())) {
                resultValue = intLogicalResult(leftValue, (int) rightValue, reverse);
            } else {
                resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            }
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        return createResult(left, result, naResult);
    }

    private RLogicalVector performIntVectorOp(RAbstractIntVector left, String rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(left);
        rightNACheck.enable(rightValue);
        boolean naResult = false;
        for (int i = 0; i < length; ++i) {
            String leftValue = RRuntime.intToString(left.getDataAt(i));
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        return createResult(left, result, naResult);
    }

    private RLogicalVector performIntVectorOp(RAbstractIntVector left, RComplex rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(left);
        rightNACheck.enable(rightValue);
        boolean naResult = false;
        for (int i = 0; i < length; ++i) {
            RComplex leftValue = RRuntime.int2complex(left.getDataAt(i));
            byte resultValue;
            if (logicOpProfile.profile(isLogicOp())) {
                resultValue = complexLogicalResult(leftValue, rightValue, reverse);
            } else {
                resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            }
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.getDimensions());
        ret.copyNamesFrom(attrProfiles, left);
        return ret;
    }

    // double vector and scalar implementation

    private RLogicalVector performDoubleVectorOp(RAbstractDoubleVector left, double rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(left);
        rightNACheck.enable(rightValue);
        boolean naResult = false;
        for (int i = 0; i < length; ++i) {
            double leftValue = left.getDataAt(i);
            byte resultValue = doubleLogicalResult(leftValue, rightValue, reverse);
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        return createResult(left, result, naResult);
    }

    private RLogicalVector performDoubleVectorOp(RAbstractDoubleVector left, String rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(left);
        rightNACheck.enable(rightValue);
        boolean naResult = false;
        for (int i = 0; i < length; ++i) {
            String leftValue = RRuntime.doubleToString(left.getDataAt(i));
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        return createResult(left, result, naResult);
    }

    private RLogicalVector performDoubleVectorOp(RAbstractDoubleVector left, RComplex rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(left);
        rightNACheck.enable(rightValue);
        boolean naResult = false;
        for (int i = 0; i < length; ++i) {
            RComplex leftValue = RRuntime.double2complex(left.getDataAt(i));
            byte resultValue;
            if (logicOpProfile.profile(isLogicOp())) {
                resultValue = complexLogicalResult(leftValue, rightValue, reverse);
            } else {
                resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            }
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.getDimensions());
        ret.copyNamesFrom(attrProfiles, left);
        return ret;
    }

    // logical vector and scalar implementation

    private RLogicalVector performLogicalVectorOp(RAbstractLogicalVector left, double rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(left);
        rightNACheck.enable(rightValue);
        boolean naResult = false;
        for (int i = 0; i < length; ++i) {
            double leftValue = RRuntime.logical2double(left.getDataAt(i));
            byte resultValue = doubleLogicalResult(leftValue, rightValue, reverse);
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        return createResult(left, result, naResult);
    }

    private RLogicalVector performLogicalVectorOp(RAbstractLogicalVector left, String rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(left);
        rightNACheck.enable(rightValue);
        boolean naResult = false;
        for (int i = 0; i < length; ++i) {
            String leftValue = RRuntime.logicalToString(left.getDataAt(i));
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.getDimensions());
        ret.copyNamesFrom(attrProfiles, left);
        return ret;
    }

    private RLogicalVector performLogicalVectorOp(RAbstractLogicalVector left, RComplex rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(left);
        rightNACheck.enable(rightValue);
        boolean naResult = false;
        for (int i = 0; i < length; ++i) {
            RComplex leftValue = RRuntime.logical2complex(left.getDataAt(i));
            byte resultValue = complexLogicalResult(leftValue, rightValue, reverse);
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        return createResult(left, result, naResult);
    }

    // string vector and scalar implementation

    private RLogicalVector performStringVectorOp(RAbstractStringVector left, String rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(left);
        rightNACheck.enable(rightValue);
        boolean naResult = false;
        for (int i = 0; i < length; ++i) {
            String leftValue = left.getDataAt(i);
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        return createResult(left, result, naResult);
    }

    // complex vector and scalar implementation

    private RLogicalVector performComplexVectorOp(RAbstractComplexVector left, RComplex rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(left);
        rightNACheck.enable(rightValue);
        boolean naResult = false;
        for (int i = 0; i < length; ++i) {
            RComplex leftValue = left.getDataAt(i);
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        return createResult(left, result, naResult);
    }

    private RLogicalVector performComplexVectorOp(RAbstractComplexVector left, String rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(left);
        rightNACheck.enable(rightValue);
        boolean naResult = false;
        for (int i = 0; i < length; ++i) {
            String leftValue = RRuntime.complexToString(left.getDataAt(i));
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        return createResult(left, result, naResult);
    }

    private RLogicalVector performRawVectorOp(RAbstractRawVector left, double rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(left);
        rightNACheck.enable(rightValue);
        boolean naResult = false;
        for (int i = 0; i < length; ++i) {
            double leftValue = RRuntime.raw2double(left.getDataAt(i));
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        return createResult(left, result, naResult);
    }

    private RLogicalVector performRawVectorOp(RAbstractRawVector left, String rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(left);
        rightNACheck.enable(rightValue);
        boolean naResult = false;
        for (int i = 0; i < length; ++i) {
            String leftValue = RRuntime.rawToString(left.getDataAt(i));
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        return createResult(left, result, naResult);
    }

    private RLogicalVector performRawVectorOp(RAbstractRawVector left, RComplex rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(left);
        rightNACheck.enable(rightValue);
        boolean naResult = false;
        for (int i = 0; i < length; ++i) {
            RComplex leftValue = RRuntime.raw2complex(left.getDataAt(i));
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        return createResult(left, result, naResult);
    }

    private RRawVector performRawVectorOp(RAbstractRawVector left, RRaw rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        for (int i = 0; i < length; ++i) {
            RRaw leftValue = left.getDataAt(i);
            byte resultValue = !reverse ? logic.op(leftValue, rightValue).getValue() : logic.op(rightValue, leftValue).getValue();
            result[i] = resultValue;
        }
        RRawVector ret = RDataFactory.createRawVector(result, left.getDimensions());
        ret.copyNamesFrom(attrProfiles, left);
        return ret;
    }

    // int vector and vectors implementation

    private RLogicalVector performIntVectorOpSameLength(RAbstractIntVector left, RAbstractIntVector right) {
        assert areSameLength(left, right);
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        boolean naResult = false;
        for (int i = 0; i < length; ++i) {
            int leftValue = left.getDataAt(i);
            int rightValue = right.getDataAt(i);
            byte resultValue = intLogicalResult(leftValue, rightValue, false);
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, !naResult && leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(),
                        left.hasDimensions() ? left.getDimensions() : right.getDimensions());
        ret.copyNamesFrom(attrProfiles, left.getNames(attrProfiles) != null ? left : right);
        return ret;
    }

    private byte intLogicalResult(int leftValueA, int rightValueA, boolean reverse) {
        int leftValue = reverse ? rightValueA : leftValueA;
        int rightValue = reverse ? leftValueA : rightValueA;
        // for logical ops behavior with NA is different
        boolean leftNA = leftNACheck.check(leftValue);
        boolean rightNA = rightNACheck.check(rightValue);
        byte resultValue;
        if (logicOpProfile.profile(isLogicOp())) {
            if (leftNA && rightNA) {
                resultValue = RRuntime.LOGICAL_NA;
            } else if (leftNA) {
                resultValue = nalogic(rightValue);
            } else if (rightNA) {
                resultValue = nalogic(leftValue);
            } else {
                resultValue = logic.op(leftValue, rightValue);
            }
        } else {
            resultValue = leftNA || rightNA ? RRuntime.LOGICAL_NA : logic.op(leftValue, rightValue);
        }
        return resultValue;
    }

    private byte nalogic(int value) {
        if (logic instanceof BinaryLogic.Or) {
            return value == 0 ? RRuntime.LOGICAL_NA : RRuntime.LOGICAL_TRUE;
        } else {
            // &
            return value == 0 ? RRuntime.LOGICAL_FALSE : RRuntime.LOGICAL_NA;
        }
    }

    private RLogicalVector performIntVectorOpDifferentLength(RAbstractIntVector left, RAbstractIntVector right) {
        assert !areSameLength(left, right);
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        int resultLength = Math.max(leftLength, rightLength);
        byte[] result = new byte[resultLength];
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        int l = 0;
        int r = 0;
        boolean naResult = false;
        for (int i = 0; i < resultLength; ++i, l = Utils.incMod(l, leftLength), r = Utils.incMod(r, rightLength)) {
            int leftValue = left.getDataAt(l);
            int rightValue = right.getDataAt(r);
            byte resultValue = intLogicalResult(leftValue, rightValue, false);
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        boolean notMultiple = l != 0 || r != 0;
        if (notMultiple) {
            RError.warning(RError.Message.LENGTH_NOT_MULTI);
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, !naResult && leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA());
        ret.setDimensions(left.hasDimensions() ? left.getDimensions() : right.getDimensions(), this.getSourceSection());
        ret.copyNamesFrom(attrProfiles, leftLength == resultLength ? left : right);
        return ret;
    }

    // double vector and vectors implementation

    private RLogicalVector performDoubleVectorOpSameLength(RAbstractDoubleVector left, RAbstractDoubleVector right) {
        assert areSameLength(left, right);
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        boolean naResult = false;
        for (int i = 0; i < length; ++i) {
            double leftValue = left.getDataAt(i);
            double rightValue = right.getDataAt(i);
            byte resultValue = doubleLogicalResult(leftValue, rightValue, false);
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, !naResult && leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(),
                        left.hasDimensions() ? left.getDimensions() : right.getDimensions());
        ret.copyNamesFrom(attrProfiles, left.getNames(attrProfiles) != null ? left : right);
        return ret;
    }

    private byte doubleLogicalResult(double leftValueA, double rightValueA, boolean reverse) {
        double leftValue = reverse ? rightValueA : leftValueA;
        double rightValue = reverse ? leftValueA : rightValueA;
        // for logical ops behavior with NA is different
        boolean leftNA = leftNACheck.check(leftValue);
        boolean rightNA = rightNACheck.check(rightValue);
        byte resultValue;
        if (logicOpProfile.profile(isLogicOp())) {
            if (leftNA && rightNA) {
                resultValue = RRuntime.LOGICAL_NA;
            } else if (leftNA) {
                resultValue = nalogic(rightValue);
            } else if (rightNA) {
                resultValue = nalogic(leftValue);
            } else {
                resultValue = logic.op(leftValue, rightValue);
            }
        } else {
            resultValue = leftNA || rightNA ? RRuntime.LOGICAL_NA : logic.op(leftValue, rightValue);
        }
        return resultValue;
    }

    private byte nalogic(double value) {
        if (logic instanceof BinaryLogic.Or) {
            return value == 0 ? RRuntime.LOGICAL_NA : RRuntime.LOGICAL_TRUE;
        } else {
            // &
            return value == 0 ? RRuntime.LOGICAL_FALSE : RRuntime.LOGICAL_NA;
        }
    }

    private RLogicalVector performDoubleVectorOpDifferentLength(RAbstractDoubleVector left, RAbstractDoubleVector right) {
        assert !areSameLength(left, right);
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        int resultLength = Math.max(leftLength, rightLength);
        byte[] result = new byte[resultLength];
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        int l = 0;
        int r = 0;
        boolean naResult = false;
        for (int i = 0; i < resultLength; ++i, l = Utils.incMod(l, leftLength), r = Utils.incMod(r, rightLength)) {
            double leftValue = left.getDataAt(l);
            double rightValue = right.getDataAt(r);
            byte resultValue = doubleLogicalResult(leftValue, rightValue, false);
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        boolean notMultiple = l != 0 || r != 0;
        if (notMultiple) {
            RError.warning(RError.Message.LENGTH_NOT_MULTI);
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, !naResult && leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA());
        ret.setDimensions(left.hasDimensions() ? left.getDimensions() : right.getDimensions(), this.getSourceSection());
        ret.copyNamesFrom(attrProfiles, leftLength == resultLength ? left : right);
        return ret;
    }

    // string vector and vectors implementation

    private RLogicalVector performStringVectorOpSameLength(RAbstractStringVector left, RAbstractStringVector right) {
        assert areSameLength(left, right);
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        boolean naResult = false;
        for (int i = 0; i < length; ++i) {
            String leftValue = left.getDataAt(i);
            String rightValue = right.getDataAt(i);
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : logic.op(leftValue, rightValue);
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, !naResult && leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(),
                        left.hasDimensions() ? left.getDimensions() : right.getDimensions());
        ret.copyNamesFrom(attrProfiles, left.getNames(attrProfiles) != null ? left : right);
        return ret;
    }

    private RLogicalVector performStringVectorOpDifferentLength(RAbstractStringVector left, RAbstractStringVector right) {
        assert !areSameLength(left, right);
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        int resultLength = Math.max(leftLength, rightLength);
        byte[] result = new byte[resultLength];
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        int l = 0;
        int r = 0;
        boolean naResult = false;
        for (int i = 0; i < resultLength; ++i, l = Utils.incMod(l, leftLength), r = Utils.incMod(r, rightLength)) {
            String leftValue = left.getDataAt(l);
            String rightValue = right.getDataAt(r);
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : logic.op(leftValue, rightValue);
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        boolean notMultiple = l != 0 || r != 0;
        if (notMultiple) {
            RError.warning(RError.Message.LENGTH_NOT_MULTI);
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, !naResult && leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA());
        ret.setDimensions(left.hasDimensions() ? left.getDimensions() : right.getDimensions(), this.getSourceSection());
        ret.copyNamesFrom(attrProfiles, leftLength == resultLength ? left : right);
        return ret;
    }

    // complex vector and vectors implementation

    private RLogicalVector performComplexVectorOpSameLength(RAbstractComplexVector left, RAbstractComplexVector right) {
        assert areSameLength(left, right);
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        boolean naResult = false;
        for (int i = 0; i < length; ++i) {
            RComplex leftValue = left.getDataAt(i);
            RComplex rightValue = right.getDataAt(i);
            byte resultValue = complexLogicalResult(leftValue, rightValue, false);
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, !naResult && leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(),
                        left.hasDimensions() ? left.getDimensions() : right.getDimensions());
        ret.copyNamesFrom(attrProfiles, left.getNames(attrProfiles) != null ? left : right);
        return ret;
    }

    private byte complexLogicalResult(RComplex leftValueA, RComplex rightValueA, boolean reverse) {
        RComplex leftValue = reverse ? rightValueA : leftValueA;
        RComplex rightValue = reverse ? leftValueA : rightValueA;
        // for logical ops behavior with NA is different
        boolean leftNA = leftNACheck.check(leftValue);
        boolean rightNA = rightNACheck.check(rightValue);
        byte resultValue;
        if (logicOpProfile.profile(isLogicOp())) {
            if (leftNA && rightNA) {
                resultValue = RRuntime.LOGICAL_NA;
            } else if (leftNA) {
                resultValue = nalogic(rightValue);
            } else if (rightNA) {
                resultValue = nalogic(leftValue);
            } else {
                resultValue = logic.op(leftValue, rightValue);
            }
        } else {
            resultValue = leftNA || rightNA ? RRuntime.LOGICAL_NA : logic.op(leftValue, rightValue);
        }
        return resultValue;
    }

    private byte nalogic(RComplex value) {
        if (logic instanceof BinaryLogic.Or) {
            return value.getRealPart() == 0 && value.getImaginaryPart() == 0 ? RRuntime.LOGICAL_NA : RRuntime.LOGICAL_TRUE;
        } else {
            // &
            return value.getRealPart() == 0 && value.getImaginaryPart() == 0 ? RRuntime.LOGICAL_FALSE : RRuntime.LOGICAL_NA;
        }
    }

    private RLogicalVector performComplexVectorOpDifferentLength(RAbstractComplexVector left, RAbstractComplexVector right) {
        assert !areSameLength(left, right);
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        int resultLength = Math.max(leftLength, rightLength);
        byte[] result = new byte[resultLength];
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        int l = 0;
        int r = 0;
        boolean naResult = false;
        for (int i = 0; i < resultLength; ++i, l = Utils.incMod(l, leftLength), r = Utils.incMod(r, rightLength)) {
            RComplex leftValue = left.getDataAt(l);
            RComplex rightValue = right.getDataAt(r);
            byte resultValue = complexLogicalResult(leftValue, rightValue, false);
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        boolean notMultiple = l != 0 || r != 0;
        if (notMultiple) {
            RError.warning(RError.Message.LENGTH_NOT_MULTI);
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, !naResult && leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA());
        ret.setDimensions(left.hasDimensions() ? left.getDimensions() : right.getDimensions(), this.getSourceSection());
        ret.copyNamesFrom(attrProfiles, leftLength == resultLength ? left : right);
        return ret;
    }

    // complex vector and vectors implementation

    private RRawVector performRawVectorOpSameLength(RAbstractRawVector left, RAbstractRawVector right) {
        assert areSameLength(left, right);
        int length = left.getLength();
        byte[] result = new byte[length];
        boolean naResult = false;
        for (int i = 0; i < length; ++i) {
            RRaw leftValue = left.getDataAt(i);
            RRaw rightValue = right.getDataAt(i);
            byte resultValue = logic.op(leftValue, rightValue).getValue();
            if (resultNAProfile.isNA(resultValue)) {
                naResult = true;
            }
            result[i] = resultValue;
        }
        RRawVector ret = RDataFactory.createRawVector(result, left.hasDimensions() ? left.getDimensions() : right.getDimensions());
        ret.copyNamesFrom(attrProfiles, left.getNames(attrProfiles) != null ? left : right);
        return ret;
    }

    private RRawVector performRawVectorOpDifferentLength(RAbstractRawVector left, RAbstractRawVector right) {
        assert !areSameLength(left, right);
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        int resultLength = Math.max(leftLength, rightLength);
        byte[] result = new byte[resultLength];
        int l = 0;
        int r = 0;
        for (int i = 0; i < resultLength; ++i, l = Utils.incMod(l, leftLength), r = Utils.incMod(r, rightLength)) {
            RRaw leftValue = left.getDataAt(l);
            RRaw rightValue = right.getDataAt(r);
            byte resultValue = logic.op(leftValue, rightValue).getValue();
            result[i] = resultValue;
        }
        boolean notMultiple = l != 0 || r != 0;
        if (notMultiple) {
            RError.warning(RError.Message.LENGTH_NOT_MULTI);
        }
        RRawVector ret = RDataFactory.createRawVector(result);
        ret.setDimensions(left.hasDimensions() ? left.getDimensions() : right.getDimensions(), this.getSourceSection());
        ret.copyNamesFrom(attrProfiles, leftLength == resultLength ? left : right);
        return ret;
    }

    protected static boolean notEmpty(RAbstractVector left, RAbstractVector right) {
        return left.getLength() != 0 && right.getLength() != 0;
    }

    public boolean isLogicOp() {
        return logic instanceof BinaryLogic.And || logic instanceof BinaryLogic.Or;
    }
}
