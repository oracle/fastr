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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.data.nodes.attributes.RemoveFixedAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.SetCommentAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.data.nodes.ShareObjectNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorReuse;

@RBuiltin(name = "comment<-", kind = INTERNAL, parameterNames = {"x", "value"}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class UpdateComment extends RBuiltinNode.Arg2 {

    static {
        Casts casts = new Casts(UpdateComment.class);
        casts.arg("x").mustNotBeMissing().mustNotBeNull(RError.Message.SET_ATTRIBUTES_ON_NULL).boxPrimitive();
        casts.arg("value").defaultError(RError.Message.SET_INVALID_ATTR, "comment").mustNotBeMissing().allowNull().mustBe(stringValue()).asStringVector();
    }

    @Specialization(guards = "vectorReuse.supports(container)", limit = "getVectorAccessCacheSize()")
    protected Object updateComment(RAbstractVector container, RAbstractStringVector value,
                    @Cached("create()") SetCommentAttributeNode setCommentAttrNode,
                    @Cached("createNonShared(container)") VectorReuse vectorReuse,
                    @Cached("create()") ShareObjectNode updateRefCountNode) {
        updateRefCountNode.execute(value);
        RAbstractVector res = vectorReuse.getMaterializedResult(container);
        setCommentAttrNode.setAttr(res, value);
        return res;
    }

    @Specialization(replaces = "updateComment")
    protected Object updateCommentGeneric(RAbstractVector container, RAbstractStringVector value,
                    @Cached("create()") SetCommentAttributeNode setCommentAttrNode,
                    @Cached("createNonSharedGeneric()") VectorReuse vectorReuse,
                    @Cached("create()") ShareObjectNode updateRefCountNode) {
        return updateComment(container, value, setCommentAttrNode, vectorReuse, updateRefCountNode);
    }

    @Specialization(guards = "!isRAbstractVector(container)")
    protected Object updateCommentNonVector(RAttributable container, RAbstractStringVector value,
                    @Cached("createClassProfile()") ValueProfile classProfile,
                    @Cached("create()") SetCommentAttributeNode setCommentAttrNode) {
        Object result = classProfile.profile(container);
        if (RSharingAttributeStorage.isShareable(result)) {
            result = ((RSharingAttributeStorage) result).copy();
        }
        setCommentAttrNode.setAttr((RAttributable) result, value);
        return result;
    }

    @Specialization(guards = "vectorReuse.supports(container)", limit = "getVectorAccessCacheSize()")
    protected Object updateCommentNull(RAbstractVector container, @SuppressWarnings("unused") RNull value,
                    @Cached("createNonShared(container)") VectorReuse vectorReuse,
                    @Cached("createComment()") RemoveFixedAttributeNode removeCommentNode) {
        RAbstractVector res = vectorReuse.getResult(container);
        removeCommentNode.execute(res);
        return res;
    }

    @Specialization(replaces = "updateCommentNull")
    protected Object updateCommentNullGeneric(RAbstractVector container, @SuppressWarnings("unused") RNull value,
                    @Cached("createNonSharedGeneric()") VectorReuse vectorReuse,
                    @Cached("createComment()") RemoveFixedAttributeNode removeCommentNode) {
        return updateCommentNull(container, value, vectorReuse, removeCommentNode);
    }

    @Specialization(guards = "!isRAbstractVector(container)")
    protected Object updateCommentNullNonVector(RAttributable container, @SuppressWarnings("unused") RNull value,
                    @Cached("createClassProfile()") ValueProfile classProfile,
                    @Cached("createComment()") RemoveFixedAttributeNode removeCommentNode) {
        Object result = classProfile.profile(container);
        if (RSharingAttributeStorage.isShareable(result)) {
            result = ((RSharingAttributeStorage) result).copy();
        }
        removeCommentNode.execute((RAttributable) result);
        return result;
    }

    @Fallback
    protected Object updateCommentFallback(Object vector, Object value) {
        // cast pipeline should ensure this:
        assert value == RNull.instance || value instanceof RAbstractStringVector;
        return vector;
    }
}
