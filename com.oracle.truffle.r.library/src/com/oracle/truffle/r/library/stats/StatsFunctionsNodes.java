/*
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2003-2015, The R Foundation
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.Function2_1NodeGen;
import com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.Function2_2NodeGen;
import com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.Function3_1NodeGen;
import com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.Function3_2NodeGen;
import com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.Function4_1NodeGen;
import com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.Function4_2NodeGen;
import com.oracle.truffle.r.nodes.attributes.UnaryCopyAttributesNode;
import com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts.Casts;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function2_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function2_2;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_2;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function4_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function4_2;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

// inspired by arithmetic.c

public final class StatsFunctionsNodes {
    @CompilationFinal private static final RDoubleVector DUMMY_VECTOR = RDataFactory.createDoubleVectorFromScalar(1);

    private StatsFunctionsNodes() {
        // private
    }

    static final class StatFunctionProfiles {
        final BranchProfile nan = BranchProfile.create();
        final NACheck aCheck = NACheck.create();
        final NACheck bCheck = NACheck.create();
        final NACheck cCheck = NACheck.create();
        final NACheck dCheck = NACheck.create();
        final ConditionProfile copyAttrsFromA = ConditionProfile.createBinaryProfile();
        final ConditionProfile copyAttrsFromB = ConditionProfile.createBinaryProfile();
        final ConditionProfile copyAttrsFromC = ConditionProfile.createBinaryProfile();
        final ConditionProfile copyAttrsFromD = ConditionProfile.createBinaryProfile();
        final VectorLengthProfile resultVectorLengthProfile = VectorLengthProfile.create();
        final LoopConditionProfile loopConditionProfile = LoopConditionProfile.createCountingProfile();

        public static StatFunctionProfiles create() {
            return new StatFunctionProfiles();
        }
    }

    private static RDoubleVector evaluate4(Node node, Function4_2 function, RDoubleVector a, RDoubleVector b, RDoubleVector c, RDoubleVector d, boolean x,
                    boolean y, StatFunctionProfiles profiles, UnaryCopyAttributesNode copyAttributesNode) {
        int aLength = a.getLength();
        int bLength = b.getLength();
        int cLength = c.getLength();
        int dLength = d.getLength();
        if (aLength == 0 || bLength == 0 || cLength == 0 || dLength == 0) {
            return RDataFactory.createEmptyDoubleVector();
        }
        int length = profiles.resultVectorLengthProfile.profile(Math.max(aLength, Math.max(bLength, Math.max(cLength, dLength))));
        RBaseNode.reportWork(node, length);
        double[] result = new double[length];

        boolean complete = true;
        boolean nans = false;
        profiles.aCheck.enable(a);
        profiles.bCheck.enable(b);
        profiles.cCheck.enable(c);
        profiles.dCheck.enable(d);
        profiles.loopConditionProfile.profileCounted(length);
        for (int i = 0; profiles.loopConditionProfile.inject(i < length); i++) {
            double aValue = a.getDataAt(i % aLength);
            double bValue = b.getDataAt(i % bLength);
            double cValue = c.getDataAt(i % cLength);
            double dValue = d.getDataAt(i % dLength);
            double value;
            if (Double.isNaN(aValue) || Double.isNaN(bValue) || Double.isNaN(cValue) || Double.isNaN(dValue)) {
                profiles.nan.enter();
                if (profiles.aCheck.check(aValue) || profiles.bCheck.check(bValue) || profiles.cCheck.check(cValue) || profiles.cCheck.check(dValue)) {
                    value = RRuntime.DOUBLE_NA;
                    complete = false;
                } else {
                    value = Double.NaN;
                }
            } else {
                value = function.evaluate(aValue, bValue, cValue, dValue, x, y);
                if (Double.isNaN(value)) {
                    profiles.nan.enter();
                    nans = true;
                }
            }
            result[i] = value;
        }
        if (nans) {
            RError.warning(RError.SHOW_CALLER, RError.Message.NAN_PRODUCED);
        }
        RDoubleVector resultVec = RDataFactory.createDoubleVector(result, complete);

        // copy attributes if necessary:
        if (profiles.copyAttrsFromA.profile(aLength == length)) {
            copyAttributesNode.execute(resultVec, a);
        } else if (profiles.copyAttrsFromB.profile(bLength == length)) {
            copyAttributesNode.execute(resultVec, b);
        } else if (profiles.copyAttrsFromC.profile(cLength == length)) {
            copyAttributesNode.execute(resultVec, c);
        } else if (profiles.copyAttrsFromD.profile((dLength == length))) {
            copyAttributesNode.execute(resultVec, d);
        }

        return resultVec;
    }

    private static void castBoolean(Casts casts, int index, byte defaultValue) {
        // defensively we map missing to the default values
        casts.arg(index).asLogicalVector().findFirst(defaultValue).map(toBoolean());
    }

    private static void castDoubleVec(Casts casts, int index) {
        casts.arg(index).mustBe(numericValue(), Message.NON_NUMERIC_MATH).asDoubleVector(true, true, true);
    }

    public abstract static class Function3_2Node extends RExternalBuiltinNode.Arg5 {
        private final Function3_2 function;

        public Function3_2Node(Function3_2 function) {
            this.function = function;
        }

        public static Function3_2Node create(Function3_2 function) {
            return Function3_2NodeGen.create(function);
        }

        static {
            Casts casts = new Casts(Function3_2Node.class);
            castDoubleVec(casts, 0);
            castDoubleVec(casts, 1);
            castDoubleVec(casts, 2);
            castBoolean(casts, 3, RRuntime.LOGICAL_TRUE);
            castBoolean(casts, 4, RRuntime.LOGICAL_FALSE);
        }

        @Specialization
        protected RDoubleVector evaluate(RDoubleVector a, RDoubleVector b, RDoubleVector c, boolean x, boolean y,
                        @Cached("create()") StatFunctionProfiles profiles,
                        @Cached("create()") UnaryCopyAttributesNode copyAttributesNode) {
            return evaluate4(this, function, a, b, c, DUMMY_VECTOR, x, y, profiles, copyAttributesNode);
        }
    }

    public abstract static class Function4_1Node extends RExternalBuiltinNode.Arg5 {
        private final Function4_1 function;

        public Function4_1Node(Function4_1 function) {
            this.function = function;
        }

        public static Function4_1Node create(Function4_1 function) {
            return Function4_1NodeGen.create(function);
        }

        static {
            Casts casts = new Casts(Function4_1Node.class);
            castDoubleVec(casts, 0);
            castDoubleVec(casts, 1);
            castDoubleVec(casts, 2);
            castDoubleVec(casts, 3);
            castBoolean(casts, 4, RRuntime.LOGICAL_TRUE);
        }

        @Specialization
        protected RDoubleVector evaluate(RDoubleVector a, RDoubleVector b, RDoubleVector c, RDoubleVector d, boolean x,
                        @Cached("create()") StatFunctionProfiles profiles,
                        @Cached("create()") UnaryCopyAttributesNode copyAttributesNode) {
            return evaluate4(this, function, a, b, c, d, x, false /* dummy */, profiles, copyAttributesNode);
        }
    }

    public abstract static class Function4_2Node extends RExternalBuiltinNode.Arg6 {
        private final Function4_2 function;

        public Function4_2Node(Function4_2 function) {
            this.function = function;
        }

        public static Function4_2Node create(Function4_2 function) {
            return Function4_2NodeGen.create(function);
        }

        static {
            Casts casts = new Casts(Function4_2Node.class);
            castDoubleVec(casts, 0);
            castDoubleVec(casts, 1);
            castDoubleVec(casts, 2);
            castDoubleVec(casts, 3);
            castBoolean(casts, 4, RRuntime.LOGICAL_TRUE);
            castBoolean(casts, 5, RRuntime.LOGICAL_FALSE);
        }

        @Specialization
        protected RDoubleVector evaluate(RDoubleVector a, RDoubleVector b, RDoubleVector c, RDoubleVector d, boolean x, boolean y,
                        @Cached("create()") StatFunctionProfiles profiles,
                        @Cached("create()") UnaryCopyAttributesNode copyAttributesNode) {
            return evaluate4(this, function, a, b, c, d, x, y, profiles, copyAttributesNode);
        }
    }

    public abstract static class Function3_1Node extends RExternalBuiltinNode.Arg4 {
        private final Function3_1 function;

        public Function3_1Node(Function3_1 function) {
            this.function = function;
        }

        public static Function3_1Node create(Function3_1 function) {
            return Function3_1NodeGen.create(function);
        }

        static {
            Casts casts = new Casts(Function3_1Node.class);
            castDoubleVec(casts, 0);
            castDoubleVec(casts, 1);
            castDoubleVec(casts, 2);
            castBoolean(casts, 3, RRuntime.LOGICAL_TRUE);
        }

        @Specialization
        protected RDoubleVector evaluate(RDoubleVector a, RDoubleVector b, RDoubleVector c, boolean x,
                        @Cached("create()") StatFunctionProfiles profiles,
                        @Cached("create()") UnaryCopyAttributesNode copyAttributesNode) {
            return evaluate4(this, function, a, b, c, DUMMY_VECTOR, x, false /* dummy */, profiles, copyAttributesNode);
        }
    }

    public abstract static class Function2_1Node extends RExternalBuiltinNode.Arg3 {
        private final Function2_1 function;

        public Function2_1Node(Function2_1 function) {
            this.function = function;
        }

        public static Function2_1Node create(Function2_1 function) {
            return Function2_1NodeGen.create(function);
        }

        static {
            Casts casts = new Casts(Function2_1Node.class);
            castDoubleVec(casts, 0);
            castDoubleVec(casts, 1);
            castBoolean(casts, 2, RRuntime.LOGICAL_TRUE);
        }

        @Specialization
        protected RDoubleVector evaluate(RDoubleVector a, RDoubleVector b, boolean x,
                        @Cached("create()") StatFunctionProfiles profiles,
                        @Cached("create()") UnaryCopyAttributesNode copyAttributesNode) {
            return evaluate4(this, function, a, b, DUMMY_VECTOR, DUMMY_VECTOR, x, false /* dummy */, profiles, copyAttributesNode);
        }
    }

    public abstract static class Function2_2Node extends RExternalBuiltinNode.Arg4 {
        private final Function2_2 function;

        public Function2_2Node(Function2_2 function) {
            this.function = function;
        }

        public static Function2_2Node create(Function2_2 function) {
            return Function2_2NodeGen.create(function);
        }

        static {
            Casts casts = new Casts(Function2_2Node.class);
            castDoubleVec(casts, 0);
            castDoubleVec(casts, 1);
            castBoolean(casts, 2, RRuntime.LOGICAL_TRUE);
            castBoolean(casts, 3, RRuntime.LOGICAL_FALSE);
        }

        @Specialization
        protected RDoubleVector evaluate(RDoubleVector a, RDoubleVector b, boolean x, boolean y,
                        @Cached("create()") StatFunctionProfiles profiles,
                        @Cached("create()") UnaryCopyAttributesNode copyAttributesNode) {
            return evaluate4(this, function, a, b, DUMMY_VECTOR, DUMMY_VECTOR, x, y, profiles, copyAttributesNode);
        }
    }
}
