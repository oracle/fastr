/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;

@NodeInfo(cost = NodeCost.NONE)
public abstract class FirstBooleanNode extends CastNode {

    private final ConditionProfile lengthNotOneProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile warningProfile = BranchProfile.create();

    @Specialization
    protected boolean firstScalar(byte argument) {
        if (RRuntime.isNA(argument)) {
            CompilerDirectives.transferToInterpreter();
            RError.error(this, RError.Message.NA_UNEXP);
        }
        return RRuntime.fromLogical(argument);
    }

    private void checkLength(RAbstractVector argument) {
        if (lengthNotOneProfile.profile(argument.getLength() != 1)) {
            if (argument.getLength() == 0) {
                CompilerDirectives.transferToInterpreter();
                throw RError.error(this, RError.Message.LENGTH_ZERO);
            } else {
                warningProfile.enter();
                RError.warning(this, RError.Message.LENGTH_GT_1);
            }
        }
    }

    @Specialization(contains = "firstScalar")
    protected boolean firstVector(RAbstractLogicalVector argument) {
        checkLength(argument);
        return firstScalar(argument.getDataAt(0));
    }

    @Specialization
    protected boolean firstVector(RAbstractIntVector argument) {
        checkLength(argument);
        return firstScalar(RRuntime.int2logical(argument.getDataAt(0)));
    }

    @Specialization
    protected boolean firstVector(RAbstractDoubleVector argument) {
        checkLength(argument);
        return firstScalar(RRuntime.double2logical(argument.getDataAt(0)));
    }

    @Specialization
    protected boolean firstVector(RAbstractRawVector argument) {
        checkLength(argument);
        return firstScalar(RRuntime.raw2logical(argument.getDataAt(0)));
    }

    @Fallback
    protected boolean fallback(@SuppressWarnings("unused") Object argument) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this, RError.Message.ARGUMENT_NOT_INTERPRETABLE_LOGICAL);
    }
}
