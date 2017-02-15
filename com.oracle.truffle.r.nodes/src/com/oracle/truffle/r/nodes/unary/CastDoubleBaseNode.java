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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;
import com.oracle.truffle.r.runtime.ops.na.NAProfile;

public abstract class CastDoubleBaseNode extends CastBaseNode {

    protected final NACheck naCheck = NACheck.create();
    protected final NAProfile naProfile = NAProfile.create();
    protected final BranchProfile warningBranch = BranchProfile.create();

    protected CastDoubleBaseNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, RBaseNode messageCallObj) {
        super(preserveNames, preserveDimensions, preserveAttributes, messageCallObj);
    }

    protected CastDoubleBaseNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        this(preserveNames, preserveDimensions, preserveAttributes, false);
    }

    protected CastDoubleBaseNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI) {
        super(preserveNames, preserveDimensions, preserveAttributes, forRFFI);
    }

    @Override
    protected final RType getTargetType() {
        return RType.Double;
    }

    public abstract Object executeDouble(int o);

    public abstract Object executeDouble(double o);

    public abstract Object executeDouble(byte o);

    public abstract Object executeDouble(Object o);

    @Specialization
    protected RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    @Specialization
    protected RMissing doMissing(RMissing missing) {
        return missing;
    }

    @Specialization
    protected double doInt(int operand) {
        naCheck.enable(operand);
        return naCheck.convertIntToDouble(operand);
    }

    @Specialization
    protected double doDouble(double operand) {
        return operand;
    }

    @Specialization
    protected double doDouble(RComplex operand) {
        naCheck.enable(operand);
        double result = naCheck.convertComplexToDouble(operand, false);
        if (operand.getImaginaryPart() != 0.0) {
            warningBranch.enter();
            RError.warning(messageCallObj, RError.Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        }
        return result;
    }

    @Specialization
    protected double doLogical(byte operand) {
        naCheck.enable(operand);
        return naCheck.convertLogicalToDouble(operand);
    }

    @Specialization
    protected double doString(String operand,
                    @Cached("createBinaryProfile()") ConditionProfile emptyStringProfile) {
        if (naProfile.isNA(operand) || emptyStringProfile.profile(operand.isEmpty())) {
            return RRuntime.DOUBLE_NA;
        }
        double result = RRuntime.string2doubleNoCheck(operand);
        if (RRuntime.isNA(result)) {
            warningBranch.enter();
            RError.warning(messageCallObj, RError.Message.NA_INTRODUCED_COERCION);
        }
        return result;
    }

    @Specialization
    protected double doRaw(RRaw operand) {
        return RRuntime.raw2double(operand);
    }
}
