/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;

@RBuiltin(name = "length<-", kind = PRIMITIVE, parameterNames = {"x", "value"}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class UpdateLength extends RBuiltinNode {

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toInteger(1, true, false, false);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isLengthOne(lengthVector)")
    protected RNull updateLength(RNull value, RAbstractIntVector lengthVector) {
        return RNull.instance;
    }

    @Specialization(guards = "isLengthOne(lengthVector)")
    protected RAbstractContainer updateLength(RAbstractContainer container, RAbstractIntVector lengthVector) {
        return container.resize(lengthVector.getDataAt(0));
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object updateLengthError(Object vector, Object lengthVector) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this, RError.Message.INVALID_UNNAMED_VALUE);
    }

    protected static boolean isLengthOne(RAbstractIntVector length) {
        return length.getLength() == 1;
    }
}
