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
package com.oracle.truffle.r.nodes.builtin.base.infix;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ReplaceVectorNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.helpers.UpdateListField;
import com.oracle.truffle.r.nodes.unary.CastListNode;
import com.oracle.truffle.r.nodes.unary.CastListNodeGen;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RSpecialFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RNode;

@NodeInfo(cost = NodeCost.NONE)
@NodeChild(value = "arguments", type = RNode[].class)
abstract class UpdateFieldSpecial extends RNode {

    @Child private UpdateListField updateListField;

    /**
     * {@link RNull} and lists have special handling when they are RHS of update. Nulls delete the
     * field and lists can cause cycles.
     */
    static boolean isNotRNullRList(Object value) {
        return value != RNull.instance && !(value instanceof RList);
    }

    @Specialization(guards = {"!list.isShared()", "isNotRNullRList(value)"})
    public Object doList(RList list, String field, Object value) {
        if (updateListField == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            updateListField = insert(UpdateListField.create());
        }
        boolean result = updateListField.execute(list, field, value);
        if (!result) {
            throw RSpecialFactory.throwFullCallNeeded(value);
        } else {
            return list;
        }
    }

    @SuppressWarnings("unused")
    @Fallback
    public RList doFallback(Object container, Object field, Object value) {
        throw RSpecialFactory.throwFullCallNeeded(value);
    }
}

@RBuiltin(name = "$<-", kind = PRIMITIVE, parameterNames = {"", "", "value"}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class UpdateField extends RBuiltinNode.Arg3 {

    @Child private ReplaceVectorNode update = ReplaceVectorNode.create(ElementAccessMode.FIELD_SUBSCRIPT, true);
    @Child private CastListNode castList;

    private final ConditionProfile coerceList = ConditionProfile.createBinaryProfile();

    static {
        Casts casts = new Casts(UpdateField.class);
        casts.arg(1).defaultError(Message.INVALID_SUBSCRIPT).mustBe(stringValue(), Message.INVALID_SUBSCRIPT_TYPE, Predef.typeName()).asStringVector().findFirst();
    }

    public static RNode createSpecial(ArgumentsSignature signature, RNode[] arguments, @SuppressWarnings("unused") boolean inReplacement) {
        return SpecialsUtils.isCorrectUpdateSignature(signature) && arguments.length == 3 ? UpdateFieldSpecialNodeGen.create(arguments) : null;
    }

    @Specialization
    protected Object update(Object container, String field, Object value) {
        Object list = coerceList.profile(container instanceof RAbstractListVector) ? container : coerceList(container);
        return update.apply(list, new Object[]{field}, value);
    }

    private Object coerceList(Object vector) {
        if (vector instanceof RAbstractVector) {
            if (castList == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castList = insert(CastListNodeGen.create(true, true, false));
            }
            warning(RError.Message.COERCING_LHS_TO_LIST);
            return castList.executeList(vector);
        }
        return vector;
    }
}
