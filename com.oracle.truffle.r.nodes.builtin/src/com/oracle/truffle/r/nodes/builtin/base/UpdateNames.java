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
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.nodes.unary.GetNonSharedNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "names<-", kind = PRIMITIVE, parameterNames = {"x", "value"}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class UpdateNames extends RBuiltinNode.Arg2 {

    @Child private CastStringNode castStringNode;

    static {
        Casts casts = new Casts(UpdateNames.class);
        casts.arg("x").mustNotBeNull(RError.Message.SET_ATTRIBUTES_ON_NULL, "NULL");
    }

    private Object castString(Object o) {
        if (castStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castStringNode = insert(CastStringNodeGen.create(false, false, false));
        }
        return castStringNode.executeString(o);
    }

    public abstract Object executeStringVector(RAbstractContainer container, Object o);

    @Specialization
    @TruffleBoundary
    protected RAbstractContainer updateNames(RAbstractContainer container, Object names,
                    @Cached("create()") GetNonSharedNode nonShared) {
        Object newNames = castString(names);
        RAbstractContainer result = ((RAbstractContainer) nonShared.execute(container)).materialize();
        if (newNames == RNull.instance) {
            result.setNames(null);
            return result;
        }

        RStringVector stringVector;
        if (newNames instanceof String) {
            stringVector = RDataFactory.createStringVector((String) newNames);
        } else {
            stringVector = (RStringVector) ((RAbstractVector) newNames).materialize().copyDropAttributes();
        }
        if (stringVector.getLength() < result.getLength()) {
            stringVector = (RStringVector) stringVector.copyResized(result.getLength(), true);
        } else if (stringVector.getLength() > result.getLength()) {
            throw error(Message.NAMES_LONGER, stringVector.getLength(), result.getLength());
        } else if (stringVector == names) {
            stringVector = (RStringVector) stringVector.copy();
        }
        if (stringVector.isTemporary()) {
            stringVector.incRefCount();
        }
        result.setNames(stringVector);
        return result;
    }
}
