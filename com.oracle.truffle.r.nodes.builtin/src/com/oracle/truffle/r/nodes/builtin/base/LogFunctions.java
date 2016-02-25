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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public class LogFunctions {
    @RBuiltin(name = "log", kind = PRIMITIVE, parameterNames = {"x", "base"})
    public abstract static class Log extends RBuiltinNode {

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, Math.E};
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            // // base argument is at index 1, and double
            // arguments[1] = CastDoubleNodeGen.create(arguments[1], true, false, false);
            casts.toDouble(1);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RNull log(RNull x, RNull base) {
            controlVisibility();
            throw RError.error(this, RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION);
        }

        @Specialization
        protected double log(int x, double base) {
            controlVisibility();
            return logb(x, base);
        }

        @Specialization
        protected double log(double x, double base) {
            controlVisibility();
            return logb(x, base);
        }

        @Specialization
        protected RDoubleVector log(RIntVector vector, double base) {
            controlVisibility();
            double[] resultVector = new double[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                int inputValue = vector.getDataAt(i);
                double result = RRuntime.DOUBLE_NA;
                if (RRuntime.isComplete(inputValue)) {
                    result = logb(inputValue, base);
                }
                resultVector[i] = result;
            }
            return RDataFactory.createDoubleVector(resultVector, vector.isComplete());
        }

        @Specialization
        protected RDoubleVector log(RDoubleVector vector, double base) {
            controlVisibility();
            double[] doubleVector = new double[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                double value = vector.getDataAt(i);
                if (RRuntime.isComplete(value)) {
                    value = logb(value, base);
                }
                doubleVector[i] = value;
            }
            return RDataFactory.createDoubleVector(doubleVector, vector.isComplete());
        }

        protected static double logb(double x, double base) {
            return Math.log(x) / Math.log(base);
        }
    }

    @RBuiltin(name = "log10", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class Log10 extends RBuiltinNode {

        private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

        @SuppressWarnings("unused")
        @Specialization
        protected RNull log(RNull x) {
            controlVisibility();
            throw RError.error(this, RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION);
        }

        @Specialization
        protected double log(int value) {
            controlVisibility();
            return Math.log10(value);
        }

        @Specialization
        protected double log(double value) {
            controlVisibility();
            return Math.log10(value);
        }

        @Specialization
        protected RDoubleVector log(RIntVector vector) {
            controlVisibility();
            double[] resultVector = new double[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                int inputValue = vector.getDataAt(i);
                double result = RRuntime.DOUBLE_NA;
                if (RRuntime.isComplete(inputValue)) {
                    result = Math.log10(inputValue);
                }
                resultVector[i] = result;
            }
            RDoubleVector res = RDataFactory.createDoubleVector(resultVector, vector.isComplete(), vector.getNames(attrProfiles));
            res.copyRegAttributesFrom(vector);
            return res;
        }

        @Specialization
        protected RDoubleVector log(RDoubleVector vector) {
            controlVisibility();
            double[] doubleVector = new double[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                double value = vector.getDataAt(i);
                if (RRuntime.isComplete(value)) {
                    value = Math.log10(value);
                }
                doubleVector[i] = value;
            }
            RDoubleVector res = RDataFactory.createDoubleVector(doubleVector, vector.isComplete(), vector.getNames(attrProfiles));
            res.copyRegAttributesFrom(vector);
            return res;
        }
    }

    @RBuiltin(name = "log2", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class Log2 extends RBuiltinNode {

        private static final double log2value = Math.log(2);

        @SuppressWarnings("unused")
        @Specialization
        protected RNull log(RNull x) {
            controlVisibility();
            throw RError.error(this, RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION);
        }

        @Specialization
        protected double log2(int value) {
            controlVisibility();
            return log2((double) value);
        }

        @Specialization
        protected double log2(double value) {
            controlVisibility();
            return Math.log(value) / log2value;
        }

        @Specialization
        protected RDoubleVector log2(RIntVector vector) {
            controlVisibility();
            double[] resultVector = new double[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                int inputValue = vector.getDataAt(i);
                double result = RRuntime.DOUBLE_NA;
                if (RRuntime.isComplete(inputValue)) {
                    result = log2(inputValue);
                }
                resultVector[i] = result;
            }
            return RDataFactory.createDoubleVector(resultVector, vector.isComplete());
        }

        @Specialization
        protected RDoubleVector log2(RDoubleVector vector) {
            controlVisibility();
            double[] doubleVector = new double[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                double value = vector.getDataAt(i);
                if (RRuntime.isComplete(value)) {
                    value = log2(value);
                }
                doubleVector[i] = value;
            }
            return RDataFactory.createDoubleVector(doubleVector, vector.isComplete());
        }
    }

    @RBuiltin(name = "log1p", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class Log1p extends RBuiltinNode {

        @SuppressWarnings("unused")
        @Specialization
        protected Object log1p(Object x) {
            throw RError.nyi(this, "log1p");
        }
    }

}
