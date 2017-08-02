/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.interop.ForeignArray2R;
import com.oracle.truffle.r.runtime.interop.ForeignArray2RNodeGen;

@ImportStatic(RRuntime.class)
public abstract class CastExpressionNode extends CastBaseNode {

    @Child private CastExpressionNode recursiveCastExpression;

    public abstract Object executeExpression(Object o);

    protected CastExpressionNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        this(preserveNames, preserveDimensions, preserveAttributes, false);
    }

    protected CastExpressionNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI) {
        super(preserveNames, preserveDimensions, preserveAttributes, forRFFI);
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
        throw error(RError.Message.CANNOT_COERCE, value.isBuiltin() ? "builtin" : "closure", "expression");
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

    @Specialization(guards = "isForeignObject(obj)")
    protected RExpression doForeignObject(TruffleObject obj,
                    @Cached("createForeignArray2RNode()") ForeignArray2R foreignArray2R) {
        Object o = foreignArray2R.execute(obj, true);
        if (!RRuntime.isForeignObject(o)) {
            return (RExpression) castExpressionRecursive(o);
        }
        throw error(RError.Message.CANNOT_COERCE_EXTERNAL_OBJECT_TO_VECTOR, "vector");
    }

    private static RExpression create(Object obj) {
        return RDataFactory.createExpression(new Object[]{obj});
    }

    /**
     * RFFI coercion to list unlike others does not preserve names it seems.
     */
    public static CastExpressionNode createForRFFI() {
        return CastExpressionNodeGen.create(false, false, false, true);
    }

    public static CastExpressionNode createNonPreserving() {
        return CastExpressionNodeGen.create(false, false, false);
    }

    protected ForeignArray2R createForeignArray2RNode() {
        return ForeignArray2RNodeGen.create();
    }

    private Object castExpressionRecursive(Object o) {
        if (recursiveCastExpression == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveCastExpression = insert(CastExpressionNodeGen.create(preserveNames(), preserveDimensions(), preserveAttributes()));
        }
        return recursiveCastExpression.executeExpression(o);
    }
}
