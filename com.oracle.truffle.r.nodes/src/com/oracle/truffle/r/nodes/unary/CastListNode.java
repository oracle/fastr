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
package com.oracle.truffle.r.nodes.unary;

import java.util.Iterator;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributes;
import com.oracle.truffle.r.runtime.data.RAttributes.RAttribute;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDataFrame;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

public abstract class CastListNode extends CastBaseNode {

    @Child private CastListNode castListRecursive;

    public abstract RList executeList(Object o);

    private RList castList(Object operand) {
        if (castListRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castListRecursive = insert(CastListNodeGen.create(false, false, false));
        }
        return castListRecursive.executeList(operand);
    }

    @Specialization
    protected RList doNull(@SuppressWarnings("unused") RNull operand) {
        return RDataFactory.createList();
    }

    @Specialization
    protected RList doDouble(double operand) {
        return RDataFactory.createList(new Object[]{operand});
    }

    @Specialization
    protected RList doInt(int operand) {
        return RDataFactory.createList(new Object[]{operand});
    }

    @Specialization
    protected RList doAbstractVector(RAbstractVector operand) {
        Object[] data = new Object[operand.getLength()];
        for (int i = 0; i < data.length; i++) {
            data[i] = operand.getDataAtAsObject(i);
        }
        RList ret = RDataFactory.createList(data, getPreservedDimensions(operand), getPreservedNames(operand));
        preserveDimensionNames(operand, ret);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization
    protected RList doExpression(RExpression operand) {
        return operand.getList();
    }

    @Specialization
    protected RList doLanguage(RLanguage operand) {
        RList result = RContext.getRRuntimeASTAccess().asList(operand);
        RAttributes operandAttrs = operand.getAttributes();
        if (operandAttrs != null) {
            // result may already have names, so can't call RVector.copyAttributesFrom
            Iterator<RAttribute> iter = operandAttrs.iterator();
            while (iter.hasNext()) {
                RAttribute attr = iter.next();
                if (attr.getName().equals(RRuntime.CLASS_ATTR_KEY)) {
                    result.setClassAttr((RStringVector) attr.getValue(), false);
                } else {
                    result.setAttr(attr.getName(), attr.getValue());
                }
            }
        }
        return result;
    }

    @Specialization
    protected RList doDataFrame(RDataFrame operand) {
        return castList(operand.getVector());
    }

    @Specialization
    @TruffleBoundary
    protected RList doPairList(RPairList pl) {
        return pl.toRList();
    }

    @Specialization
    protected RList doFunction(RFunction func) {
        return RDataFactory.createList(new Object[]{func});
    }

    @Specialization
    protected RList doEnvironment(REnvironment env) {
        return RDataFactory.createList(new Object[]{env});
    }

    @Specialization
    protected RList doS4Object(RS4Object o) {
        return RDataFactory.createList(new Object[]{o});
    }

    @Specialization
    protected RList doRSymbol(RSymbol s) {
        return RDataFactory.createList(new Object[]{s});
    }

    public static CastListNode create() {
        return CastListNodeGen.create(true, true, true);
    }
}
