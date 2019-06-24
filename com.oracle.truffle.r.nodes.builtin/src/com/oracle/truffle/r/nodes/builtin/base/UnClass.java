/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.attributes.RemoveFixedAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetClassAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.UnClassNodeGen.RemoveClassAttrNodeGen;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorReuse;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@RBuiltin(name = "unclass", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
public abstract class UnClass extends RBuiltinNode.Arg1 {
    static {
        Casts casts = new Casts(UnClass.class);
        casts.arg("x").mustNotBeMissing().asAttributable(true, true, true);
    }

    @Child private RemoveClassAttrNode removeClassAttrNode;

    @Specialization
    protected RNull unClass(RNull rnull) {
        return rnull;
    }

    @Specialization
    protected Object unClass(RAttributable arg,
                    @Cached("create()") GetClassAttributeNode getClassNode) {
        if (getClassNode.isObject(arg)) {
            if (removeClassAttrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                removeClassAttrNode = insert(RemoveClassAttrNodeGen.create());
            }
            return removeClassAttrNode.execute(arg);
        }
        return arg;
    }

    protected abstract static class RemoveClassAttrNode extends RBaseNode {
        public abstract Object execute(RAttributable attributable);

        @Specialization(guards = "reuse.supports(x)")
        protected Object doVector(RAbstractVector x,
                        @Cached("createClass()") RemoveFixedAttributeNode removeClassNode,
                        @Cached("createTemporary(x)") VectorReuse reuse) {
            RAbstractVector result = reuse.getMaterializedResult(x);
            removeClassNode.execute(result);
            return result;
        }

        @Specialization(replaces = "doVector")
        protected Object doVectorGeneric(RAbstractVector x,
                        @Cached("createClass()") RemoveFixedAttributeNode removeClassNode,
                        @Cached("createTemporaryGeneric()") VectorReuse reuse) {
            return doVector(x, removeClassNode, reuse);
        }

        @Specialization(guards = "notAbstractVector(x)")
        protected Object unClass(RAttributable x,
                        @Cached BranchProfile shareableProfile,
                        @Cached("createClass()") RemoveFixedAttributeNode removeClassNode) {
            RAttributable result = x;
            if (RSharingAttributeStorage.isShareable(x)) {
                shareableProfile.enter();
                RSharingAttributeStorage shareable = (RSharingAttributeStorage) x;
                if (!shareable.isTemporary()) {
                    result = shareable.copy();
                }
            }
            removeClassNode.execute(result);
            return result;
        }

        protected static boolean notAbstractVector(Object arg) {
            return !(arg instanceof RAbstractVector);
        }
    }
}
