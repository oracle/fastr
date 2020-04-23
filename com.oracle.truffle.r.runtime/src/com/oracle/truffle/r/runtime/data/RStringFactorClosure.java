/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.closures.RClosure;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * This wraps a factor: integer vector with attribute "levels" that gives a name to each distinct
 * number in the vector. When wrapped with this class, the values are the "names" given by the
 * "levels" attribute.
 */
@ExportLibrary(VectorDataLibrary.class)
class RStringFactorClosure implements RClosure {
    protected final RAbstractVector vector;
    protected final Object vectorData;
    protected final RStringVector levels;
    protected final Object levelsData;

    RStringFactorClosure(RAbstractVector vector, RStringVector levels) {
        this.vector = vector;
        this.levels = levels;
        this.vectorData = vector.getData();
        this.levelsData = levels.getData();
    }

    @Override
    public Object getDelegateDataAt(int idx) {
        return vector.getDataAtAsObject(idx);
    }

    @Override
    public RAbstractVector getDelegate() {
        return vector;
    }

    // VectorDataLibrary:

    @ExportMessage
    public int getLength(@CachedLibrary("this.vectorData") VectorDataLibrary vectorDataLib) {
        return vectorDataLib.getLength(vectorData);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public NACheck getNACheck() {
        return NACheck.getEnabled();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final RType getType() {
        return RType.Character;
    }

    @ExportMessage
    public RStringArrayVectorData materialize(@CachedLibrary("this.vectorData") VectorDataLibrary vectorDataLib,
                    @CachedLibrary("this.levelsData") VectorDataLibrary levelsDataLib) {
        return new RStringArrayVectorData(getStringDataCopy(vectorDataLib, levelsDataLib), true);
    }

    @ExportMessage
    public RStringFactorClosure copy(@SuppressWarnings("unused") boolean deep) {
        return new RStringFactorClosure(vector, levels);
    }

    @ExportMessage
    public boolean isComplete() {
        return false;
    }

    @ExportMessage
    public String[] getStringDataCopy(@CachedLibrary("this.vectorData") VectorDataLibrary vectorDataLib,
                    @CachedLibrary("this.levelsData") VectorDataLibrary levelsDataLib) {
        String[] result = new String[getLength(vectorDataLib)];
        for (int i = 0; i < result.length; i++) {
            result[i] = getStringImpl(i, vectorDataLib, levelsDataLib);
        }
        return result;
    }

    // Read access to the elements:

    @ExportMessage
    public SeqIterator iterator(@Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile,
                    @CachedLibrary("this.vectorData") VectorDataLibrary vectorDataLib) {
        SeqIterator it = new SeqIterator(null, getLength(vectorDataLib));
        it.initLoopConditionProfile(loopProfile);
        return it;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public boolean nextImpl(SeqIterator it, boolean loopCondition,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        return it.next(loopCondition, loopProfile);
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public void nextWithWrap(SeqIterator it,
                    @Cached("createBinaryProfile()") ConditionProfile wrapProfile) {
        it.nextWithWrap(wrapProfile);
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator() {
        return new RandomAccessIterator(null);
    }

    @ExportMessage
    public String getStringAt(int index,
                    @CachedLibrary("this.vectorData") VectorDataLibrary vectorDataLib,
                    @CachedLibrary("this.levelsData") VectorDataLibrary levelsDataLib) {
        return getStringImpl(index, vectorDataLib, levelsDataLib);
    }

    @ExportMessage
    public String getNextString(SeqIterator it,
                    @CachedLibrary("this.vectorData") VectorDataLibrary vectorDataLib,
                    @CachedLibrary("this.levelsData") VectorDataLibrary levelsDataLib) {
        return getStringImpl(it.getIndex(), vectorDataLib, levelsDataLib);
    }

    @ExportMessage
    public String getString(@SuppressWarnings("unused") RandomAccessIterator it, int index,
                    @CachedLibrary("this.vectorData") VectorDataLibrary vectorDataLib,
                    @CachedLibrary("this.levelsData") VectorDataLibrary levelsDataLib) {
        return getStringImpl(index, vectorDataLib, levelsDataLib);
    }

    // Utils:

    private String getStringImpl(int index, VectorDataLibrary vectorDataLib, VectorDataLibrary levelsDataLib) {
        int val = vectorDataLib.getIntAt(vectorData, index);
        if (!vectorDataLib.isComplete(vectorData) && RRuntime.isNA(val)) {
            return RRuntime.STRING_NA;
        } else {
            String l = levelsDataLib.getStringAt(levelsData, val - 1);
            if (!levelsDataLib.isComplete(levelsData) && RRuntime.isNA(l)) {
                return RRuntime.STRING_NA;
            } else {
                return l;
            }
        }
    }
}
