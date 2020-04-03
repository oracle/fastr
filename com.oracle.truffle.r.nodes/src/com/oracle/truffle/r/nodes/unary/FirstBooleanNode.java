/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.unary.CastNode;

/**
 * Extracts the first boolean from the (single) operand. If {@link #invalidValueName} is provided,
 * this node behaves differently: it will always generate "invalid value" errors and not produce a
 * warning if more than one element is provided.
 */
@NodeInfo(cost = NodeCost.NONE)
public abstract class FirstBooleanNode extends CastNode {

    private final ConditionProfile lengthNotOneProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile warningProfile = BranchProfile.create();
    private final String invalidValueName;

    protected FirstBooleanNode(String invalidValueName) {
        this.invalidValueName = invalidValueName;
    }

    @Specialization
    protected boolean firstScalar(byte argument) {
        if (RRuntime.isNA(argument)) {
            CompilerDirectives.transferToInterpreter();
            throw error(invalidValueName == null ? Message.NA_UNEXP : Message.INVALID_VALUE, invalidValueName);
        }
        return RRuntime.fromLogical(argument);
    }

    private void checkLength(RAbstractVector argument) {
        if (lengthNotOneProfile.profile(argument.getLength() != 1)) {
            if (argument.getLength() == 0) {
                CompilerDirectives.transferToInterpreter();
                throw error(invalidValueName == null ? Message.LENGTH_ZERO : Message.INVALID_VALUE, invalidValueName);
            } else {
                warningProfile.enter();
                if (invalidValueName == null) {
                    RError.warning(this, Message.LENGTH_GT_1);
                }
            }
        }
    }

    @Specialization(replaces = "firstScalar")
    protected boolean firstVector(RAbstractLogicalVector argument) {
        checkLength(argument);
        return firstScalar(argument.getDataAt(0));
    }

    @Specialization
    protected boolean firstVector(RIntVector argument) {
        checkLength(argument);
        return firstScalar(RRuntime.int2logical(argument.getDataAt(0)));
    }

    @Specialization
    protected boolean firstVector(RDoubleVector argument) {
        checkLength(argument);
        return firstScalar(RRuntime.double2logical(argument.getDataAt(0)));
    }

    @Specialization
    protected boolean firstVector(RRawVector argument) {
        checkLength(argument);
        return firstScalar(RRuntime.raw2logical(argument.getRawDataAt(0)));
    }

    @Fallback
    protected boolean fallback(@SuppressWarnings("unused") Object argument) {
        CompilerDirectives.transferToInterpreter();
        throw error(invalidValueName == null ? Message.ARGUMENT_NOT_INTERPRETABLE_LOGICAL : Message.INVALID_VALUE, invalidValueName);
    }
}
