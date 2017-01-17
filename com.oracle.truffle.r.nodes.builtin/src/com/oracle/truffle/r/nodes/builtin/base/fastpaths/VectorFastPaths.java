/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.fastpaths;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.nodes.RFastPathNode;

public abstract class VectorFastPaths {

    public abstract static class IntegerFastPath extends RFastPathNode {

        @Specialization
        protected RAbstractIntVector get(@SuppressWarnings("unused") RMissing length) {
            return RDataFactory.createEmptyIntVector();
        }

        @Specialization
        protected RAbstractIntVector get(int length,
                        @Cached("createBinaryProfile()") ConditionProfile emptyProfile) {
            if (emptyProfile.profile(length == 0)) {
                return RDataFactory.createIntVector(0);
            } else if (length > 0) {
                return RDataFactory.createIntSequence(0, 0, length);
            }
            return null;
        }

        @Specialization
        protected RAbstractIntVector get(double length,
                        @Cached("createBinaryProfile()") ConditionProfile emptyProfile) {
            if (!Double.isNaN(length)) {
                return get((int) length, emptyProfile);
            }
            return null;
        }

        @Fallback
        @SuppressWarnings("unused")
        protected Object fallback(Object length) {
            return null;
        }
    }

    public abstract static class DoubleFastPath extends RFastPathNode {

        @Specialization
        protected RAbstractDoubleVector get(@SuppressWarnings("unused") RMissing length) {
            return RDataFactory.createEmptyDoubleVector();
        }

        @Specialization
        protected RAbstractDoubleVector get(int length,
                        @Cached("createBinaryProfile()") ConditionProfile emptyProfile) {
            if (emptyProfile.profile(length == 0)) {
                return RDataFactory.createDoubleVector(0);
            } else if (length > 0) {
                return RDataFactory.createDoubleSequence(0, 0, length);
            }
            return null;
        }

        @Specialization
        protected RAbstractDoubleVector get(double length,
                        @Cached("createBinaryProfile()") ConditionProfile emptyProfile) {
            if (!Double.isNaN(length)) {
                return get((int) length, emptyProfile);
            }
            return null;
        }

        @Fallback
        @SuppressWarnings("unused")
        protected Object fallback(Object length) {
            return null;
        }
    }
}
