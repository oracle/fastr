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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@NodeField(name = "emptyVectorConvertedToNull", type = boolean.class)
public abstract class CastStringNode extends CastNode {

    @Child private ToStringNode toString = ToStringNodeGen.create(null, null, null);

    public abstract Object executeString(VirtualFrame frame, int o);

    public abstract Object executeString(VirtualFrame frame, double o);

    public abstract Object executeString(VirtualFrame frame, byte o);

    public abstract Object executeString(VirtualFrame frame, Object o);

    public abstract boolean isEmptyVectorConvertedToNull();

    @Specialization
    protected RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    @Specialization
    protected String doString(String value) {
        return value;
    }

    private String toString(VirtualFrame frame, Object value) {
        return toString.executeString(frame, value, false, ToStringNode.DEFAULT_SEPARATOR);
    }

    @Specialization
    protected String doInteger(VirtualFrame frame, int value) {
        return toString(frame, value);
    }

    @Specialization
    protected String doDouble(VirtualFrame frame, double value) {
        return toString(frame, value);
    }

    @Specialization
    protected String doLogical(VirtualFrame frame, byte value) {
        return toString(frame, value);
    }

    @Specialization
    protected String doRaw(VirtualFrame frame, RComplex value) {
        return toString(frame, value);
    }

    @Specialization
    protected String doRaw(VirtualFrame frame, RRaw value) {
        return toString(frame, value);
    }

    @Specialization(guards = "vector.getLength() == 0")
    protected Object doEmptyVector(@SuppressWarnings("unused") RAbstractVector vector) {
        return isEmptyVectorConvertedToNull() ? RNull.instance : RDataFactory.createStringVector(0);
    }

    @Specialization(guards = "vector.getLength() != 0")
    protected RStringVector doStringVector(RStringVector vector) {
        return vector;
    }

    @Specialization(guards = "operand.getLength() != 0")
    protected RAbstractContainer doIntVector(VirtualFrame frame, RAbstractContainer operand) {
        String[] sdata = new String[operand.getLength()];
        // conversions to character will not introduce new NAs
        for (int i = 0; i < operand.getLength(); i++) {
            sdata[i] = toString(frame, operand.getDataAtAsObject(i));
        }
        RStringVector ret = RDataFactory.createStringVector(sdata, operand.isComplete(), getPreservedDimensions(operand), getPreservedNames(operand));
        preserveDimensionNames(operand, ret);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization
    protected String doRSymbol(RSymbol s) {
        return s.getName();
    }

}
