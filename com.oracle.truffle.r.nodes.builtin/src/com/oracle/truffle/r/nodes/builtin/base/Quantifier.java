/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.typeName;
import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.QuantifierNodeGen.ProcessArgumentNodeGen;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public abstract class Quantifier extends RBuiltinNode.Arg2 {
    protected static final int MAX_CACHED_LENGTH = 10;

    @Children private final CastNode[] argCastNodes = new CastNode[Math.max(1, DSLConfig.getCacheSize(MAX_CACHED_LENGTH))];
    @Children private final ProcessArgumentNode[] processArgumentNodes = new ProcessArgumentNode[Math.max(1, DSLConfig.getCacheSize(MAX_CACHED_LENGTH))];
    private final BranchProfile nullBranch = BranchProfile.create();

    private static final class ProfileCastNode extends CastNode {

        private final ValueProfile profile = ValueProfile.createClassProfile();
        @Child private CastNode next;

        ProfileCastNode() {
            this.next = newCastBuilder().allowNull().shouldBe(integerValue().or(logicalValue()).or(instanceOf(RAbstractVector.class).and(size(0))),
                            RError.Message.COERCING_ARGUMENT, typeName(), "logical").asLogicalVector().buildCastNode();
        }

        @Override
        public Object execute(Object value) {
            return profile.profile(next.doCast(value));
        }
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RArgsValuesAndNames.EMPTY, RRuntime.LOGICAL_FALSE};
    }

    protected static Casts createCasts(Class<? extends Quantifier> extCls) {
        Casts casts = new Casts(extCls);
        casts.arg("na.rm").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
        return casts;
    }

    private void createArgCast(int index) {
        argCastNodes[index] = insert(new ProfileCastNode());
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

    @Specialization(limit = "getCacheSize(1)", guards = {"cachedLength == args.getLength()", "cachedLength < getCacheSize(10)"})
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

    @Specialization(replaces = "opCachedLength")
    protected byte op(RArgsValuesAndNames args, boolean naRm,
                    @Cached("createBinaryProfile()") ConditionProfile naRmProfile) {
        boolean profiledNaRm = naRmProfile.profile(naRm);

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
        if (argValue != RNull.instance) {
            if (argCastNodes[index] == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                createArgCast(index);
            }
            Object castValue = argCastNodes[index].doCast(argValue);
            if (processArgumentNodes[index] == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                processArgumentNodes[index] = insert(ProcessArgumentNodeGen.create(this));
            }
            return processArgumentNodes[index].execute(castValue, naRm);
        }
        nullBranch.enter();
        return RRuntime.asLogical(emptyVectorResult());
    }

    abstract static class ProcessArgumentNode extends RBaseNode {
        private final Quantifier parent;

        protected ProcessArgumentNode(Quantifier parent) {
            this.parent = parent;
        }

        public abstract byte execute(Object vector, boolean naRm);

        @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
        byte doVector(RLogicalVector vector, boolean naRm,
                        @CachedLibrary("vector.getData()") VectorDataLibrary argDataLib) {
            Object data = vector.getData();
            SeqIterator it = argDataLib.iterator(data);
            byte result = RRuntime.asLogical(parent.emptyVectorResult());
            while (argDataLib.next(data, it)) {
                byte b = argDataLib.getNextLogical(data, it);
                if (!naRm && argDataLib.getNACheck(data).check(b)) {
                    result = RRuntime.LOGICAL_NA;
                } else if (b == RRuntime.asLogical(!parent.emptyVectorResult())) {
                    return RRuntime.asLogical(!parent.emptyVectorResult());
                }
            }
            return result;
        }

        @Specialization
        byte doSingleByte(byte value, boolean naRm,
                        @Cached BranchProfile isNAProfile,
                        @Cached BranchProfile trueBranchProfile) {
            if (!naRm && RRuntime.isNA(value)) {
                isNAProfile.enter();
                return RRuntime.LOGICAL_NA;
            } else if (value == RRuntime.asLogical(!parent.emptyVectorResult())) {
                return RRuntime.asLogical(!parent.emptyVectorResult());
            } else {
                trueBranchProfile.enter();
                return RRuntime.asLogical(parent.emptyVectorResult());
            }
        }
    }
}
