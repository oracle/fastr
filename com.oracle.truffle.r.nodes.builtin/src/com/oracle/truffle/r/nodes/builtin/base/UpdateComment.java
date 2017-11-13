/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.RemoveFixedAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SetFixedAttributeNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@RBuiltin(name = "comment<-", kind = INTERNAL, parameterNames = {"x", "value"}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class UpdateComment extends RBuiltinNode.Arg2 {

    static {
        Casts casts = new Casts(UpdateComment.class);
        casts.arg("x").mustNotBeMissing();
        casts.arg("value").mustNotBeMissing().mustBe(Predef.stringValue().or(Predef.nullValue()));
    }

    protected SetFixedAttributeNode createGetCommentAttrNode() {
        return SetFixedAttributeNode.create(RRuntime.COMMENT_ATTR_KEY);
    }

    protected RemoveFixedAttributeNode createRemoveCommentAttrNode() {
        return RemoveFixedAttributeNode.create(RRuntime.COMMENT_ATTR_KEY);
    }

    @Child private SetFixedAttributeNode setCommentAttrNode;

    @Specialization
    protected Object dim(RAbstractContainer container, RAbstractStringVector value) {
        if (setCommentAttrNode == null) {
            setCommentAttrNode = insert(createGetCommentAttrNode());
        }
        setCommentAttrNode.execute(container, value);
        return container;
    }

    @Specialization
    protected Object dim(RAbstractContainer container, @SuppressWarnings("unused") RNull value,
                    @Cached("createRemoveCommentAttrNode()") RemoveFixedAttributeNode removeCommentNode) {
        removeCommentNode.execute(container);
        return container;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!isRAbstractContainer(vector)")
    protected RNull dim(Object vector, Object value) {
        return RNull.instance;
    }
}
