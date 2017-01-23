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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.binary.BinaryMapBooleanFunctionNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.Order.CmpNode;
import com.oracle.truffle.r.nodes.builtin.base.OrderNodeGen.CmpNodeGen;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ops.BinaryCompare;

@RBuiltin(name = "is.unsorted", kind = INTERNAL, parameterNames = {"x", "strictly"}, behavior = PURE)
// TODO support strictly
// TODO support lists
public abstract class IsUnsorted extends RBuiltinNode {

    @Child private BinaryMapBooleanFunctionNode ge = new BinaryMapBooleanFunctionNode(BinaryCompare.GREATER_EQUAL.createOperation());
    @Child private BinaryMapBooleanFunctionNode gt = new BinaryMapBooleanFunctionNode(BinaryCompare.GREATER_THAN.createOperation());

    private final ConditionProfile strictlyProfile = ConditionProfile.createBinaryProfile();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("strictly").asLogicalVector().findFirst(RRuntime.LOGICAL_NA).notNA().map(toBoolean());
    }

    @Specialization
    protected byte isUnsorted(RAbstractDoubleVector x, boolean strictly) {
        double last = x.getDataAt(0);
        for (int k = 1; k < x.getLength(); k++) {
            double current = x.getDataAt(k);
            if (strictlyProfile.profile(strictly)) {
                if (ge.applyLogical(last, current) == RRuntime.LOGICAL_TRUE) {
                    return RRuntime.LOGICAL_TRUE;
                }
            } else {
                if (gt.applyLogical(last, current) == RRuntime.LOGICAL_TRUE) {
                    return RRuntime.LOGICAL_TRUE;
                }
            }
            last = current;
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isUnsorted(RAbstractIntVector x, boolean strictly) {
        int last = x.getDataAt(0);
        for (int k = 1; k < x.getLength(); k++) {
            int current = x.getDataAt(k);
            if (strictlyProfile.profile(strictly)) {
                if (ge.applyLogical(last, current) == RRuntime.LOGICAL_TRUE) {
                    return RRuntime.LOGICAL_TRUE;
                }
            } else {
                if (gt.applyLogical(last, current) == RRuntime.LOGICAL_TRUE) {
                    return RRuntime.LOGICAL_TRUE;
                }
            }
            last = current;
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isUnsorted(RAbstractStringVector x, boolean strictly) {
        String last = x.getDataAt(0);
        for (int k = 1; k < x.getLength(); k++) {
            String current = x.getDataAt(k);
            if (strictlyProfile.profile(strictly)) {
                if (ge.applyLogical(last, current) == RRuntime.LOGICAL_TRUE) {
                    return RRuntime.LOGICAL_TRUE;
                }
            } else {
                if (gt.applyLogical(last, current) == RRuntime.LOGICAL_TRUE) {
                    return RRuntime.LOGICAL_TRUE;
                }
            }
            last = current;
        }
        return RRuntime.LOGICAL_FALSE;
    }

    protected CmpNode createCmpNode() {
        return CmpNodeGen.create();
    }

    @Specialization
    protected byte isUnsorted(RAbstractRawVector x, boolean strictly) {
        RRaw last = x.getDataAt(0);
        for (int k = 1; k < x.getLength(); k++) {
            RRaw current = x.getDataAt(k);
            if (strictlyProfile.profile(strictly)) {
                if (ge.applyRaw(last.getValue(), current.getValue()) == RRuntime.LOGICAL_TRUE) {
                    return RRuntime.LOGICAL_TRUE;
                }
            } else {
                if (gt.applyRaw(last.getValue(), current.getValue()) == RRuntime.LOGICAL_TRUE) {
                    return RRuntime.LOGICAL_TRUE;
                }
            }
            last = current;
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isUnsorted(RAbstractComplexVector x, boolean strictly,
                    @Cached("createCmpNode()") CmpNode cmpNode) {
        int last = 0;
        for (int k = 1; k < x.getLength(); k++) {
            if (strictlyProfile.profile(strictly)) {
                if (cmpNode.ccmp(x, last, k, true) >= 0) {
                    return RRuntime.LOGICAL_TRUE;
                }
            } else {
                if (cmpNode.ccmp(x, last, k, true) > 0) {
                    return RRuntime.LOGICAL_TRUE;
                }
            }
            last = k;
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @Fallback
    @SuppressWarnings("unused")
    protected byte isUnsortedFallback(Object x, Object strictly) {
        return RRuntime.LOGICAL_NA;
    }
}
