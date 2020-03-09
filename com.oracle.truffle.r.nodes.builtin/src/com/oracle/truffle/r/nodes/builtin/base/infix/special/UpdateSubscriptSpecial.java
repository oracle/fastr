/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.ProfiledSpecialsUtilsFactory.ProfiledUpdateSubscriptSpecialBaseNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.SpecialsUtils.ConvertIndex;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.SpecialsUtils.ConvertValue;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RSpecialFactory;
import com.oracle.truffle.r.runtime.data.AbstractContainerLibrary;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RDoubleVectorDataLibrary;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RIntVectorDataLibrary;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.nodes.RNode;

@ImportStatic(DSLConfig.class)
public abstract class UpdateSubscriptSpecial extends IndexingSpecialCommon {

    protected UpdateSubscriptSpecial(boolean inReplacement) {
        super(inReplacement);
    }

    protected abstract Object execute(VirtualFrame frame, Object vec, Object index, Object value);

    @Specialization(guards = {"access.supports(vector)", "simpleVector(vector)", "!vector.isShared()", "isValidIndex(vector, index)",
                    "intLibrary.isWriteable(vector.getData())"}, limit = "getGenericVectorAccessCacheSize()")
    protected RIntVector setInt(RIntVector vector, int index, int value,
                    @Cached("vector.access()") VectorAccess access,
                    @SuppressWarnings("unused") @CachedLibrary("vector.getData()") RIntVectorDataLibrary intLibrary,
                    @CachedLibrary("vector") AbstractContainerLibrary library) {
        return setIntInternal(access, library, vector, index, value);
    }

    @Specialization(replaces = "setInt", guards = {"simpleVector(vector)", "!vector.isShared()", "isValidIndex(vector, index)",
                    "getIntUncachedLibrary().isWriteable(vector.getData())"})
    protected RIntVector setIntGeneric(RIntVector vector, int index, int value) {
        return setIntInternal(vector.slowPathAccess(), AbstractContainerLibrary.getFactory().getUncached(), vector, index, value);
    }

    private static RIntVector setIntInternal(VectorAccess access, AbstractContainerLibrary library, RIntVector vector, int index, int value) {
        try (VectorAccess.RandomIterator iter = access.randomAccess(library, vector)) {
            access.setInt(iter, index - 1, value);
            if (RRuntime.isNA(value)) {
                vector.setComplete(false);
            }
            return vector;
        }
    }

    @Specialization(guards = {"access.supports(vector)", "simpleVector(vector)", "!vector.isShared()", "isValidIndex(vector, index)",
                    "doubleLibrary.isWriteable(vector.getData())"}, limit = "getGenericVectorAccessCacheSize()")
    protected RDoubleVector setDouble(RDoubleVector vector, int index, double value,
                    @Cached("vector.access()") VectorAccess access,
                    @SuppressWarnings("unused") @CachedLibrary("vector.getData()") RDoubleVectorDataLibrary doubleLibrary,
                    @CachedLibrary("vector") AbstractContainerLibrary library) {
        return setDoubleInternal(access, library, vector, index, value);
    }

    @Specialization(replaces = "setDouble", guards = {"simpleVector(vector)", "!vector.isShared()", "isValidIndex(vector, index)", "getDoubleUncachedLibrary().isWriteable(vector.getData())"})
    protected RDoubleVector setDoubleGeneric(RDoubleVector vector, int index, double value) {
        return setDoubleInternal(vector.slowPathAccess(), AbstractContainerLibrary.getFactory().getUncached(), vector, index, value);
    }

    private static RDoubleVector setDoubleInternal(VectorAccess access, AbstractContainerLibrary library, RDoubleVector vector, int index, double value) {
        try (VectorAccess.RandomIterator iter = access.randomAccess(library, vector)) {
            access.setDouble(iter, index - 1, value);
            if (RRuntime.isNA(value)) {
                vector.setComplete(false);
            }
            return vector;
        }
    }

    @Specialization(guards = {"access.supports(vector)", "simpleVector(vector)", "!vector.isShared()", "isValidIndex(vector, index)", "vector.isMaterialized()"})
    protected RStringVector setString(RStringVector vector, int index, String value,
                    @Cached("vector.access()") VectorAccess access) {
        try (VectorAccess.RandomIterator iter = access.randomAccess(vector)) {
            access.setString(iter, index - 1, value);
            if (RRuntime.isNA(value)) {
                vector.setComplete(false);
            }
            return vector;
        }
    }

    @Specialization(replaces = "setString", guards = {"simpleVector(vector)", "!vector.isShared()", "isValidIndex(vector, index)", "vector.isMaterialized()"})
    protected RStringVector setStringGeneric(RStringVector vector, int index, String value) {
        return setString(vector, index, value, vector.slowPathAccess());
    }

    @Specialization(guards = {"access.supports(list)", "simpleVector(list)", "!list.isShared()", "isValidIndex(list, index)", "isSingleElement(value)"})
    protected static Object setList(RList list, int index, Object value,
                    @Cached("list.access()") VectorAccess access) {
        try (VectorAccess.RandomIterator iter = access.randomAccess(list)) {
            access.setListElement(iter, index - 1, value);
            return list;
        }
    }

    @Specialization(replaces = "setList", guards = {"simpleVector(list)", "!list.isShared()", "isValidIndex(list, index)", "isSingleElement(value)"})
    protected static Object setListGeneric(RList list, int index, Object value) {
        return setList(list, index, value, list.slowPathAccess());
    }

    @Specialization(guards = {"access.supports(vector)", "simpleVector(vector)", "!vector.isShared()", "isValidIndex(vector, index)", "vector.isMaterialized()"})
    protected RDoubleVector setDoubleIntIndexIntValue(RDoubleVector vector, int index, int value,
                    @Cached("vector.access()") VectorAccess access) {
        try (VectorAccess.RandomIterator iter = access.randomAccess(vector)) {
            if (RRuntime.isNA(value)) {
                access.setDouble(iter, index - 1, RRuntime.DOUBLE_NA);
                vector.setComplete(false);
            } else {
                access.setDouble(iter, index - 1, value);
            }
            return vector;
        }
    }

    @Specialization(replaces = "setDoubleIntIndexIntValue", guards = {"simpleVector(vector)", "!vector.isShared()", "isValidIndex(vector, index)", "vector.isMaterialized()"})
    protected RDoubleVector setDoubleIntIndexIntValueGeneric(RDoubleVector vector, int index, int value) {
        return setDoubleIntIndexIntValue(vector, index, value, vector.slowPathAccess());
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static Object setFallback(Object vector, Object index, Object value) {
        throw RSpecialFactory.throwFullCallNeeded(value);
    }

    public static RNode create(boolean inReplacement, RNode vector, ConvertIndex index, ConvertValue value) {
        return ProfiledUpdateSubscriptSpecialBaseNodeGen.create(inReplacement, vector, index, value);
    }

    protected RIntVectorDataLibrary getIntUncachedLibrary() {
        return RIntVectorDataLibrary.getFactory().getUncached();
    }

    protected RDoubleVectorDataLibrary getDoubleUncachedLibrary() {
        return RDoubleVectorDataLibrary.getFactory().getUncached();
    }
}
