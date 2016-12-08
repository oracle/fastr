/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

public abstract class CastExpressionNode extends CastBaseNode {

    public abstract Object executeExpression(Object o);

    protected CastExpressionNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        super(preserveNames, preserveDimensions, preserveAttributes);
    }

    @Override
    protected final RType getTargetType() {
        return RType.Expression;
    }

    @Specialization
    protected RExpression doNull(@SuppressWarnings("unused") RNull value) {
        return create(RNull.instance);
    }

    @Specialization
    protected RExpression doDouble(double value) {
        return create(value);
    }

    @Specialization
    protected RExpression doInt(int value) {
        return create(value);
    }

    @Specialization
    protected RExpression doLogical(byte value) {
        return create(value);
    }

    @Specialization
    protected RExpression doSymbol(RSymbol value) {
        return create(value);
    }

    @Specialization
    protected RExpression doFunction(RFunction value) {
        throw RError.error(RError.SHOW_CALLER, RError.Message.CANNOT_COERCE, value.isBuiltin() ? "builtin" : "closure", "expression");
    }

    @Specialization
    protected RExpression doExpression(RExpression value) {
        return value;
    }

    @Specialization
    protected RExpression doAbstractContainer(RAbstractContainer obj,
                    @Cached("create()") GetNamesAttributeNode getNamesNode) {
        int len = obj.getLength();
        Object[] data = new Object[len];
        for (int i = 0; i < len; i++) {
            data[i] = obj.getDataAtAsObject(i);
        }
        if (obj instanceof RList) {
            RList list = (RList) obj;
            // TODO other attributes
            return RDataFactory.createExpression(data, getNamesNode.getNames(list));
        } else {
            return RDataFactory.createExpression(data);
        }
    }

    private static RExpression create(Object obj) {
        return RDataFactory.createExpression(new Object[]{obj});
    }

    public static CastExpressionNode createNonPreserving() {
        return CastExpressionNodeGen.create(false, false, false);
    }
}
