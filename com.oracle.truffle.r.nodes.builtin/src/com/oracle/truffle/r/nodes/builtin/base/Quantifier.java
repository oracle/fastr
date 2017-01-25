/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.typeName;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public abstract class Quantifier extends RBuiltinNode {
    protected static final int MAX_CACHED_LENGTH = 10;

    private final NACheck naCheck = NACheck.create();
    private final BranchProfile trueBranch = BranchProfile.create();
    private final BranchProfile falseBranch = BranchProfile.create();

    @Children private final CastNode[] argCastNodes = new CastNode[MAX_CACHED_LENGTH];

    private static final class ProfileCastNode extends CastNode {

        private final ValueProfile profile = ValueProfile.createClassProfile();
        @Child private CastNode next;

        ProfileCastNode(CastNode next) {
            this.next = next;
        }

        @Override
        public Object execute(Object value) {
            return profile.profile(next.execute(value));
        }
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RArgsValuesAndNames.EMPTY, RRuntime.LOGICAL_FALSE};
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("na.rm").asLogicalVector().findFirst(RRuntime.LOGICAL_NA).map(toBoolean());
    }

    private void createArgCast(int index) {
        CastBuilder argCastBuilder = new CastBuilder();
        argCastBuilder.arg(0).allowNull().shouldBe(integerValue().or(logicalValue()).or(instanceOf(RAbstractVector.class).and(size(0))), RError.Message.COERCING_ARGUMENT, typeName(),
                        "logical").asLogicalVector();
        argCastNodes[index] = insert(new ProfileCastNode(argCastBuilder.getCasts()[0]));
    }

    protected boolean emptyVectorResult() {
        throw RInternalError.shouldNotReachHere("should be overridden");
    }

    @Specialization
    protected byte op(@SuppressWarnings("unused") RNull vector, @SuppressWarnings("unused") boolean naRm) {
        return RRuntime.asLogical(emptyVectorResult());
    }

    @Specialization
    protected byte op(@SuppressWarnings("unused") RMissing vector, @SuppressWarnings("unused") boolean naRm) {
        return RRuntime.asLogical(emptyVectorResult());
    }

    @Specialization(limit = "1", guards = {"cachedLength == args.getLength()", "cachedLength < MAX_CACHED_LENGTH"})
    @ExplodeLoop
    protected byte opCachedLength(RArgsValuesAndNames args, boolean naRm,
                    @Cached("args.getLength()") int cachedLength) {
        Object[] arguments = args.getArguments();

        byte result = RRuntime.asLogical(emptyVectorResult());
        for (int i = 0; i < cachedLength; i++) {
            Object argValue = arguments[i];
            byte v = processArgument(argValue, i, naRm);
            if (v == RRuntime.asLogical(!emptyVectorResult())) {
                return RRuntime.asLogical(!emptyVectorResult());
            } else if (v == RRuntime.LOGICAL_NA) {
                result = RRuntime.LOGICAL_NA;
            }
        }
        return result;
    }

    @Specialization(contains = "opCachedLength")
    protected byte op(RArgsValuesAndNames args, boolean naRm) {
        boolean profiledNaRm = naRm;

        byte result = RRuntime.asLogical(emptyVectorResult());
        for (Object argValue : args.getArguments()) {
            byte v = processArgument(argValue, 0, profiledNaRm);
            if (v == RRuntime.asLogical(!emptyVectorResult())) {
                return RRuntime.asLogical(!emptyVectorResult());
            } else if (v == RRuntime.LOGICAL_NA) {
                result = RRuntime.LOGICAL_NA;
            }
        }
        return result;
    }

    private byte processArgument(Object argValue, int index, boolean naRm) {
        byte result = RRuntime.asLogical(emptyVectorResult());
        if (argValue != RNull.instance) {
            if (argCastNodes[index] == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                createArgCast(index);
            }
            Object castValue = argCastNodes[index].execute(argValue);
            if (castValue instanceof RAbstractLogicalVector) {
                RAbstractLogicalVector vector = (RAbstractLogicalVector) castValue;
                naCheck.enable(vector);
                for (int i = 0; i < vector.getLength(); i++) {
                    byte b = vector.getDataAt(i);
                    if (!naRm && naCheck.check(b)) {
                        result = RRuntime.LOGICAL_NA;
                    } else if (b == RRuntime.asLogical(!emptyVectorResult())) {
                        trueBranch.enter();
                        return RRuntime.asLogical(!emptyVectorResult());
                    }
                }
            } else {
                byte b = (byte) castValue;
                naCheck.enable(true);
                if (!naRm && naCheck.check(b)) {
                    result = RRuntime.LOGICAL_NA;
                } else if (b == RRuntime.asLogical(!emptyVectorResult())) {
                    trueBranch.enter();
                    return RRuntime.asLogical(!emptyVectorResult());
                }
            }
        }
        falseBranch.enter();
        return result;
    }
}
