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
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.intNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.typeName;
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.InitAttributesNode;
import com.oracle.truffle.r.nodes.attributes.SetFixedAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.RepeatNodeGen.FastRInternalRepeatNodeGen;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.PipelineBuilder;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.nodes.function.call.PrepareMatchInternalArguments;
import com.oracle.truffle.r.nodes.function.call.PrepareMatchInternalArgumentsNodeGen;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * The {@code rep} builtin works as follows.
 * <ol>
 * <li>If {@code each} is greater than one, all elements of {@code x} are first replicated
 * {@code each} times.
 * <li>If {@code length.out} is given, the result of the first step is truncated or extended as
 * required. In this case, {@code times} is ignored.
 * <li>If {@code length.out} is not given, {@code times} is regarded:
 * <ul>
 * <li>If {@code times} is a one-element vector, the result of the first step is replicated
 * {@code times} times.
 * <li>If {@code times} is a vector longer than one, and {@code each} is greater than one, an error
 * is issued.
 * <li>If {@code times} is a vector longer than one, and {@code each} is one, and {@code times} is
 * as long as {@code x}, each element of {@code x} is given the number of times indicated by the
 * value at the same index of {@code times}. If {@code times} has a different length, an error is
 * issued.
 * </ul>
 * </ol>
 */
@RBuiltin(name = "rep", kind = PRIMITIVE, parameterNames = {"x", "..."}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class Repeat extends RBuiltinNode {

    private static final PipelineBuilder PB_TIMES;
    private static final PipelineBuilder PB_LENGTH_OUT;
    private static final PipelineBuilder PB_EACH;
    private static final FormalArguments FORMALS;
    private static final int ARG_IDX_TIMES;
    private static final int ARG_IDX_LENGHT_OUT;
    private static final int ARG_IDX_EACH;

    @Child private FastRInternalRepeat internalNode = FastRInternalRepeatNodeGen.create();
    @Child private CastNode castTimes = PB_TIMES.buildNode();
    @Child private CastNode castLengthOut = PB_LENGTH_OUT.buildNode();
    @Child private CastNode castEach = PB_EACH.buildNode();
    @Child private PrepareMatchInternalArguments prepareArgs = PrepareMatchInternalArgumentsNodeGen.create(FORMALS, this);

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, RArgsValuesAndNames.EMPTY};
    }

    static {
        Casts casts = new Casts(Repeat.class);
        casts.arg("x").mustBe(abstractVectorValue(), RError.Message.ATTEMPT_TO_REPLICATE, typeName());

        // prepare cast pipeline nodes for vararg matching
        PB_TIMES = new PipelineBuilder("times");
        PB_TIMES.fluent().defaultError(RError.Message.INVALID_ARGUMENT, "times").mustNotBeNull().asIntegerVector();

        PB_LENGTH_OUT = new PipelineBuilder("length.out");
        PB_LENGTH_OUT.fluent().asIntegerVector().shouldBe(size(1).or(size(0)),
                        RError.Message.FIRST_ELEMENT_USED, "length.out").findFirst(RRuntime.INT_NA,
                                        RError.Message.FIRST_ELEMENT_USED, "length.out").mustBe(intNA().or(gte(0)));

        PB_EACH = new PipelineBuilder("each");
        PB_EACH.fluent().asIntegerVector().shouldBe(size(1).or(size(0)),
                        RError.Message.FIRST_ELEMENT_USED, "each").findFirst(1, RError.Message.FIRST_ELEMENT_USED,
                                        "each").replaceNA(1).mustBe(gte(0));

        ArgumentsSignature signature = ArgumentsSignature.get("times", "length.out", "each");
        ARG_IDX_TIMES = 0;
        ARG_IDX_LENGHT_OUT = 1;
        ARG_IDX_EACH = 2;
        FORMALS = FormalArguments.createForBuiltin(new Object[]{1, RRuntime.INT_NA, 1}, signature);
    }

    @Specialization
    protected Object repeat(VirtualFrame frame, RAbstractVector x, RArgsValuesAndNames args) {
        RArgsValuesAndNames margs = prepareArgs.execute(args, null);

        // cast arguments
        Object times = castTimes.execute(margs.getArgument(ARG_IDX_TIMES));
        Object lengthOut = castLengthOut.execute(margs.getArgument(ARG_IDX_LENGHT_OUT));
        Object each = castEach.execute(margs.getArgument(ARG_IDX_EACH));

        return internalNode.execute(frame, x, times, lengthOut, each);
    }

    @TypeSystemReference(RTypes.class)
    abstract static class FastRInternalRepeat extends RBaseNode {
        private final ConditionProfile lengthOutOrTimes = ConditionProfile.createBinaryProfile();
        private final ConditionProfile oneTimeGiven = ConditionProfile.createBinaryProfile();
        private final ConditionProfile replicateOnce = ConditionProfile.createBinaryProfile();

        @Child private GetNamesAttributeNode getNames = GetNamesAttributeNode.create();

        public abstract RAbstractVector execute(VirtualFrame frame, Object... args);

        protected boolean hasNames(RAbstractVector x) {
            return getNames.getNames(x) != null;
        }

        @Specialization(guards = {"x.getLength() == 1", "times.getLength() == 1", "each <= 1", "!hasNames(x)"})
        protected RAbstractVector repNoEachNoNamesSimple(RAbstractDoubleVector x, RAbstractIntVector times, int lengthOut, @SuppressWarnings("unused") int each) {
            int t = times.getDataAt(0);
            if (t < 0) {
                throw error(RError.Message.INVALID_ARGUMENT, "times");
            }
            int length = lengthOutOrTimes.profile(!RRuntime.isNA(lengthOut)) ? lengthOut : t;
            double[] data = new double[length];
            Arrays.fill(data, x.getDataAt(0));
            return RDataFactory.createDoubleVector(data, !RRuntime.isNA(x.getDataAt(0)));
        }

        @Specialization(guards = {"each > 1", "!hasNames(x)"})
        protected RAbstractVector repEachNoNames(RAbstractVector x, RAbstractIntVector times, int lengthOut, int each) {
            if (times.getLength() > 1) {
                throw error(RError.Message.INVALID_ARGUMENT, "times");
            }
            RAbstractVector input = handleEach(x, each);
            if (lengthOutOrTimes.profile(!RRuntime.isNA(lengthOut))) {
                return handleLengthOut(input, lengthOut, false);
            } else {
                return handleTimes(input, times, false);
            }
        }

        @Specialization(guards = {"each <= 1", "!hasNames(x)"})
        protected RAbstractVector repNoEachNoNames(RAbstractVector x, RAbstractIntVector times, int lengthOut, @SuppressWarnings("unused") int each) {
            if (lengthOutOrTimes.profile(!RRuntime.isNA(lengthOut))) {
                return handleLengthOut(x, lengthOut, true);
            } else {
                return handleTimes(x, times, true);
            }
        }

        @Specialization(guards = {"each > 1", "hasNames(x)"})
        protected RAbstractVector repEachNames(RAbstractVector x, RAbstractIntVector times, int lengthOut, int each,
                        @Cached("create()") InitAttributesNode initAttributes,
                        @Cached("createNames()") SetFixedAttributeNode putNames) {
            if (times.getLength() > 1) {
                throw error(RError.Message.INVALID_ARGUMENT, "times");
            }
            RAbstractVector input = handleEach(x, each);
            RStringVector names = (RStringVector) handleEach(getNames.getNames(x), each);
            RVector<?> r;
            if (lengthOutOrTimes.profile(!RRuntime.isNA(lengthOut))) {
                names = (RStringVector) handleLengthOut(names, lengthOut, false);
                r = handleLengthOut(input, lengthOut, false);
            } else {
                names = (RStringVector) handleTimes(names, times, false);
                r = handleTimes(input, times, false);
            }
            putNames.execute(initAttributes.execute(r), names);
            return r;
        }

        @Specialization(guards = {"each <= 1", "hasNames(x)"})
        protected RAbstractVector repNoEachNames(RAbstractVector x, RAbstractIntVector times, int lengthOut, @SuppressWarnings("unused") int each,
                        @Cached("create()") InitAttributesNode initAttributes,
                        @Cached("createNames()") SetFixedAttributeNode putNames) {
            RStringVector names;
            RVector<?> r;
            if (lengthOutOrTimes.profile(!RRuntime.isNA(lengthOut))) {
                names = (RStringVector) handleLengthOut(getNames.getNames(x), lengthOut, true);
                r = handleLengthOut(x, lengthOut, true);
            } else {
                names = (RStringVector) handleTimes(getNames.getNames(x), times, true);
                r = handleTimes(x, times, true);
            }
            putNames.execute(initAttributes.execute(r), names);
            return r;
        }

        /**
         * Prepare the input vector by replicating its elements.
         */
        private static RVector<?> handleEach(RAbstractVector x, int each) {
            RVector<?> r = x.createEmptySameType(x.getLength() * each, x.isComplete());
            for (int i = 0; i < x.getLength(); i++) {
                for (int j = i * each; j < (i + 1) * each; j++) {
                    r.transferElementSameType(j, x, i);
                }
            }
            return r;
        }

        /**
         * Extend or truncate the vector to a specified length.
         */
        private static RVector<?> handleLengthOut(RAbstractVector x, int lengthOut, boolean copyIfSameSize) {
            if (x.getLength() == lengthOut) {
                return (RVector<?>) (copyIfSameSize ? x.copy() : x);
            }
            return x.copyResized(lengthOut, false);
        }

        /**
         * Replicate the vector a given number of times.
         */
        private RVector<?> handleTimes(RAbstractVector x, RAbstractIntVector times, boolean copyIfSameSize) {
            if (oneTimeGiven.profile(times.getLength() == 1)) {
                // only one times value is given
                final int howManyTimes = times.getDataAt(0);
                if (howManyTimes < 0) {
                    throw error(RError.Message.INVALID_ARGUMENT, "times");
                }
                if (replicateOnce.profile(howManyTimes == 1)) {
                    return (RVector<?>) (copyIfSameSize ? x.copy() : x);
                } else {
                    return x.copyResized(x.getLength() * howManyTimes, false);
                }
            } else {
                // times is a vector with several elements
                if (x.getLength() != times.getLength()) {
                    throw error(RError.Message.INVALID_ARGUMENT, "times");
                }
                // iterate once over the times vector to determine result vector size
                int resultLength = 0;
                for (int i = 0; i < times.getLength(); i++) {
                    int t = times.getDataAt(i);
                    if (t < 0) {
                        throw error(RError.Message.INVALID_ARGUMENT, "times");
                    }
                    resultLength += t;
                }
                // create and populate result vector
                RVector<?> r = x.createEmptySameType(resultLength, x.isComplete());
                int wp = 0; // write pointer
                for (int i = 0; i < x.getLength(); i++) {
                    for (int j = 0; j < times.getDataAt(i); ++j, ++wp) {
                        r.transferElementSameType(wp, x, i);
                    }
                }
                return r;
            }
        }
    }
}
