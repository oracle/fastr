/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.nodes.helpers;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.AttributeAccess;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;

/**
 * Contains helper nodes related to factors, special R class of {@link RAbstractIntVector}. Note:
 * there is also {@link InheritsCheckNode}, which can be used to check if something is a factor.
 */
public final class RFactorNodes {

    private RFactorNodes() {
    }

    /**
     * Encapsulates the operation of deciding whether a factor is ordered.
     */
    public static final class GetOrdered extends Node {
        @Child private AttributeAccess isOrderedAccess = AttributeAccess.create(RRuntime.ORDERED_ATTR_KEY);

        public boolean execute(RAbstractIntVector factor) {
            Object value = isOrderedAccess.execute(factor.getAttributes());
            if (value instanceof RAbstractLogicalVector) {
                RAbstractLogicalVector vec = (RAbstractLogicalVector) value;
                return vec.getLength() > 0 && RRuntime.fromLogical(vec.getDataAt(0));
            }

            return false;
        }
    }

    /**
     * Encapsulates the operation of getting the 'levels' of a factor as a string vector.
     */
    public static final class GetLevels extends Node {
        @Child private CastStringNode castString;
        @Child private AttributeAccess attrAccess = AttributeAccess.create(RRuntime.LEVELS_ATTR_KEY);

        private final BranchProfile notVectorBranch = BranchProfile.create();
        private final ConditionProfile nonScalarLevels = ConditionProfile.createBinaryProfile();
        private final ConditionProfile stringVectorLevels = ConditionProfile.createBinaryProfile();

        /**
         * Returns the levels as a string vector. If the 'levels' attribute is not a string vector a
         * cast is done. May return null, if the 'levels' attribute is not present.
         */
        public RStringVector execute(RAbstractIntVector factor) {
            Object attr = attrAccess.execute(factor.getAttributes());

            // Convert scalars to vector if necessary
            RVector vec;
            if (nonScalarLevels.profile(attr instanceof RVector)) {
                vec = (RVector) attr;
            } else if (attr != null) {
                vec = (RVector) RRuntime.asAbstractVector(attr);   // scalar to vector
            } else {
                notVectorBranch.enter();
                // N.B: when a factor is lacking the 'levels' attribute, GNU R uses range 1:14331272
                // as levels, but probably only in 'split'. Following example prints a huge list:
                // { f <- factor(1:5); attr(f, 'levels') <- NULL; split(1:2, f) }
                return null;
            }

            // Convert to string vector if necessary
            if (stringVectorLevels.profile(vec instanceof RStringVector)) {
                return (RStringVector) vec;
            } else {
                if (castString == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    castString = insert(CastStringNodeGen.create(false, false, false, false));
                }
                RStringVector slevels = (RStringVector) castString.executeString(vec);
                return RDataFactory.createStringVector(slevels.getDataWithoutCopying(), RDataFactory.COMPLETE_VECTOR);
            }
        }
    }
}
