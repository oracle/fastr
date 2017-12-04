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
package com.oracle.truffle.r.runtime.data;

import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.instrumentation.AllocationReporter;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RPromise.EagerFeedback;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseState;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;

public final class RDataFactory {
    private abstract static class StaticVectorFactory extends BaseVectorFactory {

        @Override
        public void reinitializeAttributes(RVector<?> vector, int[] dims, RStringVector names, RList dimNames) {
            vector.initAttributes(dims != null || names != null || dimNames != null ? RVector.createAttributes(dims, names, dimNames) : null);
        }

        @Override
        protected <T extends RVector<?>> T initializeAttributes(T result, int[] dims, RStringVector names, RList dimNames) {
            if (dims != null || names != null || dimNames != null) {
                result.initDimsNamesDimNames(dims, names, dimNames);
            }
            return result;
        }

        @Override
        protected <T extends RVector<?>> T initializeAttributes(T result, int[] dims) {
            if (dims != null) {
                result.initDimsNamesDimNames(dims, null, null);
            }
            return result;
        }

        @Override
        protected <T extends RVector<?>> T initializeAttributes(T result, RStringVector names) {
            if (names != null) {
                result.initDimsNamesDimNames(null, names, null);
            }
            return result;
        }
    }

    private static final class DefaultStaticVectorFactory extends StaticVectorFactory {

    }

    private static final class PermanentStaticVectorFactory extends StaticVectorFactory {

        @Override
        protected <T> T traceDataCreated(T data) {
            if (data instanceof RShareable) {
                ((RShareable) data).makeSharedPermanent();
            } else {
                assert data instanceof Integer || data instanceof Double || data instanceof Byte || data instanceof String || data instanceof RRaw ||
                                data instanceof RComplex : "cannot make permanent instance of non-shareable object";
            }
            return super.traceDataCreated(data);
        }
    }

    private static final StaticVectorFactory INSTANCE = new DefaultStaticVectorFactory();
    private static final StaticVectorFactory PERMANENT = new PermanentStaticVectorFactory();

    /**
     * This factory is only intended for use on slow paths, all places where a node context is
     * available should use a dynamic {@link VectorFactory} instance.
     */
    public static BaseVectorFactory getInstance() {
        CompilerAsserts.neverPartOfCompilation("RDataFactory.getInstance() can only be used in slow paths");
        return INSTANCE;
    }

    /**
     * This factory is intended for use in initializers that want to create "permanent" objects,
     * i.e., objects that have their reference count set to "permanently shared".
     */
    public static BaseVectorFactory getPermanent() {
        CompilerAsserts.neverPartOfCompilation("RDataFactory.getPermanent() can only be used in slow paths");
        return PERMANENT;
    }

    public static final class VectorFactory extends BaseVectorFactory {

        private VectorFactory() {
            // private constructor
        }

        private final ConditionProfile hasAttributes = ConditionProfile.createBinaryProfile();

        @Override
        public void reinitializeAttributes(RVector<?> vector, int[] dims, RStringVector names, RList dimNames) {
            vector.initAttributes(hasAttributes.profile(dims != null || names != null || dimNames != null) ? RVector.createAttributes(dims, names, dimNames) : null);
        }

        @Override
        protected <T extends RVector<?>> T initializeAttributes(T result, int[] dims, RStringVector names, RList dimNames) {
            if (hasAttributes.profile(dims != null || names != null || dimNames != null)) {
                result.initDimsNamesDimNames(dims, names, dimNames);
            }
            return result;
        }

        @Override
        protected <T extends RVector<?>> T initializeAttributes(T result, int[] dims) {
            if (hasAttributes.profile(dims != null)) {
                result.initDimsNamesDimNames(dims, null, null);
            }
            return result;
        }

        @Override
        protected <T extends RVector<?>> T initializeAttributes(T result, RStringVector names) {
            if (hasAttributes.profile(names != null)) {
                result.initDimsNamesDimNames(null, names, null);
            }
            return result;
        }

        public static VectorFactory create() {
            return new VectorFactory();
        }
    }

    @SuppressWarnings("static-method")
    public abstract static class BaseVectorFactory extends Node {

        @Override
        public final NodeCost getCost() {
            return NodeCost.NONE;
        }

        public abstract void reinitializeAttributes(RVector<?> vector, int[] dims, RStringVector names, RList dimNames);

        protected abstract <T extends RVector<?>> T initializeAttributes(T result, int[] dims, RStringVector names, RList dimNames);

        protected abstract <T extends RVector<?>> T initializeAttributes(T result, int[] dims);

        protected abstract <T extends RVector<?>> T initializeAttributes(T result, RStringVector names);

        protected <T> T traceDataCreated(T data) {
            if (stateAssumption.isEnabled()) {
                reportDataCreated(data);
            }
            return data;
        }

        public final RIntVector createIntVector(int[] data, boolean complete) {
            return traceDataCreated(new RIntVector(data, complete));
        }

        public final RIntVector createIntVector(int[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
            return traceDataCreated(initializeAttributes(new RIntVector(data, complete), dims, names, dimNames));
        }

        public final RIntVector createIntVector(int[] data, boolean complete, int[] dims) {
            return traceDataCreated(initializeAttributes(new RIntVector(data, complete), dims));
        }

        public final RIntVector createIntVector(int[] data, boolean complete, RStringVector names) {
            return traceDataCreated(initializeAttributes(new RIntVector(data, complete), names));
        }

        public final RDoubleVector createDoubleVector(double[] data, boolean complete) {
            return traceDataCreated(new RDoubleVector(data, complete));
        }

        public final RDoubleVector createDoubleVector(double[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
            return traceDataCreated(initializeAttributes(new RDoubleVector(data, complete), dims, names, dimNames));
        }

        public final RDoubleVector createDoubleVector(double[] data, boolean complete, int[] dims) {
            return traceDataCreated(initializeAttributes(new RDoubleVector(data, complete), dims));
        }

        public final RDoubleVector createDoubleVector(double[] data, boolean complete, RStringVector names) {
            return traceDataCreated(initializeAttributes(new RDoubleVector(data, complete), names));
        }

        public final RLogicalVector createLogicalVector(byte[] data, boolean complete) {
            return traceDataCreated(new RLogicalVector(data, complete));
        }

        public final RLogicalVector createLogicalVector(byte[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
            return traceDataCreated(initializeAttributes(new RLogicalVector(data, complete), dims, names, dimNames));
        }

        public final RLogicalVector createLogicalVector(byte[] data, boolean complete, int[] dims) {
            return traceDataCreated(initializeAttributes(new RLogicalVector(data, complete), dims));
        }

        public final RLogicalVector createLogicalVector(byte[] data, boolean complete, RStringVector names) {
            return traceDataCreated(initializeAttributes(new RLogicalVector(data, complete), names));
        }

        public final RStringVector createStringVector(String[] data, boolean complete) {
            return traceDataCreated(new RStringVector(data, complete));
        }

        public final RStringVector createStringVector(String[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
            return traceDataCreated(initializeAttributes(new RStringVector(data, complete), dims, names, dimNames));
        }

        public final RStringVector createStringVector(String[] data, boolean complete, int[] dims) {
            return traceDataCreated(initializeAttributes(new RStringVector(data, complete), dims));
        }

        public final RStringVector createStringVector(String[] data, boolean complete, RStringVector names) {
            return traceDataCreated(initializeAttributes(new RStringVector(data, complete), names));
        }

        public final RComplexVector createComplexVector(double[] data, boolean complete) {
            return traceDataCreated(new RComplexVector(data, complete));
        }

        public final RComplexVector createComplexVector(double[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
            return traceDataCreated(initializeAttributes(new RComplexVector(data, complete), dims, names, dimNames));
        }

        public final RComplexVector createComplexVector(double[] data, boolean complete, int[] dims) {
            return traceDataCreated(initializeAttributes(new RComplexVector(data, complete), dims));
        }

        public final RComplexVector createComplexVector(double[] data, boolean complete, RStringVector names) {
            return traceDataCreated(initializeAttributes(new RComplexVector(data, complete), names));
        }

        public final RRawVector createRawVector(byte[] data) {
            return traceDataCreated(new RRawVector(data));
        }

        public final RRawVector createRawVector(byte[] data, int[] dims, RStringVector names, RList dimNames) {
            return traceDataCreated(initializeAttributes(new RRawVector(data), dims, names, dimNames));
        }

        public final RRawVector createRawVector(byte[] data, int[] dims) {
            return traceDataCreated(initializeAttributes(new RRawVector(data), dims));
        }

        public final RRawVector createRawVector(byte[] data, RStringVector names) {
            return traceDataCreated(initializeAttributes(new RRawVector(data), names));
        }

        public final RList createList(Object[] data) {
            return traceDataCreated(new RList(data));
        }

        public final RList createList(Object[] data, int[] dims, RStringVector names, RList dimNames) {
            return traceDataCreated(initializeAttributes(new RList(data), dims, names, dimNames));
        }

        public final RList createList(Object[] data, int[] dims) {
            return traceDataCreated(initializeAttributes(new RList(data), dims));
        }

        public final RList createList(Object[] data, RStringVector names) {
            return traceDataCreated(initializeAttributes(new RList(data), names));
        }

        public RExpression createExpression(int size) {
            return createExpression(createRNullArray(size));
        }

        public RExpression createExpression(int size, RStringVector names) {
            if (names == null) {
                return createExpression(size);
            }
            assert size == names.getLength();
            return createExpression(createRNullArray(size), names);
        }

        public RExpression createExpression(Object[] data, int[] newDimensions) {
            return traceDataCreated(new RExpression(data, newDimensions, null, null));
        }

        public RExpression createExpression(Object[] data, RStringVector names) {
            return traceDataCreated(new RExpression(data, null, names, null));
        }

        public RExpression createExpression(Object[] data, int[] newDimensions, RStringVector names, RList dimNames) {
            return traceDataCreated(new RExpression(data, newDimensions, names, dimNames));
        }

        public RExpression createExpression(Object[] data) {
            return traceDataCreated(new RExpression(data, null, null, null));
        }

        public final RVector<?> createEmptyVector(RType type) {
            switch (type) {
                case Double:
                    return createEmptyDoubleVector();
                case Integer:
                    return createEmptyIntVector();
                case Complex:
                    return createEmptyComplexVector();
                case Logical:
                    return createEmptyLogicalVector();
                case Character:
                    return createEmptyStringVector();
                case Raw:
                    return createEmptyRawVector();
                case List:
                    return createList(new Object[0]);
                default:
                    throw RInternalError.shouldNotReachHere();
            }
        }

        public final RVector<?> createUninitializedVector(RType type, int length, int[] dims, RStringVector names, RList dimNames) {
            switch (type) {
                case Logical:
                    return createLogicalVector(new byte[length], false, dims, names, dimNames);
                case Integer:
                    return createIntVector(new int[length], false, dims, names, dimNames);
                case Double:
                    return createDoubleVector(new double[length], false, dims, names, dimNames);
                case Complex:
                    return createComplexVector(new double[length * 2], false, dims, names, dimNames);
                case Character:
                    return createStringVector(new String[length], false, dims, names, dimNames);
                case Expression: {
                    Object[] data = new Object[length];
                    Arrays.fill(data, RNull.instance);
                    return createExpression(data, dims, names, dimNames);
                }
                case List: {
                    Object[] data = new Object[length];
                    Arrays.fill(data, RNull.instance);
                    return createList(data, dims, names, dimNames);
                }
                case Raw:
                    return createRawVector(new byte[length], dims, names, dimNames);
                default:
                    throw RInternalError.shouldNotReachHere();
            }

        }

        public final RVector<?> createVector(RType type, int length, boolean fillNA) {
            switch (type) {
                case Logical: {
                    byte[] data = new byte[length];
                    if (fillNA) {
                        Arrays.fill(data, RRuntime.LOGICAL_NA);
                    }
                    return createLogicalVector(data, !fillNA);
                }
                case Integer: {
                    int[] data = new int[length];
                    if (fillNA) {
                        Arrays.fill(data, RRuntime.INT_NA);
                    }
                    return createIntVector(data, !fillNA);
                }
                case Double: {
                    double[] data = new double[length];
                    if (fillNA) {
                        Arrays.fill(data, RRuntime.LOGICAL_NA);
                    }
                    return createDoubleVector(data, !fillNA);
                }
                case Complex: {
                    double[] data = new double[length * 2];
                    if (fillNA) {
                        Arrays.fill(data, RRuntime.LOGICAL_NA);
                        for (int i = 0; i < data.length; i += 2) {
                            data[i] = RRuntime.COMPLEX_NA_REAL_PART;
                            data[i + 1] = RRuntime.COMPLEX_NA_IMAGINARY_PART;
                        }
                    }
                    return createComplexVector(data, !fillNA);
                }
                case Character: {
                    String[] data = new String[length];
                    Arrays.fill(data, fillNA ? RRuntime.STRING_NA : "");
                    return createStringVector(data, !fillNA);
                }
                case Expression: {
                    Object[] data = new Object[length];
                    Arrays.fill(data, RNull.instance);
                    return createExpression(data);
                }
                case List: {
                    Object[] data = new Object[length];
                    Arrays.fill(data, RNull.instance);
                    return createList(data);
                }
                case Raw:
                    return createRawVector(new byte[length]);
                default:
                    throw RInternalError.shouldNotReachHere();
            }
        }

        /*
         * the following functions are not cleaned up yet:
         */

        public final RIntVector createIntVectorFromNative(long address, int length) {
            return traceDataCreated(RIntVector.fromNative(address, length));
        }

        public final RIntVector createIntVector(int length) {
            return createIntVector(new int[length], true);
        }

        public final RDoubleVector createDoubleVectorFromNative(long address, int length) {
            return traceDataCreated(RDoubleVector.fromNative(address, length));
        }

        public final RDoubleVector createDoubleVector(int length) {
            return createDoubleVector(new double[length], true);
        }

        public final RRawVector createRawVector(int length) {
            return createRawVector(new byte[length]);
        }

        public final RComplexVector createComplexVectorFromNative(long address, int length) {
            return traceDataCreated(RComplexVector.fromNative(address, length));
        }

        public final RComplexVector createComplexVector(int length) {
            return createComplexVector(new double[length << 1], true);
        }

        public final RStringVector createStringVector(String value) {
            return createStringVector(new String[]{value}, !RRuntime.isNA(value));
        }

        public final RStringVector createStringVector(int length) {
            return createStringVector(createAndfillStringVector(length, ""), true);
        }

        private static String[] createAndfillStringVector(int length, String string) {
            String[] strings = new String[length];
            Arrays.fill(strings, string);
            return strings;
        }

        public final RLogicalVector createLogicalVectorFromNative(long address, int length) {
            return traceDataCreated(RLogicalVector.fromNative(address, length));
        }

        public final RLogicalVector createLogicalVector(int length) {
            return createLogicalVector(length, false);
        }

        public final RLogicalVector createLogicalVector(int length, boolean fillNA) {
            byte[] data = new byte[length];
            if (fillNA) {
                Arrays.fill(data, RRuntime.LOGICAL_NA);
            }
            return createLogicalVector(data, !fillNA);
        }

        public final RIntSequence createAscendingRange(int start, int end) {
            assert start <= end;
            return traceDataCreated(new RIntSequence(start, 1, end - start + 1));
        }

        public final RIntSequence createDescendingRange(int start, int end) {
            assert start > end;
            return traceDataCreated(new RIntSequence(start, -1, start - end + 1));
        }

        public final RIntSequence createIntSequence(int start, int stride, int length) {
            return traceDataCreated(new RIntSequence(start, stride, length));
        }

        public final RDoubleSequence createAscendingRange(double start, double end) {
            assert start <= end;
            return traceDataCreated(new RDoubleSequence(start, 1, (int) ((end - start) + 1)));
        }

        public final RDoubleSequence createDescendingRange(double start, double end) {
            assert start > end;
            return traceDataCreated(new RDoubleSequence(start, -1, (int) ((start - end) + 1)));
        }

        public final RDoubleSequence createDoubleSequence(double start, double stride, int length) {
            return traceDataCreated(new RDoubleSequence(start, stride, length));
        }

        public final RIntVector createEmptyIntVector() {
            return createIntVector(new int[0], true);
        }

        public final RDoubleVector createEmptyDoubleVector() {
            return createDoubleVector(new double[0], true);
        }

        public final RStringVector createEmptyStringVector() {
            return createStringVector(new String[0], true);
        }

        public final RStringVector createNAStringVector() {
            return createStringVector(new String[]{RRuntime.STRING_NA}, false);
        }

        public final RStringSequence createStringSequence(String prefix, String suffix, int start, int stride, int length) {
            return traceDataCreated(new RStringSequence(prefix, suffix, start, stride, length));
        }

        public final RComplexVector createEmptyComplexVector() {
            return createComplexVector(new double[0], true);
        }

        public final RLogicalVector createEmptyLogicalVector() {
            return createLogicalVector(new byte[0], true);
        }

        public final RRawVector createEmptyRawVector() {
            return createRawVector(new byte[0]);
        }

        public final RList createEmptyList() {
            return createList(new Object[0]);
        }

        public final RComplex createComplex(double realPart, double imaginaryPart) {
            return traceDataCreated(RComplex.valueOf(realPart, imaginaryPart));
        }

        public final RRawVector createRawVectorFromNative(long address, int length) {
            return traceDataCreated(RRawVector.fromNative(address, length));
        }

        public final RRaw createRaw(byte value) {
            return traceDataCreated(new RRaw(value));
        }

        /*
         * Shared scalar conversion functions: these need to be replaced with
         * createXyzVectorFromScalar(...).makeSharedPermanent() if scalar types are removed.
         */

        public final Object createSharedStringVectorFromScalar(String value) {
            return value;
        }

        public final Object createSharedLogicalVectorFromScalar(boolean value) {
            return RRuntime.asLogical(value);
        }

        public final Object createSharedLogicalVectorFromScalar(byte value) {
            return value;
        }

        public final Object createSharedIntVectorFromScalar(int value) {
            return value;
        }

        public final Object createSharedDoubleVectorFromScalar(double value) {
            return value;
        }

        public final Object createSharedComplexVectorFromScalar(RComplex value) {
            return value;
        }

        public final Object createSharedRawVectorFromScalar(RRaw value) {
            return value;
        }

        public final RList createList(int n) {
            return createList(createRNullArray(n));
        }

        public final RList createList(Object[] data, int[] newDimensions, RStringVector names) {
            return traceDataCreated(new RList(data, newDimensions, names, null));
        }

        public final RSymbol createSymbol(String name) {
            assert Utils.isInterned(name);
            return traceDataCreated(new RSymbol(name));
        }

        /*
         * A version of {@link createSymbol} method mostly used from native code and in
         * serialization/deparsing.
         */
        public final RSymbol createSymbolInterned(String name) {
            return createSymbol(Utils.intern(name));
        }

        public final RLanguage createLanguage(Closure closure) {
            return traceDataCreated(new RLanguage(closure));
        }

        public final RPromise createPromise(PromiseState state, Closure closure, MaterializedFrame env) {
            assert closure != null;
            assert closure.getExpr() != null;
            return traceDataCreated(new RPromise(state, env, closure));
        }

        public final RPromise createEvaluatedPromise(PromiseState state, Closure closure, Object argumentValue) {
            return traceDataCreated(new RPromise(state, closure, argumentValue));
        }

        public final RPromise createEvaluatedPromise(Closure closure, Object value) {
            return traceDataCreated(new RPromise(PromiseState.Explicit, closure, value));
        }

        public RPromise createEagerPromise(PromiseState state, Closure exprClosure, Object eagerValue, Assumption notChangedNonLocally, RCaller targetFrame, EagerFeedback feedback,
                        int wrapIndex, MaterializedFrame execFrame) {
            if (FastROptions.noEagerEval()) {
                throw RInternalError.shouldNotReachHere();
            }
            return traceDataCreated(new RPromise.EagerPromise(state, exprClosure, eagerValue, notChangedNonLocally, targetFrame, feedback, wrapIndex, execFrame));
        }

        public RPromise createPromisedPromise(Closure exprClosure, Object eagerValue, Assumption notChangedNonLocally, RCaller targetFrame, EagerFeedback feedback, MaterializedFrame execFrame) {
            if (FastROptions.noEagerEval()) {
                throw RInternalError.shouldNotReachHere();
            }
            return traceDataCreated(new RPromise.EagerPromise(PromiseState.Promised, exprClosure, eagerValue, notChangedNonLocally, targetFrame, feedback, -1, execFrame));
        }

        public final Object createLangPairList(int size) {
            if (size == 0) {
                return RNull.instance;
            } else {
                return traceDataCreated(RPairList.create(size, SEXPTYPE.LANGSXP));
            }
        }

        public final Object createPairList(int size) {
            if (size == 0) {
                return RNull.instance;
            } else {
                return traceDataCreated(RPairList.create(size));
            }
        }

        public final RPairList createPairList() {
            return traceDataCreated(new RPairList());
        }

        public final RPairList createPairList(Object car) {

            return traceDataCreated(new RPairList(car, RNull.instance, RNull.instance, null));
        }

        public final RPairList createPairList(Object car, Object cdr) {
            return traceDataCreated(new RPairList(car, cdr, RNull.instance, null));
        }

        public final RPairList createPairList(Object car, Object cdr, Object tag) {
            return traceDataCreated(new RPairList(car, cdr, tag, null));
        }

        public final RPairList createPairList(Object car, Object cdr, Object tag, SEXPTYPE type) {
            return traceDataCreated(new RPairList(car, cdr, tag, type));
        }

        public final RFunction createFunction(String name, String packageName, RootCallTarget target, RBuiltinDescriptor builtin, MaterializedFrame enclosingFrame) {
            return traceDataCreated(new RFunction(name, packageName, target, builtin, enclosingFrame));
        }

        private static final AtomicInteger environmentCount = new AtomicInteger();

        @TruffleBoundary
        public final REnvironment createInternalEnv() {
            return traceDataCreated(new REnvironment.NewEnv(RRuntime.createNonFunctionFrame("<internal-env-" + environmentCount.incrementAndGet() + ">"), REnvironment.UNNAMED));
        }

        @TruffleBoundary
        public final REnvironment.NewEnv createNewEnv(FrameDescriptor desc, String name) {
            return traceDataCreated(new REnvironment.NewEnv(RRuntime.createNewFrame(desc), name));
        }

        @TruffleBoundary
        public final REnvironment.NewEnv createNewEnv(String name) {
            return traceDataCreated(new REnvironment.NewEnv(RRuntime.createNonFunctionFrame("<new-env-" + environmentCount.incrementAndGet() + ">"), name));
        }

        @TruffleBoundary
        public final REnvironment createNewEnv(String name, boolean hashed, int initialSize) {
            REnvironment.NewEnv env = new REnvironment.NewEnv(RRuntime.createNonFunctionFrame("<new-env-" + environmentCount.incrementAndGet() + ">"), name);
            env.setHashed(hashed);
            env.setInitialSize(initialSize);
            return traceDataCreated(env);
        }

        public final RS4Object createS4Object() {
            return traceDataCreated(new RS4Object());
        }

        public final RExternalPtr createExternalPtr(SymbolHandle value, Object externalObject, Object tag, Object prot) {
            assert tag != null : "null tag, use RNull.instance instead";
            assert prot != null : "null prot, use RNull.instance instead";
            return traceDataCreated(new RExternalPtr(value, externalObject, tag, prot));
        }

        public final RExternalPtr createExternalPtr(SymbolHandle value, Object tag, Object prot) {
            assert tag != null : "null tag, use RNull.instance instead";
            assert prot != null : "null prot, use RNull.instance instead";
            return traceDataCreated(new RExternalPtr(value, null, tag, prot));
        }

        public final RExternalPtr createExternalPtr(SymbolHandle value, Object tag) {
            assert tag != null : "null tag, use RNull.instance instead";
            return traceDataCreated(new RExternalPtr(value, null, tag, RNull.instance));
        }

        public RStringVector createStringVectorFromScalar(String value) {
            return traceDataCreated(RDataFactory.createStringVectorFromScalar(value));
        }

        public RLogicalVector createLogicalVectorFromScalar(boolean value) {
            return traceDataCreated(RDataFactory.createLogicalVectorFromScalar(value));
        }

        public RLogicalVector createLogicalVectorFromScalar(byte value) {
            return traceDataCreated(RDataFactory.createLogicalVectorFromScalar(value));
        }

        public RIntVector createIntVectorFromScalar(int value) {
            return traceDataCreated(RDataFactory.createIntVectorFromScalar(value));
        }

        public RDoubleVector createDoubleVectorFromScalar(double value) {
            return traceDataCreated(RDataFactory.createDoubleVectorFromScalar(value));
        }

        public RComplexVector createComplexVectorFromScalar(RComplex value) {
            return traceDataCreated(RDataFactory.createComplexVectorFromScalar(value));
        }

        public RRawVector createRawVectorFromScalar(RRaw value) {
            return traceDataCreated(RDataFactory.createRawVectorFromScalar(value));
        }
    }

    public static final boolean INCOMPLETE_VECTOR = false;
    public static final boolean COMPLETE_VECTOR = true;

    public static RIntVector createIntVectorFromNative(long address, int length) {
        return traceDataCreated(RIntVector.fromNative(address, length));
    }

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
        return traceDataCreated(new RIntVector(data, complete));
    }

    public static RIntVector createIntVector(int[] data, boolean complete, int[] dims) {
        return createIntVector(data, complete, dims, null);
    }

    public static RIntVector createIntVector(int[] data, boolean complete, RStringVector names) {
        return createIntVector(data, complete, null, names);
    }

    public static RIntVector createIntVector(int[] data, boolean complete, int[] dims, RStringVector names) {
        return createIntVector(data, complete, dims, names, null);
    }

    public static RIntVector createIntVector(int[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
        return traceDataCreated(new RIntVector(data, complete, dims, names, dimNames));
    }

    public static RDoubleVector createDoubleVectorFromNative(long address, int length) {
        return traceDataCreated(RDoubleVector.fromNative(address, length));
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
        return traceDataCreated(new RDoubleVector(data, complete));
    }

    public static RDoubleVector createDoubleVector(double[] data, boolean complete, int[] dims) {
        return createDoubleVector(data, complete, dims, null);
    }

    public static RDoubleVector createDoubleVector(double[] data, boolean complete, RStringVector names) {
        return createDoubleVector(data, complete, null, names);
    }

    public static RDoubleVector createDoubleVector(double[] data, boolean complete, int[] dims, RStringVector names) {
        return createDoubleVector(data, complete, dims, names, null);
    }

    public static RDoubleVector createDoubleVector(double[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
        return traceDataCreated(new RDoubleVector(data, complete, dims, names, dimNames));
    }

    public static RRawVector createRawVector(int length) {
        return createRawVector(new byte[length]);
    }

    public static RRawVector createRawVector(byte[] data) {
        return traceDataCreated(new RRawVector(data));
    }

    public static RRawVector createRawVector(byte[] data, int[] dims) {
        return createRawVector(data, dims, null);
    }

    public static RRawVector createRawVector(byte[] data, RStringVector names) {
        return createRawVector(data, null, names);
    }

    public static RRawVector createRawVector(byte[] data, int[] dims, RStringVector names) {
        return createRawVector(data, dims, names, null);
    }

    public static RRawVector createRawVector(byte[] data, int[] dims, RStringVector names, RList dimNames) {
        return traceDataCreated(new RRawVector(data, dims, names, dimNames));
    }

    public static RComplexVector createComplexVectorFromNative(long address, int length) {
        return traceDataCreated(RComplexVector.fromNative(address, length));
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
        return createComplexVector(data, !fillNA);
    }

    public static RComplexVector createComplexVector(double[] data, boolean complete) {
        return traceDataCreated(new RComplexVector(data, complete));
    }

    public static RComplexVector createComplexVector(double[] data, boolean complete, int[] dims) {
        return createComplexVector(data, complete, dims, null);
    }

    public static RComplexVector createComplexVector(double[] data, boolean complete, RStringVector names) {
        return createComplexVector(data, complete, null, names);
    }

    public static RComplexVector createComplexVector(double[] data, boolean complete, int[] dims, RStringVector names) {
        return createComplexVector(data, complete, dims, names, null);
    }

    public static RComplexVector createComplexVector(double[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
        return traceDataCreated(new RComplexVector(data, complete, dims, names, dimNames));
    }

    public static RStringVector createStringVector(String value) {
        return createStringVector(new String[]{value}, !RRuntime.isNA(value));
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
        return traceDataCreated(new RStringVector(data, complete));
    }

    public static RStringVector createStringVector(String[] data, boolean complete, int[] dims) {
        return createStringVector(data, complete, dims, null);
    }

    public static RStringVector createStringVector(String[] data, boolean complete, RStringVector names) {
        return createStringVector(data, complete, null, names);
    }

    public static RStringVector createStringVector(String[] data, boolean complete, int[] dims, RStringVector names) {
        return createStringVector(data, complete, dims, names, null);
    }

    public static RStringVector createStringVector(String[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
        return traceDataCreated(new RStringVector(data, complete, dims, names, dimNames));
    }

    public static RLogicalVector createLogicalVectorFromNative(long address, int length) {
        return traceDataCreated(RLogicalVector.fromNative(address, length));
    }

    public static RLogicalVector createLogicalVector(int length) {
        return createLogicalVector(length, false);
    }

    public static RLogicalVector createLogicalVector(int length, boolean fillNA) {
        byte[] data = new byte[length];
        if (fillNA) {
            Arrays.fill(data, RRuntime.LOGICAL_NA);
        }
        return createLogicalVector(data, !fillNA);
    }

    public static RLogicalVector createLogicalVector(byte[] data, boolean complete) {
        return traceDataCreated(new RLogicalVector(data, complete));
    }

    public static RLogicalVector createLogicalVector(byte[] data, boolean complete, int[] dims) {
        return createLogicalVector(data, complete, dims, null);
    }

    public static RLogicalVector createLogicalVector(byte[] data, boolean complete, RStringVector names) {
        return createLogicalVector(data, complete, null, names);
    }

    public static RLogicalVector createLogicalVector(byte[] data, boolean complete, int[] dims, RStringVector names) {
        return createLogicalVector(data, complete, dims, names, null);
    }

    public static RLogicalVector createLogicalVector(byte[] data, boolean complete, int[] dims, RStringVector names, RList dimNames) {
        return traceDataCreated(new RLogicalVector(data, complete, dims, names, dimNames));
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

    public static RStringSequence createStringSequence(String prefix, String suffix, int start, int stride, int length) {
        return traceDataCreated(new RStringSequence(prefix, suffix, start, stride, length));
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

    public static RRawVector createRawVectorFromNative(long address, int length) {
        return traceDataCreated(RRawVector.fromNative(address, length));
    }

    public static RRaw createRaw(byte value) {
        return traceDataCreated(new RRaw(value));
    }

    public static RStringVector createStringVectorFromScalar(String value) {
        return createStringVector(new String[]{value}, !RRuntime.isNA(value));
    }

    public static RLogicalVector createLogicalVectorFromScalar(boolean value) {
        return createLogicalVector(new byte[]{value ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE}, COMPLETE_VECTOR);
    }

    public static RLogicalVector createLogicalVectorFromScalar(byte value) {
        return createLogicalVector(new byte[]{value}, !RRuntime.isNA(value));
    }

    public static RIntVector createIntVectorFromScalar(int value) {
        return createIntVector(new int[]{value}, !RRuntime.isNA(value));
    }

    public static RDoubleVector createDoubleVectorFromScalar(double value) {
        return createDoubleVector(new double[]{value}, !RRuntime.isNA(value));
    }

    public static RComplexVector createComplexVectorFromScalar(RComplex value) {
        return createComplexVector(new double[]{value.getRealPart(), value.getImaginaryPart()}, !value.isNA());
    }

    public static RRawVector createRawVectorFromScalar(RRaw value) {
        return createRawVector(new byte[]{value.getValue()});
    }

    /*
     * Shared scalar conversion functions: these need to be replaced with
     * createXyzVectorFromScalar(...).makeSharedPermanent() if scalar types are removed.
     */

    public static Object createSharedStringVectorFromScalar(String value) {
        return value;
    }

    public static Object createSharedLogicalVectorFromScalar(boolean value) {
        return RRuntime.asLogical(value);
    }

    public static Object createSharedLogicalVectorFromScalar(byte value) {
        return value;
    }

    public static Object createSharedIntVectorFromScalar(int value) {
        return value;
    }

    public static Object createSharedDoubleVectorFromScalar(double value) {
        return value;
    }

    public static Object createSharedComplexVectorFromScalar(RComplex value) {
        return value;
    }

    public static Object createSharedRawVectorFromScalar(RRaw value) {
        return value;
    }

    public static RComplex createComplexRealOne() {
        return createComplex(1.0, 0.0);
    }

    public static RList createList(Object[] data) {
        return traceDataCreated(new RList(data));
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
        return createList(new Object[0]);
    }

    public static RList createList(int n) {
        return createList(createRNullArray(n));
    }

    public static RList createList(int size, RStringVector names) {
        if (names == null) {
            return createList(size);
        }
        assert size == names.getLength();
        return createList(createRNullArray(names.getLength()), null, names);
    }

    public static RList createList(Object[] data, int[] newDimensions, RStringVector names) {
        return createList(data, newDimensions, names, null);
    }

    public static RList createList(Object[] data, int[] newDimensions, RStringVector names, RList dimNames) {
        return traceDataCreated(new RList(data, newDimensions, names, dimNames));
    }

    public static RExpression createExpression(int size) {
        return createExpression(createRNullArray(size));
    }

    public static RExpression createExpression(int size, RStringVector names) {
        if (names == null) {
            return createExpression(size);
        }
        assert size == names.getLength();
        return createExpression(createRNullArray(size), names);
    }

    public static RExpression createExpression(Object[] data, int[] newDimensions) {
        return traceDataCreated(new RExpression(data, newDimensions, null, null));
    }

    public static RExpression createExpression(Object[] data, RStringVector names) {
        return traceDataCreated(new RExpression(data, null, names, null));
    }

    public static RExpression createExpression(Object[] data) {
        return traceDataCreated(new RExpression(data, null, null, null));
    }

    public static RSymbol createSymbol(String name) {
        assert Utils.isInterned(name);
        return traceDataCreated(new RSymbol(name));
    }

    /*
     * A version of {@link createSymbol} method mostly used from native code and in
     * serialization/deparsing.
     */
    public static RSymbol createSymbolInterned(String name) {
        return createSymbol(Utils.intern(name));
    }

    public static RLanguage createLanguage(Closure closure) {
        return traceDataCreated(new RLanguage(closure));
    }

    public static RPromise createPromise(PromiseState state, Closure closure, MaterializedFrame env) {
        assert closure != null;
        assert closure.getExpr() != null;
        return traceDataCreated(new RPromise(state, env, closure));
    }

    public static RPromise createEvaluatedPromise(PromiseState state, Closure closure, Object argumentValue) {
        return traceDataCreated(new RPromise(state, closure, argumentValue));
    }

    public static RPromise createEvaluatedPromise(Closure closure, Object value) {
        return traceDataCreated(new RPromise(PromiseState.Explicit, closure, value));
    }

    public static RPromise createEagerPromise(PromiseState state, Closure exprClosure, Object eagerValue, Assumption notChangedNonLocally, RCaller targetFrame, EagerFeedback feedback,
                    int wrapIndex, MaterializedFrame execFrame) {
        if (FastROptions.noEagerEval()) {
            throw RInternalError.shouldNotReachHere();
        }
        return traceDataCreated(new RPromise.EagerPromise(state, exprClosure, eagerValue, notChangedNonLocally, targetFrame, feedback, wrapIndex, execFrame));
    }

    public static RPromise createPromisedPromise(Closure exprClosure, Object eagerValue, Assumption notChangedNonLocally, RCaller targetFrame, EagerFeedback feedback, MaterializedFrame execFrame) {
        if (FastROptions.noEagerEval()) {
            throw RInternalError.shouldNotReachHere();
        }
        return traceDataCreated(new RPromise.EagerPromise(PromiseState.Promised, exprClosure, eagerValue, notChangedNonLocally, targetFrame, feedback, -1, execFrame));
    }

    public static Object createLangPairList(int size) {
        if (size == 0) {
            return RNull.instance;
        } else {
            return traceDataCreated(RPairList.create(size, SEXPTYPE.LANGSXP));
        }
    }

    public static Object createPairList(int size) {
        if (size == 0) {
            return RNull.instance;
        } else {
            return traceDataCreated(RPairList.create(size));
        }
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

    public static RFunction createFunction(String name, String packageName, RootCallTarget target, RBuiltinDescriptor builtin, MaterializedFrame enclosingFrame) {
        return traceDataCreated(new RFunction(name, packageName, target, builtin, enclosingFrame));
    }

    private static final AtomicInteger environmentCount = new AtomicInteger();

    @TruffleBoundary
    public static REnvironment createInternalEnv() {
        return traceDataCreated(new REnvironment.NewEnv(RRuntime.createNonFunctionFrame("<internal-env-" + environmentCount.incrementAndGet() + ">"), REnvironment.UNNAMED));
    }

    @TruffleBoundary
    public static REnvironment.NewEnv createNewEnv(FrameDescriptor desc, String name) {
        return traceDataCreated(new REnvironment.NewEnv(RRuntime.createNewFrame(desc), name));
    }

    @TruffleBoundary
    public static REnvironment.NewEnv createNewEnv(String name) {
        return traceDataCreated(new REnvironment.NewEnv(RRuntime.createNonFunctionFrame("<new-env-" + environmentCount.incrementAndGet() + ">"), name));
    }

    @TruffleBoundary
    public static REnvironment createNewEnv(String name, boolean hashed, int initialSize) {
        REnvironment.NewEnv env = new REnvironment.NewEnv(RRuntime.createNonFunctionFrame("<new-env-" + environmentCount.incrementAndGet() + ">"), name);
        env.setHashed(hashed);
        env.setInitialSize(initialSize);
        return traceDataCreated(env);
    }

    public static RS4Object createS4Object() {
        return traceDataCreated(new RS4Object());
    }

    public static RExternalPtr createExternalPtr(SymbolHandle value, Object externalObject, Object tag, Object prot) {
        assert tag != null : "null tag, use RNull.instance instead";
        assert prot != null : "null prot, use RNull.instance instead";
        return traceDataCreated(new RExternalPtr(value, externalObject, tag, prot));
    }

    public static RExternalPtr createExternalPtr(SymbolHandle value, Object tag, Object prot) {
        assert tag != null : "null tag, use RNull.instance instead";
        assert prot != null : "null prot, use RNull.instance instead";
        return traceDataCreated(new RExternalPtr(value, null, tag, prot));
    }

    public static RExternalPtr createExternalPtr(SymbolHandle value, Object tag) {
        assert tag != null : "null tag, use RNull.instance instead";
        return traceDataCreated(new RExternalPtr(value, null, tag, RNull.instance));
    }

    /*
     * Support for collecting information on allocations in this class. Rprofmem/Rprof register a
     * listener when active which, when memory profiling is enabled, is called with the object being
     * allocated. Owing to the use of the Assumption, there should be no overhead when disabled.
     */

    private static final Deque<Listener> listeners = new ConcurrentLinkedDeque<>();
    private static boolean allocationTracingEnabled = false;

    @CompilationFinal private static StateAssumption stateAssumption = new StateAssumption();

    /**
     * As long as "off" is valid, the feature is off. It is on as long as "on" is valid.
     */
    public static final class StateAssumption {
        private final Assumption off = Truffle.getRuntime().createAssumption("off");
        private final Assumption on = Truffle.getRuntime().createAssumption("on");

        public boolean isEnabled() {
            return !off.isValid() && on.isValid();
        }

        public StateAssumption setState(boolean enabled) {
            if (enabled && !stateAssumption.isEnabled()) {
                assert !isEnabled();
                off.invalidate();
                return this;
            } else if (!enabled && stateAssumption.isEnabled()) {
                assert isEnabled();
                on.invalidate();
                return new StateAssumption();
            } else {
                // no change needed
                return this;
            }
        }
    }

    public static synchronized void setAllocationTracingEnabled(boolean enabled) {
        allocationTracingEnabled = enabled;
        updateTracingState();
    }

    private static synchronized void updateTracingState() {
        boolean enabled = !listeners.isEmpty() || allocationTracingEnabled;
        stateAssumption = stateAssumption.setState(enabled);
    }

    private static <T> T traceDataCreated(T data) {
        if (stateAssumption.isEnabled()) {
            reportDataCreated(data);
        }
        return data;
    }

    @TruffleBoundary
    private static void reportDataCreated(Object data) {
        if (allocationTracingEnabled) {
            RContext ctx = RContext.getInstance();
            assert ctx != null;
            AllocationReporter allocationReporter = ctx.getAllocationReporter();

            allocationReporter.onEnter(null, 0, AllocationReporter.SIZE_UNKNOWN);

            long size = data instanceof RTypedValue ? getSize((RTypedValue) data) : AllocationReporter.SIZE_UNKNOWN;
            allocationReporter.onReturnValue(data, 0, size);
        }
        for (Listener listener : listeners) {
            listener.reportAllocation((RTypedValue) data);
        }
    }

    public interface Listener {
        /**
         * Invoked when an instance of an {@link RTypedValue} is created. Note that the initial
         * state of the complex objects, i.e., those with additional {@code Object} subclass fields,
         * which may also be {@link RTypedValue} instances is undefined other than by inspection. A
         * listener that computes the "size" of an object must take into account that
         * {@link RTypedValue} instances passed to a {@code createXXX} method will already have been
         * reported, but other data such as {@code int[]} instances for array dimensions will not.
         */
        void reportAllocation(RTypedValue data);
    }

    /**
     * Sets the listener of memory tracing events. For the time being there can only be one
     * listener. This can be extended to an array should we need more listeners.
     */
    public static synchronized void addListener(Listener listener) {
        listeners.addLast(listener);
        updateTracingState();
    }

    public static synchronized void removeListener(Listener listener) {
        listeners.remove(listener);
        updateTracingState();
    }

    private static Object[] createRNullArray(int size) {
        Object[] data = new Object[size];
        Arrays.fill(data, RNull.instance);
        return data;
    }

    private static long getSize(RTypedValue data) {
        return RObjectSize.getObjectSize(data);
    }
}
