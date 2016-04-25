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
package com.oracle.truffle.r.nodes.unary;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.samples;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNodeGen;
import com.oracle.truffle.r.nodes.builtin.ArgumentFilter;
import com.oracle.truffle.r.nodes.builtin.ArgumentMapper;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.CastUtils;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public class CastFunctions {

    public static void handleArgumentError(Object arg, CastNode node, RError.Message message, Object[] messageArgs) {
        if (RContext.getRRuntimeASTAccess() == null) {
            throw new IllegalArgumentException(String.format(message.message, CastBuilder.substituteArgPlaceholder(arg, messageArgs)));
        } else {
            throw RError.error(node, message, CastBuilder.substituteArgPlaceholder(arg, messageArgs));
        }
    }

    public static void handleArgumentWarning(Object arg, CastNode node, RError.Message message, Object[] messageArgs, PrintWriter out) {
        if (message == null) {
            return;
        }

        if (out != null) {
            out.printf(message.message, CastBuilder.substituteArgPlaceholder(arg, messageArgs));
        } else if (RContext.getRRuntimeASTAccess() == null) {
            System.err.println(String.format(message.message, CastBuilder.substituteArgPlaceholder(arg, messageArgs)));
        } else {
            RError.warning(node, message, CastBuilder.substituteArgPlaceholder(arg, messageArgs));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public abstract static class ArgumentValueConditionNode extends CastNode {

        private final ArgumentFilter filter;
        private final RError.Message message;
        private final Object[] messageArgs;
        private final boolean boxPrimitives;
        private final boolean isWarning;
        private final PrintWriter out;
        private final Set<Class<?>> resTypes;

        @Child private BoxPrimitiveNode boxPrimitiveNode = BoxPrimitiveNodeGen.create();

        protected ArgumentValueConditionNode(ArgumentFilter<?, ?> filter, boolean isWarning, RError.Message message, Object[] messageArgs, boolean boxPrimitives, PrintWriter out) {
            this.filter = filter;
            this.isWarning = isWarning;
            this.message = message;
            this.messageArgs = messageArgs;
            this.boxPrimitives = boxPrimitives;
            this.out = out;
            this.resTypes = filter.allowedTypes();
        }

        @Specialization
        protected Object evalCondition(Object x) {
            Object y = boxPrimitives ? boxPrimitiveNode.execute(x) : x;
            if (!filter.test(y)) {
                if (isWarning) {
                    handleArgumentWarning(x, this, message, messageArgs, out);
                } else {
                    handleArgumentError(x, this, message, messageArgs);
                }
            }
            return x;
        }

        @Override
        protected Set<Class<?>> resultTypes(Set<Class<?>> inputTypes) {
            return resTypes.isEmpty() ? inputTypes : resTypes;
        }

        @Override
        protected Samples<?> collectSamples(Samples<?> downStreamSamples) {
            Samples samplesForError = filter.collectSamples(downStreamSamples);
            return isWarning ? new Samples<>(samplesForError.positiveSamples(), samples()) : samplesForError;
        }

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public abstract static class MapNode extends CastNode {

        private final ArgumentMapper mapFn;

        protected MapNode(ArgumentMapper<?, ?> mapFn) {
            this.mapFn = mapFn;
        }

        @Specialization
        protected Object mapNull(RNull x) {
            Object res = mapFn.map(null);
            return res == null ? x : res;
        }

        @Specialization
        protected Object map(Object x) {
            return mapFn.map(x);
        }

        @Override
        protected Set<Class<?>> resultTypes(Set<Class<?>> inputTypes) {
            return mapFn.resultTypes();
        }

        @Override
        protected Samples<?> collectSamples(Samples<?> downStreamSamples) {
            return mapFn.collectSamples(downStreamSamples);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public abstract static class ConditionalMapNode extends CastNode {

        private final ArgumentFilter argFilter;
        private final ArgumentMapper mapFn;
        private final Set<Class<?>> conditionTypes;

        protected ConditionalMapNode(ArgumentFilter<?, ?> argFilter, ArgumentMapper<?, ?> mapFn) {
            this.argFilter = argFilter;
            this.mapFn = mapFn;
            this.conditionTypes = argFilter.allowedTypes();
        }

        @Override
        protected Set<Class<?>> resultTypes(Set<Class<?>> inputTypes) {
            if (conditionTypes.isEmpty()) {
                return inputTypes;
            } else {
                Set<Class<?>> newTypes = new HashSet<>(inputTypes);
                newTypes.removeIf(x -> conditionTypes.contains(x));
                newTypes.addAll(mapFn.resultTypes());
                return newTypes;
            }
        }

        protected boolean doMap(Object x) {
            return argFilter.test(x);
        }

        @Specialization(guards = "doMap(x)")
        protected Object map(Object x) {
            return mapFn.map(x);
        }

        @Specialization
        protected Object mapNull(RNull x) {
            Object res = mapFn.map(null);
            return res == null ? x : res;
        }

        @Specialization(guards = "!doMap(x)")
        protected Object noMap(Object x) {
            return x;
        }

        private boolean isDefined(Object x) {
            Set<Class<?>> mapperResultTypes = mapFn.resultTypes();
            return mapperResultTypes.stream().anyMatch(cls -> cls.isInstance(x));
        }

        @Override
        protected Samples<?> collectSamples(Samples<?> downStreamSamples) {
            // filter out the incompatible samples
            Samples<?> definedSamples = downStreamSamples.filter(x -> isDefined(x));

            Set<?> undefinedPositive = downStreamSamples.positiveSamples().stream().filter(x -> !isDefined(x)).collect(Collectors.toSet());
            Set<Object> undefinedNegative = downStreamSamples.negativeSamples().stream().filter(x -> !isDefined(x)).collect(Collectors.toSet());

            Samples<Object> unmappedDefinedSamples = mapFn.collectSamples(definedSamples);

            return argFilter.collectSamples(new Samples<>(undefinedPositive, undefinedNegative).and(unmappedDefinedSamples));
        }
    }

    public abstract static class FindFirstNode extends CastNode {

        @Child private BoxPrimitiveNode boxPrimitive = BoxPrimitiveNodeGen.create();
        private final Class<?> elementClass;
        private final RError.Message message;
        private final Object[] messageArgs;
        private final PrintWriter out;
        private final Object defaultValue;

        protected FindFirstNode(Class<?> elementClass, RError.Message message, Object[] messageArgs, PrintWriter out, Object defaultValue) {
            this.elementClass = elementClass;
            this.defaultValue = defaultValue == null ? RNull.instance : defaultValue;
            this.message = message;
            this.messageArgs = messageArgs;
            this.out = out;
        }

        protected FindFirstNode(Class<?> elementClass, Object defaultValue) {
            this(elementClass, null, null, null, defaultValue);
        }

        @Specialization
        protected Object onVector(RAbstractVector x) {
            return x.getLength() > 0 ? x.getDataAtAsObject(0) : defaultValue;
        }

        @Specialization
        protected Object onNull(@SuppressWarnings("unused") RNull x) {
            return defaultValue;
        }

        @Specialization
        protected Object onScalar(int x) {
            return x;
        }

        @Specialization
        protected Object onScalar(double x) {
            return x;
        }

        @Specialization
        protected Object onScalar(byte x) {
            return x;
        }

        @Specialization
        protected Object onScalar(String x) {
            return x;
        }

        @Specialization
        protected Object onScalar(RComplex x) {
            return x;
        }

        @Specialization
        protected Object onNull(RNull x) {
            return handleMissingElement(x);
        }

        @Specialization(guards = "isVectorEmpty(x)")
        protected Object onEmptyVector(RAbstractVector x) {
            return handleMissingElement(x);
        }

        private Object handleMissingElement(Object x) {
            if (defaultValue != null) {
                handleArgumentWarning(x, this, message, messageArgs, out);
                return defaultValue;
            } else {
                handleArgumentError(x, this, message, messageArgs);
                return null;
            }
        }

        @Specialization(guards = "!isVectorEmpty(x)")
        protected Object onVector(RAbstractVector x) {
            return x.getDataAtAsObject(0);
        }

        protected boolean isVectorEmpty(RAbstractVector x) {
            return x.getLength() == 0;
        }

        @Override
        protected Samples<?> collectSamples(Samples<?> downStreamSamples) {
            Samples<Object> defaultSamples = defaultSamples();

            // convert scalar samples to vector ones
            Samples<Object> vectorizedSamples = downStreamSamples.map(x -> CastUtils.singletonVector(x));
            return defaultSamples.and(vectorizedSamples);
        }

        private Samples<Object> defaultSamples() {
            Set<Object> defaultNegativeSamples = new HashSet<>();

            if (defaultValue == null) {
                defaultNegativeSamples.add(RNull.instance);
                Object emptyVec = CastUtils.emptyVector(elementClass);
                if (emptyVec != null) {
                    defaultNegativeSamples.add(emptyVec);
                }
                defaultNegativeSamples.add(RNull.instance);
            }

            return new Samples<>(Collections.emptySet(), defaultNegativeSamples);
        }

        @Override
        protected Set<Class<?>> resultTypes(Set<Class<?>> inputTypes) {
            return inputTypes.stream().map(x -> CastUtils.elementType(x)).collect(Collectors.toCollection(HashSet::new));
        }

    }

    public abstract static class NonNANode extends CastNode {

        private final RError.Message message;
        private final Object[] messageArgs;
        private final PrintWriter out;
        private final Object naReplacement;

        protected NonNANode(RError.Message message, Object[] messageArgs, PrintWriter out, Object naReplacement) {
            this.message = message;
            this.messageArgs = messageArgs;
            this.out = out;
            this.naReplacement = naReplacement;
        }

        protected NonNANode(Object naReplacement) {
            this(null, null, null, naReplacement);
        }

        private Object handleNA(Object arg) {
            if (naReplacement != null) {
                handleArgumentWarning(arg, this, message, messageArgs, out);
                return naReplacement;
            } else {
                handleArgumentError(arg, this, message, messageArgs);
                return null;
            }
        }

        @Specialization
        protected Object onLogical(byte x) {
            return RRuntime.isNA(x) ? handleNA(x) : x;
        }

        @Specialization
        protected Object onBoolean(boolean x) {
            return x;
        }

        @Specialization
        protected Object onInteger(int x) {
            return RRuntime.isNA(x) ? handleNA(x) : x;
        }

        @Specialization
        protected Object onDouble(double x) {
            return RRuntime.isNAorNaN(x) ? handleNA(x) : x;
        }

        @Specialization
        protected Object onComplex(RComplex x) {
            return RRuntime.isNA(x) ? handleNA(x) : x;
        }

        @Specialization
        protected Object onString(String x) {
            return RRuntime.isNA(x) ? handleNA(x) : x;
        }

        @Specialization
        protected Object onNull(RNull x) {
            return x;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Samples<?> collectSamples(Samples<?> downStreamSamples) {
            Set<Object> defaultPositiveSamples = Collections.emptySet();
            Set<?> defaultNegativeSamples = naReplacement != null ? samples() : samples(RRuntime.INT_NA, RRuntime.DOUBLE_NA, RRuntime.LOGICAL_NA, RRuntime.STRING_NA, RComplex.createNA());
            Samples<Object> defaultSamples = new Samples<>(defaultPositiveSamples, defaultNegativeSamples);
            return defaultSamples.and((Samples<Object>) downStreamSamples);
        }

    }

}
