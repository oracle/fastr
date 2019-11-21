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
import com.oracle.truffle.r.nodes.builtin.base.infix.special.SpecialsUtils.SubInterface;
import com.oracle.truffle.r.runtime.builtins.RSpecialFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

/**
 * Subscript code for vectors minus list is the same as subset code, this class allows sharing it.
 */
public abstract class AccessSpecial extends IndexingSpecialCommon implements SubInterface {

    protected AccessSpecial(boolean inReplacement) {
        super(inReplacement);
    }

    public abstract double executeDouble(RAbstractDoubleVector vec, int index);

    public abstract int executeInteger(RIntVector vec, int index);

    @Specialization(guards = {"access.supports(vector)", "simpleVector(vector)", "isValidIndex(vector, index)"})
    protected int accessInt(RIntVector vector, int index,
                            @Cached("vector.access()") VectorAccess access) {
        try (VectorAccess.RandomIterator iter = access.randomAccess(vector)) {
            return access.getInt(iter, index - 1);
        }
    }

    @Specialization(replaces = "accessInt", guards = {"simpleVector(vector)", "isValidIndex(vector, index)"})
    protected int accessIntGeneric(RIntVector vector, int index) {
        return accessInt(vector, index, vector.slowPathAccess());
    }

    @Specialization(guards = {"access.supports(vector)", "simpleVector(vector)", "isValidIndex(vector, index)"})
    protected double accessDouble(RAbstractDoubleVector vector, int index,
                    @Cached("vector.access()") VectorAccess access) {
        try (VectorAccess.RandomIterator iter = access.randomAccess(vector)) {
            return access.getDouble(iter, index - 1);
        }
    }

    @Specialization(replaces = "accessDouble", guards = {"simpleVector(vector)", "isValidIndex(vector, index)"})
    protected double accessDoubleGeneric(RAbstractDoubleVector vector, int index) {
        return accessDouble(vector, index, vector.slowPathAccess());
    }

    @Specialization(guards = {"access.supports(vector)", "simpleVector(vector)", "isValidIndex(vector, index)"})
    protected String accessString(RAbstractStringVector vector, int index,
                    @Cached("vector.access()") VectorAccess access) {
        try (VectorAccess.RandomIterator iter = access.randomAccess(vector)) {
            return access.getString(iter, index - 1);
        }
    }

    @Specialization(replaces = "accessString", guards = {"simpleVector(vector)", "isValidIndex(vector, index)"})
    protected String accessStringGeneric(RAbstractStringVector vector, int index) {
        return accessString(vector, index, vector.slowPathAccess());
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static Object access(Object vector, Object index) {
        throw RSpecialFactory.throwFullCallNeeded();
    }
}
