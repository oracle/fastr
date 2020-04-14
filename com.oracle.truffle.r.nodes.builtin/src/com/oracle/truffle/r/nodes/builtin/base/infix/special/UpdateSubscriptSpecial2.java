/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.ProfiledSpecialsUtilsFactory.ProfiledUpdateSubscriptSpecial2NodeGen;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.SpecialsUtils.ConvertIndex;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.SpecialsUtils.ConvertValue;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RSpecialFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.nodes.RNode;

public abstract class UpdateSubscriptSpecial2 extends IndexingSpecial2Common {

    protected UpdateSubscriptSpecial2(boolean inReplacement) {
        super(inReplacement);
    }

    protected abstract Object execute(VirtualFrame frame, Object vec, Object index1, Object index2, Object value);

    @Specialization(guards = {"simpleVector(vector)", "!vector.isShared()", "isValidIndex(vector, index1, index2)", "dataLib.isWriteable(vector.getData())"}, limit = "getVectorAccessCacheSize()")
    protected RIntVector setInt(RIntVector vector, int index1, int index2, int value,
                    @CachedLibrary("vector.getData()") VectorDataLibrary dataLib) {
        Object vectorData = vector.getData();
        try (VectorDataLibrary.RandomAccessWriteIterator it = dataLib.randomAccessWriteIterator(vectorData)) {
            dataLib.setInt(vectorData, it, matrixIndex(vector, index1, index2), value);
            dataLib.commitRandomAccessWriteIterator(vectorData, it, !RRuntime.isNA(value));
        }
        return vector;
    }

    @Specialization(guards = {"simpleVector(vector)", "!vector.isShared()", "isValidIndex(vector, index1, index2)", "dataLib.isWriteable(vector.getData())"}, limit = "getVectorAccessCacheSize()")
    protected RDoubleVector setDouble(RDoubleVector vector, int index1, int index2, double value,
                    @CachedLibrary("vector.getData()") VectorDataLibrary dataLib) {
        Object vectorData = vector.getData();
        try (VectorDataLibrary.RandomAccessWriteIterator it = dataLib.randomAccessWriteIterator(vectorData)) {
            dataLib.setDouble(vectorData, it, matrixIndex(vector, index1, index2), value);
            dataLib.commitRandomAccessWriteIterator(vectorData, it, !RRuntime.isNA(value));
        }
        return vector;
    }

    @Specialization(guards = {"simpleVector(vector)", "!vector.isShared()", "isValidIndex(vector, index1, index2)", "dataLib.isWriteable(vector.getData())"}, limit = "getVectorAccessCacheSize()")
    protected RStringVector setString(RStringVector vector, int index1, int index2, String value,
                    @CachedLibrary("vector.getData()") VectorDataLibrary dataLib) {
        Object vectorData = vector.getData();
        try (VectorDataLibrary.RandomAccessWriteIterator it = dataLib.randomAccessWriteIterator(vectorData)) {
            dataLib.setString(vectorData, it, matrixIndex(vector, index1, index2), value);
            dataLib.commitRandomAccessWriteIterator(vectorData, it, !RRuntime.isNA(value));
        }
        return vector;
    }

    @Specialization(guards = {"simpleVector(list)", "!list.isShared()", "isValidIndex(list, index1, index2)", "dataLib.isWriteable(list.getData())"}, limit = "getVectorAccessCacheSize()")
    protected Object setList(RList list, int index1, int index2, Object value,
                    @CachedLibrary("list.getData()") VectorDataLibrary dataLib) {
        Object vectorData = list.getData();
        try (VectorDataLibrary.RandomAccessWriteIterator it = dataLib.randomAccessWriteIterator(vectorData)) {
            dataLib.setElement(vectorData, it, matrixIndex(list, index1, index2), value);
            dataLib.commitRandomAccessWriteIterator(vectorData, it, false);
        }
        return list;
    }

    @Specialization(guards = {"simpleVector(vector)", "!vector.isShared()", "isValidIndex(vector, index1, index2)", "dataLib.isWriteable(vector.getData())"}, limit = "getVectorAccessCacheSize()")
    protected RDoubleVector setDoubleIntIndexIntValue(RDoubleVector vector, int index1, int index2, int value,
                    @Cached("createBinaryProfile()") ConditionProfile naProfile,
                    @CachedLibrary("vector.getData()") VectorDataLibrary dataLib) {
        Object vectorData = vector.getData();
        try (VectorDataLibrary.RandomAccessWriteIterator it = dataLib.randomAccessWriteIterator(vectorData)) {
            if (naProfile.profile(RRuntime.isNA(value))) {
                dataLib.setDouble(vectorData, it, matrixIndex(vector, index1, index2), RRuntime.DOUBLE_NA);
                dataLib.commitRandomAccessWriteIterator(vectorData, it, false);
            } else {
                dataLib.setDouble(vectorData, it, matrixIndex(vector, index1, index2), value);
                dataLib.commitRandomAccessWriteIterator(vectorData, it, true);
            }
        }
        return vector;
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static Object setFallback(Object vector, Object index1, Object index2, Object value) {
        throw RSpecialFactory.throwFullCallNeeded(value);
    }

    public static RNode create(boolean inReplacement, RNode vector, ConvertIndex index1, ConvertIndex index2, ConvertValue value) {
        return ProfiledUpdateSubscriptSpecial2NodeGen.create(inReplacement, vector, index1, index2, value);
    }
}
