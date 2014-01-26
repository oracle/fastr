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
package com.oracle.truffle.r.nodes.binary;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.closures.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.na.*;

import static com.oracle.truffle.r.runtime.RRuntime.*;

@SuppressWarnings("unused")
public abstract class BinaryBooleanNode extends BinaryNode {

    private final BooleanOperationFactory factory;
    @Child private BooleanOperation logic;

    private final NACheck leftNACheck = NACheck.create();
    private final NACheck rightNACheck = NACheck.create();

    public BinaryBooleanNode(BooleanOperationFactory factory) {
        this.factory = factory;
        this.logic = adoptChild(factory.create());
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

    @Specialization(order = 1, guards = {"isEmpty", "expectLogical"})
    public RLogicalVector doEmptyLogical(RRawVector left, RRaw right) {
        return RDataFactory.createLogicalVector(0);
    }

    @Specialization(order = 2, guards = {"isEmpty", "expectLogical"})
    public RLogicalVector doEmptyLogical(RRaw left, RRawVector right) {
        return RDataFactory.createLogicalVector(0);
    }

    @Specialization(order = 3, guards = {"isEmpty", "!expectLogical"})
    public RRawVector doEmptyRaw(RRawVector left, RRaw right) {
        return RDataFactory.createRawVector(0);
    }

    @Specialization(order = 4, guards = {"isEmpty", "!expectLogical"})
    public RRawVector doEmptyRaw(RRaw left, RRawVector right) {
        return RDataFactory.createRawVector(0);
    }

    @Specialization(order = 5, guards = {"isEmpty", "expectLogical"})
    public RLogicalVector doEmptyLogical(RRawVector left, RRawVector right) {
        return RDataFactory.createLogicalVector(0);
    }

    @Specialization(order = 6, guards = {"isEmpty", "!expectLogical"})
    public RRawVector doEmptyRaw(RRawVector left, RRawVector right) {
        return RDataFactory.createRawVector(0);
    }

    // int

    @Specialization(order = 20)
    public byte doInt(int left, int right) {
        return logic.op(left, right);
    }

    @Specialization(order = 21)
    public byte doInt(int left, double right) {
        return logic.op(RRuntime.int2double(left), right);
    }

    @Specialization(order = 22)
    public byte doInt(double left, int right) {
        return logic.op(left, RRuntime.int2double(right));
    }

    @Specialization(order = 23)
    public byte doInt(int left, byte right) {
        return logic.op(left, RRuntime.logical2int(right));
    }

    @Specialization(order = 24)
    public byte doInt(byte left, int right) {
        return logic.op(RRuntime.logical2int(left), right);
    }

    @Specialization(order = 25)
    public byte doInt(int left, String right) {
        return logic.op(RRuntime.intToString(left, false), right);
    }

    @Specialization(order = 26)
    public byte doInt(String left, int right) {
        return logic.op(left, RRuntime.intToString(right, false));
    }

    @Specialization(order = 27)
    public byte doInt(int left, RComplex right) {
        return logic.op(RRuntime.int2complex(left), right);
    }

    @Specialization(order = 28)
    public byte doInt(RComplex left, int right) {
        return logic.op(left, RRuntime.int2complex(right));
    }

    // double

    @Specialization(order = 30)
    public byte doDouble(double left, double right) {
        return logic.op(left, right);
    }

    @Specialization(order = 32)
    public byte doDouble(double left, byte right) {
        return logic.op(left, RRuntime.logical2double(right));
    }

    @Specialization(order = 34)
    public byte doDouble(byte left, double right) {
        return logic.op(RRuntime.logical2double(left), right);
    }

    @Specialization(order = 36)
    public byte doDouble(double left, String right) {
        return logic.op(RRuntime.doubleToString(left), right);
    }

    @Specialization(order = 38)
    public byte doDouble(String left, double right) {
        return logic.op(left, RRuntime.doubleToString(right));
    }

    @Specialization(order = 40)
    public byte doDouble(double left, RComplex right) {
        return logic.op(RRuntime.double2complex(left), right);
    }

    @Specialization(order = 42)
    public byte doDouble(RComplex left, double right) {
        return logic.op(left, RRuntime.double2complex(right));
    }

    @Specialization(order = 44, guards = "convertRawToNumericObject")
    public byte doDouble(double left, RRaw right) {
        return logic.op(left, RRuntime.raw2double(right));
    }

    @Specialization(order = 46, guards = "convertRawToNumericObject")
    public byte doDouble(RRaw left, double right) {
        return logic.op(RRuntime.raw2double(left), right);
    }

    // logical

    @Specialization(order = 50)
    public byte doLogical(byte left, byte right) {
        return logic.op(RRuntime.logical2int(left), RRuntime.logical2int(right));
    }

    @Specialization(order = 52)
    public byte doBoolean(byte left, String right) {
        return logic.op(RRuntime.logicalToString(left), right);
    }

    @Specialization(order = 54)
    public byte doBoolean(String left, byte right) {
        return logic.op(left, RRuntime.logicalToString(right));
    }

    @Specialization(order = 56)
    public byte doLogical(byte left, RComplex right) {
        return logic.op(RRuntime.logical2complex(left), right);
    }

    @Specialization(order = 58)
    public byte doLogical(RComplex left, byte right) {
        return logic.op(left, RRuntime.logical2complex(right));
    }

    @Specialization(order = 60, guards = "convertRawToNumericObject")
    public byte doLogical(byte left, RRaw right) {
        return logic.op(left, RRuntime.raw2int(right));
    }

    @Specialization(order = 62, guards = "convertRawToNumericObject")
    public byte doLogical(RRaw left, byte right) {
        return logic.op(RRuntime.raw2int(left), right);
    }

    // string

    @Specialization(order = 100)
    public byte doString(String left, String right) {
        return logic.op(left, right);
    }

    @Specialization(order = 102)
    public byte doString(String left, RComplex right) {
        return logic.op(left, RRuntime.complexToString(right));
    }

    @Specialization(order = 104)
    public byte doString(RComplex left, String right) {
        return logic.op(RRuntime.complexToString(left), right);
    }

    @Specialization(order = 106, guards = "convertRawToNumericObject")
    public byte doString(String left, RRaw right) {
        return logic.op(left, RRuntime.rawToString(right));
    }

    @Specialization(order = 108, guards = "convertRawToNumericObject")
    public byte doString(RRaw left, String right) {
        return logic.op(RRuntime.rawToString(left), right);
    }

    // complex

    @Specialization(order = 150)
    public byte doComplex(RComplex left, RComplex right) {
        return logic.op(left, right);
    }

    @Specialization(order = 152, guards = "convertRawToNumericObject")
    public byte doComplex(RComplex left, RRaw right) {
        return logic.op(left, RRuntime.raw2complex(right));
    }

    @Specialization(order = 154, guards = "convertRawToNumericObject")
    public byte doComplex(RRaw left, RComplex right) {
        return logic.op(RRuntime.raw2complex(left), right);
    }

    // raw

    @Specialization(order = 200, guards = "!convertRawToNumeric")
    public RRaw doRawRaw(RRaw left, RRaw right) {
        return logic.op(left, right);
    }

    @Specialization(order = 201, guards = "convertRawToNumeric")
    public byte doRawLogical(RRaw left, RRaw right) {
        return logic.op(RRuntime.raw2int(left), RRuntime.raw2int(right));
    }

    // null

    @Specialization(order = 250)
    public RLogicalVector doNull(RNull left, Object right) {
        return RDataFactory.createLogicalVector(0);
    }

    @Specialization(order = 251)
    public RLogicalVector doNull(Object left, RNull right) {
        return RDataFactory.createLogicalVector(0);
    }

    // empty vectors

    @Specialization(order = 280, guards = "isEmpty")
    public RLogicalVector doEmpty(RAbstractVector left, Object right) {
        return RDataFactory.createLogicalVector(0);
    }

    @Specialization(order = 281, guards = "isEmpty")
    public RLogicalVector doEmpty(Object left, RAbstractVector right) {
        return RDataFactory.createLogicalVector(0);
    }

    // int vector and scalar

    @Specialization(order = 300)
    public RLogicalVector doIntVectorOp(RAbstractIntVector left, int right) {
        return performIntVectorOp(left, RRuntime.int2double(right), false);
    }

    @Specialization(order = 301)
    public RLogicalVector doIntVectorOp(int left, RAbstractIntVector right) {
        return performIntVectorOp(right, RRuntime.int2double(left), true);
    }

    @Specialization(order = 302)
    public RLogicalVector doIntVectorOp(RAbstractIntVector left, double right) {
        return performIntVectorOp(left, right, false);
    }

    @Specialization(order = 303)
    public RLogicalVector doIntVectorOp(double left, RAbstractIntVector right) {
        return performIntVectorOp(right, left, true);
    }

    @Specialization(order = 304)
    public RLogicalVector doIntVectorOp(RAbstractIntVector left, byte right) {
        return performIntVectorOp(left, RRuntime.logical2double(right), false);
    }

    @Specialization(order = 305)
    public RLogicalVector doIntVectorOp(byte left, RAbstractIntVector right) {
        return performIntVectorOp(right, RRuntime.logical2double(left), true);
    }

    @Specialization(order = 306)
    public RLogicalVector doIntVectorOp(RAbstractIntVector left, String right) {
        return performIntVectorOp(left, right, false);
    }

    @Specialization(order = 307)
    public RLogicalVector doIntVectorOp(String left, RAbstractIntVector right) {
        return performIntVectorOp(right, left, true);
    }

    @Specialization(order = 308)
    public RLogicalVector doIntVectorOp(RAbstractIntVector left, RComplex right) {
        return performIntVectorOp(left, right, false);
    }

    @Specialization(order = 309)
    public RLogicalVector doIntVectorOp(RComplex left, RAbstractIntVector right) {
        return performIntVectorOp(right, left, true);
    }

    @Specialization(order = 310, guards = "convertRawToNumericVector")
    public RLogicalVector doIntVectorOp(RAbstractIntVector left, RRaw right) {
        return performIntVectorOp(left, RRuntime.raw2double(right), false);
    }

    @Specialization(order = 311, guards = "convertRawToNumericVector")
    public RLogicalVector doIntVectorOp(RRaw left, RAbstractIntVector right) {
        return performIntVectorOp(right, RRuntime.raw2double(left), true);
    }

    // double vector and scalar

    @Specialization(order = 400)
    public RLogicalVector doDoubleVectorOp(RAbstractDoubleVector left, int right) {
        return performDoubleVectorOp(left, RRuntime.int2double(right), false);
    }

    @Specialization(order = 401)
    public RLogicalVector doDoubleVectorOp(int left, RAbstractDoubleVector right) {
        return performDoubleVectorOp(right, RRuntime.int2double(left), true);
    }

    @Specialization(order = 402)
    public RLogicalVector doDoubleVectorOp(RAbstractDoubleVector left, double right) {
        return performDoubleVectorOp(left, right, false);
    }

    @Specialization(order = 403)
    public RLogicalVector doDoubleVectorOp(double left, RAbstractDoubleVector right) {
        return performDoubleVectorOp(right, left, true);
    }

    @Specialization(order = 404)
    public RLogicalVector doDoubleVectorOp(RAbstractDoubleVector left, byte right) {
        return performDoubleVectorOp(left, RRuntime.logical2double(right), false);
    }

    @Specialization(order = 405)
    public RLogicalVector doDoubleVectorOp(byte left, RAbstractDoubleVector right) {
        return performDoubleVectorOp(right, RRuntime.logical2double(left), true);
    }

    @Specialization(order = 406)
    public RLogicalVector doDoubleVectorOp(RAbstractDoubleVector left, String right) {
        return performDoubleVectorOp(left, right, false);
    }

    @Specialization(order = 407)
    public RLogicalVector doDoubleVectorOp(String left, RAbstractDoubleVector right) {
        return performDoubleVectorOp(right, left, true);
    }

    @Specialization(order = 408)
    public RLogicalVector doDoubleVectorOp(RAbstractDoubleVector left, RComplex right) {
        return performDoubleVectorOp(left, right, false);
    }

    @Specialization(order = 409)
    public RLogicalVector doDoubleVectorOp(RComplex left, RAbstractDoubleVector right) {
        return performDoubleVectorOp(right, left, true);
    }

    @Specialization(order = 410, guards = "convertRawToNumericVector")
    public RLogicalVector doDoubleVectorOp(RAbstractDoubleVector left, RRaw right) {
        return performDoubleVectorOp(left, RRuntime.raw2double(right), false);
    }

    @Specialization(order = 411, guards = "convertRawToNumericVector")
    public RLogicalVector doDoubleVectorOp(RRaw left, RAbstractDoubleVector right) {
        return performDoubleVectorOp(right, RRuntime.raw2double(left), true);
    }

    // logical vector and scalar

    @Specialization(order = 500)
    public RLogicalVector doLogicalVectorOp(RLogicalVector left, int right) {
        return performLogicalVectorOp(left, RRuntime.int2double(right), false);
    }

    @Specialization(order = 501)
    public RLogicalVector doLogicalVectorOp(int left, RLogicalVector right) {
        return performLogicalVectorOp(right, RRuntime.int2double(left), true);
    }

    @Specialization(order = 502)
    public RLogicalVector doLogicalVectorOp(RLogicalVector left, double right) {
        return performLogicalVectorOp(left, right, false);
    }

    @Specialization(order = 503)
    public RLogicalVector doLogicalVectorOp(double left, RLogicalVector right) {
        return performLogicalVectorOp(right, left, true);
    }

    @Specialization(order = 504)
    public RLogicalVector doLogicalVectorOp(RLogicalVector left, byte right) {
        return performLogicalVectorOp(left, RRuntime.logical2int(right), false);
    }

    @Specialization(order = 505)
    public RLogicalVector doLogicalVectorOp(byte left, RLogicalVector right) {
        return performLogicalVectorOp(right, RRuntime.logical2double(left), true);
    }

    @Specialization(order = 506)
    public RLogicalVector doLogicalVectorOp(RLogicalVector left, String right) {
        return performLogicalVectorOp(left, right, false);
    }

    @Specialization(order = 507)
    public RLogicalVector doLogicalVectorOp(String left, RLogicalVector right) {
        return performLogicalVectorOp(right, left, true);
    }

    @Specialization(order = 508)
    public RLogicalVector doLogicalVectorOp(RLogicalVector left, RComplex right) {
        return performLogicalVectorOp(left, right, false);
    }

    @Specialization(order = 509)
    public RLogicalVector doLogicalVectorOp(RComplex left, RLogicalVector right) {
        return performLogicalVectorOp(right, left, true);
    }

    @Specialization(order = 510, guards = "convertRawToNumericVector")
    public RLogicalVector doLogicalVectorOp(RLogicalVector left, RRaw right) {
        return performLogicalVectorOp(left, RRuntime.raw2double(right), false);
    }

    @Specialization(order = 511, guards = "convertRawToNumericVector")
    public RLogicalVector doLogicalVectorOp(RRaw left, RLogicalVector right) {
        return performLogicalVectorOp(right, RRuntime.raw2double(left), true);
    }

    // string vector and scalar

    @Specialization(order = 600)
    public RLogicalVector doStringVectorOp(RStringVector left, int right) {
        return performStringVectorOp(left, RRuntime.intToString(right, false), false);
    }

    @Specialization(order = 601)
    public RLogicalVector doStringVectorOp(int left, RStringVector right) {
        return performStringVectorOp(right, RRuntime.intToString(left, false), true);
    }

    @Specialization(order = 602)
    public RLogicalVector doStringVectorOp(RStringVector left, double right) {
        return performStringVectorOp(left, RRuntime.doubleToString(right), false);
    }

    @Specialization(order = 603)
    public RLogicalVector doStringVectorOp(double left, RStringVector right) {
        return performStringVectorOp(right, RRuntime.doubleToString(left), true);
    }

    @Specialization(order = 604)
    public RLogicalVector doStringVectorOp(RStringVector left, byte right) {
        return performStringVectorOp(left, RRuntime.logicalToString(right), false);
    }

    @Specialization(order = 605)
    public RLogicalVector doStringVectorOp(byte left, RStringVector right) {
        return performStringVectorOp(right, RRuntime.logicalToString(left), false);
    }

    @Specialization(order = 606)
    public RLogicalVector doStringVectorOp(RStringVector left, String right) {
        return performStringVectorOp(left, right, false);
    }

    @Specialization(order = 607)
    public RLogicalVector doStringVectorOp(String left, RStringVector right) {
        return performStringVectorOp(right, left, true);
    }

    @Specialization(order = 608)
    public RLogicalVector doStringVectorOp(RStringVector left, RComplex right) {
        return performStringVectorOp(left, RRuntime.complexToString(right), false);
    }

    @Specialization(order = 609)
    public RLogicalVector doStringVectorOp(RComplex left, RStringVector right) {
        return performStringVectorOp(right, RRuntime.complexToString(left), true);
    }

    @Specialization(order = 610, guards = "convertRawToNumericVector")
    public RLogicalVector doStringVectorOp(RStringVector left, RRaw right) {
        return performStringVectorOp(left, RRuntime.rawToString(right), false);
    }

    @Specialization(order = 611, guards = "convertRawToNumericVector")
    public RLogicalVector doStringVectorOp(RRaw left, RStringVector right) {
        return performStringVectorOp(right, RRuntime.rawToString(left), true);
    }

    // complex vector and scalar

    @Specialization(order = 700)
    public RLogicalVector doComplexVectorOp(RComplexVector left, int right) {
        return performComplexVectorOp(left, RRuntime.int2complex(right), false);
    }

    @Specialization(order = 701)
    public RLogicalVector doComplexVectorOp(int left, RComplexVector right) {
        return performComplexVectorOp(right, RRuntime.int2complex(left), true);
    }

    @Specialization(order = 702)
    public RLogicalVector doComplexVectorOp(RComplexVector left, double right) {
        return performComplexVectorOp(left, RRuntime.double2complex(right), false);
    }

    @Specialization(order = 703)
    public RLogicalVector doComplexVectorOp(double left, RComplexVector right) {
        return performComplexVectorOp(right, RRuntime.double2complex(left), true);
    }

    @Specialization(order = 704)
    public RLogicalVector doComplexVectorOp(RComplexVector left, byte right) {
        return performComplexVectorOp(left, RRuntime.logical2complex(right), false);
    }

    @Specialization(order = 705)
    public RLogicalVector doComplexVectorOp(byte left, RComplexVector right) {
        return performComplexVectorOp(right, RRuntime.logical2complex(left), true);
    }

    @Specialization(order = 706)
    public RLogicalVector doComplexVectorOp(RComplexVector left, String right) {
        return performComplexVectorOp(left, right, false);
    }

    @Specialization(order = 707)
    public RLogicalVector doComplexVectorOp(String left, RComplexVector right) {
        return performComplexVectorOp(right, left, true);
    }

    @Specialization(order = 708)
    public RLogicalVector doComplexVectorOp(RComplexVector left, RComplex right) {
        return performComplexVectorOp(left, right, false);
    }

    @Specialization(order = 709)
    public RLogicalVector doComplexVectorOp(RComplex left, RComplexVector right) {
        return performComplexVectorOp(right, left, true);
    }

    @Specialization(order = 710, guards = "convertRawToNumericVector")
    public RLogicalVector doComplexVectorOp(RComplexVector left, RRaw right) {
        return performComplexVectorOp(left, RRuntime.raw2complex(right), false);
    }

    @Specialization(order = 711, guards = "convertRawToNumericVector")
    public RLogicalVector doComplexVectorOp(RRaw left, RComplexVector right) {
        return performComplexVectorOp(right, RRuntime.raw2complex(left), true);
    }

    // raw vector and scalar

    @Specialization(order = 800, guards = "convertRawToNumericObject")
    public RLogicalVector doRawVectorOp(RRawVector left, int right) {
        return performRawVectorOp(left, RRuntime.int2double(right), false);
    }

    @Specialization(order = 801, guards = "convertRawToNumericObject")
    public RLogicalVector doRawVectorOp(int left, RRawVector right) {
        return performRawVectorOp(right, RRuntime.int2double(left), true);
    }

    @Specialization(order = 802, guards = "convertRawToNumericObject")
    public RLogicalVector doRawVectorOp(RRawVector left, double right) {
        return performRawVectorOp(left, right, false);
    }

    @Specialization(order = 803, guards = "convertRawToNumericObject")
    public RLogicalVector doRawVectorOp(double left, RRawVector right) {
        return performRawVectorOp(right, left, true);
    }

    @Specialization(order = 804, guards = "convertRawToNumericObject")
    public RLogicalVector doRawVectorOp(RRawVector left, byte right) {
        return performRawVectorOp(left, RRuntime.logical2int(right), false);
    }

    @Specialization(order = 805, guards = "convertRawToNumericObject")
    public RLogicalVector doRawVectorOp(byte left, RRawVector right) {
        return performRawVectorOp(right, RRuntime.logical2int(left), true);
    }

    @Specialization(order = 806, guards = "convertRawToNumericObject")
    public RLogicalVector doRawVectorOp(RRawVector left, String right) {
        return performRawVectorOp(left, right, false);
    }

    @Specialization(order = 807, guards = "convertRawToNumericObject")
    public RLogicalVector doRawVectorOp(String left, RRawVector right) {
        return performRawVectorOp(right, left, true);
    }

    @Specialization(order = 808, guards = "convertRawToNumericObject")
    public RLogicalVector doRawVectorOp(RRawVector left, RComplex right) {
        return performRawVectorOp(left, right, false);
    }

    @Specialization(order = 809, guards = "convertRawToNumericObject")
    public RLogicalVector doRawVectorOp(RComplex left, RRawVector right) {
        return performRawVectorOp(right, left, true);
    }

    @Specialization(order = 810, guards = "convertRawToNumeric")
    public RLogicalVector doRawVectorOpLogical(RRawVector left, RRaw right) {
        return performRawVectorOp(left, RRuntime.raw2int(right), false);
    }

    @Specialization(order = 811, guards = "convertRawToNumeric")
    public RLogicalVector doRawVectorOpLogical(RRaw left, RRawVector right) {
        return performRawVectorOp(right, RRuntime.raw2int(left), true);
    }

    @Specialization(order = 812, guards = "!convertRawToNumeric")
    public RRawVector doRawVectorOpRaw(RRawVector left, RRaw right) {
        return performRawVectorOp(left, right, false);
    }

    @Specialization(order = 813, guards = "!convertRawToNumeric")
    public RRawVector doRawVectorOpRaw(RRaw left, RRawVector right) {
        return performRawVectorOp(right, left, true);
    }

    @Specialization(order = 1000, guards = "differentDimensions")
    public RLogicalVector doIntVectorDifferentLength(RAbstractVector left, RAbstractVector right) {
        throw RError.getNonConformableArrays(getSourceSection());
    }

    protected boolean differentDimensions(RAbstractVector left, RAbstractVector right) {
        if (!left.hasDimensions() || !right.hasDimensions()) {
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

    @Specialization(order = 1001, guards = "!areSameLength")
    public RLogicalVector doIntVectorDifferentLength(RAbstractIntVector left, RAbstractIntVector right) {
        return performIntVectorOpDifferentLength(left, right);
    }

    @Specialization(order = 1002, guards = "areSameLength")
    public RLogicalVector doIntVectorSameLength(RAbstractIntVector left, RAbstractIntVector right) {
        return performIntVectorOpSameLength(left, right);
    }

    @Specialization(order = 1003, guards = "!areSameLength")
    public RLogicalVector doIntVectorDifferentLength(RAbstractIntVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpDifferentLength(RClosures.createIntToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(order = 1004, guards = "areSameLength")
    public RLogicalVector doIntVectorSameLength(RAbstractIntVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpSameLength(RClosures.createIntToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(order = 1005, guards = "!areSameLength")
    public RLogicalVector doIntVectorDifferentLength(RAbstractDoubleVector left, RAbstractIntVector right) {
        return performDoubleVectorOpDifferentLength(left, RClosures.createIntToDoubleVector(right, rightNACheck));
    }

    @Specialization(order = 1006, guards = "areSameLength")
    public RLogicalVector doIntVectorIntVectorSameLength(RAbstractDoubleVector left, RAbstractIntVector right) {
        return performDoubleVectorOpSameLength(left, RClosures.createIntToDoubleVector(right, rightNACheck));
    }

    @Specialization(order = 1007, guards = "!areSameLength")
    public RLogicalVector doIntVectorDifferentLength(RAbstractIntVector left, RAbstractLogicalVector right) {
        return performIntVectorOpDifferentLength(left, RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    @Specialization(order = 1008, guards = "areSameLength")
    public RLogicalVector doIntVectorSameLength(RAbstractIntVector left, RAbstractLogicalVector right) {
        return performIntVectorOpSameLength(left, RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    @Specialization(order = 1009, guards = "!areSameLength")
    public RLogicalVector doIntVectorDifferentLength(RAbstractLogicalVector left, RAbstractIntVector right) {
        return performIntVectorOpDifferentLength(RClosures.createLogicalToIntVector(left, leftNACheck), right);
    }

    @Specialization(order = 1010, guards = "areSameLength")
    public RLogicalVector doIntVectorSameLength(RAbstractLogicalVector left, RAbstractIntVector right) {
        return performIntVectorOpSameLength(RClosures.createLogicalToIntVector(left, leftNACheck), right);
    }

    @Specialization(order = 1011, guards = "!areSameLength")
    public RLogicalVector doIntVectorDifferentLength(RAbstractIntVector left, RStringVector right) {
        return performStringVectorOpDifferentLength(RClosures.createIntToStringVector(left, leftNACheck), right);
    }

    @Specialization(order = 1012, guards = "areSameLength")
    public RLogicalVector doIntVectorSameLength(RAbstractIntVector left, RStringVector right) {
        return performStringVectorOpSameLength(RClosures.createIntToStringVector(left, leftNACheck), right);
    }

    @Specialization(order = 1013, guards = "!areSameLength")
    public RLogicalVector doIntVectorDifferentLength(RStringVector left, RAbstractIntVector right) {
        return performStringVectorOpDifferentLength(left, RClosures.createIntToStringVector(right, rightNACheck));
    }

    @Specialization(order = 1014, guards = "areSameLength")
    public RLogicalVector doIntVectorSameLength(RStringVector left, RAbstractIntVector right) {
        return performStringVectorOpSameLength(left, RClosures.createIntToStringVector(right, rightNACheck));
    }

    @Specialization(order = 1015, guards = "!areSameLength")
    public RLogicalVector doIntVectorDifferentLength(RAbstractIntVector left, RComplexVector right) {
        return performComplexVectorOpDifferentLength(RClosures.createIntToComplexVector(left, leftNACheck), right);
    }

    @Specialization(order = 1016, guards = "areSameLength")
    public RLogicalVector doIntVectorSameLength(RAbstractIntVector left, RComplexVector right) {
        return performComplexVectorOpSameLength(RClosures.createIntToComplexVector(left, leftNACheck), right);
    }

    @Specialization(order = 1017, guards = "!areSameLength")
    public RLogicalVector doIntVectorDifferentLength(RComplexVector left, RAbstractIntVector right) {
        return performComplexVectorOpDifferentLength(left, RClosures.createIntToComplexVector(right, rightNACheck));
    }

    @Specialization(order = 1018, guards = "areSameLength")
    public RLogicalVector doIntVectorSameLength(RComplexVector left, RAbstractIntVector right) {
        return performComplexVectorOpSameLength(left, RClosures.createIntToComplexVector(right, rightNACheck));
    }

    @Specialization(order = 1019, guards = {"!areSameLength", "convertRawToNumericVector"})
    public RLogicalVector doIntVectorDifferentLength(RAbstractIntVector left, RRawVector right) {
        return performIntVectorOpDifferentLength(left, RClosures.createRawToIntVector(right, rightNACheck));
    }

    @Specialization(order = 1020, guards = {"areSameLength", "convertRawToNumericVector"})
    public RLogicalVector doIntVectorSameLength(RAbstractIntVector left, RRawVector right) {
        return performIntVectorOpSameLength(left, RClosures.createRawToIntVector(right, rightNACheck));
    }

    @Specialization(order = 1021, guards = {"!areSameLength", "convertRawToNumericVector"})
    public RLogicalVector doIntVectorDifferentLength(RRawVector left, RAbstractIntVector right) {
        return performIntVectorOpDifferentLength(RClosures.createRawToIntVector(left, leftNACheck), right);
    }

    @Specialization(order = 1023, guards = {"areSameLength", "convertRawToNumericVector"})
    public RLogicalVector doIntVectorSameLength(RRawVector left, RAbstractIntVector right) {
        return performIntVectorOpSameLength(RClosures.createRawToIntVector(left, leftNACheck), right);
    }

    // double vector and vectors

    @Specialization(order = 1100, guards = "!areSameLength")
    public RLogicalVector doDoubleVectorDifferentLength(RAbstractDoubleVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpDifferentLength(left, right);
    }

    @Specialization(order = 1101, guards = "areSameLength")
    public RLogicalVector doDoubleVectorSameLength(RAbstractDoubleVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpSameLength(left, right);
    }

    @Specialization(order = 1102, guards = "!areSameLength")
    public RLogicalVector doDoubleVectorDifferentLength(RAbstractDoubleVector left, RAbstractLogicalVector right) {
        return performDoubleVectorOpDifferentLength(left, RClosures.createLogicalToDoubleVector(right, rightNACheck));
    }

    @Specialization(order = 1103, guards = "areSameLength")
    public RLogicalVector doDoubleVectorSameLength(RAbstractDoubleVector left, RAbstractLogicalVector right) {
        return performDoubleVectorOpSameLength(left, RClosures.createLogicalToDoubleVector(right, rightNACheck));
    }

    @Specialization(order = 1104, guards = "!areSameLength")
    public RLogicalVector doDoubleVectorDifferentLength(RAbstractLogicalVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpDifferentLength(RClosures.createLogicalToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(order = 1105, guards = "areSameLength")
    public RLogicalVector doDoubleVectorSameLength(RAbstractLogicalVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpSameLength(RClosures.createLogicalToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(order = 1106, guards = "!areSameLength")
    public RLogicalVector doDoubleVectorDifferentLength(RAbstractDoubleVector left, RStringVector right) {
        return performStringVectorOpDifferentLength(RClosures.createDoubleToStringVector(left, leftNACheck), right);
    }

    @Specialization(order = 1107, guards = "areSameLength")
    public RLogicalVector doDoubleVectorSameLength(RAbstractDoubleVector left, RStringVector right) {
        return performStringVectorOpSameLength(RClosures.createDoubleToStringVector(left, leftNACheck), right);
    }

    @Specialization(order = 1108, guards = "!areSameLength")
    public RLogicalVector doDoubleVectorDifferentLength(RStringVector left, RAbstractDoubleVector right) {
        return performStringVectorOpDifferentLength(left, RClosures.createDoubleToStringVector(right, rightNACheck));
    }

    @Specialization(order = 1109, guards = "areSameLength")
    public RLogicalVector doDoubleVectorSameLength(RStringVector left, RAbstractDoubleVector right) {
        return performStringVectorOpSameLength(left, RClosures.createDoubleToStringVector(right, rightNACheck));
    }

    @Specialization(order = 1110, guards = "!areSameLength")
    public RLogicalVector doDoubleVectorDifferentLength(RAbstractDoubleVector left, RComplexVector right) {
        return performComplexVectorOpDifferentLength(RClosures.createDoubleToComplexVector(left, leftNACheck), right);
    }

    @Specialization(order = 1111, guards = "areSameLength")
    public RLogicalVector doDoubleVectorSameLength(RAbstractDoubleVector left, RComplexVector right) {
        return performComplexVectorOpSameLength(RClosures.createDoubleToComplexVector(left, leftNACheck), right);
    }

    @Specialization(order = 1112, guards = "!areSameLength")
    public RLogicalVector doDoubleVectorDifferentLength(RComplexVector left, RAbstractDoubleVector right) {
        return performComplexVectorOpDifferentLength(left, RClosures.createDoubleToComplexVector(right, rightNACheck));
    }

    @Specialization(order = 1113, guards = "areSameLength")
    public RLogicalVector doDoubleVectorSameLength(RComplexVector left, RAbstractDoubleVector right) {
        return performComplexVectorOpSameLength(left, RClosures.createDoubleToComplexVector(right, rightNACheck));
    }

    @Specialization(order = 1114, guards = {"!areSameLength", "convertRawToNumericVector"})
    public RLogicalVector doDoubleVectorDifferentLength(RAbstractDoubleVector left, RRawVector right) {
        return performDoubleVectorOpDifferentLength(left, RClosures.createRawToDoubleVector(right, rightNACheck));
    }

    @Specialization(order = 1115, guards = {"areSameLength", "convertRawToNumericVector"})
    public RLogicalVector doDoubleVectorSameLength(RAbstractDoubleVector left, RRawVector right) {
        return performDoubleVectorOpSameLength(left, RClosures.createRawToDoubleVector(right, rightNACheck));
    }

    @Specialization(order = 1116, guards = {"!areSameLength", "convertRawToNumericVector"})
    public RLogicalVector doDoubleVectorDifferentLength(RRawVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpDifferentLength(RClosures.createRawToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(order = 1117, guards = {"areSameLength", "convertRawToNumericVector"})
    public RLogicalVector doDoubleVectorSameLength(RRawVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpSameLength(RClosures.createRawToDoubleVector(left, leftNACheck), right);
    }

    // logical vector and vectors

    @Specialization(order = 1200, guards = "!areSameLength")
    public RLogicalVector doLogicalVectorDifferentLength(RLogicalVector left, RLogicalVector right) {
        return performIntVectorOpDifferentLength(RClosures.createLogicalToIntVector(left, leftNACheck), RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    @Specialization(order = 1201, guards = "areSameLength")
    public RLogicalVector doLogicalVectorSameLength(RLogicalVector left, RLogicalVector right) {
        return performIntVectorOpSameLength(RClosures.createLogicalToIntVector(left, leftNACheck), RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    @Specialization(order = 1202, guards = "!areSameLength")
    public RLogicalVector doLogicalVectorDifferentLength(RAbstractLogicalVector left, RStringVector right) {
        return performStringVectorOpDifferentLength(RClosures.createLogicalToStringVector(left, leftNACheck), right);
    }

    @Specialization(order = 1203, guards = "areSameLength")
    public RLogicalVector doLogicalVectorSameLength(RAbstractLogicalVector left, RStringVector right) {
        return performStringVectorOpSameLength(RClosures.createLogicalToStringVector(left, leftNACheck), right);
    }

    @Specialization(order = 1204, guards = "!areSameLength")
    public RLogicalVector doLogicalVectorDifferentLength(RStringVector left, RAbstractLogicalVector right) {
        return performStringVectorOpDifferentLength(left, RClosures.createLogicalToStringVector(right, rightNACheck));
    }

    @Specialization(order = 1205, guards = "areSameLength")
    public RLogicalVector doLogicalVectorSameLength(RStringVector left, RAbstractLogicalVector right) {
        return performStringVectorOpSameLength(left, RClosures.createLogicalToStringVector(right, rightNACheck));
    }

    @Specialization(order = 1206, guards = "!areSameLength")
    public RLogicalVector doLogicalVectorDifferentLength(RAbstractLogicalVector left, RComplexVector right) {
        return performComplexVectorOpDifferentLength(RClosures.createLogicalToComplexVector(left, leftNACheck), right);
    }

    @Specialization(order = 1207, guards = "areSameLength")
    public RLogicalVector doLogicalVectorSameLength(RAbstractLogicalVector left, RComplexVector right) {
        return performComplexVectorOpSameLength(RClosures.createLogicalToComplexVector(left, leftNACheck), right);
    }

    @Specialization(order = 1208, guards = "!areSameLength")
    public RLogicalVector doLogicalVectorDifferentLength(RComplexVector left, RAbstractLogicalVector right) {
        return performComplexVectorOpDifferentLength(left, RClosures.createLogicalToComplexVector(right, rightNACheck));
    }

    @Specialization(order = 1209, guards = "areSameLength")
    public RLogicalVector doLogicalVectorSameLength(RComplexVector left, RAbstractLogicalVector right) {
        return performComplexVectorOpSameLength(left, RClosures.createLogicalToComplexVector(right, rightNACheck));
    }

    @Specialization(order = 1210, guards = {"!areSameLength", "convertRawToNumericVector"})
    public RLogicalVector doLogicalVectorDifferentLength(RAbstractLogicalVector left, RRawVector right) {
        return performIntVectorOpDifferentLength(RClosures.createLogicalToIntVector(left, leftNACheck), RClosures.createRawToIntVector(right, rightNACheck));
    }

    @Specialization(order = 1211, guards = {"areSameLength", "convertRawToNumericVector"})
    public RLogicalVector doLogicalVectorSameLength(RAbstractLogicalVector left, RRawVector right) {
        return performIntVectorOpSameLength(RClosures.createLogicalToIntVector(left, leftNACheck), RClosures.createRawToIntVector(right, rightNACheck));
    }

    @Specialization(order = 1212, guards = {"!areSameLength", "convertRawToNumericVector"})
    public RLogicalVector doLogicalVectorDifferentLength(RRawVector left, RAbstractLogicalVector right) {
        return performIntVectorOpDifferentLength(RClosures.createRawToIntVector(left, leftNACheck), RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    @Specialization(order = 1213, guards = {"areSameLength", "convertRawToNumericVector"})
    public RLogicalVector doLogicalVectorSameLength(RRawVector left, RAbstractLogicalVector right) {
        return performIntVectorOpSameLength(RClosures.createRawToIntVector(left, leftNACheck), RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    // string vector and vectors

    @Specialization(order = 1300, guards = "!areSameLength")
    public RLogicalVector doStringVectorDifferentLength(RStringVector left, RStringVector right) {
        return performStringVectorOpDifferentLength(left, right);
    }

    @Specialization(order = 1301, guards = "areSameLength")
    public RLogicalVector doStringVectorSameLength(RStringVector left, RStringVector right) {
        return performStringVectorOpSameLength(left, right);
    }

    @Specialization(order = 1302, guards = "!areSameLength")
    public RLogicalVector doStringVectorDifferentLength(RStringVector left, RAbstractComplexVector right) {
        return performStringVectorOpDifferentLength(left, RClosures.createComplexToStringVector(right, rightNACheck));
    }

    @Specialization(order = 1303, guards = "areSameLength")
    public RLogicalVector doStringVectorSameLength(RStringVector left, RAbstractComplexVector right) {
        return performStringVectorOpSameLength(left, RClosures.createComplexToStringVector(right, rightNACheck));
    }

    @Specialization(order = 1304, guards = "!areSameLength")
    public RLogicalVector doStringVectorDifferentLength(RAbstractComplexVector left, RStringVector right) {
        return performStringVectorOpDifferentLength(RClosures.createComplexToStringVector(left, leftNACheck), right);
    }

    @Specialization(order = 1305, guards = "areSameLength")
    public RLogicalVector doStringVectorSameLength(RAbstractComplexVector left, RStringVector right) {
        return performStringVectorOpSameLength(RClosures.createComplexToStringVector(left, leftNACheck), right);
    }

    @Specialization(order = 1306, guards = {"!areSameLength", "convertRawToNumericVector"})
    public RLogicalVector doStringVectorDifferentLength(RStringVector left, RRawVector right) {
        return performStringVectorOpDifferentLength(left, RClosures.createRawToStringVector(right, rightNACheck));
    }

    @Specialization(order = 1307, guards = {"areSameLength", "convertRawToNumericVector"})
    public RLogicalVector doStringVectorSameLength(RStringVector left, RRawVector right) {
        return performStringVectorOpSameLength(left, RClosures.createRawToStringVector(right, rightNACheck));
    }

    @Specialization(order = 1308, guards = {"!areSameLength", "convertRawToNumericVector"})
    public RLogicalVector doStringVectorDifferentLengthRRawVector(RRawVector left, RStringVector right) {
        return performStringVectorOpDifferentLength(RClosures.createRawToStringVector(left, leftNACheck), right);
    }

    @Specialization(order = 1309, guards = {"areSameLength", "convertRawToNumericVector"})
    public RLogicalVector doStringVectorSameLengthRRawVector(RRawVector left, RStringVector right) {
        return performStringVectorOpSameLength(RClosures.createRawToStringVector(left, leftNACheck), right);
    }

    // complex vector and vectors

    @Specialization(order = 1400, guards = "!areSameLength")
    public RLogicalVector doComplexVectorDifferentLength(RComplexVector left, RComplexVector right) {
        return performComplexVectorOpDifferentLength(left, right);
    }

    @Specialization(order = 1401, guards = "areSameLength")
    public RLogicalVector doComplexVectorSameLength(RComplexVector left, RComplexVector right) {
        return performComplexVectorOpSameLength(left, right);
    }

    @Specialization(order = 1402, guards = {"!areSameLength", "convertRawToNumericVector"})
    public RLogicalVector doComplexVectorDifferentLength(RComplexVector left, RRawVector right) {
        return performComplexVectorOpDifferentLength(left, RClosures.createRawToComplexVector(right, rightNACheck));
    }

    @Specialization(order = 1403, guards = {"areSameLength", "convertRawToNumericVector"})
    public RLogicalVector doComplexVectorSameLength(RComplexVector left, RRawVector right) {
        return performComplexVectorOpSameLength(left, RClosures.createRawToComplexVector(right, rightNACheck));
    }

    @Specialization(order = 1404, guards = {"!areSameLength", "convertRawToNumericVector"})
    public RLogicalVector doComplexVectorDifferentLength(RRawVector left, RComplexVector right) {
        return performComplexVectorOpDifferentLength(RClosures.createRawToComplexVector(left, leftNACheck), right);
    }

    @Specialization(order = 1405, guards = {"areSameLength", "convertRawToNumericVector"})
    public RLogicalVector doComplexVectorSameLength(RRawVector left, RComplexVector right) {
        return performComplexVectorOpSameLength(RClosures.createRawToComplexVector(left, leftNACheck), right);
    }

    // raw vector and vectors

    @Specialization(order = 1500, guards = {"!areSameLength", "convertRawToNumeric"})
    public RLogicalVector doRawVectorDifferentLengthLogical(RRawVector left, RRawVector right) {
        return performIntVectorOpDifferentLength(RClosures.createRawToIntVector(left, leftNACheck), RClosures.createRawToIntVector(right, rightNACheck));
    }

    @Specialization(order = 1501, guards = {"areSameLength", "convertRawToNumeric"})
    public RLogicalVector doRawVectorSameLengthLogical(RRawVector left, RRawVector right) {
        return performIntVectorOpSameLength(RClosures.createRawToIntVector(left, leftNACheck), RClosures.createRawToIntVector(right, rightNACheck));
    }

    @Specialization(order = 1502, guards = {"!areSameLength", "!convertRawToNumeric"})
    public RRawVector doRawVectorDifferentLengthRaw(RRawVector left, RRawVector right) {
        return performRawVectorOpDifferentLength(left, right);
    }

    @Specialization(order = 1503, guards = {"areSameLength", "!convertRawToNumeric"})
    public RRawVector doRawVectorSameLengthRaw(RRawVector left, RRawVector right) {
        return performRawVectorOpSameLength(left, right);
    }

    // non-convertible raw - other cases are guarded with convertRawToNumeric and
    // convertRawToNumericVector

    @Specialization(order = 1600)
    public byte doRaw(RRaw left, Object right) {
        return logic.op(left, right);
    }

    @Specialization(order = 1601)
    public byte doRaw(Object left, RRaw right) {
        return logic.op(left, right);
    }

    @Specialization(order = 1602)
    public byte doRaw(RRawVector left, Object right) {
        // perhaps not the cleanest solution but others would be (unnecessarily) more verbose (e.g.
        // introduce another abstract method to BooleanOperation just to signal an error in one
        // case)
        assert left.getLength() > 0; // checked by isEmpty guard
        return logic.op(left.getDataAt(0), right);
    }

    @Specialization(order = 1603)
    public byte doRaw(Object left, RRawVector right) {
        // perhaps not the cleanest solution but others would be (unnecessarily) more verbose (e.g.
        // introduce another abstract method to BooleanOperation just to signal an error in one
        // case)
        assert right.getLength() > 0; // checked by isEmpty guard
        return logic.op(left, right.getDataAt(0));
    }

    // guards

    private boolean isVectorizedLogicalOp() {
        return !(logic instanceof BinaryLogic.And || logic instanceof BinaryLogic.Or);
    }

    public boolean convertRawToNumericObject(RRaw left, Object right) {
        return isVectorizedLogicalOp();
    }

    public boolean convertRawToNumericObject(Object left, RRaw right) {
        return isVectorizedLogicalOp();
    }

    public boolean convertRawToNumericObject(RRawVector left, Object right) {
        return isVectorizedLogicalOp();
    }

    public boolean convertRawToNumericObject(Object left, RRawVector right) {
        return isVectorizedLogicalOp();
    }

    public boolean convertRawToNumericVector(RRaw left, RAbstractVector right) {
        return isVectorizedLogicalOp();
    }

    public boolean convertRawToNumericVector(RAbstractVector left, RRaw right) {
        return isVectorizedLogicalOp();
    }

    public boolean convertRawToNumericVector(RRawVector left, RAbstractVector right) {
        return isVectorizedLogicalOp();
    }

    public boolean convertRawToNumericVector(RAbstractVector left, RRawVector right) {
        return isVectorizedLogicalOp();
    }

    public boolean convertRawToNumeric(RRaw left, RRaw right) {
        return isVectorizedLogicalOp();
    }

    public boolean convertRawToNumeric(RRawVector left, RRawVector right) {
        return isVectorizedLogicalOp();
    }

    public boolean convertRawToNumeric(RRaw left, RRawVector right) {
        return isVectorizedLogicalOp();
    }

    public boolean convertRawToNumeric(RRawVector left, RRaw right) {
        return isVectorizedLogicalOp();
    }

    public boolean expectLogical(RRaw left, RRaw right) {
        return isVectorizedLogicalOp();
    }

    public boolean expectLogical(RRawVector left, RRawVector right) {
        return isVectorizedLogicalOp();
    }

    public boolean expectLogical(RRawVector left, RRaw right) {
        return isVectorizedLogicalOp();
    }

    public boolean expectLogical(RRaw left, RRawVector right) {
        return isVectorizedLogicalOp();
    }

    // int vector and scalar implementation

    private RLogicalVector performIntVectorOp(RAbstractIntVector left, double rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(rightValue);
        for (int i = 0; i < length; ++i) {
            int leftValue = left.getDataAt(i);
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.getDimensions());
        ret.copyNamesFrom(left);
        return ret;
    }

    private RLogicalVector performIntVectorOp(RAbstractIntVector left, String rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(rightValue);
        for (int i = 0; i < length; ++i) {
            String leftValue = RRuntime.intToString(left.getDataAt(i), false);
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.getDimensions());
        ret.copyNamesFrom(left);
        return ret;
    }

    private RLogicalVector performIntVectorOp(RAbstractIntVector left, RComplex rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(rightValue);
        for (int i = 0; i < length; ++i) {
            RComplex leftValue = RRuntime.int2complex(left.getDataAt(i));
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.getDimensions());
        ret.copyNamesFrom(left);
        return ret;
    }

    // double vector and scalar implementation

    private RLogicalVector performDoubleVectorOp(RAbstractDoubleVector left, double rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(rightValue);
        for (int i = 0; i < length; ++i) {
            double leftValue = left.getDataAt(i);
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.getDimensions());
        ret.copyNamesFrom(left);
        return ret;
    }

    private RLogicalVector performDoubleVectorOp(RAbstractDoubleVector left, String rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(rightValue);
        for (int i = 0; i < length; ++i) {
            String leftValue = RRuntime.doubleToString(left.getDataAt(i));
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.getDimensions());
        ret.copyNamesFrom(left);
        return ret;
    }

    private RLogicalVector performDoubleVectorOp(RAbstractDoubleVector left, RComplex rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(rightValue);
        for (int i = 0; i < length; ++i) {
            RComplex leftValue = RRuntime.double2complex(left.getDataAt(i));
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.getDimensions());
        ret.copyNamesFrom(left);
        return ret;
    }

    // logical vector and scalar implementation

    private RLogicalVector performLogicalVectorOp(RAbstractLogicalVector left, double rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(rightValue);
        for (int i = 0; i < length; ++i) {
            byte leftValue = left.getDataAt(i);
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.getDimensions());
        ret.copyNamesFrom(left);
        return ret;
    }

    private RLogicalVector performLogicalVectorOp(RAbstractLogicalVector left, String rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(rightValue);
        for (int i = 0; i < length; ++i) {
            String leftValue = RRuntime.logicalToString(left.getDataAt(i));
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.getDimensions());
        ret.copyNamesFrom(left);
        return ret;
    }

    private RLogicalVector performLogicalVectorOp(RAbstractLogicalVector left, RComplex rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(rightValue);
        for (int i = 0; i < length; ++i) {
            RComplex leftValue = RRuntime.logical2complex(left.getDataAt(i));
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.getDimensions());
        ret.copyNamesFrom(left);
        return ret;
    }

    // string vector and scalar implementation

    private RLogicalVector performStringVectorOp(RAbstractStringVector left, String rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(rightValue);
        for (int i = 0; i < length; ++i) {
            String leftValue = left.getDataAt(i);
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.getDimensions());
        ret.copyNamesFrom(left);
        return ret;
    }

    // complex vector and scalar implementation

    private RLogicalVector performComplexVectorOp(RAbstractComplexVector left, RComplex rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(rightValue);
        for (int i = 0; i < length; ++i) {
            RComplex leftValue = left.getDataAt(i);
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.getDimensions());
        ret.copyNamesFrom(left);
        return ret;
    }

    private RLogicalVector performComplexVectorOp(RAbstractComplexVector left, String rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(rightValue);
        for (int i = 0; i < length; ++i) {
            String leftValue = RRuntime.complexToString(left.getDataAt(i));
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.getDimensions());
        ret.copyNamesFrom(left);
        return ret;
    }

    private RLogicalVector performRawVectorOp(RAbstractRawVector left, double rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(rightValue);
        for (int i = 0; i < length; ++i) {
            double leftValue = RRuntime.raw2double(left.getDataAt(i));
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.getDimensions());
        ret.copyNamesFrom(left);
        return ret;
    }

    private RLogicalVector performRawVectorOp(RAbstractRawVector left, String rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(rightValue);
        for (int i = 0; i < length; ++i) {
            String leftValue = RRuntime.rawToString(left.getDataAt(i));
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.getDimensions());
        ret.copyNamesFrom(left);
        return ret;
    }

    private RLogicalVector performRawVectorOp(RAbstractRawVector left, RComplex rightValue, boolean reverse) {
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(rightValue);
        for (int i = 0; i < length; ++i) {
            RComplex leftValue = RRuntime.raw2complex(left.getDataAt(i));
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : (!reverse ? logic.op(leftValue, rightValue) : logic.op(rightValue, leftValue));
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.getDimensions());
        ret.copyNamesFrom(left);
        return ret;
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
        ret.copyNamesFrom(left);
        return ret;
    }

    // int vector and vectors implementation

    private RLogicalVector performIntVectorOpSameLength(RAbstractIntVector left, RAbstractIntVector right) {
        assert areSameLength(left, right);
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(!right.isComplete());
        for (int i = 0; i < length; ++i) {
            int leftValue = left.getDataAt(i);
            int rightValue = right.getDataAt(i);
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : logic.op(leftValue, rightValue);
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.hasDimensions() ? left.getDimensions() : right.getDimensions());
        ret.copyNamesFrom(left.getNames() != null && left.getNames() != RNull.instance ? left : right);
        return ret;
    }

    private RLogicalVector performIntVectorOpDifferentLength(RAbstractIntVector left, RAbstractIntVector right) {
        assert !areSameLength(left, right);
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        int resultLength = Math.max(leftLength, rightLength);
        byte[] result = new byte[resultLength];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(!right.isComplete());
        int l = 0;
        int r = 0;
        for (int i = 0; i < resultLength; ++i, l = Utils.incMod(l, leftLength), r = Utils.incMod(r, rightLength)) {
            int leftValue = left.getDataAt(l);
            int rightValue = right.getDataAt(r);
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : logic.op(leftValue, rightValue);
            result[i] = resultValue;
        }
        boolean notMultiple = l != 0 || r != 0;
        if (notMultiple) {
            getContext().setEvalWarning(RError.LENGTH_NOT_MULTI);
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA());
        ret.setDimensions(left.hasDimensions() ? left.getDimensions() : right.getDimensions(), this.getSourceSection());
        ret.copyNamesFrom(leftLength == resultLength ? left : right);
        return ret;
    }

    // double vector and vectors implementation

    private RLogicalVector performDoubleVectorOpSameLength(RAbstractDoubleVector left, RAbstractDoubleVector right) {
        assert areSameLength(left, right);
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(!right.isComplete());
        for (int i = 0; i < length; ++i) {
            double leftValue = left.getDataAt(i);
            double rightValue = right.getDataAt(i);
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : logic.op(leftValue, rightValue);
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.hasDimensions() ? left.getDimensions() : right.getDimensions());
        ret.copyNamesFrom(left.getNames() != null && left.getNames() != RNull.instance ? left : right);
        return ret;
    }

    private RLogicalVector performDoubleVectorOpDifferentLength(RAbstractDoubleVector left, RAbstractDoubleVector right) {
        assert !areSameLength(left, right);
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        int resultLength = Math.max(leftLength, rightLength);
        byte[] result = new byte[resultLength];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(!right.isComplete());
        int l = 0;
        int r = 0;
        for (int i = 0; i < resultLength; ++i, l = Utils.incMod(l, leftLength), r = Utils.incMod(r, rightLength)) {
            double leftValue = left.getDataAt(l);
            double rightValue = right.getDataAt(r);
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : logic.op(leftValue, rightValue);
            result[i] = resultValue;
        }
        boolean notMultiple = l != 0 || r != 0;
        if (notMultiple) {
            getContext().setEvalWarning(RError.LENGTH_NOT_MULTI);
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA());
        ret.setDimensions(left.hasDimensions() ? left.getDimensions() : right.getDimensions(), this.getSourceSection());
        ret.copyNamesFrom(leftLength == resultLength ? left : right);
        return ret;
    }

    // string vector and vectors implementation

    private RLogicalVector performStringVectorOpSameLength(RAbstractStringVector left, RAbstractStringVector right) {
        assert areSameLength(left, right);
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(!right.isComplete());
        for (int i = 0; i < length; ++i) {
            String leftValue = left.getDataAt(i);
            String rightValue = right.getDataAt(i);
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : logic.op(leftValue, rightValue);
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.hasDimensions() ? left.getDimensions() : right.getDimensions());
        ret.copyNamesFrom(left.getNames() != null && left.getNames() != RNull.instance ? left : right);
        return ret;
    }

    private RLogicalVector performStringVectorOpDifferentLength(RAbstractStringVector left, RAbstractStringVector right) {
        assert !areSameLength(left, right);
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        int resultLength = Math.max(leftLength, rightLength);
        byte[] result = new byte[resultLength];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(!right.isComplete());
        int l = 0;
        int r = 0;
        for (int i = 0; i < resultLength; ++i, l = Utils.incMod(l, leftLength), r = Utils.incMod(r, rightLength)) {
            String leftValue = left.getDataAt(l);
            String rightValue = right.getDataAt(r);
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : logic.op(leftValue, rightValue);
            result[i] = resultValue;
        }
        boolean notMultiple = l != 0 || r != 0;
        if (notMultiple) {
            getContext().setEvalWarning(RError.LENGTH_NOT_MULTI);
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA());
        ret.setDimensions(left.hasDimensions() ? left.getDimensions() : right.getDimensions(), this.getSourceSection());
        ret.copyNamesFrom(leftLength == resultLength ? left : right);
        return ret;
    }

    // complex vector and vectors implementation

    private RLogicalVector performComplexVectorOpSameLength(RAbstractComplexVector left, RAbstractComplexVector right) {
        assert areSameLength(left, right);
        int length = left.getLength();
        byte[] result = new byte[length];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(!right.isComplete());
        for (int i = 0; i < length; ++i) {
            RComplex leftValue = left.getDataAt(i);
            RComplex rightValue = right.getDataAt(i);
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : logic.op(leftValue, rightValue);
            result[i] = resultValue;
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA(), left.hasDimensions() ? left.getDimensions() : right.getDimensions());
        ret.copyNamesFrom(left.getNames() != null && left.getNames() != RNull.instance ? left : right);
        return ret;
    }

    private RLogicalVector performComplexVectorOpDifferentLength(RAbstractComplexVector left, RAbstractComplexVector right) {
        assert !areSameLength(left, right);
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        int resultLength = Math.max(leftLength, rightLength);
        byte[] result = new byte[resultLength];
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(!right.isComplete());
        int l = 0;
        int r = 0;
        for (int i = 0; i < resultLength; ++i, l = Utils.incMod(l, leftLength), r = Utils.incMod(r, rightLength)) {
            RComplex leftValue = left.getDataAt(l);
            RComplex rightValue = right.getDataAt(r);
            byte resultValue = leftNACheck.check(leftValue) || rightNACheck.check(rightValue) ? RRuntime.LOGICAL_NA : logic.op(leftValue, rightValue);
            result[i] = resultValue;
        }
        boolean notMultiple = l != 0 || r != 0;
        if (notMultiple) {
            getContext().setEvalWarning(RError.LENGTH_NOT_MULTI);
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA());
        ret.setDimensions(left.hasDimensions() ? left.getDimensions() : right.getDimensions(), this.getSourceSection());
        ret.copyNamesFrom(leftLength == resultLength ? left : right);
        return ret;
    }

    // complex vector and vectors implementation

    private RRawVector performRawVectorOpSameLength(RAbstractRawVector left, RAbstractRawVector right) {
        assert areSameLength(left, right);
        int length = left.getLength();
        byte[] result = new byte[length];
        for (int i = 0; i < length; ++i) {
            RRaw leftValue = left.getDataAt(i);
            RRaw rightValue = right.getDataAt(i);
            byte resultValue = logic.op(leftValue, rightValue).getValue();
            result[i] = resultValue;
        }
        RRawVector ret = RDataFactory.createRawVector(result, left.hasDimensions() ? left.getDimensions() : right.getDimensions());
        ret.copyNamesFrom(left.getNames() != null && left.getNames() != RNull.instance ? left : right);
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
            getContext().setEvalWarning(RError.LENGTH_NOT_MULTI);
        }
        RRawVector ret = RDataFactory.createRawVector(result);
        ret.setDimensions(left.hasDimensions() ? left.getDimensions() : right.getDimensions(), this.getSourceSection());
        ret.copyNamesFrom(leftLength == resultLength ? left : right);
        return ret;
    }
}
