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
package com.oracle.truffle.r.runtime.data.nodes.attributes;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RRuntimeASTAccess.ArrayAttributeAccess;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RAttributesLayout.RAttribute;

@GenerateUncached
public abstract class ArrayAttributeNode extends AttributeIterativeAccessNode implements ArrayAttributeAccess {

    private static final RAttribute[] EMPTY = new RAttribute[0];

    public static ArrayAttributeNode create() {
        return ArrayAttributeNodeGen.create();
    }

    @Override
    public abstract RAttribute[] execute(Object attrs);

    @ExplodeLoop
    @Specialization(guards = {"cachedLen <= EXPLODE_LOOP_LIMIT", "cachedLen == keyArray.length"}, limit = "getCacheSize(1)")
    protected RAttribute[] getArrayExplode(DynamicObject attrs,
                                           @CachedLibrary("attrs") DynamicObjectLibrary dylib,
                                           @Bind("dylib.getKeyArray(attrs)") Object[] keyArray,
                                           @Cached("keyArray.length") int cachedLen) {
        RAttribute[] result = new RAttribute[cachedLen];
        for (int i = 0; i < result.length; i++) {
            Object value = dylib.getOrDefault(attrs, keyArray[i], null);
            result[i] = new RAttributesLayout.RAttribute((String) keyArray[i], value);
        }
        return result;
    }

    @Specialization(replaces = "getArrayExplode", limit = "getShapeCacheLimit()")
    protected RAttribute[] getArrayGeneric(DynamicObject attrs,
                                           @Cached LoopConditionProfile loopProfile,
                                           @CachedLibrary("attrs") DynamicObjectLibrary dylib,
                                           @Bind("dylib.getKeyArray(attrs)") Object[] keyArray) {
        RAttribute[] result = new RAttribute[keyArray.length];
        loopProfile.profileCounted(result.length);
        for (int i = 0; loopProfile.inject(i < result.length); i++) {
            Object value = dylib.getOrDefault(attrs, keyArray[i], null);
            result[i] = new RAttributesLayout.RAttribute((String) keyArray[i], value);
        }
        return result;
    }

    @Specialization(guards = "hasAttributes(x)")
    protected RAttribute[] getArrayFallback(RAttributable x,
                    @Cached() ArrayAttributeNode recursive) {
        return recursive.execute(x.getAttributes());
    }

    @Specialization(guards = "!hasAttributes(x)")
    protected RAttribute[] getArrayFallback(@SuppressWarnings("unused") RAttributable x) {
        return EMPTY;
    }

    protected static boolean hasAttributes(RAttributable x) {
        return x.getAttributes() != null;
    }
}
