/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@NodeChildren({@NodeChild(value = "object", type = RNode.class), @NodeChild(value = "value", type = RNode.class)})
@NodeField(name = "field", type = String.class)
public abstract class UpdateFieldNode extends RNode {

    public abstract String getField();

    @CompilationFinal private boolean inexactMatch = false;

    @Child private CastListNode castList;

    @Specialization(order = 1)
    public Object updateField(RList object, Object value) {
        int index = object.getElementIndexByName(getField());
        if (index == -1) {
            if (!inexactMatch) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                inexactMatch = true;
            }
            index = object.getElementIndexByNameInexact(getField());
        }

        int newLength = object.getLength() + (index == -1 ? 1 : 0);
        if (index == -1) {
            index = newLength - 1;
        }

        Object[] resultData = new Object[newLength];
        System.arraycopy(object.getDataCopy(), 0, resultData, 0, object.getLength());

        String[] resultNames = new String[newLength];
        boolean namesComplete = true;
        if (object.getNames() == RNull.instance) {
            Arrays.fill(resultNames, "");
        } else {
            RStringVector names = (RStringVector) object.getNames();
            System.arraycopy(names.getDataCopy(), 0, resultNames, 0, names.getLength());
            namesComplete = names.isComplete();
        }

        resultData[index] = value;
        resultNames[index] = getField();

        RList result = RDataFactory.createList(resultData);
        result.copyAttributesFrom(object);
        result.setNames(RDataFactory.createStringVector(resultNames, namesComplete));

        return result;
    }

    @Specialization(order = 1000)
    public Object updateField(VirtualFrame frame, RAbstractVector object, Object value) {
        if (castList == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castList = adoptChild(CastListNodeFactory.create(null, true, true));
        }
        RError.warning(getEncapsulatingSourceSection(), RError.COERCING_LHS_TO_LIST);
        return updateField(castList.executeList(frame, object), value);
    }

}
