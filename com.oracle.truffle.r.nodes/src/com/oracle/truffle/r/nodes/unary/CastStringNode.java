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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.base.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@NodeField(name = "emptyVectorConvertedToNull", type = boolean.class)
public abstract class CastStringNode extends CastNode {

    @Child private ToString toString = ToStringFactory.create(new RNode[1], null);

    public abstract Object executeString(VirtualFrame frame, int o);

    public abstract Object executeString(VirtualFrame frame, double o);

    public abstract Object executeString(VirtualFrame frame, byte o);

    public abstract Object executeString(VirtualFrame frame, Object o);

    public abstract boolean isEmptyVectorConvertedToNull();

    public CastStringNode() {
        toString.setQuotes(false);
    }

    @Specialization
    public RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    @Specialization
    public String doString(String value) {
        return value;
    }

    @Specialization
    public String doInteger(VirtualFrame frame, int value) {
        return toString.executeString(frame, value);
    }

    @Specialization
    public String doDouble(VirtualFrame frame, double value) {
        return toString.executeString(frame, value);
    }

    @Specialization
    public String doLogical(VirtualFrame frame, byte value) {
        return toString.executeString(frame, value);
    }

    @Specialization
    public String doRaw(VirtualFrame frame, RRaw value) {
        return toString.executeString(frame, value);
    }

    private String[] dataFromLogical(VirtualFrame frame, RLogicalVector operand) {
        String[] sdata = new String[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            byte value = operand.getDataAt(i);
            sdata[i] = toString.executeString(frame, value);
        }
        return sdata;
    }

    private String[] dataFromComplex(VirtualFrame frame, RComplexVector operand) {
        String[] sdata = new String[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            RComplex value = operand.getDataAt(i);
            sdata[i] = toString.executeString(frame, value);
        }
        return sdata;
    }

    private String[] dataFromRaw(VirtualFrame frame, RRawVector operand) {
        String[] sdata = new String[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            RRaw value = operand.getDataAt(i);
            sdata[i] = toString.executeString(frame, value);
        }
        return sdata;
    }

    private String[] dataFromList(VirtualFrame frame, RList operand) {
        String[] sdata = new String[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            Object value = operand.getDataAt(i);
            sdata[i] = toString.executeString(frame, value);
        }
        return sdata;
    }

    @Specialization(order = 100, guards = "isZeroLength")
    public Object doEmptyVector(@SuppressWarnings("unused") RAbstractVector vector) {
        return isEmptyVectorConvertedToNull() ? RNull.instance : RDataFactory.createStringVector(0);
    }

    @Specialization(order = 102, guards = "!isZeroLength")
    public RStringVector doStringVector(RStringVector vector) {
        return vector;
    }

    @Specialization(order = 103, guards = "!isZeroLength")
    public RStringVector doIntVector(VirtualFrame frame, RIntVector vector) {
        return performAbstractIntVector(frame, vector);
    }

    @Specialization(order = 104, guards = "!isZeroLength")
    public RStringVector doDoubleVector(VirtualFrame frame, RDoubleVector vector) {
        return performAbstractDoubleVector(frame, vector);
    }

    @Specialization(order = 105, guards = "!isZeroLength")
    public RStringVector doIntSequence(VirtualFrame frame, RIntSequence vector) {
        return performAbstractIntVector(frame, vector);
    }

    @Specialization(order = 106, guards = "!isZeroLength")
    public RStringVector doDoubleSequence(VirtualFrame frame, RDoubleSequence vector) {
        return performAbstractDoubleVector(frame, vector);
    }

    @Specialization(order = 107, guards = {"!isZeroLength", "!preserveNames", "preserveDimensions"})
    public RStringVector doLogicalVectorDims(VirtualFrame frame, RLogicalVector vector) {
        String[] result = dataFromLogical(frame, vector);
        RStringVector ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR, vector.getDimensions());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(order = 108, guards = {"!isZeroLength", "preserveNames", "!preserveDimensions"})
    public RStringVector doLogicalVectorNames(VirtualFrame frame, RLogicalVector vector) {
        String[] result = dataFromLogical(frame, vector);
        RStringVector ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR, vector.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(order = 109, guards = {"!isZeroLength", "preserveNames", "preserveDimensions"})
    public RStringVector doLogicalVectorDimsNames(VirtualFrame frame, RLogicalVector vector) {
        String[] result = dataFromLogical(frame, vector);
        RStringVector ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR, vector.getDimensions(), vector.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(order = 110, guards = {"!isZeroLength", "!preserveNames", "!preserveDimensions"})
    public RStringVector doLogicalVector(VirtualFrame frame, RLogicalVector vector) {
        String[] result = dataFromLogical(frame, vector);
        RStringVector ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(order = 111, guards = {"!isZeroLength", "!preserveNames", "preserveDimensions"})
    public RStringVector doComplexVectorDims(VirtualFrame frame, RComplexVector vector) {
        String[] result = dataFromComplex(frame, vector);
        RStringVector ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR, vector.getDimensions());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(order = 112, guards = {"!isZeroLength", "preserveNames", "!preserveDimensions"})
    public RStringVector doComplexVectorNames(VirtualFrame frame, RComplexVector vector) {
        String[] result = dataFromComplex(frame, vector);
        RStringVector ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR, vector.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(order = 113, guards = {"!isZeroLength", "preserveNames", "preserveDimensions"})
    public RStringVector doComplexVectorDimsNames(VirtualFrame frame, RComplexVector vector) {
        String[] result = dataFromComplex(frame, vector);
        RStringVector ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR, vector.getDimensions(), vector.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(order = 114, guards = {"!isZeroLength", "!preserveNames", "!preserveDimensions"})
    public RStringVector doComplexVector(VirtualFrame frame, RComplexVector vector) {
        String[] result = dataFromComplex(frame, vector);
        RStringVector ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(order = 115, guards = {"!isZeroLength", "!preserveNames", "preserveDimensions"})
    public RStringVector doRawVectorDims(VirtualFrame frame, RRawVector vector) {
        String[] result = dataFromRaw(frame, vector);
        RStringVector ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR, vector.getDimensions());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(order = 116, guards = {"!isZeroLength", "preserveNames", "!preserveDimensions"})
    public RStringVector doRawVectorNames(VirtualFrame frame, RRawVector vector) {
        String[] result = dataFromRaw(frame, vector);
        RStringVector ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR, vector.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(order = 117, guards = {"!isZeroLength", "preserveNames", "preserveDimensions"})
    public RStringVector doRawVectorDimsNames(VirtualFrame frame, RRawVector vector) {
        String[] result = dataFromRaw(frame, vector);
        RStringVector ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR, vector.getDimensions(), vector.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(order = 118, guards = {"!isZeroLength", "!preserveNames", "!preserveDimensions"})
    public RStringVector doRawVector(VirtualFrame frame, RRawVector vector) {
        String[] result = dataFromRaw(frame, vector);
        RStringVector ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(order = 119, guards = {"!isZeroLength", "!preserveNames", "preserveDimensions"})
    public RStringVector doListDims(VirtualFrame frame, RList list) {
        String[] result = dataFromList(frame, list);
        RStringVector ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR, list.getDimensions());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(list);
        }
        return ret;
    }

    @Specialization(order = 120, guards = {"!isZeroLength", "preserveNames", "!preserveDimensions"})
    public RStringVector doListNames(VirtualFrame frame, RList list) {
        String[] result = dataFromList(frame, list);
        RStringVector ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR, list.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(list);
        }
        return ret;
    }

    @Specialization(order = 121, guards = {"!isZeroLength", "preserveNames", "preserveDimensions"})
    public RStringVector doListDimsNames(VirtualFrame frame, RList list) {
        String[] result = dataFromList(frame, list);
        RStringVector ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR, list.getDimensions(), list.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(list);
        }
        return ret;
    }

    @Specialization(order = 122, guards = {"!isZeroLength", "!preserveNames", "!preserveDimensions"})
    public RStringVector doList(VirtualFrame frame, RList list) {
        String[] result = dataFromList(frame, list);
        RStringVector ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(list);
        }
        return ret;
    }

    private RStringVector performAbstractIntVector(VirtualFrame frame, RAbstractIntVector vector) {
        int length = vector.getLength();
        String[] result = new String[length];
        for (int i = 0; i < length; i++) {
            result[i] = toString.executeString(frame, vector.getDataAt(i));
        }
        RStringVector ret;
        if (preserveDimensions()) {
            ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR, vector.getDimensions());
        } else if (preserveNames()) {
            ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR, vector.getNames());
        } else {
            ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
        }
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    private RStringVector performAbstractDoubleVector(VirtualFrame frame, RAbstractDoubleVector vector) {
        int length = vector.getLength();
        String[] result = new String[length];
        for (int i = 0; i < length; i++) {
            result[i] = toString.executeString(frame, vector.getDataAt(i));
        }
        RStringVector ret;
        if (preserveDimensions() && preserveNames()) {
            ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR, vector.getDimensions(), vector.getNames());
        } else if (preserveDimensions()) {
            ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR, vector.getDimensions());
        } else if (preserveNames()) {
            ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR, vector.getNames());
        } else {
            ret = RDataFactory.createStringVector(result, RDataFactory.COMPLETE_VECTOR);
        }
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    protected boolean isZeroLength(@SuppressWarnings("unused") VirtualFrame frame, RAbstractVector vector) {
        return vector.getLength() == 0;
    }

}
