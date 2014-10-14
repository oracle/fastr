/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public class IsListFunctions {
    /**
     * {@link IsList} does not override {@link IsTypeNode} because it needs to specialize on
     * {@link RAbstractVector}.
     */
    @RBuiltin(name = "is.list", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsList extends IsTypeNodeMissingAdapter {

        private final ConditionProfile dataFrameIsListProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isAbsVectorListProfile = ConditionProfile.createBinaryProfile();

        @Specialization
        protected byte isType(@SuppressWarnings("unused") RList value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

        @Specialization
        protected byte isType(RDataFrame value) {
            controlVisibility();
            if (dataFrameIsListProfile.profile(isList(value))) {
                return RRuntime.LOGICAL_TRUE;
            } else {
                return RRuntime.LOGICAL_FALSE;
            }
        }

        @Specialization
        protected byte isType(@SuppressWarnings("unused") RPairList pl) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }

        protected boolean isList(RAbstractVector vector) {
            return vector.getElementClass() == Object.class;
        }

        protected boolean isList(RDataFrame frame) {
            return isList(frame.getVector());
        }

        /**
         * All the subclasses of {@link RAbstractVector} are lists iff the class of the vector
         * element is {@code Object}.
         */
        @Specialization
        protected byte isType(RAbstractVector value) {
            controlVisibility();
            if (isAbsVectorListProfile.profile(isList(value))) {
                return RRuntime.LOGICAL_TRUE;
            } else {
                return RRuntime.LOGICAL_FALSE;
            }
        }

        @Fallback
        protected byte isType(@SuppressWarnings("unused") Object value) {
            return RRuntime.LOGICAL_FALSE;
        }

    }

    @RBuiltin(name = "is.pairlist", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsPairList extends IsTypeNode {
        @Specialization
        @Override
        protected byte isType(RPairList value) {
            controlVisibility();
            return RRuntime.LOGICAL_TRUE;
        }
    }

}
