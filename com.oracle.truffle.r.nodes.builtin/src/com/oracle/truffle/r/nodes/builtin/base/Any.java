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

import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastLogicalNode;
import com.oracle.truffle.r.nodes.unary.CastLogicalNodeGen;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RDispatch;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "any", kind = PRIMITIVE, parameterNames = {"...", "na.rm"}, dispatch = RDispatch.SUMMARY_GROUP_GENERIC)
public abstract class Any extends RBuiltinNode {

    protected static final int MAX_CACHED_LENGTH = 10;

    private final NACheck naCheck = NACheck.create();
    private final ConditionProfile naRmProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile trueBranch = BranchProfile.create();
    private final BranchProfile falseBranch = BranchProfile.create();

    @Children private final CastLogicalNode[] castLogicalNode = new CastLogicalNode[MAX_CACHED_LENGTH];

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RArgsValuesAndNames.EMPTY, RRuntime.LOGICAL_FALSE};
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toLogical(1);
    }

    @Specialization(limit = "1", guards = {"cachedLength == args.getLength()", "cachedLength < MAX_CACHED_LENGTH"})
    @ExplodeLoop
    protected byte anyCachedLength(RArgsValuesAndNames args, byte naRm, //
                    @Cached("args.getLength()") int cachedLength) {
        boolean profiledNaRm = naRmProfile.profile(naRm != RRuntime.LOGICAL_FALSE);
        Object[] arguments = args.getArguments();

        byte result = RRuntime.LOGICAL_FALSE;
        for (int i = 0; i < cachedLength; i++) {
            Object argValue = arguments[i];
            byte v = processArgument(argValue, i, profiledNaRm);
            if (v == RRuntime.LOGICAL_TRUE) {
                return RRuntime.LOGICAL_TRUE;
            } else if (v == RRuntime.LOGICAL_NA) {
                result = RRuntime.LOGICAL_NA;
            }
        }
        return result;
    }

    @Specialization(contains = "anyCachedLength")
    protected byte any(RArgsValuesAndNames args, byte naRm) {
        boolean profiledNaRm = naRmProfile.profile(naRm != RRuntime.LOGICAL_FALSE);

        byte result = RRuntime.LOGICAL_FALSE;
        for (Object argValue : args.getArguments()) {
            byte v = processArgument(argValue, 0, profiledNaRm);
            if (v == RRuntime.LOGICAL_TRUE) {
                return RRuntime.LOGICAL_TRUE;
            } else if (v == RRuntime.LOGICAL_NA) {
                result = RRuntime.LOGICAL_NA;
            }
        }
        return result;
    }

    private byte processArgument(Object argValue, int index, boolean profiledNaRm) {
        byte result = RRuntime.LOGICAL_FALSE;
        if (argValue != RNull.instance) {
            if (castLogicalNode[index] == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castLogicalNode[index] = insert(CastLogicalNodeGen.create(true, false, false));
            }
            Object castValue = castLogicalNode[index].execute(argValue);
            if (castValue instanceof RAbstractLogicalVector) {
                RAbstractLogicalVector vector = (RAbstractLogicalVector) castValue;
                naCheck.enable(vector);
                for (int i = 0; i < vector.getLength(); i++) {
                    byte b = vector.getDataAt(i);
                    if (!profiledNaRm && naCheck.check(b)) {
                        result = RRuntime.LOGICAL_NA;
                    } else if (b == RRuntime.LOGICAL_TRUE) {
                        trueBranch.enter();
                        return RRuntime.LOGICAL_TRUE;
                    }
                }
            } else {
                byte b = (byte) castValue;
                naCheck.enable(true);
                if (!profiledNaRm && naCheck.check(b)) {
                    result = RRuntime.LOGICAL_NA;
                } else if (b == RRuntime.LOGICAL_TRUE) {
                    trueBranch.enter();
                    return RRuntime.LOGICAL_TRUE;
                }
            }
        }
        falseBranch.enter();
        return result;
    }
}
