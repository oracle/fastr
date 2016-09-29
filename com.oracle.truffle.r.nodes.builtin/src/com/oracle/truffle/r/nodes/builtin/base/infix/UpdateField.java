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
package com.oracle.truffle.r.nodes.builtin.base.infix;

import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ReplaceVectorNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastListNode;
import com.oracle.truffle.r.nodes.unary.CastListNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "$<-", kind = PRIMITIVE, parameterNames = {"", "", "value"}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class UpdateField extends RBuiltinNode {

    @Child private ReplaceVectorNode extract = ReplaceVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
    @Child private CastListNode castList;

    private final ConditionProfile coerceList = ConditionProfile.createBinaryProfile();

    @Specialization
    protected Object update(VirtualFrame frame, Object container, String field, Object value) {
        Object updatedObject = container;
        if (!coerceList.profile(container instanceof RAbstractListVector)) {
            updatedObject = coerceList(container, updatedObject);
        }
        return extract.apply(frame, updatedObject, new Object[]{field}, value);
    }

    private Object coerceList(Object object, Object vector) {
        Object updatedVector = vector;
        if (object instanceof RAbstractVector) {
            if (castList == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castList = insert(CastListNodeGen.create(true, true, false));
            }
            RError.warning(this, RError.Message.COERCING_LHS_TO_LIST);
            updatedVector = castList.executeList(vector);
        }
        return updatedVector;
    }

    @Fallback
    @TruffleBoundary
    protected Object fallbackError(@SuppressWarnings("unused") Object container, Object field, @SuppressWarnings("unused") Object value) {
        // TODO: the error message is not quite correct for all types;
        // for example: x<-list(a=7); `$<-`(x, c("a"), 42);)
        throw RError.error(this, RError.Message.INVALID_SUBSCRIPT_TYPE, RRuntime.classToString(field.getClass()));
    }
}
