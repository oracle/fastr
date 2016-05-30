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
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNodeGen;
import com.oracle.truffle.r.nodes.builtin.ArgumentFilter;
import com.oracle.truffle.r.nodes.builtin.ArgumentMapper;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.CastUtils;
import com.oracle.truffle.r.nodes.builtin.CastUtils.Cast.Coverage;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public class CastFunctions {

    @TruffleBoundary
    public static void handleArgumentError(Object arg, CastNode node, RError.Message message, Object[] messageArgs) {
        if (RContext.getRRuntimeASTAccess() == null) {
            throw new IllegalArgumentException(String.format(message.message, CastBuilder.substituteArgPlaceholder(arg, messageArgs)));
        } else {
            throw RError.error(node, message, CastBuilder.substituteArgPlaceholder(arg, messageArgs));
        }
    }

    @TruffleBoundary
    public static void handleArgumentWarning(Object arg, CastNode node, RError.Message message, Object[] messageArgs, PrintWriter out) {
        if (message == null) {
            return;
        }

        if (out != null) {
            out.printf(message.message, CastBuilder.substituteArgPlaceholder(arg, messageArgs));
        } else if (RContext.getRRuntimeASTAccess() == null) {
            System.err.println(String.format(message.message, CastBuilder.substituteArgPlaceholder(arg,
                            messageArgs)));
        } else {
            RError.warning(node, message, CastBuilder.substituteArgPlaceholder(arg, messageArgs));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public abstract static class FilterNode extends CastNode {

        private final ArgumentFilter filter;
        private final RError.Message message;
        private final Object[] messageArgs;
        private final boolean boxPrimitives;
        private final boolean isWarning;
        private final PrintWriter out;
        private final TypeExpr resType;

        private final BranchProfile warningProfile = BranchProfile.create();

        @Child private BoxPrimitiveNode boxPrimitiveNode = BoxPrimitiveNodeGen.create();

        protected FilterNode(ArgumentFilter<?, ?> filter, boolean isWarning, RError.Message message, Object[] messageArgs, boolean boxPrimitives, PrintWriter out) {
            this.filter = filter;
            this.isWarning = isWarning;
            this.message = message;
            this.messageArgs = messageArgs;
            this.boxPrimitives = boxPrimitives;
            this.out = out;
            this.resType = filter.allowedTypes();
        }

        private void handleMessage(Object x) {
            if (isWarning) {
                if (message != null) {
                    warningProfile.enter();
                    handleArgumentWarning(x, this, message, messageArgs, out);
                }
            } else {
                handleArgumentError(x, this, message, messageArgs);
            }
        }

        @Specialization(guards = "evalCondition(x)")
        protected Object onTrue(Object x) {
            return x;
        }

        @Fallback
        protected Object onFalse(Object x) {
            handleMessage(x);
            return x;
        }

        protected boolean evalCondition(Object x) {
            Object y = boxPrimitives ? boxPrimitiveNode.execute(x) : x;
            return filter.test(y);
        }

        @Override
        protected TypeExpr resultTypes(TypeExpr inputType) {
            if (isWarning) {
                return inputType;
            } else {
                return inputType.and(resType);
            }
        }

        @Override
        protected Samples<?> collectSamples(TypeExpr inputType, Samples<?> downStreamSamples) {
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
        protected TypeExpr resultTypes(TypeExpr inputType) {
            if (inputType.coverageFrom(RNull.class, true) == Coverage.none) {
                return mapFn.resultTypes().and(TypeExpr.atom(RNull.class).not());
            } else {
                return mapFn.resultTypes().or(TypeExpr.atom(RNull.class));
            }
        }

        @Override
        protected Samples<?> collectSamples(TypeExpr inputType, Samples<?> downStreamSamples) {
            return mapFn.collectSamples(downStreamSamples);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public abstract static class ConditionalMapNode extends CastNode {

        private final ArgumentFilter argFilter;
        @Child private CastNode trueBranch;
        @Child private CastNode falseBranch;
        private final TypeExpr conditionType;

        protected ConditionalMapNode(ArgumentFilter<?, ?> argFilter, CastNode trueBranch, CastNode falseBranch) {
            this.argFilter = argFilter;
            this.trueBranch = trueBranch;
            this.falseBranch = falseBranch;
            this.conditionType = argFilter.allowedTypes();
        }

        @Override
        protected TypeExpr resultTypes(TypeExpr inputType) {
            return trueBranchResultTypes(inputType).or(falseBranchResultTypes(inputType));
        }

        private TypeExpr trueBranchResultTypes(TypeExpr inputType) {
            return trueBranch.resultTypes(conditionType.and(inputType));
        }

        private TypeExpr falseBranchResultTypes(TypeExpr inputType) {
            if (falseBranch != null) {
                return falseBranch.resultTypes(inputType.and(conditionType.not()));
            } else {
                return inputType.and(conditionType.not());
            }
        }

        protected boolean doMap(Object x) {
            return argFilter.test(x);
        }

        @Specialization(guards = "doMap(x)")
        protected Object map(Object x) {
            return trueBranch.execute(x);
        }

        @Specialization(guards = "!doMap(x)")
        protected Object noMap(Object x) {
            return x;
        }

        @Override
        protected Samples<?> collectSamples(TypeExpr inputType, Samples<?> downStreamSamples) {
            TypeExpr trueBranchResultType = trueBranchResultTypes(inputType);
            TypeExpr falseBranchResultType = falseBranchResultTypes(inputType);

            // filter out the incompatible samples
            Samples definedTrueBranchSamples = downStreamSamples.filter(x -> trueBranchResultType.isInstance(x));
            Samples definedFalseBranchSamples = downStreamSamples.filter(x -> falseBranchResultType.isInstance(x));

            Samples unmappedTrueBranchDefinedSamples = argFilter.collectSamples(trueBranch.collectSamples(conditionType.and(inputType), definedTrueBranchSamples));
            Samples unmappedFalseBranchDefinedSamples = falseBranch == null ? definedFalseBranchSamples :
                            falseBranch.collectSamples(inputType.and(conditionType.not()), definedFalseBranchSamples);

            return unmappedTrueBranchDefinedSamples.or(unmappedFalseBranchDefinedSamples);
        }
    }

    public abstract static class FindFirstNode extends CastNode {

        @Child private BoxPrimitiveNode boxPrimitive = BoxPrimitiveNodeGen.create();
        private final Class<?> elementClass;
        private final RError.Message message;
        private final Object[] messageArgs;
        private final PrintWriter out;
        private final Object defaultValue;

        private final BranchProfile warningProfile = BranchProfile.create();

        protected FindFirstNode(Class<?> elementClass, RError.Message message, Object[] messageArgs, PrintWriter out, Object defaultValue) {
            this.elementClass = elementClass;
            this.defaultValue = defaultValue;
            this.message = message;
            this.messageArgs = messageArgs;
            this.out = out;
        }

        protected FindFirstNode(Class<?> elementClass, Object defaultValue) {
            this(elementClass, null, null, null, defaultValue);
        }

        @Specialization
        protected Object onNull(RNull x) {
            return handleMissingElement(x);
        }

        @Specialization
        protected Object onMissing(RMissing x) {
            return handleMissingElement(x);
        }

        @Specialization(guards = "isVectorEmpty(x)")
        protected Object onEmptyVector(RAbstractVector x) {
            return handleMissingElement(x);
        }

        @Specialization(guards = "!isVector(x)")
        protected Object onNonVector(Object x) {
            return x;
        }

        private Object handleMissingElement(Object x) {
            if (defaultValue != null) {
                if (message != null) {
                    warningProfile.enter();
                    handleArgumentWarning(x, this, message, messageArgs, out);
                }
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

        protected boolean isVector(Object x) {
            return x instanceof RAbstractVector;
        }

        @Override
        protected Samples<?> collectSamples(TypeExpr inputType, Samples<?> downStreamSamples) {
            Samples<Object> defaultSamples = defaultSamples();

            // convert scalar samples to vector ones
            Samples<Object> vectorizedSamples = downStreamSamples.map(x -> CastUtils.singletonVector(x), x -> CastUtils.singletonVector(x));
            return defaultSamples.and(vectorizedSamples);
        }

        private Samples<Object> defaultSamples() {
            Set<Object> defaultNegativeSamples = new HashSet<>();
            Set<Object> defaultPositiveSamples = new HashSet<>();

            if (defaultValue == null) {
                defaultNegativeSamples.add(RNull.instance);
                Object emptyVec = CastUtils.emptyVector(elementClass);
                if (emptyVec != null) {
                    defaultNegativeSamples.add(emptyVec);
                }
                defaultNegativeSamples.add(RNull.instance);
            } else {
                defaultPositiveSamples.add(CastUtils.singletonVector(defaultValue));
            }

            return new Samples<>(defaultPositiveSamples, defaultNegativeSamples);
        }

        @Override
        protected TypeExpr resultTypes(TypeExpr inputType) {
            if (elementClass == null || elementClass == Object.class) {
                if (inputType.isAnything()) {
                    return TypeExpr.atom(RAbstractVector.class).not();
                } else {
                    Set<Type> resTypes = inputType.classify().stream().
                                    map(c -> CastUtils.elementType(c)).
                                    collect(Collectors.toSet());
                    return TypeExpr.union(resTypes);
                }
            } else {
                return TypeExpr.atom(elementClass).or(TypeExpr.atom(RNull.class)).or(TypeExpr.atom(RMissing.class));
            }
        }
    }

    public abstract static class NonNANode extends CastNode {

        private final RError.Message message;
        private final Object[] messageArgs;
        private final PrintWriter out;
        private final Object naReplacement;

        private final BranchProfile warningProfile = BranchProfile.create();

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
                if (message != null) {
                    warningProfile.enter();
                    handleArgumentWarning(arg, this, message, messageArgs, out);
                }
                return naReplacement;
            } else {
                handleArgumentError(arg, this, message, messageArgs);
                return null;
            }
        }

        @Override
        protected TypeExpr resultTypes(TypeExpr inputType) {
            return inputType;
        }

        @Specialization(guards = "!isLogicalNA(x)")
        protected Object onLogicalNonNA(byte x) {
            return x;
        }

        @Specialization(guards = "isLogicalNA(x)")
        protected Object onLogicalNA(byte x) {
            return handleNA(x);
        }

        protected boolean isLogicalNA(byte x) {
            return RRuntime.isNA(x);
        }

        @Specialization
        protected Object onBoolean(boolean x) {
            return x;
        }

        @Specialization(guards = "!isIntegerNA(x)")
        protected Object onIntegerNonNA(int x) {
            return x;
        }

        @Specialization(guards = "isIntegerNA(x)")
        protected Object onIntegerNA(int x) {
            return handleNA(x);
        }

        protected boolean isIntegerNA(int x) {
            return RRuntime.isNA(x);
        }

        @Specialization(guards = "!isDoubleNA(x)")
        protected Object onDoubleNonNA(double x) {
            return x;
        }

        @Specialization(guards = "isDoubleNA(x)")
        protected Object onDoubleNA(double x) {
            return handleNA(x);
        }

        protected boolean isDoubleNA(double x) {
            return RRuntime.isNAorNaN(x);
        }

        @Specialization(guards = "!isComplexNA(x)")
        protected Object onComplexNonNA(RComplex x) {
            return x;
        }

        @Specialization(guards = "isComplexNA(x)")
        protected Object onComplex(RComplex x) {
            return handleNA(x);
        }

        protected boolean isComplexNA(RComplex x) {
            return RRuntime.isNA(x);
        }

        @Specialization(guards = "!isStringNA(x)")
        protected Object onStringNonNA(String x) {
            return x;
        }

        @Specialization(guards = "isStringNA(x)")
        protected Object onStringNA(String x) {
            return handleNA(x);
        }

        protected boolean isStringNA(String x) {
            return RRuntime.isNA(x);
        }

        @Specialization
        protected Object onNull(RNull x) {
            return x;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Samples<?> collectSamples(TypeExpr inputTypes, Samples<?> downStreamSamples) {
            Set<Object> defaultPositiveSamples;
            Set<Object> defaultNegativeSamples;

            if (naReplacement != null) {
                defaultNegativeSamples = Collections.emptySet();
                defaultPositiveSamples = Collections.singleton(naReplacement);
            } else {
                defaultNegativeSamples = inputTypes.normalize().stream().
                                filter(t -> t instanceof Class).
                                map(t -> CastUtils.naValue((Class<?>) t)).
                                filter(x -> x != null).
                                collect(Collectors.toSet());
                defaultPositiveSamples = Collections.emptySet();
            }

            Samples<Object> defaultSamples = new Samples<>(defaultPositiveSamples, defaultNegativeSamples);
            return defaultSamples.and((Samples<Object>) downStreamSamples);
        }
    }

}
