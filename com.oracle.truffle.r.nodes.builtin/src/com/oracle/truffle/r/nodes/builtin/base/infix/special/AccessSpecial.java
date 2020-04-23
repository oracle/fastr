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

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.nodes.helpers.SpecialsUtils.SubInterface;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.builtins.RSpecialFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RStringVector;

/**
 * Subscript code for vectors minus list is the same as subset code, this class allows sharing it.
 */
@ImportStatic(DSLConfig.class)
public abstract class AccessSpecial extends IndexingSpecialCommon implements SubInterface {

    protected AccessSpecial(boolean inReplacement) {
        super(inReplacement);
    }

    public abstract double executeDouble(RDoubleVector vec, int index);

    public abstract int executeInteger(RIntVector vec, int index);

    @Specialization(guards = {"simpleVector(vector)", "isValidIndexCached(dataLib, vector, index)"}, limit = "getGenericVectorAccessCacheSize()")
    protected int accessInt(RIntVector vector, int index,
                    @CachedLibrary("vector.getData()") VectorDataLibrary dataLib) {
        return dataLib.getIntAt(vector.getData(), index - 1);
    }

    @Specialization(guards = {"simpleVector(vector)", "isValidIndexCached(dataLib, vector, index)"}, limit = "getGenericVectorAccessCacheSize()")
    protected double accessDouble(RDoubleVector vector, int index,
                    @CachedLibrary("vector.getData()") VectorDataLibrary dataLib) {
        return dataLib.getDoubleAt(vector.getData(), index - 1);
    }

    @Specialization(guards = {"simpleVector(vector)", "isValidIndexCached(dataLib, vector, index)"}, limit = "getGenericVectorAccessCacheSize()")
    protected String accessString(RStringVector vector, int index,
                    @CachedLibrary("vector.getData()") VectorDataLibrary dataLib) {
        return dataLib.getStringAt(vector.getData(), index - 1);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static Object accessFallback(Object vector, Object index) {
        throw RSpecialFactory.throwFullCallNeeded();
    }
}
