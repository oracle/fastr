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
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.typeName;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory.VectorFactory;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;

@RBuiltin(name = "rep.int", kind = INTERNAL, parameterNames = {"x", "times"}, behavior = PURE)
public abstract class RepeatInternal extends RBuiltinNode.Arg2 {

    private final ConditionProfile timesOneProfile = ConditionProfile.createBinaryProfile();

    static {
        Casts casts = new Casts(RepeatInternal.class);
        casts.arg("x").mustBe(abstractVectorValue(), RError.Message.ATTEMPT_TO_REPLICATE, typeName());
        casts.arg("times").defaultError(RError.Message.INVALID_TYPE, typeName(), "times", "vector").mustBe(abstractVectorValue()).asIntegerVector().mustBe(notEmpty(),
                        RError.Message.INVALID_VALUE, "times");
    }

    private RAbstractVector performRep(RAbstractVector value, RAbstractIntVector times, VectorFactory factory, VectorAccess valueAccess, VectorAccess timesAccess, VectorAccess resultAccess) {
        try (SequentialIterator valueIter = valueAccess.access(value); SequentialIterator timesIter = timesAccess.access(times)) {
            int valueLength = valueAccess.getLength(valueIter);
            int timesLength = timesAccess.getLength(timesIter);

            RVector<?> result;
            if (timesOneProfile.profile(timesLength == 1)) {
                timesAccess.next(timesIter);
                int timesValue = timesAccess.getInt(timesIter);
                if (timesValue < 0) {
                    throw error(RError.Message.INVALID_VALUE, "times");
                }
                result = factory.createVector(valueAccess.getType(), timesValue * valueLength, false);
                try (SequentialIterator resultIter = resultAccess.access(result)) {
                    for (int i = 0; i < timesValue; i++) {
                        while (valueAccess.next(valueIter)) {
                            resultAccess.next(resultIter);
                            resultAccess.setFromSameType(resultIter, valueAccess, valueIter);
                        }
                        valueAccess.reset(valueIter);
                    }
                }
            } else if (timesLength == valueLength) {
                int count = 0;
                while (timesAccess.next(timesIter)) {
                    int num = timesAccess.getInt(timesIter);
                    if (num < 0) {
                        throw error(RError.Message.INVALID_VALUE, "times");
                    }
                    count += num;
                }
                result = factory.createVector(valueAccess.getType(), count, false);

                timesAccess.reset(timesIter);
                try (SequentialIterator resultIter = resultAccess.access(result)) {
                    while (timesAccess.next(timesIter) && valueAccess.next(valueIter)) {
                        int num = timesAccess.getInt(timesIter);
                        for (int i = 0; i < num; i++) {
                            resultAccess.next(resultIter);
                            resultAccess.setFromSameType(resultIter, valueAccess, valueIter);
                        }
                    }
                }
            } else {
                throw error(RError.Message.INVALID_VALUE, "times");
            }
            result.setComplete(!valueAccess.na.isEnabled());
            return result;
        }
    }

    @Specialization(guards = {"valueAccess.supports(value)", "timesAccess.supports(times)"})
    protected RAbstractVector repCached(RAbstractVector value, RAbstractIntVector times,
                    @Cached("create()") VectorFactory factory,
                    @Cached("value.access()") VectorAccess valueAccess,
                    @Cached("times.access()") VectorAccess timesAccess,
                    @Cached("createNew(value.getRType())") VectorAccess resultAccess) {
        return performRep(value, times, factory, valueAccess, timesAccess, resultAccess);
    }

    @Specialization(replaces = "repCached")
    @TruffleBoundary
    protected RAbstractVector repGeneric(RAbstractVector value, RAbstractIntVector times,
                    @Cached("create()") VectorFactory factory) {
        return performRep(value, times, factory, value.slowPathAccess(), times.slowPathAccess(), VectorAccess.createSlowPathNew(value.getRType()));
    }
}
