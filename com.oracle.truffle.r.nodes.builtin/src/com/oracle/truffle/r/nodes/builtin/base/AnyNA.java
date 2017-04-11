/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.control.RLengthNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "anyNA", kind = PRIMITIVE, parameterNames = {"x", "recursive"}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class AnyNA extends RBuiltinNode.Arg2 {

    private final NACheck naCheck = NACheck.create();

    public abstract byte execute(VirtualFrame frame, Object value, boolean recursive);

    static {
        Casts casts = new Casts(AnyNA.class);
        casts.arg("recursive").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, RRuntime.LOGICAL_FALSE};
    }

    private static byte doScalar(boolean isNA) {
        return RRuntime.asLogical(isNA);
    }

    @FunctionalInterface
    private interface VectorIndexPredicate<T extends RAbstractVector> {
        boolean apply(T vector, int index);
    }

    private <T extends RAbstractVector> byte doVector(T vector, VectorIndexPredicate<T> predicate) {
        naCheck.enable(vector);
        for (int i = 0; i < vector.getLength(); i++) {
            if (predicate.apply(vector, i)) {
                return RRuntime.LOGICAL_TRUE;
            }
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isNA(byte value, @SuppressWarnings("unused") boolean recursive) {
        return doScalar(RRuntime.isNA(value));
    }

    @Specialization
    protected byte isNA(int value, @SuppressWarnings("unused") boolean recursive) {
        return doScalar(RRuntime.isNA(value));
    }

    @Specialization
    protected byte isNA(double value, @SuppressWarnings("unused") boolean recursive) {
        return doScalar(RRuntime.isNAorNaN(value));
    }

    @Specialization
    protected byte isNA(RComplex value, @SuppressWarnings("unused") boolean recursive) {
        return doScalar(RRuntime.isNA(value));
    }

    @Specialization
    protected byte isNA(String value, @SuppressWarnings("unused") boolean recursive) {
        return doScalar(RRuntime.isNA(value));
    }

    @Specialization
    @SuppressWarnings("unused")
    protected byte isNA(RRaw value, boolean recursive) {
        return doScalar(false);
    }

    @Specialization
    protected byte isNA(@SuppressWarnings("unused") RNull value, @SuppressWarnings("unused") boolean recursive) {
        return doScalar(false);
    }

    @Specialization
    protected byte isNA(RAbstractIntVector vector, @SuppressWarnings("unused") boolean recursive) {
        return doVector(vector, (v, i) -> naCheck.check(v.getDataAt(i)));
    }

    @Specialization
    protected byte isNA(RAbstractDoubleVector vector, @SuppressWarnings("unused") boolean recursive) {
        // since
        return doVector(vector, (v, i) -> naCheck.checkNAorNaN(v.getDataAt(i)));
    }

    @Specialization
    protected byte isNA(RAbstractComplexVector vector, @SuppressWarnings("unused") boolean recursive) {
        return doVector(vector, (v, i) -> naCheck.check(v.getDataAt(i)));
    }

    @Specialization
    protected byte isNA(RAbstractStringVector vector, @SuppressWarnings("unused") boolean recursive) {
        return doVector(vector, (v, i) -> naCheck.check(v.getDataAt(i)));
    }

    @Specialization
    protected byte isNA(RAbstractLogicalVector vector, @SuppressWarnings("unused") boolean recursive) {
        return doVector(vector, (v, i) -> naCheck.check(v.getDataAt(i)));
    }

    @Specialization
    protected byte isNA(@SuppressWarnings("unused") RAbstractRawVector vector, @SuppressWarnings("unused") boolean recursive) {
        return doScalar(false);
    }

    protected AnyNA createRecursive() {
        return AnyNANodeGen.create();
    }

    @Specialization(guards = "recursive")
    protected byte isNA(VirtualFrame frame, RList list, boolean recursive,
                    @Cached("createRecursive()") AnyNA recursiveNode,
                    @Cached("createClassProfile()") ValueProfile elementProfile,
                    @Cached("create()") RLengthNode length) {

        for (int i = 0; i < list.getLength(); i++) {
            Object value = elementProfile.profile(list.getDataAt(i));
            if (length.executeInteger(frame, value) > 0) {
                byte result = recursiveNode.execute(frame, value, recursive);
                if (result == RRuntime.LOGICAL_TRUE) {
                    return RRuntime.LOGICAL_TRUE;
                }
            }
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization(guards = "!recursive")
    @SuppressWarnings("unused")
    protected byte isNA(VirtualFrame frame, RList list, boolean recursive) {
        return RRuntime.LOGICAL_FALSE;
    }
}
