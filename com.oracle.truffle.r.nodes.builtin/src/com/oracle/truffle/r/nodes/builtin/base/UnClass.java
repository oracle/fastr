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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.attributes.RemoveFixedAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetClassAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "unclass", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
public abstract class UnClass extends RBuiltinNode.Arg1 {
    private final BranchProfile objectProfile = BranchProfile.create();
    private final BranchProfile shareableProfile = BranchProfile.create();

    static {
        Casts casts = new Casts(UnClass.class);
        casts.arg("x").mustNotBeMissing().asAttributable(true, true, true);
    }

    @Specialization
    protected RNull unClass(RNull rnull) {
        return rnull;
    }

    @TruffleBoundary
    private static Object unClassVector(RAbstractVector arg) {
        RVector<?> resultVector = arg.materialize();
        if (!resultVector.isTemporary()) {
            resultVector = resultVector.copy();
            resultVector.incRefCount();
        }
        return RVector.setVectorClassAttr(resultVector, null);
    }

    // TODO: this specialization could go away if connections were simple vectors (we wouldn't need
    // special method for setting class attributes then)
    @Specialization
    protected Object unClass(RAbstractVector arg,
                    @Cached("create()") GetClassAttributeNode getClassNode) {
        if (getClassNode.isObject(arg)) {
            objectProfile.enter();
            return unClassVector(arg);
        }
        return arg;
    }

    @Specialization(guards = "notAbstractVector(arg)")
    protected Object unClass(RAttributable arg,
                    @Cached("createClass()") RemoveFixedAttributeNode removeClassNode,
                    @Cached("create()") GetClassAttributeNode getClassNode) {
        if (getClassNode.getClassAttr(arg) != null) {
            objectProfile.enter();
            if (arg instanceof RShareable) {
                shareableProfile.enter();
                RShareable shareable = (RShareable) arg;
                if (!shareable.isTemporary()) {
                    shareable = shareable.copy();
                    shareable.incRefCount();
                }
            }
            removeClassNode.execute(arg);
        }
        return arg;
    }

    protected boolean notAbstractVector(Object arg) {
        return !(arg instanceof RAbstractVector);
    }
}
