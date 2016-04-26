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
package com.oracle.truffle.r.runtime.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RPerfStats;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.data.RPromise.EagerFeedback;
import com.oracle.truffle.r.runtime.data.RPromise.OptType;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseType;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;

public final class RDataFactory {

    /**
     * Profile for creation tracing; must precede following declarations.
     */
    private static final ConditionProfile statsProfile = ConditionProfile.createBinaryProfile();

    public static final boolean INCOMPLETE_VECTOR = false;
    public static final boolean COMPLETE_VECTOR = true;

    public static RIntVector createIntVector(int length) {
        return createIntVector(length, false);
    }

    public static RIntVector createIntVector(int length, boolean fillNA) {
        int[] data = new int[length];
        if (fillNA) {
            Arrays.fill(data, RRuntime.INT_NA);
        }
        return createIntVector(data, !fillNA);
    }

    public static RIntVector createIntVector(int[] data, boolean complete) {
        return createIntVector(data, complete, null, null);
    }

    public static RIntVector createIntVector(int[] data, boolean complete, int[] dims) {
        return createIntVector(data, complete, dims, null);
    }

    public static RIntVector createIntVector(int[] data, boolean complete, RStringVector names) {
        return createIntVector(data, complete, null, names);
    }

    public static RIntVector createIntVector(int[] data, boolean complete, int[] dims, RStringVector names) {
        return traceDataCreated(new RIntVector(data, complete, dims, names));
    }

    public static RDoubleVector createDoubleVector(int length) {
        return createDoubleVector(length, false);
    }

    public static RDoubleVector createDoubleVector(int length, boolean fillNA) {
        double[] data = new double[length];
        if (fillNA) {
            Arrays.fill(data, RRuntime.DOUBLE_NA);
        }
        return createDoubleVector(data, !fillNA);
    }

    public static RDoubleVector createDoubleVector(double[] data, boolean complete) {
        return createDoubleVector(data, complete, null, null);
    }

    public static RDoubleVector createDoubleVector(double[] data, boolean complete, int[] dims) {
        return createDoubleVector(data, complete, dims, null);
    }

    public static RDoubleVector createDoubleVector(double[] data, boolean complete, RStringVector names) {
        return createDoubleVector(data, complete, null, names);
    }

    public static RDoubleVector createDoubleVector(double[] data, boolean complete, int[] dims, RStringVector names) {
        return traceDataCreated(new RDoubleVector(data, complete, dims, names));
    }

    public static RRawVector createRawVector(int length) {
        return createRawVector(new byte[length]);
    }

    public static RRawVector createRawVector(byte[] data) {
        return createRawVector(data, null, null);
    }

    public static RRawVector createRawVector(byte[] data, int[] dims) {
        return createRawVector(data, dims, null);
    }

    public static RRawVector createRawVector(byte[] data, RStringVector names) {
        return createRawVector(data, null, names);
    }

    public static RRawVector createRawVector(byte[] data, int[] dims, RStringVector names) {
        return traceDataCreated(new RRawVector(data, dims, names));
    }

    public static RComplexVector createComplexVector(int length) {
        return createComplexVector(length, false);
    }

    public static RComplexVector createComplexVector(int length, boolean fillNA) {
        double[] data = new double[length << 1];
        if (fillNA) {
            for (int i = 0; i < data.length; i += 2) {
                data[i] = RRuntime.COMPLEX_NA_REAL_PART;
                data[i + 1] = RRuntime.COMPLEX_NA_IMAGINARY_PART;
            }
        }
        return createComplexVector(data, false, null, null);
    }

    public static RComplexVector createComplexVector(double[] data, boolean complete) {
        return createComplexVector(data, complete, null, null);
    }

    public static RComplexVector createComplexVector(double[] data, boolean complete, int[] dims) {
        return createComplexVector(data, complete, dims, null);
    }

    public static RComplexVector createComplexVector(double[] data, boolean complete, RStringVector names) {
        return createComplexVector(data, complete, null, names);
    }

    public static RComplexVector createComplexVector(double[] data, boolean complete, int[] dims, RStringVector names) {
        return traceDataCreated(new RComplexVector(data, complete, dims, names));
    }

    public static RStringVector createStringVector(String value) {
        return createStringVector(new String[]{value}, !RRuntime.isNA(value), null, null);
    }

    public static RStringVector createStringVector(int length) {
        return createStringVector(length, false);
    }

    public static RStringVector createStringVector(int length, boolean fillNA) {
        return createStringVector(createAndfillStringVector(length, fillNA ? RRuntime.STRING_NA : ""), !fillNA, null, null);
    }

    private static String[] createAndfillStringVector(int length, String string) {
        String[] strings = new String[length];
        Arrays.fill(strings, string);
        return strings;
    }

    public static RStringVector createStringVector(String[] data, boolean complete) {
        return createStringVector(data, complete, null, null);
    }

    public static RStringVector createStringVector(String[] data, boolean complete, int[] dims) {
        return createStringVector(data, complete, dims, null);
    }

    public static RStringVector createStringVector(String[] data, boolean complete, RStringVector names) {
        return createStringVector(data, complete, null, names);
    }

    public static RStringVector createStringVector(String[] data, boolean complete, int[] dims, RStringVector names) {
        return traceDataCreated(new RStringVector(data, complete, dims, names));
    }

    public static RLogicalVector createLogicalVector(int length) {
        return createLogicalVector(length, false);
    }

    public static RLogicalVector createLogicalVector(int length, boolean fillNA) {
        byte[] data = new byte[length];
        if (fillNA) {
            Arrays.fill(data, RRuntime.LOGICAL_NA);
        }
        return createLogicalVector(data, false, null, null);
    }

    public static RLogicalVector createLogicalVector(byte[] data, boolean complete) {
        return createLogicalVector(data, complete, null, null);
    }

    public static RLogicalVector createLogicalVector(byte[] data, boolean complete, int[] dims) {
        return createLogicalVector(data, complete, dims, null);
    }

    public static RLogicalVector createLogicalVector(byte[] data, boolean complete, RStringVector names) {
        return createLogicalVector(data, complete, null, names);
    }

    public static RLogicalVector createLogicalVector(byte[] data, boolean complete, int[] dims, RStringVector names) {
        return traceDataCreated(new RLogicalVector(data, complete, dims, names));
    }

    public static RLogicalVector createNAVector(int length) {
        return createLogicalVector(length, true);
    }

    public static RIntSequence createAscendingRange(int start, int end) {
        assert start <= end;
        return traceDataCreated(new RIntSequence(start, 1, end - start + 1));
    }

    public static RIntSequence createDescendingRange(int start, int end) {
        assert start > end;
        return traceDataCreated(new RIntSequence(start, -1, start - end + 1));
    }

    public static RIntSequence createIntSequence(int start, int stride, int length) {
        return traceDataCreated(new RIntSequence(start, stride, length));
    }

    public static RDoubleSequence createAscendingRange(double start, double end) {
        assert start <= end;
        return traceDataCreated(new RDoubleSequence(start, 1, (int) ((end - start) + 1)));
    }

    public static RDoubleSequence createDescendingRange(double start, double end) {
        assert start > end;
        return traceDataCreated(new RDoubleSequence(start, -1, (int) ((start - end) + 1)));
    }

    public static RDoubleSequence createDoubleSequence(double start, double stride, int length) {
        return traceDataCreated(new RDoubleSequence(start, stride, length));
    }

    public static RIntVector createEmptyIntVector() {
        return createIntVector(new int[0], true);
    }

    public static RDoubleVector createEmptyDoubleVector() {
        return createDoubleVector(new double[0], true);
    }

    public static RStringVector createEmptyStringVector() {
        return createStringVector(new String[0], true);
    }

    public static RStringVector createNAStringVector() {
        return createStringVector(new String[]{RRuntime.STRING_NA}, false);
    }

    public static RComplexVector createEmptyComplexVector() {
        return createComplexVector(new double[0], true);
    }

    public static RLogicalVector createEmptyLogicalVector() {
        return createLogicalVector(new byte[0], true);
    }

    public static RRawVector createEmptyRawVector() {
        return createRawVector(new byte[0]);
    }

    public static RComplex createComplex(double realPart, double imaginaryPart) {
        return traceDataCreated(RComplex.valueOf(realPart, imaginaryPart));
    }

    public static RRaw createRaw(byte value) {
        return traceDataCreated(new RRaw(value));
    }

    public static RStringVector createStringVectorFromScalar(String operand) {
        return createStringVector(new String[]{operand}, !RRuntime.isNA(operand));
    }

    public static RLogicalVector createLogicalVectorFromScalar(boolean data) {
        return createLogicalVector(new byte[]{data ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE}, COMPLETE_VECTOR);
    }

    public static RLogicalVector createLogicalVectorFromScalar(byte operand) {
        return createLogicalVector(new byte[]{operand}, !RRuntime.isNA(operand));
    }

    public static RIntVector createIntVectorFromScalar(int operand) {
        return createIntVector(new int[]{operand}, !RRuntime.isNA(operand));
    }

    public static RDoubleVector createDoubleVectorFromScalar(double operand) {
        return createDoubleVector(new double[]{operand}, !RRuntime.isNA(operand));
    }

    public static RComplexVector createComplexVectorFromScalar(RComplex operand) {
        return createComplexVector(new double[]{operand.getRealPart(), operand.getImaginaryPart()}, !operand.isNA());
    }

    public static RRawVector createRawVectorFromScalar(RRaw operand) {
        return createRawVector(new byte[]{operand.getValue()});
    }

    public static RComplex createComplexRealOne() {
        return createComplex(1.0, 0.0);
    }

    public static RList createList(Object[] data) {
        return createList(data, null, null);
    }

    public static RComplex createComplexZero() {
        return createComplex(0.0, 0.0);
    }

    public static RList createList(Object[] data, int[] newDimensions) {
        return createList(data, newDimensions, null);
    }

    public static RList createList(Object[] data, RStringVector names) {
        return createList(data, null, names);
    }

    public static RList createList() {
        return createList(new Object[0], null, null);
    }

    public static RList createList(int n) {
        return createList(new Object[n], null, null);
    }

    public static RList createList(Object[] data, int[] newDimensions, RStringVector names) {
        return traceDataCreated(new RList(data, newDimensions, names));
    }

    public static RExpression createExpression(RList list) {
        return traceDataCreated(new RExpression(list));
    }

    public static RFactor createFactor(RIntVector vector, boolean ordered) {
        return traceDataCreated(new RFactor(vector, ordered));
    }

    public static RSymbol createSymbol(String name) {
        assert name == name.intern();
        return traceDataCreated(new RSymbol(name));
    }

    /*
     * A version of {@link createSymbol} method mostly used from native code and in
     * serialization/deparsing.
     */
    public static RSymbol createSymbolInterned(String name) {
        return createSymbol(name.intern());
    }

    public static RLanguage createLanguage(RNode rep) {
        return traceDataCreated(new RLanguage(rep));
    }

    public static RPromise createPromise(PromiseType type, MaterializedFrame execFrame, Closure closure) {
        assert closure != null;
        assert closure.getExpr() != null;
        return traceDataCreated(new RPromise(type, OptType.DEFAULT, execFrame, closure));
    }

    @TruffleBoundary
    public static RPromise createPromise(RBaseNode rep, REnvironment env) {
        // TODO Cache closures? Maybe in the callers of this function?
        Closure closure = Closure.create(rep);
        return traceDataCreated(new RPromise(PromiseType.NO_ARG, OptType.DEFAULT, env.getFrame(), closure));
    }

    public static RPromise createPromise(PromiseType type, OptType opt, RBaseNode expr, Object argumentValue) {
        return traceDataCreated(new RPromise(type, opt, expr, argumentValue));
    }

    public static RPromise createEagerPromise(PromiseType type, OptType eager, Closure exprClosure, Object eagerValue, Assumption notChangedNonLocally, int nFrameId, EagerFeedback feedback,
                    int wrapIndex) {
        return traceDataCreated(new RPromise.EagerPromise(type, eager, exprClosure, eagerValue, notChangedNonLocally, nFrameId, feedback, wrapIndex));
    }

    public static RPromise createVarargPromise(PromiseType type, RPromise promisedVararg, Closure exprClosure) {
        return traceDataCreated(new RPromise.VarargPromise(type, promisedVararg, exprClosure));
    }

    public static RPairList createPairList() {
        return traceDataCreated(new RPairList());
    }

    public static RPairList createPairList(Object car) {
        return traceDataCreated(new RPairList(car, RNull.instance, RNull.instance, null));
    }

    public static RPairList createPairList(Object car, Object cdr) {
        return traceDataCreated(new RPairList(car, cdr, RNull.instance, null));
    }

    public static RPairList createPairList(Object car, Object cdr, Object tag) {
        return traceDataCreated(new RPairList(car, cdr, tag, null));
    }

    public static RPairList createPairList(Object car, Object cdr, Object tag, SEXPTYPE type) {
        return traceDataCreated(new RPairList(car, cdr, tag, type));
    }

    public static RFunction createFunction(String name, RootCallTarget target, RBuiltinDescriptor builtin, MaterializedFrame enclosingFrame, FastPathFactory fastPath, boolean containsDispatch) {
        return traceDataCreated(new RFunction(name, target, builtin, enclosingFrame, fastPath, containsDispatch));
    }

    private static final AtomicInteger environmentCount = new AtomicInteger();

    public static REnvironment createInternalEnv() {
        return traceDataCreated(new REnvironment.NewEnv(RRuntime.createNonFunctionFrame("<internal-env-" + environmentCount.incrementAndGet() + ">"), REnvironment.UNNAMED));
    }

    public static REnvironment.NewEnv createNewEnv(String name) {
        return traceDataCreated(new REnvironment.NewEnv(RRuntime.createNonFunctionFrame("<new-env-" + environmentCount.incrementAndGet() + ">"), name));
    }

    public static REnvironment createNewEnv(String name, boolean hashed, int initialSize) {
        REnvironment.NewEnv env = new REnvironment.NewEnv(RRuntime.createNonFunctionFrame("<new-env-" + environmentCount.incrementAndGet() + ">"), name);
        env.setHashed(hashed);
        env.setInitialSize(initialSize);
        return traceDataCreated(env);
    }

    public static RS4Object createS4Object() {
        return traceDataCreated(new RS4Object());
    }

    public static RExternalPtr createExternalPtr(long value, Object tag, Object prot) {
        return traceDataCreated(new RExternalPtr(value, tag, prot));
    }

    public static RExternalPtr createExternalPtr(long value, Object tag) {
        return traceDataCreated(new RExternalPtr(value, tag, RNull.instance));
    }

    @CompilationFinal private static PerfHandler stats;

    private static <T> T traceDataCreated(T data) {
        if (statsProfile.profile(stats != null)) {
            stats.record(data);
        }
        return data;
    }

    static {
        RPerfStats.register(new PerfHandler());
    }

    private static class PerfHandler implements RPerfStats.Handler {
        private static Map<Class<?>, RPerfStats.Histogram> histMap;

        @TruffleBoundary
        void record(Object data) {
            Class<?> klass = data.getClass();
            boolean isBounded = data instanceof RAbstractVector;
            RPerfStats.Histogram hist = histMap.get(klass);
            if (hist == null) {
                hist = new RPerfStats.Histogram(isBounded ? 10 : 1);
                histMap.put(klass, hist);
            }
            int length = isBounded ? ((RAbstractVector) data).getLength() : 0;
            hist.inc(length);
        }

        @Override
        public void initialize(String optionData) {
            stats = this;
            histMap = new HashMap<>();
        }

        @Override
        public String getName() {
            return "datafactory";
        }

        @Override
        public void report() {
            RPerfStats.out().println("Scalar types");
            for (Map.Entry<Class<?>, RPerfStats.Histogram> entry : histMap.entrySet()) {
                RPerfStats.Histogram hist = entry.getValue();
                if (hist.numBuckets() == 1) {
                    RPerfStats.out().printf("%s: %d%n", entry.getKey().getSimpleName(), hist.getTotalCount());
                }
            }
            RPerfStats.out().println();
            RPerfStats.out().println("Vector types");
            for (Map.Entry<Class<?>, RPerfStats.Histogram> entry : histMap.entrySet()) {
                RPerfStats.Histogram hist = entry.getValue();
                if (hist.numBuckets() > 1) {
                    RPerfStats.out().printf("%s: %d, max size %d%n", entry.getKey().getSimpleName(), hist.getTotalCount(), hist.getMaxSize());
                    entry.getValue().report();
                }
            }
            RPerfStats.out().println();
        }
    }
}
