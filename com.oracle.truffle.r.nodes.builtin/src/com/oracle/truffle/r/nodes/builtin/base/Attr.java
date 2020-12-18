/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.InternStringNode;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.nodes.UpdateShareableChildValueNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.ForEachAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.ForEachAttributeNode.AttributeAction;
import com.oracle.truffle.r.runtime.data.nodes.attributes.ForEachAttributeNode.Context;
import com.oracle.truffle.r.runtime.data.nodes.attributes.GetAttributeNode;

@RBuiltin(name = "attr", kind = PRIMITIVE, parameterNames = {"x", "which", "exact"}, behavior = PURE)
public abstract class Attr extends RBuiltinNode.Arg3 {

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, RMissing.instance, RRuntime.asLogical(false)};
    }

    static {
        Casts casts = new Casts(Attr.class);
        // Note: checking RAttributable.class does not work for scalars
        // casts.arg("x").mustBe(RAttributable.class, Message.UNIMPLEMENTED_ARGUMENT_TYPE);
        casts.arg("x").boxPrimitive();
        casts.arg("which").mustBe(stringValue(), Message.MUST_BE_CHARACTER, "which").asStringVector().mustBe(singleElement(), RError.Message.EXACTLY_ONE_WHICH).findFirst();
        casts.arg("exact").asLogicalVector().findFirst().map(toBoolean());
    }

    protected static boolean isForeign(Object object) {
        return RRuntime.isForeignObject(object);
    }

    protected static boolean isNamesAttribute(String attributeName) {
        return RRuntime.NAMES_ATTR_KEY.startsWith(attributeName);
    }

    private static Object searchKeyPartial(RAttributable attributable, String attributeName, PartialSearchCache partialSearchCache) {
        DynamicObject attributes = attributable.getAttributes();
        if (attributes == null) {
            return RNull.instance;
        }
        return partialSearchCache.execute(attributes, attributeName);
    }

    private static Object getAttrFromAttributable(RAttributable attributable, String attributeName, boolean exact,
                    GetAttributeNode getAttributeNode,
                    UpdateShareableChildValueNode updateShareableValueNode,
                    PartialSearchCache partialSearchCache,
                    InternStringNode internStringNode,
                    ConditionProfile partialSearchProfile) {
        String internedAttrName = internStringNode.execute(attributeName);
        Object result = getAttributeNode.execute(attributable, internedAttrName);
        if (partialSearchProfile.profile(result == null && !exact)) {
            return searchKeyPartial(attributable, internStringNode.execute(attributeName), partialSearchCache);
        } else {
            return result == null ? RNull.instance : updateShareableValueNode.updateState(attributable, result);
        }
    }

    @Specialization
    protected RNull attrForNull(@SuppressWarnings("unused") RNull rNull, @SuppressWarnings("unused") String name,
                    @SuppressWarnings("unused") boolean exact) {
        return RNull.instance;
    }

    /**
     * Pairlist needs special handling because it does not have "names" attribute internally.
     */
    @Specialization(guards = {"isRPairList(pairList)"})
    protected Object attrForPairList(RPairList pairList, String attributeName, boolean exact,
                    @Cached PartialSearchCache partialSearchCache,
                    @Cached InternStringNode internStringNode,
                    @Cached GetAttributeNode getAttributeNode,
                    @Cached UpdateShareableChildValueNode updateShareableValueNode,
                    @Cached ConditionProfile partialSearchProfile) {
        if (isNamesAttribute(attributeName)) {
            return pairList.getNames();
        } else {
            return getAttrFromAttributable(pairList, attributeName, exact, getAttributeNode, updateShareableValueNode,
                            partialSearchCache, internStringNode, partialSearchProfile);
        }
    }

    @Specialization(guards = {"!isRPairList(attributable)"})
    protected Object attrForAttributable(RAttributable attributable, String attributeName,
                    boolean exact,
                    @Cached GetAttributeNode getAttributeNode,
                    @Cached UpdateShareableChildValueNode updateShareableValueNode,
                    @Cached PartialSearchCache partialSearchCache,
                    @Cached InternStringNode internStringNode,
                    @Cached ConditionProfile partialSearchProfile) {
        return getAttrFromAttributable(attributable, attributeName, exact, getAttributeNode, updateShareableValueNode, partialSearchCache, internStringNode, partialSearchProfile);
    }

    @Fallback
    @TruffleBoundary
    protected Object fallback(Object object, @SuppressWarnings("unused") Object attributeName,
                    @SuppressWarnings("unused") Object exact) {
        if (RRuntime.isForeignObject(object)) {
            throw RError.error(this, Message.OBJ_CANNOT_BE_ATTRIBUTED);
        } else {
            throw RInternalError.unimplemented("object cannot be attributed");
        }
    }

    @ImportStatic(DSLConfig.class)
    protected abstract static class PartialSearchCache extends Node {
        @Child protected ForEachAttributeNode iterAttrAccess = ForEachAttributeNode.create(new PartialAttrSearchAction());

        public abstract Object execute(DynamicObject attributes, String name);

        @Specialization(guards = {"attrs.getShape() == cachedShape", "name.equals(cachedName)"}, limit = "getCacheSize(8)")
        protected Object doCached(@SuppressWarnings("unused") DynamicObject attrs, @SuppressWarnings("unused") String name,
                        @SuppressWarnings("unused") @Cached("attrs.getShape()") Shape cachedShape,
                        @SuppressWarnings("unused") @Cached("name") String cachedName,
                        @Cached("iterAttrAccess.execute(attrs,name)") Object result) {
            return result;
        }

        @Specialization(replaces = "doCached")
        protected Object doUncached(DynamicObject attrs, String name) {
            return iterAttrAccess.execute(attrs, name);
        }
    }

    private static final class PartialAttrSearchAction extends AttributeAction {
        @Override
        public void init(Context context) {
            context.result = RNull.instance;
        }

        @Override
        public boolean action(String name, Object value, Context ctx) {
            if (name.startsWith((String) ctx.param)) {
                if (ctx.result == RNull.instance) {
                    ctx.result = value;
                } else {
                    // non-unique match
                    ctx.result = RNull.instance;
                    return false;
                }
            }
            return true;
        }
    }
}
