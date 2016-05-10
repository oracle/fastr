/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.control.RLengthNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RDispatch;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RList;
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

@RBuiltin(name = "anyNA", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"}, dispatch = RDispatch.INTERNAL_GENERIC)
public abstract class AnyNA extends RBuiltinNode {

    private final NACheck naCheck = NACheck.create();

    public abstract byte execute(VirtualFrame frame, Object value);

    private byte doScalar(boolean isNA) {
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
    protected byte isNA(byte value) {
        return doScalar(RRuntime.isNA(value));
    }

    @Specialization
    protected byte isNA(int value) {
        return doScalar(RRuntime.isNA(value));
    }

    @Specialization
    protected byte isNA(double value) {
        return doScalar(RRuntime.isNAorNaN(value));
    }

    @Specialization
    protected byte isNA(RComplex value) {
        return doScalar(RRuntime.isNA(value));
    }

    @Specialization
    protected byte isNA(String value) {
        return doScalar(RRuntime.isNA(value));
    }

    @Specialization
    protected byte isNA(@SuppressWarnings("unused") RRaw value) {
        return doScalar(false);
    }

    @Specialization
    protected byte isNA(@SuppressWarnings("unused") RNull value) {
        return doScalar(false);
    }

    @Specialization
    protected byte isNA(RAbstractIntVector vector) {
        return doVector(vector, (v, i) -> naCheck.check(v.getDataAt(i)));
    }

    @Specialization
    protected byte isNA(RAbstractDoubleVector vector) {
        // since
        return doVector(vector, (v, i) -> naCheck.checkNAorNaN(v.getDataAt(i)));
    }

    @Specialization
    protected byte isNA(RAbstractComplexVector vector) {
        return doVector(vector, (v, i) -> naCheck.check(v.getDataAt(i)));
    }

    @Specialization
    protected byte isNA(RAbstractStringVector vector) {
        return doVector(vector, (v, i) -> naCheck.check(v.getDataAt(i)));
    }

    @Specialization
    protected byte isNA(RAbstractLogicalVector vector) {
        return doVector(vector, (v, i) -> naCheck.check(v.getDataAt(i)));
    }

    @Specialization
    protected byte isNA(@SuppressWarnings("unused") RAbstractRawVector vector) {
        return doScalar(false);
    }

    protected AnyNA createRecursive() {
        return AnyNANodeGen.create(null);
    }

    @Specialization
    protected byte isNA(VirtualFrame frame, RList list, //
                    @Cached("createRecursive()") AnyNA recursive, //
                    @Cached("createClassProfile()") ValueProfile elementProfile, //
                    @Cached("create()") RLengthNode length) {
        for (int i = 0; i < list.getLength(); i++) {
            Object value = elementProfile.profile(list.getDataAt(i));
            if (length.executeInteger(frame, value) == 1) {
                byte result = recursive.execute(frame, value);
                if (result == RRuntime.LOGICAL_TRUE) {
                    return RRuntime.LOGICAL_TRUE;
                }
            }
        }
        return RRuntime.LOGICAL_FALSE;
    }
}
