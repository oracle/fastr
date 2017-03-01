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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte0;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lte;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.IntValueProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetRowNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

@RBuiltin(name = "shortRowNames", kind = INTERNAL, parameterNames = {"x", "type"}, behavior = PURE)
public abstract class ShortRowNames extends RBuiltinNode {

    private final BranchProfile naValueMet = BranchProfile.create();
    private final ValueProfile operandTypeProfile = ValueProfile.createClassProfile();

    @Child private GetRowNamesAttributeNode getRowNamesNode = GetRowNamesAttributeNode.create();

    static {
        Casts casts = new Casts(ShortRowNames.class);
        casts.arg("type").asIntegerVector().findFirst().mustBe(gte0().and(lte(2)));
    }

    private final IntValueProfile typeProfile = IntValueProfile.createIdentityProfile();

    @Specialization
    protected Object getNames(Object originalOperand, int originalType) {
        Object operand = operandTypeProfile.profile(originalOperand);
        Object rowNames;
        if (operand instanceof RAbstractContainer) {
            rowNames = getRowNamesNode.getRowNames((RAbstractContainer) operand);
        } else if (operand instanceof REnvironment) {
            rowNames = getRowNamesNode.execute(operand);
        } else {
            // for any other type GnuR returns 0
            return 0;
        }

        int type = typeProfile.profile(originalType);
        if (type >= 1) {
            int n = calculateN(rowNames);
            rowNames = type == 1 ? n : Math.abs(n);
        }

        if (rowNames == null) {
            return RNull.instance;
        }

        return rowNames;
    }

    private int calculateN(Object rowNames) {
        if (rowNames == null || rowNames instanceof RNull) {
            return 0;
        } else if (rowNames instanceof RAbstractIntVector) {
            RAbstractIntVector intVector = ((RAbstractIntVector) rowNames);
            if (intVector.getLength() == 2) {
                if (RRuntime.isNA(intVector.getDataAt(0))) {
                    naValueMet.enter();
                    return intVector.getDataAt(1);
                }
            }
            return intVector.getLength();
        } else if (rowNames instanceof RAbstractContainer) {
            return ((RAbstractContainer) rowNames).getLength();
        } else {
            throw error(RError.Message.INVALID_ARGUMENT, "type");
        }
    }
}
