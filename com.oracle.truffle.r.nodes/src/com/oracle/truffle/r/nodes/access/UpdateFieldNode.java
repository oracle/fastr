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
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.*;

@NodeChildren({@NodeChild(value = "object", type = RNode.class), @NodeChild(value = "value", type = RNode.class)})
@NodeField(name = "field", type = String.class)
public abstract class UpdateFieldNode extends RNode {

    public abstract RNode getObject();

    public abstract RNode getValue();

    public abstract String getField();

    private final BranchProfile inexactMatch = BranchProfile.create();
    private final BranchProfile noRemoval = BranchProfile.create();
    private final ConditionProfile nullValueProfile = ConditionProfile.createBinaryProfile();

    @Child private CastListNode castList;

    @Specialization(guards = "!isNullValue")
    protected Object updateField(RList object, Object value) {
        String field = getField();
        int index = object.getElementIndexByName(field);
        if (index == -1) {
            inexactMatch.enter();
            index = object.getElementIndexByNameInexact(field);
        }

        int newLength = object.getLength() + (index == -1 ? 1 : 0);
        if (index == -1) {
            index = newLength - 1;
        }

        Object[] resultData = new Object[newLength];
        System.arraycopy(object.getDataWithoutCopying(), 0, resultData, 0, object.getLength());

        String[] resultNames = new String[newLength];
        boolean namesComplete = true;
        if (object.getNames() == RNull.instance) {
            Arrays.fill(resultNames, "");
        } else {
            RStringVector names = (RStringVector) object.getNames();
            System.arraycopy(names.getDataWithoutCopying(), 0, resultNames, 0, names.getLength());
            namesComplete = names.isComplete();
        }

        resultData[index] = value;
        resultNames[index] = field;

        RList result = RDataFactory.createList(resultData);
        result.copyAttributesFrom(object);
        result.setNames(RDataFactory.createStringVector(resultNames, namesComplete));

        return result;
    }

    @Specialization(guards = "isNullValue")
    protected Object updateFieldNullValue(RList object, @SuppressWarnings("unused") Object value) {
        String field = getField();
        int index = object.getElementIndexByName(field);
        if (index == -1) {
            inexactMatch.enter();
            index = object.getElementIndexByNameInexact(field);
        }

        if (index == -1) {
            noRemoval.enter();
            return object;
        }

        int newLength = object.getLength() - 1;

        Object[] resultData = new Object[newLength];
        int ind = 0;
        for (int i = 0; i < object.getLength(); i++) {
            if (i != index) {
                resultData[ind++] = object.getDataAt(i);
            }
        }

        String[] resultNames = new String[newLength];
        boolean namesComplete = true;
        if (object.getNames() == RNull.instance) {
            Arrays.fill(resultNames, "");
        } else {
            RStringVector names = (RStringVector) object.getNames();
            ind = 0;
            for (int i = 0; i < names.getLength(); i++) {
                if (i != index) {
                    resultNames[ind++] = names.getDataAt(i);
                }
            }
            namesComplete = names.isComplete();
        }

        RList result = RDataFactory.createList(resultData);
        result.copyAttributesFrom(object);
        result.setNames(RDataFactory.createStringVector(resultNames, namesComplete));

        return result;
    }

    @Specialization
    protected Object updateField(REnvironment env, Object value) {
        // reference semantics for environments
        try {
            env.put(getField(), value);
        } catch (PutException ex) {
            throw RError.error(getEncapsulatingSourceSection(), ex);
        }
        return env;
    }

    @Specialization
    protected Object updateField(VirtualFrame frame, RAbstractVector object, Object value) {
        if (castList == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castList = insert(CastListNodeGen.create(null, true, true, false));
        }
        RError.warning(getEncapsulatingSourceSection(), RError.Message.COERCING_LHS_TO_LIST);
        if (nullValueProfile.profile(value == RNull.instance)) {
            return updateFieldNullValue(castList.executeList(frame, object), value);
        } else {
            return updateField(castList.executeList(frame, object), value);
        }
    }

    protected boolean isNullValue(@SuppressWarnings("unused") RAbstractVector object, Object value) {
        return value == RNull.instance;
    }

    @Override
    public boolean isSyntax() {
        return true;
    }

    @Override
    public void deparse(State state) {
        // This is rather strange as it effectively includes an assignment
        getObject().deparse(state);
        state.append('$');
        state.append(getField());
        state.append(" <- ");
        getValue().deparse(state);
    }

}
