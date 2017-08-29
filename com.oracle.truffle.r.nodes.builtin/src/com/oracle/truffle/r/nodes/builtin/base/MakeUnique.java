/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.opt.ReuseNonSharedNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringSequence;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "make.unique", kind = INTERNAL, parameterNames = {"names", "sep"}, behavior = PURE)
public abstract class MakeUnique extends RBuiltinNode.Arg2 {

    private static final int SIMPLE_ALGORITHM_THRESHOLD = 1000;

    @Child private ReuseNonSharedNode reuseNonSharedNode;

    private final ConditionProfile trivialSizeProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile largeVectorProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile duplicatesProfile = ConditionProfile.createBinaryProfile();
    private final NACheck dummyCheck = NACheck.create(); // never triggered (used for vector update)

    static {
        Casts casts = new Casts(MakeUnique.class);
        casts.arg("names").defaultError(RError.Message.NOT_CHARACTER_VECTOR, "names").mustBe(stringValue());
        casts.arg("sep").defaultError(RError.Message.MUST_BE_STRING, "sep").mustBe(stringValue()).asStringVector().mustBe(size(1)).findFirst();

    }

    @Specialization
    protected RAbstractStringVector makeUnique(String names, @SuppressWarnings("unused") String sep) {
        return RDataFactory.createStringVectorFromScalar(names);
    }

    @Specialization
    protected RAbstractStringVector makeUnique(RStringVector names, String sep) {
        if (trivialSizeProfile.profile(names.getLength() == 0 || names.getLength() == 1)) {
            return names;
        } else if (largeVectorProfile.profile(names.getLength() <= SIMPLE_ALGORITHM_THRESHOLD)) {
            return doGeneric(names, sep);
        }
        if (reuseNonSharedNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            reuseNonSharedNode = insert(ReuseNonSharedNode.create());
        }

        RStringVector reused = (RStringVector) reuseNonSharedNode.execute(names);
        return doLargeVector(reused, sep);
    }

    @Specialization
    protected RAbstractStringVector makeUnique(RStringSequence names, @SuppressWarnings("unused") String sep) {
        if (names.getStride() != 0) {
            return names;
        }
        throw RInternalError.unimplemented("make.unique for string sequence with zero stride is not implemented");
    }

    /**
     * Uses a O(n^2) algorithm for checking if there are duplicates. So, do not use for large
     * vectors.
     */
    protected RAbstractStringVector doGeneric(RAbstractStringVector names, String sep) {
        // TODO: perhaps for longer vectors there is a faster algorithm using hash maps, but
        // then it would probably have to be put on the slow path even for cases when no
        // duplicates actually exist
        int[] duplicates = new int[names.getLength()];
        boolean duplicatesExist = false;
        for (int i = 0; i < duplicates.length; i++) {
            if (duplicates[i] > 0) {
                // already processed
                continue;
            }
            String current = names.getDataAt(i);
            int duplicatesCount = 0;
            for (int j = i + 1; j < duplicates.length; j++) {
                if (current.equals(names.getDataAt(j))) {
                    duplicatesExist = true;
                    duplicates[j] = ++duplicatesCount;
                }
            }
        }
        if (duplicatesProfile.profile(!duplicatesExist)) {
            return names;
        } else {
            if (reuseNonSharedNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                reuseNonSharedNode = insert(ReuseNonSharedNode.create());
            }
            RStringVector newNames = (RStringVector) reuseNonSharedNode.execute(names);
            if (newNames.isShared()) {
                newNames = (RStringVector) newNames.copy();
            }
            // start with 1 as the first one is never the duplicate
            for (int i = 1; i < duplicates.length; i++) {
                if (duplicates[i] > 0) {
                    newNames.updateDataAt(i, concat(newNames.getDataAt(i), sep, duplicates[i]), dummyCheck);
                }
            }
            return newNames;
        }
    }

    @TruffleBoundary
    protected RStringVector doLargeVector(RStringVector names, String sep) {
        HashMap<String, AtomicInteger> keys = new HashMap<>(names.getLength());
        boolean containsDuplicates = false;
        for (int i = 0; i < names.getLength(); i++) {
            if (keys.put(names.getDataAt(i), new AtomicInteger(0)) != null) {
                containsDuplicates = true;
            }
        }
        if (containsDuplicates) {
            for (int i = 1; i < names.getLength(); i++) {
                AtomicInteger cnt = keys.get(names.getDataAt(i));
                if (cnt.get() > 0) {
                    int curCnt = cnt.getAndIncrement();
                    names.updateDataAt(i, concat(names.getDataAt(i), sep, curCnt), dummyCheck);
                }
            }
        }
        return names;
    }

    @TruffleBoundary
    private static String concat(String s1, String sep, int index) {
        return s1 + sep + index;
    }
}
