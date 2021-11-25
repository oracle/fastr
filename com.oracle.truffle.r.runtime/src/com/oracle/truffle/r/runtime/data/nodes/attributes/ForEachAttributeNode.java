/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;

/**
 * Efficient iteration over the attributes without creating an array or iterator.
 */
public abstract class ForEachAttributeNode extends AttributeIterativeAccessNode {

    private final AttributeAction actionNode;

    protected ForEachAttributeNode(AttributeAction actionNode) {
        this.actionNode = actionNode;
    }

    public static ForEachAttributeNode create(AttributeAction action) {
        return ForEachAttributeNodeGen.create(action);
    }

    public abstract Object execute(RAttributable attributable, Object argument);

    /**
     * Return {@code false} to stop the attributes iteration prematurely. Context gives access to
     * the parameter passed to {@link #execute(RAttributable, Object)} and to the result, which
     * should be set by this function.
     */
    public abstract static class AttributeAction {
        public abstract boolean action(String name, Object value, Context context);
    }

    protected static boolean hasNullAttributes(RAttributable attributable) {
        return attributable.getAttributes() == null;
    }

    protected static DynamicObject getAttributes(RAttributable attributable) {
        return attributable.getAttributes();
    }

    @Specialization(guards = {"!hasNullAttributes(attributable)", "!isRPairList(attributable)",
                    "cachedLen <= EXPLODE_LOOP_LIMIT", "cachedLen == keys.length"}, limit = "1")
    @ExplodeLoop
    protected Object iterateExploded(@SuppressWarnings("unused") RAttributable attributable, Object argument,
                    @Bind("getAttributes(attributable)") DynamicObject attrs,
                    @CachedLibrary("attrs") DynamicObjectLibrary dylib,
                    @Bind("dylib.getKeyArray(attrs)") Object[] keys,
                    @Cached("keys.length") int cachedLen) {
        Context ctx = new Context(argument);
        for (int i = 0; i < cachedLen; i++) {
            Object value = dylib.getOrDefault(attrs, keys[i], null);
            if (!actionNode.action((String) keys[i], value, ctx)) {
                break;
            }
        }
        return ctx.result;
    }

    @Specialization(replaces = "iterateExploded", guards = {"!hasNullAttributes(attributable)", "!isRPairList(attributable)"}, limit = "getShapeCacheLimit()")
    protected Object iterateGeneric(@SuppressWarnings("unused") RAttributable attributable, Object argument,
                    @Cached LoopConditionProfile loopProfile,
                    @Bind("getAttributes(attributable)") DynamicObject attrs,
                    @CachedLibrary("attrs") DynamicObjectLibrary dylib,
                    @Bind("dylib.getKeyArray(attrs)") Object[] keys) {
        Context ctx = new Context(argument);
        return iterateGeneric(loopProfile, attrs, dylib, keys, ctx);
    }

    private Object iterateGeneric(LoopConditionProfile loopProfile, DynamicObject attrs, DynamicObjectLibrary dylib, Object[] keys, Context ctx) {
        loopProfile.profileCounted(keys.length);
        for (int i = 0; loopProfile.inject(i < keys.length); i++) {
            Object value = dylib.getOrDefault(attrs, keys[i], null);
            if (!actionNode.action((String) keys[i], value, ctx)) {
                break;
            }
        }
        return ctx.result;
    }

    /**
     * Pairlists need special iteration, because they do not have names attribute internally.
     */
    @Specialization(guards = {"isRPairList(pairList)"})
    protected Object iteratePairList(RPairList pairList, Object argument,
                    @Cached LoopConditionProfile loopProfile,
                    @CachedLibrary(limit = "getShapeCacheLimit()") DynamicObjectLibrary dylib,
                    @Cached ConditionProfile nullAttributesProfile,
                    @Cached BranchProfile pairListHasNamesBranch) {
        Context ctx = new Context(argument);
        if (pairList.getNames() != null) {
            pairListHasNamesBranch.enter();
            if (!actionNode.action(RRuntime.NAMES_ATTR_KEY, pairList.getNames(), ctx)) {
                return ctx.result;
            }
        }
        DynamicObject attributes = pairList.getAttributes();
        if (nullAttributesProfile.profile(attributes == null)) {
            return ctx.result != null ? ctx.result : RNull.instance;
        } else {
            return iterateGeneric(loopProfile, attributes, dylib, dylib.getKeyArray(attributes), ctx);
        }
    }

    @Specialization(guards = {"hasNullAttributes(attributable)", "!isRPairList(attributable)"})
    protected Object fallback(@SuppressWarnings("unused") RAttributable attributable, @SuppressWarnings("unused") Object attributeName) {
        return RNull.instance;
    }

    @ValueType
    public static final class Context {
        public final Object argument;
        public Object result;

        private Context(Object argument) {
            this.argument = argument;
            this.result = RNull.instance;
        }
    }
}
