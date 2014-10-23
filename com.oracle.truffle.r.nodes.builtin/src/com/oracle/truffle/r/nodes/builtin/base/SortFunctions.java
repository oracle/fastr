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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.utilities.ConditionProfile;
import com.oracle.truffle.r.nodes.RNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

import java.util.Arrays;

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;
import static com.oracle.truffle.r.runtime.RBuiltinKind.SUBSTITUTE;

/**
 * Temporary minimal implementation for b25 benchmarks. Eventually this should be combined with
 * {@link Order} and made consistent with {@code sort.R}.
 *
 */
public class SortFunctions {

    @RBuiltin(name = "sort.list", kind = SUBSTITUTE, parameterNames = {"x", "partial", "na.last", "decreasing", "method"})
    // TODO Implement in R
    public abstract static class SortList extends RBuiltinNode {
        private final ConditionProfile orderProfile = ConditionProfile.createBinaryProfile();
        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RNull.instance), ConstantNode.create(RRuntime.LOGICAL_TRUE), ConstantNode.create(RRuntime.LOGICAL_FALSE),
                            ConstantNode.create(RMissing.instance)};
        }

        @Child private Order order;

        @SuppressWarnings("unused")
        @Specialization
        protected RIntVector sortList(VirtualFrame frame, RAbstractVector vec, RNull partial, byte naLast, byte decreasing, RMissing method) {
            controlVisibility();
            if (order == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                order = insert(OrderFactory.create(new RNode[2], getBuiltin(), getSuppliedArgsNames()));
            }
            RIntVector result = (RIntVector) order.executeDoubleVector(frame, vec, RMissing.instance);
            if (orderProfile.profile(RRuntime.fromLogical(decreasing))) {
                int[] data = result.getDataWithoutCopying();
                int[] rdata = new int[data.length];
                for (int i = 0; i < data.length; i++) {
                    rdata[i] = data[data.length - (i + 1)];
                }
                return RDataFactory.createIntVector(rdata, RDataFactory.COMPLETE_VECTOR);
            } else {
                return result;
            }
        }
    }

    @RBuiltin(name = "qsort", kind = INTERNAL, parameterNames = {"x", "index.return"})
    // TODO full implementation in Java handling NAs
    public abstract static class QSort extends RBuiltinNode {

        @SlowPath
        private static void sort(double[] data) {
            Arrays.sort(data);
        }

        @SlowPath
        private static void sort(int[] data) {
            Arrays.sort(data);
        }

        @Specialization
        protected RDoubleVector qsort(RAbstractDoubleVector vec, @SuppressWarnings("unused") Object indexReturn) {
            double[] data = vec.materialize().getDataCopy();
            sort(data);
            return RDataFactory.createDoubleVector(data, vec.isComplete());
        }

        @Specialization
        protected RIntVector qsort(RAbstractIntVector vec, @SuppressWarnings("unused") Object indexReturn) {
            int[] data = vec.materialize().getDataCopy();
            sort(data);
            return RDataFactory.createIntVector(data, vec.isComplete());
        }
    }

}
