/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public abstract class CastListNode extends CastNode {

    @Child CastListNode castListRecursive;

    public abstract RList executeList(VirtualFrame frame, Object o);

    private RList castList(VirtualFrame frame, Object operand) {
        if (castListRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castListRecursive = insert(CastListNodeFactory.create(null, false, false, false));
        }
        return castListRecursive.executeList(frame, operand);
    }

    @Specialization
    @SuppressWarnings("unused")
    protected RList doNull(RNull operand) {
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
        for (int i = 0; i < data.length; ++i) {
            data[i] = operand.getDataAtAsObject(i);
        }
        RList ret;
        if (preserveDimensions() && preserveNames()) {
            ret = RDataFactory.createList(data, operand.getDimensions(), operand.getNames());
        } else if (preserveDimensions()) {
            ret = RDataFactory.createList(data, operand.getDimensions());
        } else if (preserveNames()) {
            ret = RDataFactory.createList(data, operand.getNames());
        } else {
            ret = RDataFactory.createList(data);
        }
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
    protected RList doLanguage(VirtualFrame frame, RLanguage operand) {
        return castList(frame, operand.getRep());
    }

}
