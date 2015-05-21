/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public abstract class CastListNode extends CastNode {

    @Child private CastListNode castListRecursive;

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    public abstract RList executeList(Object o);

    private RList castList(Object operand) {
        if (castListRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castListRecursive = insert(CastListNodeGen.create(null, false, false, false));
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
        result.copyAttributesFrom(attrProfiles, operand);
        return result;
    }

    @Specialization
    protected RList doDataFrame(RDataFrame operand) {
        return castList(operand.getVector());
    }

    @Specialization
    @TruffleBoundary
    protected RList doPairList(RPairList pl) {
        // One list type into another, not performance critical!
        int length = pl.getLength();
        if (length == 0) {
            return RDataFactory.createList();
        }
        Object[] data = new Object[length];
        String[] names = new String[length];
        boolean complete = RDataFactory.COMPLETE_VECTOR;
        int i = 0;
        RPairList tpl = pl;
        while (tpl != null) {
            Object tag = tpl.getTag();
            names[i] = pl.isNullTag() ? RRuntime.NAMES_ATTR_EMPTY_VALUE : (String) tag;
            if (tag == RRuntime.STRING_NA) {
                complete = RDataFactory.INCOMPLETE_VECTOR;
            }
            data[i] = tpl.car();
            Object cdr = tpl.cdr();
            if (RPairList.isNull(cdr)) {
                break;
            } else {
                tpl = (RPairList) cdr;
            }
            i++;
        }
        return RDataFactory.createList(data, RDataFactory.createStringVector(names, complete));
    }

    @Specialization
    protected RList doFunction(RFunction func) {
        return RDataFactory.createList(new Object[]{func});
    }

    public static CastListNode create() {
        return CastListNodeGen.create(null, true, true, true);
    }
}
