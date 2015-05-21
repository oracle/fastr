/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.*;
import com.oracle.truffle.r.runtime.gnur.*;

@NodeChildren({@NodeChild(value = "value", type = RNode.class), @NodeChild(value = "object", type = RNode.class), @NodeChild(value = "field", type = RNode.class)})
public abstract class UpdateFieldNode extends UpdateNode implements RSyntaxNode {

    public abstract Object executeUpdate(VirtualFrame frame, Object o, Object value, String field);

    public abstract RNode getObject();

    public abstract RNode getField();

    @Child private UpdateFieldNode accessRecursive;
    @Child private UseMethodInternalNode dcn;
    public final boolean forObjects;

    protected final ConditionProfile hasNamesProfile = ConditionProfile.createBinaryProfile();
    protected final BranchProfile inexactMatch = BranchProfile.create();
    protected final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    public UpdateFieldNode(boolean forObjects) {
        this.forObjects = forObjects;
    }

    private Object accessRecursive(VirtualFrame frame, RAbstractContainer container, Object value, String field) {
        if (accessRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            accessRecursive = insert(UpdateFieldNodeGen.create(false, null, null, null));
        }
        return accessRecursive.executeUpdate(frame, container, value, field);
    }

    @TruffleBoundary
    public static int getElementIndexByName(RStringVector names, String name) {
        for (int i = 0; i < names.getLength(); i++) {
            if (names.getDataAt(i).equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public abstract RNode getValue();

    private final BranchProfile noRemoval = BranchProfile.create();
    private final ConditionProfile nullValueProfile = ConditionProfile.createBinaryProfile();

    @Child private CastListNode castList;

    @Specialization(guards = "isObject(container)")
    protected Object accessField(VirtualFrame frame, RAbstractContainer container, Object value, String field) {
        if (dcn == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            dcn = insert(new UseMethodInternalNode("$<-", ArgumentsSignature.get("", "", "")));
        }
        try {
            return dcn.execute(frame, container.getClassHierarchy(), new Object[]{container, field, value});
        } catch (S3FunctionLookupNode.NoGenericMethodException e) {
            return accessRecursive(frame, container, value, field);
        }
    }

    @Specialization(guards = "!isNull(value)")
    protected Object updateField(RList object, Object value, String field) {
        RStringVector names = object.getNames(attrProfiles);
        int index = -1;
        if (hasNamesProfile.profile(names != null)) {
            index = getElementIndexByName(names, field);
            if (index == -1) {
                inexactMatch.enter();
                index = object.getElementIndexByNameInexact(attrProfiles, field);
            }
        }
        int newLength = object.getLength() + (index == -1 ? 1 : 0);
        if (index == -1) {
            index = newLength - 1;
        }

        Object[] resultData = new Object[newLength];
        System.arraycopy(object.getDataWithoutCopying(), 0, resultData, 0, object.getLength());

        String[] resultNames = new String[newLength];
        boolean namesComplete = true;
        if (object.getNames(attrProfiles) == null) {
            Arrays.fill(resultNames, "");
        } else {
            System.arraycopy(names.getDataWithoutCopying(), 0, resultNames, 0, names.getLength());
            namesComplete = names.isComplete();
        }

        resultData[index] = value;
        resultNames[index] = field;

        RList result = RDataFactory.createList(resultData);
        result.copyAttributesFrom(attrProfiles, object);
        result.setNames(RDataFactory.createStringVector(resultNames, namesComplete));

        return result;
    }

    @Specialization(guards = "isNull(value)")
    protected Object updateFieldNullValue(RList object, @SuppressWarnings("unused") Object value, String field) {
        RStringVector names = object.getNames(attrProfiles);
        int index = -1;
        if (hasNamesProfile.profile(names != null)) {
            index = getElementIndexByName(names, field);
            if (index == -1) {
                inexactMatch.enter();
                index = object.getElementIndexByNameInexact(attrProfiles, field);
            }
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
        if (object.getNames(attrProfiles) == null) {
            Arrays.fill(resultNames, "");
        } else {
            ind = 0;
            for (int i = 0; i < names.getLength(); i++) {
                if (i != index) {
                    resultNames[ind++] = names.getDataAt(i);
                }
            }
            namesComplete = names.isComplete();
        }

        RList result = RDataFactory.createList(resultData);
        result.copyAttributesFrom(attrProfiles, object);
        result.setNames(RDataFactory.createStringVector(resultNames, namesComplete));

        return result;
    }

    @Specialization
    protected Object updateField(REnvironment env, Object value, String field) {
        // reference semantics for environments
        try {
            env.put(field, value);
        } catch (PutException ex) {
            throw RError.error(getEncapsulatingSourceSection(), ex);
        }
        return env;
    }

    @Specialization
    protected Object updateField(RAbstractVector object, Object value, String field) {
        if (castList == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castList = insert(CastListNodeGen.create(null, true, true, false));
        }
        RError.warning(getEncapsulatingSourceSection(), RError.Message.COERCING_LHS_TO_LIST);
        if (nullValueProfile.profile(value == RNull.instance)) {
            return updateFieldNullValue(castList.executeList(object), value, field);
        } else {
            return updateField(castList.executeList(object), value, field);
        }
    }

    protected static boolean isNull(Object value) {
        return value == RNull.instance;
    }

    @Override
    public void deparse(RDeparse.State state) {
        RSyntaxNode.cast(getObject()).deparse(state);
        state.append('$');
        RSyntaxNode.cast(getField()).deparse(state);
        state.append(" <- ");
        RSyntaxNode.cast(getValue()).deparse(state);
    }

    @Override
    public void serialize(RSerialize.State state) {
        state.setAsBuiltin("<-");
        state.openPairList(SEXPTYPE.LISTSXP);
        // field access
        state.openPairList(SEXPTYPE.LANGSXP);
        state.setAsBuiltin("$");
        state.openPairList(SEXPTYPE.LISTSXP);
        state.serializeNodeSetCar(getObject());
        state.openPairList(SEXPTYPE.LISTSXP);
        state.serializeNodeSetCar(getField());
        state.linkPairList(2);
        state.setCdr(state.closePairList());
        // end field access
        state.setCar(state.closePairList());
        state.openPairList(SEXPTYPE.LISTSXP);
        state.serializeNodeSetCar(getValue());
        state.linkPairList(2);
        state.setCdr(state.closePairList());
    }

    protected boolean isObject(RAbstractContainer container) {
        return container.isObject(attrProfiles) && forObjects;
    }
}
