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

import java.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@SuppressWarnings("unused")
/** Takes only RNull, double or RDoubleVector as arguments. Use CastDoubleNode to cast the operands. */
public abstract class CombineBinaryDoubleNode extends CombineBinaryNode {

    @Specialization(order = 0)
    public RNull combine(RNull left, RNull right) {
        return RNull.instance;
    }

    @Specialization(order = 1)
    public RAbstractDoubleVector combine(RNull left, RAbstractDoubleVector right) {
        return right;
    }

    @Specialization(order = 2)
    public RAbstractDoubleVector combine(RAbstractDoubleVector left, RNull right) {
        return left;
    }

    @Specialization(order = 3)
    public RDoubleVector combine(double left, double right) {
        return RDataFactory.createDoubleVector(new double[]{left, right}, RRuntime.isComplete(left) && RRuntime.isComplete(right));
    }

    @Specialization(order = 4)
    public RDoubleVector combine(RAbstractDoubleVector left, double right) {
        int dataLength = left.getLength();
        double[] result = new double[dataLength + 1];
        for (int i = 0; i < dataLength; ++i) {
            result[i] = left.getDataAt(i);
        }
        result[dataLength] = right;
        return RDataFactory.createDoubleVector(result, left.isComplete() && RRuntime.isComplete(right), combineNames(left, false));
    }

    @Specialization(order = 5)
    public RDoubleVector combine(double left, RAbstractDoubleVector right) {
        int dataLength = right.getLength();
        double[] result = new double[dataLength + 1];
        result[0] = left;
        for (int i = 0; i < dataLength; ++i) {
            result[i + 1] = right.getDataAt(i);
        }
        return RDataFactory.createDoubleVector(result, RRuntime.isComplete(left) && right.isComplete(), combineNames(right, true));
    }

    @Specialization(order = 6)
    public RDoubleVector combine(RAbstractDoubleVector left, RAbstractDoubleVector right) {
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        double[] result = new double[leftLength + rightLength];
        int i = 0;
        for (; i < leftLength; ++i) {
            result[i] = left.getDataAt(i);
        }
        for (; i < leftLength + rightLength; ++i) {
            result[i] = right.getDataAt(i - leftLength);
        }
        return RDataFactory.createDoubleVector(result, RDataFactory.COMPLETE_VECTOR, combineNames(left, right));
    }

}
