/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.fastr;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.missingValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.instrumentation.RFunctionProfiler;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.tools.Profiler;

public class FastRFunctionProfiler {

    @RBuiltin(name = ".fastr.profiler.create", visibility = OFF, kind = PRIMITIVE, parameterNames = {"func", "mode"}, behavior = COMPLEX)
    public abstract static class Create extends RBuiltinNode {
        private static final int COUNTING = 1;
        private static final int TIMING = 2;

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, "counting"};
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("func").mustBe(instanceOf(RFunction.class).or(missingValue()));

            casts.arg("mode").mustBe(stringValue()).asStringVector();
        }

        private int checkMode(RAbstractStringVector modeVec) {
            int result = 0;
            for (int i = 0; i < modeVec.getLength(); i++) {
                String mode = modeVec.getDataAt(i);
                switch (mode) {
                    case "counting":
                        result |= COUNTING;
                        break;
                    case "timing":
                        result |= TIMING;
                        break;
                    default:
                        throw RError.error(this, RError.Message.GENERIC, "invalid 'mode', one of 'count, timning' expected");
                }
            }
            return result;
        }

        @Specialization
        @TruffleBoundary
        protected RNull createFunctionProfiler(RFunction function, RAbstractStringVector modeVec) {
            int mode = checkMode(modeVec);
            if (!function.isBuiltin()) {
                RFunctionProfiler.installTimer(function, (mode & COUNTING) != 0, (mode & TIMING) != 0);
            } else {
                throw RError.error(this, RError.Message.GENERIC, "cannot profile builtin functions");
            }
            return RNull.instance;
        }

        @Specialization
        @TruffleBoundary
        protected RNull createFunctionProfiler(@SuppressWarnings("unused") RMissing value, RAbstractStringVector modeVec) {
            int mode = checkMode(modeVec);
            RFunctionProfiler.installTimer(null, (mode & COUNTING) != 0, (mode & TIMING) != 0);
            return RNull.instance;
        }

    }

    @RBuiltin(name = ".fastr.profiler.get", kind = PRIMITIVE, parameterNames = {"func", "threshold", "scale"}, behavior = COMPLEX)
    public abstract static class Get extends RBuiltinNode {

        private static final RStringVector COLNAMES = RDataFactory.createStringVector(new String[]{"Invocations", "TotalTime", "SelfTime"}, RDataFactory.COMPLETE_VECTOR);
        private static final RStringVector ROWNAMES = RDataFactory.createStringVector(new String[]{"Combined", "Interpreted", "Compiled"}, RDataFactory.COMPLETE_VECTOR);
        private static final int NCOLS = 3;
        private static final int NROWS = 3;

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("func").mustBe(instanceOf(RFunction.class).or(missingValue()));

            casts.arg("threshold").mustBe(integerValue().or(doubleValue())).asDoubleVector();

            casts.arg("scale").mustBe(stringValue()).asStringVector();
        }

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, 0.0, "nanos"};
        }

        private void checkScale(String s) throws RError {
            if (!(s.equals("nanos") || s.equals("millis") || s.equals("micros") || s.equals("secs"))) {
                throw RError.error(this, RError.Message.GENERIC, "invalid scale: one of 'nanos, micros, millis, secs' expected");
            }
        }

        @Specialization
        @TruffleBoundary
        protected Object get(@SuppressWarnings("unused") RMissing value, RAbstractDoubleVector thresholdVec, RAbstractStringVector scaleVec) {
            String scale = scaleVec.getDataAt(0);
            checkScale(scale);
            double threshold = thresholdVec.getDataAt(0);
            Profiler.Counter[] counters = RFunctionProfiler.getCounters();
            if (counters == null) {
                throw RError.error(this, RError.Message.GENERIC, "profiling not enabled");
            }
            ArrayList<RDoubleVector> dataList = new ArrayList<>();
            ArrayList<String> nameList = new ArrayList<>();
            for (int i = 0; i < counters.length; i++) {
                Profiler.Counter counter = counters[i];
                if (threshold > 0.0) {
                    long time = counter.getTotalTime(Profiler.Counter.TimeKind.INTERPRETED_AND_COMPILED);
                    if (time <= threshold) {
                        continue;
                    }
                }
                dataList.add(getFunctionMatrix(counter, scale));
                nameList.add(counter.getName());
            }
            Object[] data = new Object[dataList.size()];
            String[] names = new String[nameList.size()];
            return RDataFactory.createList(dataList.toArray(data), RDataFactory.createStringVector(nameList.toArray(names), RDataFactory.COMPLETE_VECTOR));
        }

        @Specialization
        @TruffleBoundary
        protected Object get(RFunction function, @SuppressWarnings("unused") RAbstractDoubleVector threshold, RAbstractStringVector scaleVec) {
            String scale = scaleVec.getDataAt(0);
            checkScale(scale);
            if (!function.isBuiltin()) {
                Profiler.Counter counter = RFunctionProfiler.getCounter(function);
                if (counter == null) {
                    throw RError.error(this, RError.Message.GENERIC, "profiling not enabled");
                } else {
                    return getFunctionMatrix(counter, scale);
                }
            } else {
                throw RError.error(this, RError.Message.GENERIC, "cannot profile builtin functions");
            }
        }

        private static RDoubleVector getFunctionMatrix(Profiler.Counter counter, String scale) {
            double[] data = new double[NROWS * NCOLS];
            boolean isTiming = RFunctionProfiler.isTiming();
            boolean complete = isTiming ? RDataFactory.COMPLETE_VECTOR : RDataFactory.INCOMPLETE_VECTOR;
            for (int r = 0; r < NROWS; r++) {
                Profiler.Counter.TimeKind timeKind = Profiler.Counter.TimeKind.values()[r];
                for (int c = 0; c < NCOLS; c++) {
                    int index = c * NROWS + r;
                    double value = 0.0;
                    switch (c) {
                        case 0:
                            value = counter.getInvocations(timeKind);
                            break;
                        case 1:
                            value = isTiming ? counter.getTotalTime(timeKind) : RRuntime.DOUBLE_NA;
                            break;
                        case 2:
                            value = isTiming ? counter.getSelfTime(timeKind) : RRuntime.DOUBLE_NA;
                    }
                    data[index] = c == 0 ? value : scaledTime(scale, value);
                }
            }
            RDoubleVector result = RDataFactory.createDoubleVector(data, complete, new int[]{3, 3});
            Object[] dimNamesData = new Object[2];
            dimNamesData[0] = ROWNAMES;
            dimNamesData[1] = COLNAMES;
            RList dimNames = RDataFactory.createList(dimNamesData);
            result.setDimNames(dimNames);
            return result;
        }

        private static double scaledTime(String scale, double time) {
            if (RRuntime.isNA(time)) {
                return time;
            }
            switch (scale) {
                case "nanos":
                    return time;
                case "micros":
                    return time / 1000.0;
                case "millis":
                    return time / 1000000.0;
                case "secs":
                    return time / 1000000000.0;
                default:
                    throw RInternalError.shouldNotReachHere();
            }
        }
    }

    @RBuiltin(name = ".fastr.profiler.reset", visibility = OFF, kind = PRIMITIVE, parameterNames = {}, behavior = COMPLEX)
    public abstract static class Reset extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected Object reset() {
            RFunctionProfiler.reset();
            return RNull.instance;
        }
    }

    @RBuiltin(name = ".fastr.profiler.clear", visibility = OFF, kind = PRIMITIVE, parameterNames = {}, behavior = COMPLEX)
    public abstract static class Clear extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected Object clear() {
            RFunctionProfiler.clear();
            return RNull.instance;
        }
    }
}
