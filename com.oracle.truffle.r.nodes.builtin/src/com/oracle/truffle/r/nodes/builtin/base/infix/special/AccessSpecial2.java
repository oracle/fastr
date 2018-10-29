/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.infix.special;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.SpecialsUtils.Sub2Interface;
import com.oracle.truffle.r.runtime.builtins.RSpecialFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

/**
 * Subscript code for matrices minus list is the same as subset code, this class allows sharing it.
 */
public abstract class AccessSpecial2 extends IndexingSpecial2Common implements Sub2Interface {

    protected AccessSpecial2(boolean inReplacement) {
        super(inReplacement);
    }

    public abstract double executeDouble(RAbstractDoubleVector vec, int index1, int index2);

    public abstract int executeInteger(RAbstractIntVector vec, int index1, int index2);

    @Specialization(guards = {"access.supports(vector)", "simpleVector(vector)", "isValidIndex(vector, index1, index2)"})
    protected int accessInt(RAbstractIntVector vector, int index1, int index2,
                    @Cached("vector.access()") VectorAccess access) {
        try (VectorAccess.RandomIterator iter = access.randomAccess(vector)) {
            return access.getInt(iter, matrixIndex(vector, index1, index2));
        }
    }

    @Specialization(replaces = "accessInt", guards = {"simpleVector(vector)", "isValidIndex(vector, index1, index2)"})
    protected int accessIntGeneric(RAbstractIntVector vector, int index1, int index2) {
        return accessInt(vector, index1, index2, vector.slowPathAccess());
    }

    @Specialization(guards = {"access.supports(vector)", "simpleVector(vector)", "isValidIndex(vector, index1, index2)"})
    protected double accessDouble2(RAbstractDoubleVector vector, int index1, int index2,
                    @Cached("vector.access()") VectorAccess access) {
        try (VectorAccess.RandomIterator iter = access.randomAccess(vector)) {
            return access.getDouble(iter, matrixIndex(vector, index1, index2));
        }
    }

    @Specialization(replaces = "accessDouble2", guards = {"simpleVector(vector)", "isValidIndex(vector, index1, index2)"})
    protected double accessDoubleGeneric(RAbstractDoubleVector vector, int index1, int index2) {
        return accessDouble2(vector, index1, index2, vector.slowPathAccess());
    }

    @Specialization(guards = {"access.supports(vector)", "simpleVector(vector)", "isValidIndex(vector, index1, index2)"})
    protected String accessString(RAbstractStringVector vector, int index1, int index2,
                    @Cached("vector.access()") VectorAccess access) {
        try (VectorAccess.RandomIterator iter = access.randomAccess(vector)) {
            return access.getString(iter, matrixIndex(vector, index1, index2));
        }
    }

    @Specialization(replaces = "accessString", guards = {"simpleVector(vector)", "isValidIndex(vector, index1, index2)"})
    protected String accessStringGeneric(RAbstractStringVector vector, int index1, int index2) {
        return accessString(vector, index1, index2, vector.slowPathAccess());
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static Object access(Object vector, Object index1, Object index2) {
        throw RSpecialFactory.throwFullCallNeeded();
    }
}
