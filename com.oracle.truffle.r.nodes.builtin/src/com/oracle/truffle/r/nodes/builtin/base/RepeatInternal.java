/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.typeName;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory.VectorFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "rep.int", kind = INTERNAL, parameterNames = {"x", "times"}, behavior = PURE)
public abstract class RepeatInternal extends RBuiltinNode.Arg2 {

    private final ConditionProfile timesOneProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile valueLen0Profile = ConditionProfile.createBinaryProfile();

    static {
        Casts casts = new Casts(RepeatInternal.class);
        casts.arg("x").mustBe(abstractVectorValue(), RError.Message.ATTEMPT_TO_REPLICATE, typeName());
        casts.arg("times").defaultError(RError.Message.INVALID_TYPE, typeName(), "times", "vector").mustBe(abstractVectorValue()).asIntegerVector();
    }

    private RAbstractVector performRep(RAbstractVector value, VectorDataLibrary valueDataLib, RIntVector times, VectorDataLibrary timesDataLib,
                    VectorFactory factory, VectorDataLibrary resultDataLib) {
        Object valueData = value.getData();
        Object timesData = times.getData();
        SeqIterator valueIter = valueDataLib.iterator(valueData);
        SeqIterator timesIter = timesDataLib.iterator(timesData);
        int valueLen = valueDataLib.getLength(valueData);
        int timesLen = timesDataLib.getLength(timesData);
        RType valueType = valueDataLib.getType(valueData);
        if (valueLen0Profile.profile(valueLen == 0)) {
            return factory.createVector(valueType, 0, false);
        }
        if (timesLen == 0) {
            throw error(RError.Message.INVALID_VALUE, "times");
        }
        RAbstractVector result;
        if (timesOneProfile.profile(timesLen == 1)) {
            timesDataLib.next(timesData, timesIter);
            int timesValue = timesDataLib.getNextInt(timesData, timesIter);
            if (timesValue < 0) {
                throw error(RError.Message.INVALID_VALUE, "times");
            }
            int resultLen = timesValue * valueLen;
            result = factory.createVector(valueType, resultLen, false);
            Object resultData = result.getData();
            try (SeqWriteIterator resultIter = resultDataLib.writeIterator(resultData)) {
                while (resultDataLib.nextLoopCondition(resultData, resultIter)) {
                    valueDataLib.nextWithWrap(valueData, valueIter);
                    resultDataLib.transferNext(resultData, resultIter, valueDataLib, valueIter, valueData);
                }
                boolean neverSeenNA = valueDataLib.getNACheck(valueData).neverSeenNA();
                resultDataLib.commitWriteIterator(resultData, resultIter, neverSeenNA);
            }
        } else if (timesLen == valueLen) {
            int count = 0;
            while (timesDataLib.nextLoopCondition(timesData, timesIter)) {
                int num = timesDataLib.getNextInt(timesData, timesIter);
                if (num < 0) {
                    throw error(RError.Message.INVALID_VALUE, "times");
                }
                count += num;
            }
            result = factory.createVector(valueType, count, false);
            Object resultData = result.getData();

            timesIter.reset();
            try (SeqWriteIterator resultIter = resultDataLib.writeIterator(resultData)) {
                while (timesDataLib.nextLoopCondition(timesData, timesIter) && valueDataLib.nextLoopCondition(valueData, valueIter)) {
                    int num = timesDataLib.getNextInt(timesData, timesIter);
                    for (int i = 0; i < num; i++) {
                        resultDataLib.next(resultData, resultIter);
                        resultDataLib.transferNext(resultData, resultIter, valueDataLib, valueIter, valueData);
                    }
                }
                boolean neverSeenNA = valueDataLib.getNACheck(valueData).neverSeenNA();
                resultDataLib.commitWriteIterator(resultData, resultIter, neverSeenNA);
            }
        } else {
            throw error(RError.Message.INVALID_VALUE, "times");
        }
        return result;
    }

    @Specialization(limit = "getGenericDataLibraryCacheSize()")
    protected RAbstractVector repCached(RAbstractVector value, RIntVector times,
                    @Cached("create()") VectorFactory factory,
                    @CachedLibrary("value.getData()") VectorDataLibrary valueDataLib,
                    @CachedLibrary("times.getData()") VectorDataLibrary timesDataLib,
                    @CachedLibrary(limit = "getGenericDataLibraryCacheSize()") VectorDataLibrary resultDataLib) {
        return performRep(value, valueDataLib, times, timesDataLib, factory, resultDataLib);
    }

    @Specialization(replaces = "repCached")
    @TruffleBoundary
    protected RAbstractVector repGeneric(RAbstractVector value, RIntVector times,
                    @Cached("create()") VectorFactory factory) {
        VectorDataLibrary uncachedDataLib = VectorDataLibrary.getFactory().getUncached();
        return performRep(value, uncachedDataLib, times, uncachedDataLib, factory, uncachedDataLib);
    }
}
