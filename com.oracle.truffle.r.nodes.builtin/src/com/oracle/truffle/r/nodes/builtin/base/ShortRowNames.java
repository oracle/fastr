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

import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.IntValueProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

@RBuiltin(name = "shortRowNames", kind = INTERNAL, parameterNames = {"x", "type"})
public abstract class ShortRowNames extends RBuiltinNode {

    private final BranchProfile naValueMet = BranchProfile.create();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();
    private final BranchProfile errorProfile = BranchProfile.create();
    private final ValueProfile operandTypeProfile = ValueProfile.createClassProfile();

    public abstract Object executeObject(VirtualFrame frame, Object operand, Object type);

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toInteger(1);
    }

    private final IntValueProfile typeProfile = IntValueProfile.createIdentityProfile();

    @Specialization
    protected Object getNames(Object originalOperand, RAbstractIntVector originalType) {

        if (originalType.getLength() == 0) {
            errorProfile.enter();
            throw typeError();
        }
        int type = typeProfile.profile(originalType.getDataAt(0));

        if (type < 0 || type > 2) {
            errorProfile.enter();
            throw typeError();
        }

        Object operand = operandTypeProfile.profile(originalOperand);
        Object rowNames;
        if (operand instanceof RAbstractContainer) {
            rowNames = ((RAbstractContainer) operand).getRowNames(attrProfiles);
        } else if (operand instanceof REnvironment) {
            rowNames = ((REnvironment) operand).getAttr(attrProfiles, RRuntime.ROWNAMES_ATTR_KEY);
        } else if (operand instanceof RNull) {
            return 0;
        } else {
            errorProfile.enter();
            throw typeError();
        }

        if (type >= 1) {
            int n = calculateN(rowNames);
            rowNames = type == 1 ? n : Math.abs(n);
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
            errorProfile.enter();
            throw typeError();
        }
    }

    private RError typeError() {
        return RError.error(this, RError.Message.INVALID_ARGUMENT, "type");
    }
}
