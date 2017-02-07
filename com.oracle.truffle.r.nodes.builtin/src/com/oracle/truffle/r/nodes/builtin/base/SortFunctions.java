/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

/**
 * The internal functions mandated by {@code base/sort.R}. N.B. We use the standard JDK sorting
 * algorithms and not the specific algorithms specified in the R manual entry.
 */
public class SortFunctions {

    private abstract static class Adapter extends RBuiltinNode {
        protected static void addCastForX(Casts casts) {
            casts.arg("x").allowNull().mustBe(abstractVectorValue(), SHOW_CALLER, ONLY_ATOMIC_CAN_BE_SORTED);
        }

        protected static void addCastForDecreasing(Casts casts) {
            casts.arg("decreasing").defaultError(SHOW_CALLER, INVALID_LOGICAL, "decreasing").mustBe(numericValue()).asLogicalVector().findFirst().map(toBoolean());
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

        static {
            Casts casts = new Casts(Sort.class);
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

        static {
            Casts casts = new Casts(QSort.class);
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

        static {
            Casts casts = new Casts(PartialSort.class);
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

    /**
     * This a helper function for the code in sort.R. It does NOT return the input vectors sorted,
     * but returns an {@link RIntVector} of indices (positions) indicating the sort order (Or
     * {@link RNull#instance} if no vectors). In short it is a special variant of {@code order}. For
     * now we delegate to {@code order} and do not implement the {@code retgrp} argument.
     */
    @RBuiltin(name = "radixsort", kind = INTERNAL, parameterNames = {"na.last", "decreasing", "retgrp", "sortstr", "..."}, behavior = PURE)
    public abstract static class RadixSort extends Adapter {
        @Child private Order orderNode = OrderNodeGen.create();

        static {
            Casts casts = new Casts(RadixSort.class);
            casts.arg("na.last").asLogicalVector().findFirst();
            casts.arg("decreasing").mustBe(numericValue(), SHOW_CALLER, INVALID_LOGICAL, "decreasing").asLogicalVector();
            casts.arg("retgrp").asLogicalVector().findFirst().map(toBoolean());
            casts.arg("sortstr").asLogicalVector().findFirst().map(toBoolean());
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object radixSort(byte naLast, RAbstractLogicalVector decreasingVec, boolean retgrp, boolean sortstr, RArgsValuesAndNames zz) {
            // Partial implementation just to get startup to work
            if (retgrp) {
                // sortstr only has an effect when retrgrp == true
                throw RError.nyi(this, "radixsort: retgrp == TRUE not implemented");
            }
            int nargs = zz.getLength();
            if (nargs == 0) {
                return RNull.instance;
            }
            if (nargs != decreasingVec.getLength()) {
                throw RError.error(this, RError.Message.RADIX_SORT_DEC_MATCH);
            }
            /*
             * Order takes a single decreasing argument that applies to all the vectors. We
             * potentially have a different value for each vector, so we have to process one by one.
             * However, OrderNode can't yet handle that, so we abort if nargs > 1 and the decreasing
             * values don't match.
             */
            byte lastdb = RRuntime.LOGICAL_NA;
            for (int i = 0; i < nargs; i++) {
                byte db = decreasingVec.getDataAt(i);
                if (RRuntime.isNA(db)) {
                    throw RError.error(this, RError.Message.RADIX_SORT_DEC_NOT_LOGICAL);
                }
                if (lastdb != RRuntime.LOGICAL_NA && db != lastdb) {
                    throw RError.nyi(this, "radixsort: args > 1 with differing 'decreasing' values not implemented");
                }
                lastdb = db;
            }
            boolean decreasing = RRuntime.fromLogical(decreasingVec.getDataAt(0));
            Object result = orderNode.execute(naLast, decreasing, zz);
            return result;
        }
    }
}
