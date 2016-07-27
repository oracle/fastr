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
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.objects.AsS4;
import com.oracle.truffle.r.nodes.objects.AsS4NodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSequence;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;

@RBuiltin(name = "setS4Object", kind = INTERNAL, parameterNames = {"object", "flag", "complete"})
public abstract class SetS4Object extends RBuiltinNode {

    @Child private AsS4 asS4 = AsS4NodeGen.create();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toAttributable(0, true, true, true);
        casts.toLogical(1);
        casts.toInteger(2);
    }

    private boolean checkArgs(RAbstractLogicalVector flagVec, RAbstractIntVector completeVec) {
        if (flagVec.getLength() == 0 || (flagVec.getLength() == 1 && flagVec.getDataAt(0) == RRuntime.LOGICAL_NA) || flagVec.getLength() > 1) {
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "flag");
        }
        if (completeVec.getLength() == 0 || flagVec.getDataAt(0) == RRuntime.LOGICAL_NA) {
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "complete");
        }
        return RRuntime.fromLogical(flagVec.getDataAt(0));
    }

    @Specialization
    protected RNull asS4(RNull object, RAbstractLogicalVector flagVec, RAbstractIntVector completeVec) {
        boolean flag = checkArgs(flagVec, completeVec);
        if (flag) {
            object.setS4();
        } else {
            boolean wasS4 = object.isS4();
            object.unsetS4();
            if (wasS4) {
                throw RError.error(this, RError.Message.GENERIC, "object of class \"NULL\" does not correspond to a valid S3 object");
            }
        }
        return object;
    }

    @Specialization(guards = "!isSequence(object)")
    protected Object asS4(RAttributable object, RAbstractLogicalVector flagVec, RAbstractIntVector completeVec) {
        boolean flag = checkArgs(flagVec, completeVec);
        return asS4.executeObject(object, flag, completeVec.getDataAt(0));
    }

    @Specialization
    protected Object asS4(RSequence seq, RAbstractLogicalVector flagVec, RAbstractIntVector completeVec) {
        return asS4(seq.materialize(), flagVec, completeVec);
    }

    protected boolean isSequence(Object o) {
        return o instanceof RSequence;
    }
}
