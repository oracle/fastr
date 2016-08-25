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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.*;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.SUBSTITUTE;

import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.ArgCastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.SeqNodeGen.SeqInternalNodeGen;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleSequence;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "seq.default", aliases = {"seq.int"}, kind = SUBSTITUTE, parameterNames = {"from", "to", "by", "length.out", "along.with"}, behavior = PURE)
// Implement in R, but seq.int is PRIMITIVE (and may have to contain most, if not all, of the code
// below)
@SuppressWarnings("unused")
public abstract class Seq extends RBuiltinNode {

    private final CastBuilder argCastBuilder = new CastBuilder();
    private final ConditionProfile isEmptyProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile oneElementProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile singleLogical = BranchProfile.create();

    @Children private final CastNode[] argCastNodes = new CastNode[5];
    @Child private SeqInternal seqInternal;

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("from").mapIf(missingValue().not(), asVector());
        casts.arg("to").mapIf(missingValue().not(), asVector());
    }

    private void initSeqInternal() {
        // this looks a bit crazy because the conversion to int should only happen if the
        // param is not originally double (unless its value is 0), but the checks have to be
        // performed on converted and "original" values; also "32" should be converted to int,
        // but TRUE should become double
        //@formatter:off
        seqInternal = insert(SeqInternalNodeGen.create());
        Function<ArgCastBuilder<Object, ?>, CastNode> castNonDoubleFrom =
                        chain(asDoubleVector()).
                        with(mustBe(singleElement(), RError.SHOW_CALLER, true, RError.Message.MUST_BE_SCALAR, "from")).
                        with(findFirst().doubleElement()).
                        with(mustBe(notDoubleNA().and(isFinite()), RError.SHOW_CALLER, false, RError.Message.CANNOT_BE_INVALID, "from")).
                        with(mapIf(isFractional().not().and(neq((double) 0)),
                                        chain(map(doubleToInt())).
                                        end())).
                        end();
        argCastBuilder.arg(0).mapIf(missingValue().not().and(doubleValue().not().and(logicalValue().not())), castNonDoubleFrom).mapIf(doubleValue().or(logicalValue()),
                        chain(asDoubleVector()).
                        with(mustBe(singleElement(), RError.SHOW_CALLER, true, RError.Message.MUST_BE_SCALAR, "from")).
                        with(findFirst().doubleElement()).
                        with(mustBe(notDoubleNA().and(isFinite()), RError.SHOW_CALLER, false, RError.Message.CANNOT_BE_INVALID, "from")).
                        end());
        argCastNodes[0] = insert(argCastBuilder.getCasts()[0]);

        Function<ArgCastBuilder<Object, ?>, CastNode> castNonDoubleTo =
                        chain(asDoubleVector()).
                        with(mustBe(singleElement(), RError.SHOW_CALLER, true, RError.Message.MUST_BE_SCALAR, "to")).
                        with(findFirst().doubleElement()).
                        with(mustBe(notDoubleNA().and(isFinite()), RError.SHOW_CALLER, false, RError.Message.CANNOT_BE_INVALID, "to")).
                        with(mapIf(isFractional().not().and(neq((double) 0)),
                                        chain(map(doubleToInt())).
                                        end())).
                        end();
        argCastBuilder.arg(1).mapIf(missingValue().not().and(doubleValue().not()), castNonDoubleTo).mapIf(doubleValue().or(logicalValue()),
                        chain(asDoubleVector()).
                        with(mustBe(singleElement(), RError.SHOW_CALLER, true, RError.Message.MUST_BE_SCALAR, "to")).
                        with(findFirst().doubleElement()).
                        with(mustBe(notDoubleNA().and(isFinite()), RError.SHOW_CALLER, false, RError.Message.CANNOT_BE_INVALID, "from")).
                        end());
        argCastNodes[1] = insert(argCastBuilder.getCasts()[1]);

        Function<ArgCastBuilder<Object, ?>, CastNode> castNonDoubleStride =
                        chain(asDoubleVector()).
                        with(mustBe(singleElement(), RError.SHOW_CALLER, true, RError.Message.MUST_BE_SCALAR, "by")).
                        with(findFirst().doubleElement()).
                        with(mustBe(notDoubleNA().and(isFinite()), RError.SHOW_CALLER, false, RError.Message.CANNOT_BE_INVALID, "by")).
                        with(mapIf(isFractional().not().and(neq((double) 0)),
                                        chain(map(doubleToInt())).
                                        end())).
                        end();
        argCastBuilder.arg(2).mapIf(missingValue().not().and(doubleValue().not()), castNonDoubleTo).mapIf(doubleValue().or(logicalValue()),
                        chain(asDoubleVector()).
                        with(mustBe(singleElement(), RError.SHOW_CALLER, true, RError.Message.MUST_BE_SCALAR, "by")).
                        with(findFirst().doubleElement()).
                        with(mustBe(notDoubleNA().and(isFinite()), RError.SHOW_CALLER, false, RError.Message.CANNOT_BE_INVALID, "by")).
                        end());
        argCastNodes[2] = insert(argCastBuilder.getCasts()[2]);

        Function<ArgCastBuilder<Object, ?>, CastNode> castNonDoubleLengthOut =
                        chain(asDoubleVector()).
                        with(mustBe(singleElement(), RError.SHOW_CALLER, true, RError.Message.MUST_BE_SCALAR, "by")).
                        with(findFirst().doubleElement()).
                        with(mustBe(notDoubleNA().and(isFinite()), RError.SHOW_CALLER, false, RError.Message.CANNOT_BE_INVALID, "length.out")).
                        with(mapIf(isFractional().not().and(neq((double) 0)),
                                        chain(map(doubleToInt())).
                                        end())).
                        end();
        argCastBuilder.arg(3).mapIf(missingValue().not().and(doubleValue().not()), castNonDoubleTo).mapIf(doubleValue().or(logicalValue()),
                        chain(asDoubleVector()).
                        with(mustBe(singleElement(), RError.SHOW_CALLER, true, RError.Message.MUST_BE_SCALAR, "length.out")).
                        with(findFirst().doubleElement()).
                        with(mustBe(notDoubleNA().and(isFinite()), RError.SHOW_CALLER, false, RError.Message.CANNOT_BE_INVALID, "length.out")).
                        end());
        argCastNodes[3] = insert(argCastBuilder.getCasts()[3]);

        argCastBuilder.arg(4).mapIf(missingValue().not(), asVector());
        argCastNodes[4] = insert(argCastBuilder.getCasts()[4]);
        //@formatter:on
    }

    @Specialization
    protected Object seq(RAbstractVector start, RMissing to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        if (isEmptyProfile.profile(start.getLength() == 0)) {
            return RDataFactory.createEmptyIntVector();
        } else if (oneElementProfile.profile(start.getLength() == 1)) {
            if (start.getDataAtAsObject(0) instanceof Byte) {
                // this is mostly to handle somewhat unintuitive:
                // > seq.int(FALSE)
                // [1] 1
                singleLogical.enter();
                return 1;
            }
            initSeqInternal();
            Object newTo = argCastNodes[0].execute(start);
            // this is real:
            // > seq(10)
            // [1] 1 2 3 4 5 6 7 8 9 10
            // but
            // > seq(NaN)
            // Error in seq.default(NaN) : 'from' cannot be NA, NaN or infinite
            return seqInternal.execute(1, newTo, stride, lengthOut, alongWith);
        } else {
            // GNU R really does that (take the length of start to create a sequence)
            // > seq(c(10, 10))
            // [1] 1 2
            // > seq(c(NaN, NaN))
            // [1] 1 2
            return RDataFactory.createIntSequence(1, 1, start.getLength());
        }
    }

    @Specialization(guards = "notStartOnly(to, stride, lengthOut, alongWith)")
    protected Object seq(RAbstractVector start, Object to, Object stride, Object lengthOut, Object alongWith) {
        initSeqInternal();
        return seqInternal.execute(argCastNodes[0].execute(start), argCastNodes[1].execute(to), argCastNodes[2].execute(stride), argCastNodes[3].execute(lengthOut),
                        argCastNodes[4].execute(alongWith));
    }

    @Specialization()
    protected Object seq(RMissing start, RAbstractVector to, Object stride, RMissing lengthOut, RMissing alongWith) {
        initSeqInternal();
        return seqInternal.execute(1.0, argCastNodes[1].execute(to), argCastNodes[2].execute(stride), argCastNodes[3].execute(lengthOut), argCastNodes[4].execute(alongWith));
    }

    @Specialization(guards = "passMissingStart(to, lengthOut, alongWith)") // complement of previous
                                                                           // specialization
    protected Object seq(RMissing start, Object to, Object stride, Object lengthOut, Object alongWith) {
        initSeqInternal();
        return seqInternal.execute(start, argCastNodes[1].execute(to), argCastNodes[2].execute(stride), argCastNodes[3].execute(lengthOut), argCastNodes[4].execute(alongWith));
    }

    protected boolean notStartOnly(Object to, Object stride, Object lengthOut, Object alongWith) {
        return !(to == RMissing.instance && stride == RMissing.instance && lengthOut == RMissing.instance && alongWith == RMissing.instance);
    }

    protected boolean passMissingStart(Object to, Object lengthOut, Object alongWith) {
        return !(to instanceof RAbstractVector && lengthOut == RMissing.instance && alongWith == RMissing.instance);
    }

    protected abstract static class SeqInternal extends RBaseNode {

        protected abstract Object execute(Object start, Object to, Object stride, Object lengthOut, Object alongWith);

        private final ConditionProfile lengthProfile1 = ConditionProfile.createBinaryProfile();
        private final ConditionProfile lengthProfile2 = ConditionProfile.createBinaryProfile();
        private final ConditionProfile topLengthProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile error = BranchProfile.create();

        private RDoubleVector getVectorWithComputedStride(double start, double to, double lengthOut, boolean ascending) {
            int length = (int) Math.ceil(lengthOut);
            if (lengthProfile1.profile(length == 1)) {
                return RDataFactory.createDoubleVector(new double[]{start}, RDataFactory.COMPLETE_VECTOR);
            } else if (lengthProfile2.profile(length == 2)) {
                return RDataFactory.createDoubleVector(new double[]{start, to}, RDataFactory.COMPLETE_VECTOR);
            } else {
                double[] data = new double[length];
                data[0] = start;
                double newStride = (to - start) / (length - 1);
                if (!ascending) {
                    newStride = -newStride;
                }
                for (int i = 1; i < length - 1; i++) {
                    data[i] = start + (i * newStride);
                }
                data[length - 1] = to;
                return RDataFactory.createDoubleVector(data, RDataFactory.COMPLETE_VECTOR);
            }
        }

        private RIntVector getVectorWithComputedStride(int start, int to, double lengthOut, boolean ascending) {
            int length = (int) Math.ceil(lengthOut);
            if (lengthProfile1.profile(length == 1)) {
                return RDataFactory.createIntVector(new int[]{start}, RDataFactory.COMPLETE_VECTOR);
            } else if (lengthProfile2.profile(length == 2)) {
                return RDataFactory.createIntVector(new int[]{start, to}, RDataFactory.COMPLETE_VECTOR);
            } else {
                int[] data = new int[length];
                data[0] = start;
                int newStride = (to - start) / (length - 1);
                if (!ascending) {
                    newStride = -newStride;
                }
                for (int i = 1; i < length - 1; i++) {
                    data[i] = start + (i * newStride);
                }
                data[length - 1] = to;
                return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
            }
        }

        @Specialization(guards = {"zero(start, to)"})
        protected int seqZero(double start, double to, Object stride, RMissing lengthOut, RMissing alongWith) {
            return 0;
        }

        // int vector start, missing to

        @Specialization
        protected RAbstractVector seq(int start, RMissing to, RMissing stride, int lengthOut, RMissing alongWith) {
            if (topLengthProfile.profile(lengthOut == 0)) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return RDataFactory.createDoubleSequence(RRuntime.int2double(start), 1, lengthOut);
            }
        }

        @Specialization
        protected RAbstractVector seq(int start, RMissing to, RMissing stride, double lengthOut, RMissing alongWith) {
            if (topLengthProfile.profile(lengthOut == 0)) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return RDataFactory.createDoubleSequence(RRuntime.int2double(start), 1, (int) Math.ceil(lengthOut));
            }
        }

        // int vector start, int vector to

        @Specialization
        protected Object seq(int start, int to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
            if (topLengthProfile.profile(zero(start, to))) {
                return 0;
            } else {
                return RDataFactory.createIntSequence(start, ascending(start, to) ? 1 : -1, Math.abs(to - start) + 1);
            }
        }

        @Specialization
        protected Object seq(int start, int to, int stride, RMissing lengthOut, RMissing alongWith) {
            if (topLengthProfile.profile(zero(start, to))) {
                return 0;
            } else {
                return RDataFactory.createIntSequence(start, stride, Math.abs((to - start) / stride) + 1);
            }
        }

        @Specialization
        protected Object seq(int start, int to, double stride, RMissing lengthOut, RMissing alongWith) {
            if (topLengthProfile.profile(zero(start, to))) {
                return 0;
            } else {
                return RDataFactory.createDoubleSequence(RRuntime.int2double(start), stride, (int) (Math.abs((to - start) / stride)) + 1);
            }
        }

        @Specialization
        protected RIntVector seq(int start, int to, RMissing stride, int lengthOut, RMissing alongWith) {
            if (topLengthProfile.profile(lengthOut == 0)) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return getVectorWithComputedStride(start, to, RRuntime.int2double(lengthOut), ascending(start, to));
            }
        }

        @Specialization
        protected RIntVector seq(int start, int to, RMissing stride, double lengthOut, RMissing alongWith) {
            if (topLengthProfile.profile(lengthOut == 0)) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return getVectorWithComputedStride(start, to, lengthOut, ascending(start, to));
            }
        }

        @Specialization
        protected RAbstractVector seq(int start, RMissing to, RMissing stride, Object lengthOut, RAbstractVector alongWith) {
            if (topLengthProfile.profile(alongWith.getLength() == 0)) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return RDataFactory.createIntSequence(start, 1, alongWith.getLength());
            }
        }

        // int vector start, double vector to

        @Specialization
        protected Object seq(int start, double to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
            if (topLengthProfile.profile(zero(start, to))) {
                return 0;
            } else {
                return RDataFactory.createIntSequence(start, ascending(start, to) ? 1 : -1, (int) Math.abs(to - start) + 1);
            }
        }

        @Specialization
        protected Object seq(int start, double to, int stride, RMissing lengthOut, RMissing alongWith) {
            return seq(start, to, (double) stride, lengthOut, alongWith);
        }

        @Specialization
        protected Object seq(int start, double to, double stride, RMissing lengthOut, RMissing alongWith) {
            if (topLengthProfile.profile(zero(start, to))) {
                return 0;
            } else {
                int length = (int) (Math.abs(to - start) / stride);
                if (start + length * stride == to) {
                    length++;
                }
                return RDataFactory.createDoubleSequence(start, stride, length);
            }
        }

        @Specialization
        protected RAbstractVector seqLengthZero(int start, double to, RMissing stride, int lengthOut, RMissing alongWith) {
            if (topLengthProfile.profile(lengthOut == 0)) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return getVectorWithComputedStride(RRuntime.int2double(start), to, lengthOut, ascending(start, to));
            }
        }

        @Specialization
        protected RAbstractVector seqLengthZero(int start, double to, RMissing stride, double lengthOut, RMissing alongWith) {
            if (topLengthProfile.profile(lengthOut == 0)) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return getVectorWithComputedStride(RRuntime.int2double(start), to, lengthOut, ascending(start, to));
            }
        }

        // double vector start, missing to

        @Specialization
        protected RAbstractVector seqStartLengthOne(double start, RMissing to, RMissing stride, int lengthOut, RMissing alongWith) {
            if (topLengthProfile.profile(lengthOut == 0)) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return RDataFactory.createDoubleSequence(start, 1, lengthOut);
            }
        }

        @Specialization
        protected RAbstractVector seqStartLengthOne(double start, RMissing to, RMissing stride, double lengthOut, RMissing alongWith) {
            if (topLengthProfile.profile(lengthOut == 0)) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return RDataFactory.createDoubleSequence(start, 1, (int) Math.ceil(lengthOut));
            }
        }

        @Specialization
        protected RAbstractVector seqStartLengthOne(double start, RMissing to, RMissing stride, Object lengthOut, RAbstractVector alongWith) {
            if (topLengthProfile.profile(alongWith.getLength() == 0)) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return RDataFactory.createDoubleSequence(start, 1, alongWith.getLength());
            }
        }

        // double vector start, int vector to

        @Specialization
        protected Object seq(double start, int to, RMissing stride, RMissing lengthOut, RMissing alongWith,
                        @Cached("createBinaryProfile()") ConditionProfile intProfile) {
            if (topLengthProfile.profile(zero(start, to))) {
                return 0;
            } else {
                if (intProfile.profile((int) start == start)) {
                    return RDataFactory.createIntSequence((int) start, ascending(start, to) ? 1 : -1, (int) (Math.abs(to - start) + 1));
                } else {
                    return RDataFactory.createDoubleSequence(start, ascending(start, to) ? 1 : -1, (int) (Math.abs(to - start) + 1));
                }
            }
        }

        @Specialization
        protected Object seq(double start, int to, int stride, RMissing lengthOut, RMissing alongWith) {
            return seq(start, to, (double) stride, lengthOut, alongWith);
        }

        @Specialization
        protected Object seq(double start, int to, double stride, RMissing lengthOut, RMissing alongWith) {
            if (topLengthProfile.profile(zero(start, to))) {
                return 0;
            } else {
                return RDataFactory.createDoubleSequence(start, stride, (int) Math.abs((to - start) / stride) + 1);
            }
        }

        @Specialization
        protected RAbstractVector seqLengthZero(double start, int to, RMissing stride, int lengthOut, RMissing alongWith) {
            if (topLengthProfile.profile(lengthOut == 0)) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return getVectorWithComputedStride(start, RRuntime.int2double(to), lengthOut, ascending(start, to));
            }
        }

        @Specialization
        protected RAbstractVector seqLengthZero(double start, int to, RMissing stride, double lengthOut, RMissing alongWith) {
            if (topLengthProfile.profile(lengthOut == 0)) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return getVectorWithComputedStride(start, RRuntime.int2double(to), lengthOut, ascending(start, to));
            }
        }

        // double vector start, double vector to

        @Specialization
        protected Object seq(double start, double to, RMissing stride, RMissing lengthOut, RMissing alongWith, //
                        @Cached("createBinaryProfile()") ConditionProfile intProfile) {
            if (topLengthProfile.profile(zero(start, to))) {
                return 0;
            } else {
                if (intProfile.profile((int) start == start && (int) to == to)) {
                    return RDataFactory.createIntSequence((int) start, ascending(start, to) ? 1 : -1, (int) (Math.abs(to - start) + 1));
                } else {
                    return RDataFactory.createDoubleSequence(start, ascending(start, to) ? 1 : -1, (int) (Math.abs(to - start) + 1));
                }
            }
        }

        @Specialization
        protected Object seq(double start, double to, int stride, RMissing lengthOut, RMissing alongWith) {
            return seq(start, to, (double) stride, lengthOut, alongWith);
        }

        @Specialization
        protected Object seq(double start, double to, double stride, RMissing lengthOut, RMissing alongWith) {
            if (topLengthProfile.profile(zero(start, to))) {
                return 0;
            } else {
                return RDataFactory.createDoubleSequence(start, stride, (int) (Math.abs((to - start) / stride) + 1));
            }
        }

        @Specialization
        protected RAbstractVector seqLengthZero(double start, double to, RMissing stride, int lengthOut, RMissing alongWith) {
            if (topLengthProfile.profile(lengthOut == 0)) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return getVectorWithComputedStride(start, to, RRuntime.int2double(lengthOut), ascending(start, to));
            }
        }

        @Specialization
        protected RAbstractVector seqLengthZero(double start, double to, RMissing stride, double lengthOut, RMissing alongWith) {
            if (topLengthProfile.profile(lengthOut == 0)) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return getVectorWithComputedStride(start, to, lengthOut, ascending(start, to));
            }
        }

        @Specialization
        protected RAbstractVector seqLengthZero(RMissing start, RMissing to, RMissing stride, int lengthOut, RMissing alongWith) {
            if (topLengthProfile.profile(lengthOut == 0)) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return RDataFactory.createIntSequence(1, 1, lengthOut);
            }
        }

        @Specialization
        protected RAbstractVector seqLengthZero(RMissing start, RMissing to, RMissing stride, double lengthOut, RMissing alongWith) {
            if (topLengthProfile.profile(lengthOut == 0)) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return RDataFactory.createIntSequence(1, 1, (int) Math.ceil(lengthOut));
            }
        }

        @Specialization
        protected RAbstractVector seqLengthZeroAlong(RMissing start, RMissing to, RMissing stride, Object lengthOut, RAbstractVector alongWith) {
            if (topLengthProfile.profile(alongWith.getLength() == 0)) {
                return RDataFactory.createEmptyIntVector();
            } else {
                return RDataFactory.createIntSequence(1, 1, alongWith.getLength());
            }
        }

        @Specialization
        protected RDoubleSequence seq(RMissing start, int to, RMissing stride, int lengthOut, RMissing alongWith) {
            positiveLengthOut(lengthOut);
            return RDataFactory.createDoubleSequence(to - lengthOut + 1, 1, lengthOut);
        }

        @Specialization
        protected RDoubleSequence seq(RMissing start, int to, RMissing stride, double lengthOut, RMissing alongWith) {
            return seq(start, to, stride, (int) Math.ceil(lengthOut), alongWith);
        }

        @Specialization
        protected RDoubleSequence seq(RMissing start, double to, RMissing stride, double lengthOut, RMissing alongWith) {
            positiveLengthOut(lengthOut);
            return RDataFactory.createDoubleSequence(to - lengthOut + 1, 1, (int) Math.ceil(lengthOut));
        }

        private static boolean ascending(int start, int to) {
            return to > start;
        }

        private static boolean ascending(int start, double to) {
            return to > start;
        }

        private static boolean ascending(double start, int to) {
            return to > start;
        }

        protected static boolean ascending(double start, double to) {
            return to > start;
        }

        protected static boolean zero(int start, int to) {
            return start == 0 && to == 0;
        }

        protected static boolean zero(int start, double to) {
            return start == 0 && to == 0;
        }

        protected static boolean zero(double start, int to) {
            return start == 0 && to == 0;
        }

        protected static boolean zero(double start, double to) {
            return start == 0 && to == 0;
        }

        protected boolean positiveLengthOut(int lengthOut) {
            if (lengthOut < 0) {
                error.enter();
                throw RError.error(this, RError.Message.MUST_BE_POSITIVE, "length.out");
            }
            return true;
        }

        protected boolean positiveLengthOut(double lengthOut) {
            if (lengthOut < 0) {
                error.enter();
                throw RError.error(this, RError.Message.MUST_BE_POSITIVE, "length.out");
            }
            return true;
        }
    }
}
