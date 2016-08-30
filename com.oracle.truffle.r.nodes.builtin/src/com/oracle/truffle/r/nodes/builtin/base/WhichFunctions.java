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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.WhichFunctionsFactory.WhichMinMaxNodeGen;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * Encapsulates all which* as nested static classes.
 */
public class WhichFunctions {

    @RBuiltin(name = "which", kind = INTERNAL, parameterNames = {"x"}, behavior = PURE)
    public abstract static class Which extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("x").mustBe(logicalValue()).asLogicalVector();
        }

        @Specialization
        protected RIntVector which(RAbstractLogicalVector x,
                        @Cached("create()") VectorLengthProfile lengthProfile,
                        @Cached("createCountingProfile()") LoopConditionProfile loopProfile,
                        @Cached("createBinaryProfile()") ConditionProfile hasNamesProfile,
                        @Cached("create()") RAttributeProfiles attrProfiles,
                        @Cached("create()") NACheck naCheck) {
            int length = lengthProfile.profile(x.getLength());
            loopProfile.profileCounted(length);
            // determine the length of the result
            int resultLength = 0;
            for (int i = 0; loopProfile.inject(i < length); i++) {
                if (x.getDataAt(i) == RRuntime.LOGICAL_TRUE) {
                    resultLength++;
                }
            }
            // collect result indexes
            int[] result = new int[resultLength];
            int pos = 0;
            for (int i = 0; loopProfile.inject(i < length); i++) {
                if (x.getDataAt(i) == RRuntime.LOGICAL_TRUE) {
                    result[pos++] = i + 1;
                }
            }
            RStringVector names = x.getNames(attrProfiles);
            if (hasNamesProfile.profile(names != null)) {
                // collect result names
                String[] resultNames = new String[resultLength];
                naCheck.enable(names);
                pos = 0;
                for (int i = 0; i < x.getLength(); i++) {
                    if (x.getDataAt(i) == RRuntime.LOGICAL_TRUE) {
                        String name = names.getDataAt(i);
                        naCheck.check(name);
                        resultNames[pos++] = name;
                    }
                }
                return RDataFactory.createIntVector(result, RDataFactory.COMPLETE_VECTOR, RDataFactory.createStringVector(resultNames, naCheck.neverSeenNA()));
            } else {
                return RDataFactory.createIntVector(result, RDataFactory.COMPLETE_VECTOR);
            }
        }
    }

    @RBuiltin(name = "which.max", kind = INTERNAL, parameterNames = {"x"}, behavior = PURE)
    public abstract static class WhichMax {
        private WhichMax() {
            // private
        }

        public static WhichMinMax create(RNode[] arguments) {
            return WhichMinMaxNodeGen.create(true, arguments);
        }
    }

    @RBuiltin(name = "which.min", kind = INTERNAL, parameterNames = {"x"}, behavior = PURE)
    public abstract static class WhichMin {
        private WhichMin() {
            // private
        }

        public static WhichMinMax create(RNode[] arguments) {
            return WhichMinMaxNodeGen.create(false, arguments);
        }
    }

    public abstract static class WhichMinMax extends RBuiltinNode {

        private final boolean isMax;

        protected WhichMinMax(boolean isMax) {
            this.isMax = isMax;
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg(0, "x").allowNull().asDoubleVector(true, false, false);
        }

        @Specialization
        protected RIntVector which(RAbstractDoubleVector x,
                        @Cached("create()") VectorLengthProfile lengthProfile,
                        @Cached("createCountingProfile()") LoopConditionProfile loopProfile,
                        @Cached("createBinaryProfile()") ConditionProfile isNaNProfile,
                        @Cached("createBinaryProfile()") ConditionProfile hasNamesProfile,
                        @Cached("create()") RAttributeProfiles attrProfiles) {
            int length = lengthProfile.profile(x.getLength());
            loopProfile.profileCounted(length);
            double extreme = Double.NaN;
            int extremeIndex = -1;
            for (int i = 0; loopProfile.inject(i < length); i++) {
                double d = x.getDataAt(i);
                // inverted comparison to pass when extreme is NaN
                if (!Double.isNaN(d) && (isMax ? !(d <= extreme) : !(d >= extreme))) {
                    extreme = x.getDataAt(i);
                    extremeIndex = i;
                }
            }
            if (isNaNProfile.profile(extremeIndex == -1)) {
                return RDataFactory.createEmptyIntVector();
            }
            RStringVector names = x.getNames(attrProfiles);
            if (hasNamesProfile.profile(names != null)) {
                // collect result names
                RStringVector resultNames = RDataFactory.createStringVectorFromScalar(names.getDataAt(extremeIndex));
                return RDataFactory.createIntVector(new int[]{extremeIndex + 1}, true, resultNames);
            } else {
                return RDataFactory.createIntVectorFromScalar(extremeIndex + 1);
            }
        }

        @Specialization
        protected RIntVector which(@SuppressWarnings("unused") RNull x) {
            return RDataFactory.createEmptyIntVector();
        }
    }
}
