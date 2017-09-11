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

    @Child private ReuseNonSharedNode reuseNonSharedNode;

    private final ConditionProfile trivialSizeProfile = ConditionProfile.createBinaryProfile();
    private final NACheck dummyCheck = NACheck.create(); // never triggered (used for vector update)

    static {
        Casts casts = new Casts(MakeUnique.class);
        casts.arg("names").defaultError(RError.Message.NOT_CHARACTER_VECTOR, "names").mustBe(stringValue());
        casts.arg("sep").defaultError(RError.Message.MUST_BE_STRING, "sep").mustBe(stringValue()).asStringVector().mustBe(size(1)).findFirst();

    }

    @Specialization
    protected RAbstractStringVector makeUnique(String names, @SuppressWarnings("unused") String sep) {
        // a single string cannot have duplicates
        return RDataFactory.createStringVectorFromScalar(names);
    }

    @Specialization
    protected RAbstractStringVector makeUnique(RStringVector names, String sep) {
        if (trivialSizeProfile.profile(names.getLength() == 0 || names.getLength() == 1)) {
            return names;
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
        // a string sequence cannot have duplicates if stride is not zero
        if (names.getStride() != 0) {
            return names;
        }
        throw RInternalError.unimplemented("make.unique for string sequence with zero stride is not implemented");
    }

    @TruffleBoundary
    protected RStringVector doLargeVector(RStringVector names, String sep) {
        HashMap<String, AtomicInteger> keys = new HashMap<>(names.getLength());
        boolean containsDuplicates = false;
        boolean containsClashes = true;
        for (int i = 0; i < names.getLength(); i++) {
            AtomicInteger value = new AtomicInteger(0);
            String element = names.getDataAt(i);
            AtomicInteger prev = keys.put(element, value);
            if (prev != null) {
                containsDuplicates = true;
                value.incrementAndGet();
                if (!containsClashes) {
                    int lastIndexOf = element.lastIndexOf(sep);
                    // If an element contains the separator string followed by a digit, we may
                    // encounter clashes.
                    containsClashes = lastIndexOf != -1 && lastIndexOf + 1 < element.length() && Character.isDigit(element.charAt(lastIndexOf + 1));
                }
            }
        }
        if (containsDuplicates) {
            for (int i = 0; i < names.getLength(); i++) {
                AtomicInteger atomicInteger = keys.get(names.getDataAt(i));
                int curCnt = atomicInteger.getAndIncrement() - 1;
                if (curCnt > 0) {
                    String updatedElement;
                    do {
                        updatedElement = names.getDataAt(i) + sep + curCnt;

                        // The generated string may already be in the vector.
                        if (containsClashes && keys.containsKey(updatedElement)) {
                            curCnt = atomicInteger.getAndIncrement() - 1;
                        } else {
                            break;
                        }
                    } while (true);
                    names.updateDataAt(i, updatedElement, dummyCheck);
                }
            }
        }
        return names;
    }
}
