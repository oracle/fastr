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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

import java.util.*;

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

/**
 * The internal functions mandated by {@code base/sort.R}. N.B. We use the standard JDK sorting
 * algorithms and not the specific algorithms specified in the R manual entry. TODO: implement psort
 * and radixsort.
 */
public class SortFunctions {

    private abstract static class Adapter extends RBuiltinNode {
        @TruffleBoundary
        private static double[] sort(double[] data, byte decreasing) {
            // no reverse comparator for primitives
            Arrays.sort(data);
            if (RRuntime.fromLogical(decreasing)) {
                int len = data.length;
                double[] revData = new double[len];
                for (int i = 0; i < len; i++) {
                    revData[i] = data[len - i - 1];
                }
                return revData;
            } else {
                return data;
            }
        }

        @TruffleBoundary
        private static int[] sort(int[] data, byte decreasing) {
            Arrays.sort(data);
            if (RRuntime.fromLogical(decreasing)) {
                int len = data.length;
                int[] revData = new int[len];
                for (int i = 0; i < len; i++) {
                    revData[i] = data[len - i - 1];
                }
                return revData;
            } else {
                return data;
            }
        }

        @TruffleBoundary
        private static byte[] sort(byte[] data, byte decreasing) {
            Arrays.sort(data);
            if (RRuntime.fromLogical(decreasing)) {
                int len = data.length;
                byte[] revData = new byte[len];
                for (int i = 0; i < len; i++) {
                    revData[i] = data[len - i - 1];
                }
                return revData;
            } else {
                return data;
            }
        }

        @TruffleBoundary
        private static String[] sort(String[] data, byte decreasing) {
            if (RRuntime.fromLogical(decreasing)) {
                Arrays.sort(data, Collections.reverseOrder());
            } else {
                Arrays.sort(data);
            }
            return data;
        }

        protected RDoubleVector jdkSort(RAbstractDoubleVector vec, byte decreasing) {
            double[] data = vec.materialize().getDataCopy();
            return RDataFactory.createDoubleVector(sort(data, decreasing), vec.isComplete());
        }

        protected RIntVector jdkSort(RAbstractIntVector vec, byte decreasing) {
            int[] data = vec.materialize().getDataCopy();
            return RDataFactory.createIntVector(sort(data, decreasing), vec.isComplete());
        }

        protected RStringVector jdkSort(RAbstractStringVector vec, byte decreasing) {
            String[] data = vec.materialize().getDataCopy();
            return RDataFactory.createStringVector(sort(data, decreasing), vec.isComplete());
        }

        protected RLogicalVector jdkSort(RAbstractLogicalVector vec, byte decreasing) {
            byte[] data = vec.materialize().getDataCopy();
            return RDataFactory.createLogicalVector(sort(data, decreasing), vec.isComplete());
        }

    }

    /**
     * In GnuR this is a shell sort variant, see <a href =
     * "https://stat.ethz.ch/R-manual/R-devel/library/base/html/sort.html>here">here</a>. The JDK
     * does not have a shell sort so for now we just use the default JDK sort (quicksort).
     *
     * N.B. The R code strips out {@code NA} and {@code NaN} values before calling the builtin.
     */
    @RBuiltin(name = "sort", kind = INTERNAL, parameterNames = {"x", "decreasing"})
    public abstract static class Sort extends Adapter {
        @Specialization
        protected RDoubleVector sort(RAbstractDoubleVector vec, byte decreasing) {
            return jdkSort(vec, decreasing);
        }

        @Specialization
        protected RIntVector sort(RAbstractIntVector vec, byte decreasing) {
            return jdkSort(vec, decreasing);
        }

        @Specialization
        protected RStringVector sort(RAbstractStringVector vec, byte decreasing) {
            return jdkSort(vec, decreasing);
        }

        @Specialization
        protected RLogicalVector sort(RAbstractLogicalVector vec, byte decreasing) {
            return jdkSort(vec, decreasing);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object sort(Object vec, Object decreasing) {
            throw RError.nyi(getEncapsulatingSourceSection(), " .Internal(sort)");
        }

    }

    @RBuiltin(name = "qsort", kind = INTERNAL, parameterNames = {"x", "decreasing"})
    public abstract static class QSort extends Adapter {

        @Specialization
        protected RDoubleVector qsort(RAbstractDoubleVector vec, byte decreasing) {
            return jdkSort(vec, decreasing);
        }

        @Specialization
        protected RIntVector qsort(RAbstractIntVector vec, byte decreasing) {
            return jdkSort(vec, decreasing);
        }

    }

    @RBuiltin(name = "psort", kind = INTERNAL, parameterNames = {"x", "decreasing"})
    public abstract static class PartialSort extends RBuiltinNode {
        @SuppressWarnings("unused")
        @Specialization
        protected Object pSort(Object zz, Object naLast, Object decreasing) {
            throw RError.nyi(getEncapsulatingSourceSection(), " .Internal(psort)");
        }
    }

    @RBuiltin(name = "radixsort", kind = INTERNAL, parameterNames = {"zz", "na.last", "decreasing"})
    public abstract static class RadixSort extends RBuiltinNode {
        @SuppressWarnings("unused")
        @Specialization
        protected Object radixSort(Object zz, Object naLast, Object decreasing) {
            throw RError.nyi(getEncapsulatingSourceSection(), " .Internal(raxdixsort)");
        }
    }

}
