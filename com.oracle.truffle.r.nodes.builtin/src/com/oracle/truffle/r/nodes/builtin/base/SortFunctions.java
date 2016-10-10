/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RError.SHOW_CALLER;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_LOGICAL;
import static com.oracle.truffle.r.runtime.RError.Message.ONLY_ATOMIC_CAN_BE_SORTED;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.Arrays;
import java.util.Collections;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

/**
 * The internal functions mandated by {@code base/sort.R}. N.B. We use the standard JDK sorting
 * algorithms and not the specific algorithms specified in the R manual entry. TODO: implement psort
 * and radixsort.
 */
public class SortFunctions {

    private abstract static class Adapter extends RBuiltinNode {
        protected static void addCastForX(CastBuilder castBuilder) {
            castBuilder.arg("x").allowNull().mustBe(abstractVectorValue(), SHOW_CALLER, ONLY_ATOMIC_CAN_BE_SORTED);
        }

        protected static void addCastForDecreasing(CastBuilder castBuilder) {
            castBuilder.arg("decreasing").defaultError(SHOW_CALLER, INVALID_LOGICAL, "decreasing").mustBe(numericValue()).asLogicalVector().findFirst().map(toBoolean());
        }

        @TruffleBoundary
        private static double[] sort(double[] data, boolean decreasing) {
            // no reverse comparator for primitives
            Arrays.parallelSort(data);
            if (decreasing) {
                int len = data.length;
                for (int i = len / 2 - 1; i >= 0; i--) {
                    double temp = data[i];
                    data[i] = data[len - i - 1];
                    data[len - i - 1] = temp;
                }
            }
            return data;
        }

        @TruffleBoundary
        private static int[] sort(int[] data, boolean decreasing) {
            Arrays.parallelSort(data);
            if (decreasing) {
                int len = data.length;
                for (int i = len / 2 - 1; i >= 0; i--) {
                    int temp = data[i];
                    data[i] = data[len - i - 1];
                    data[len - i - 1] = temp;
                }
            }
            return data;
        }

        @TruffleBoundary
        private static byte[] sort(byte[] data, boolean decreasing) {
            Arrays.parallelSort(data);
            if (decreasing) {
                int len = data.length;
                for (int i = len / 2 - 1; i >= 0; i--) {
                    byte temp = data[i];
                    data[i] = data[len - i - 1];
                    data[len - i - 1] = temp;
                }
            }
            return data;
        }

        @TruffleBoundary
        private static String[] sort(String[] data, boolean decreasing) {
            if (decreasing) {
                Arrays.parallelSort(data, Collections.reverseOrder());
            } else {
                Arrays.parallelSort(data);
            }
            return data;
        }

        protected RDoubleVector jdkSort(RAbstractDoubleVector vec, boolean decreasing) {
            double[] data = vec.materialize().getDataCopy();
            return RDataFactory.createDoubleVector(sort(data, decreasing), vec.isComplete());
        }

        protected RIntVector jdkSort(RAbstractIntVector vec, boolean decreasing) {
            int[] data = vec.materialize().getDataCopy();
            return RDataFactory.createIntVector(sort(data, decreasing), vec.isComplete());
        }

        protected RStringVector jdkSort(RAbstractStringVector vec, boolean decreasing) {
            String[] data = vec.materialize().getDataCopy();
            return RDataFactory.createStringVector(sort(data, decreasing), vec.isComplete());
        }

        protected RLogicalVector jdkSort(RAbstractLogicalVector vec, boolean decreasing) {
            byte[] data = vec.materialize().getDataCopy();
            return RDataFactory.createLogicalVector(sort(data, decreasing), vec.isComplete());
        }
    }

    /**
     * In GnuR this is a shell sort variant, see
     * <a href = "https://stat.ethz.ch/R-manual/R-devel/library/base/html/sort.html>here">here</a>.
     * The JDK does not have a shell sort so for now we just use the default JDK sort (quicksort).
     *
     * N.B. The R code strips out {@code NA} and {@code NaN} values before calling the builtin.
     */
    @RBuiltin(name = "sort", kind = INTERNAL, parameterNames = {"x", "decreasing"}, behavior = PURE)
    public abstract static class Sort extends Adapter {

        @Override
        protected void createCasts(CastBuilder casts) {
            addCastForX(casts);
            addCastForDecreasing(casts);
        }

        @Specialization
        protected RDoubleVector sort(RAbstractDoubleVector vec, boolean decreasing) {
            return jdkSort(vec, decreasing);
        }

        @Specialization
        protected RIntVector sort(RAbstractIntVector vec, boolean decreasing) {
            return jdkSort(vec, decreasing);
        }

        @Specialization
        protected RStringVector sort(RAbstractStringVector vec, boolean decreasing) {
            return jdkSort(vec, decreasing);
        }

        @Specialization
        protected RLogicalVector sort(RAbstractLogicalVector vec, boolean decreasing) {
            return jdkSort(vec, decreasing);
        }
    }

    @RBuiltin(name = "qsort", kind = INTERNAL, parameterNames = {"x", "decreasing"}, behavior = PURE)
    public abstract static class QSort extends Adapter {

        @Override
        protected void createCasts(CastBuilder casts) {
            addCastForX(casts);
            addCastForDecreasing(casts);
        }

        @Specialization
        protected RDoubleVector qsort(RAbstractDoubleVector vec, boolean decreasing) {
            return jdkSort(vec, decreasing);
        }

        @Specialization
        protected RIntVector qsort(RAbstractIntVector vec, boolean decreasing) {
            return jdkSort(vec, decreasing);
        }
    }

    @RBuiltin(name = "psort", kind = INTERNAL, parameterNames = {"x", "partial"}, behavior = PURE)
    public abstract static class PartialSort extends Adapter {

        @Override
        protected void createCasts(CastBuilder casts) {
            addCastForX(casts);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RDoubleVector sort(RAbstractDoubleVector vec, Object partial) {
            return jdkSort(vec, false);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RIntVector sort(RAbstractIntVector vec, Object partial) {
            return jdkSort(vec, false);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RStringVector sort(RAbstractStringVector vec, Object partial) {
            return jdkSort(vec, false);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RLogicalVector sort(RAbstractLogicalVector vec, Object partial) {
            return jdkSort(vec, false);
        }
    }

    @RBuiltin(name = "radixsort", kind = INTERNAL, parameterNames = {"zz", "na.last", "decreasing"}, behavior = PURE)
    public abstract static class RadixSort extends Adapter {

        @Override
        protected void createCasts(CastBuilder casts) {
            addCastForDecreasing(casts);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object radixSort(Object zz, Object naLast, Object decreasing) {
            throw RError.nyi(this, ".Internal(raxdixsort)");
        }
    }
}
