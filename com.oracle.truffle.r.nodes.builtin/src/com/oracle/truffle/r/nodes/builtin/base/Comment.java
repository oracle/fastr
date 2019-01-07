/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetCommentAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;

@RBuiltin(name = "comment", kind = INTERNAL, parameterNames = {"x"}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class Comment extends RBuiltinNode.Arg1 {

    static {
        Casts casts = new Casts(Comment.class);
        casts.arg("x").mustNotBeMissing();
    }

    @Specialization
    protected Object comment(RSharingAttributeStorage x,
                    @Cached("createBinaryProfile()") ConditionProfile hasCommentProfile,
                    @Cached("create()") GetCommentAttributeNode getComment) {
        Object commentAttr = getComment.execute(x);
        if (hasCommentProfile.profile(commentAttr != null)) {
            return commentAttr;
        } else {
            return RNull.instance;
        }
    }

    @Fallback
    protected RNull comment(@SuppressWarnings("unused") Object vector) {
        RSharingAttributeStorage.verify(vector);
        return RNull.instance;
    }
}
